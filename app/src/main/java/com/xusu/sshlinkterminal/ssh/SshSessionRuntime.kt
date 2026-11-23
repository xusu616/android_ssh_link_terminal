package com.xusu.sshlinkterminal.ssh

import android.content.Context
import android.os.SystemClock
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
import com.xusu.sshlinkterminal.LinkTerminalApp
import com.xusu.sshlinkterminal.crypto.SshKeyManager
import com.xusu.sshlinkterminal.crypto.VaultCrypto
import com.xusu.sshlinkterminal.data.AppRepository
import com.xusu.sshlinkterminal.data.AuthType
import com.xusu.sshlinkterminal.data.DetectedAuthLink
import com.xusu.sshlinkterminal.data.HostProfile
import com.xusu.sshlinkterminal.data.InteractivePromptRequest
import com.xusu.sshlinkterminal.data.SessionState
import com.xusu.sshlinkterminal.service.TerminalForegroundService
import com.xusu.sshlinkterminal.terminal.AnsiEmulator
import com.xusu.sshlinkterminal.terminal.LinkDetector
import com.xusu.sshlinkterminal.terminal.TerminalRenderLine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private class LocalForwardSetupException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class SessionUiSnapshot(
    val sessionId: String = "",
    val hostId: String,
    val sessionIndex: Int = 1,
    val hostName: String,
    val userAtHost: String,
    val state: SessionState = SessionState.IDLE,
    val statusDetail: String = "未连接",
    val lastRttMs: Long? = null,
    val connectedSinceMillis: Long? = null,
    val reconnectAttempts: Int = 0,
    val terminalLines: List<TerminalRenderLine> = emptyList(),
    val terminalRevision: Long = 0L,
    val detectedAuthLink: DetectedAuthLink? = null,
    val pendingInteractivePrompt: InteractivePromptRequest? = null,
    val windowTitle: String = "",
    val ptyCols: Int = 80,
    val ptyRows: Int = 28,
    val hasCustomPtySize: Boolean = false
)

class SshSessionController(
    private val context: Context,
    private val repository: AppRepository,
    private val onAutoOpenAuthUrl: (String) -> Unit,
    initialHost: HostProfile,
    val sessionId: String = initialHost.id,
    val sessionIndex: Int = 1
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val uiSyncDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val connectMutex = Mutex()
    private val snapshotApplyLock = Any()
    private val connectionGen = AtomicLong(0L)
    private val emulator = AnsiEmulator(initialCols = 80, initialRows = 28, onTerminalResponse = ::sendText)
    private var appliedMutationSeq = -1L

    @Volatile
    var hostProfile: HostProfile = initialHost
        private set

    private val _snapshot = MutableStateFlow(
        SessionUiSnapshot(
            sessionId = sessionId,
            hostId = initialHost.id,
            sessionIndex = sessionIndex,
            hostName = initialHost.name,
            userAtHost = "${initialHost.username}@${initialHost.host}:${initialHost.port}"
        )
    )
    val snapshot: StateFlow<SessionUiSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var jschSession: Session? = null

    @Volatile
    private var shellChannel: ChannelShell? = null

    @Volatile
    private var shellOut: OutputStream? = null

    @Volatile
    private var connectionJob: Job? = null

    @Volatile
    private var keepAliveJob: Job? = null

    @Volatile
    private var userInitiatedStop = false

    @Volatile
    internal var automaticReconnectBlocked = false
        private set

    @Volatile
    private var lastAutoOpenedUrl: String? = null

    @Volatile
    private var lastCols = 80

    @Volatile
    private var lastRows = 28

    @Volatile
    private var hasCustomPtySize = false

    @Volatile
    private var reviveNoticeShown = false

    private val promptLock = Any()
    private var pendingPromptDeferred: CompletableDeferred<List<String>?>? = null
    private val authRollingBuffer = StringBuilder()

    private fun sessionLabel(host: HostProfile = hostProfile): String =
        if (sessionIndex <= 1) host.name else "${host.name} #$sessionIndex"

    private companion object {
        const val RTT_REPLY_TIMEOUT_NANOS = 5_000_000_000L
    }

    fun updateHostProfile(updated: HostProfile) {
        hostProfile = updated
        _snapshot.update {
            it.copy(
                hostName = updated.name,
                userAtHost = "${updated.username}@${updated.host}:${updated.port}"
            )
        }
    }

    private fun cancelPendingPrompt() {
        synchronized(promptLock) {
            pendingPromptDeferred?.cancel()
            pendingPromptDeferred = null
            _snapshot.update { it.copy(pendingInteractivePrompt = null) }
        }
    }

    private fun isAttemptActive(gen: Long, coroutineActive: Boolean): Boolean =
        !userInitiatedStop && coroutineActive && connectionGen.get() == gen

    fun connect(forceRestart: Boolean = false) {
        val currentState = _snapshot.value.state
        if (!forceRestart && (currentState.isActiveChannel() || currentState == SessionState.RECONNECTING)) {
            return
        }
        TerminalForegroundService.ensureStarted(context)
        val gen = connectionGen.incrementAndGet()
        userInitiatedStop = false
        automaticReconnectBlocked = false
        cancelPendingPrompt()
        val previousConnection = connectionJob
        val previousKeepAlive = keepAliveJob
        previousKeepAlive?.cancel()
        previousConnection?.cancel()
        // Unblock any blocking socket read immediately off Main thread before awaiting old loop teardown.
        closeTransportAsync()
        val newJob = scope.launch {
            connectMutex.withLock {
                previousConnection?.cancelAndJoin()
                if (isAttemptActive(gen, currentCoroutineContext().isActive)) {
                    runConnectionLoop(gen)
                }
            }
        }
        connectionJob = newJob
    }

    fun disconnect() {
        connectionGen.incrementAndGet()
        userInitiatedStop = true
        cancelPendingPrompt()
        keepAliveJob?.cancel()
        connectionJob?.cancel()
        closeTransportAsync()
        _snapshot.update {
            it.copy(
                state = SessionState.DISCONNECTED,
                statusDetail = "已断开",
                connectedSinceMillis = null,
                pendingInteractivePrompt = null
            )
        }
        scope.launch(uiSyncDispatcher) {
            emulator.exitAltScreenIfActive()
            appendSystemLine("\r\n\u001B[33m[已手动断开连接]\u001B[0m\r\n")
        }
    }

    fun destroy() {
        connectionGen.incrementAndGet()
        userInitiatedStop = true
        cancelPendingPrompt()
        keepAliveJob?.cancel()
        connectionJob?.cancel()
        closeTransportAsync()
        scope.cancel()
    }

    fun onNetworkAvailable() {
        val state = _snapshot.value.state
        if (!userInitiatedStop && !automaticReconnectBlocked && hostProfile.autoReconnect &&
            (state == SessionState.RECONNECTING || state == SessionState.ERROR || state == SessionState.DISCONNECTED)
        ) {
            repository.appendLog("INFO", "SSH", "${hostProfile.name}: 检测到网络恢复，立即尝试重连")
            connect(forceRestart = true)
        }
    }

    fun sendBytes(bytes: ByteArray) {
        scope.launch(writeDispatcher) {
            try {
                shellOut?.apply {
                    write(bytes)
                    flush()
                }
            } catch (e: Exception) {
                repository.appendLog("WARN", "SSH", "${hostProfile.name}: 发送数据失败 (${e.message})")
            }
        }
    }

    fun sendText(text: String) {
        sendBytes(text.toByteArray(StandardCharsets.UTF_8))
    }

    fun resizePty(cols: Int, rows: Int) {
        val safeCols = cols.coerceIn(24, 220)
        val safeRows = rows.coerceIn(10, 100)
        val dimsChanged = safeCols != lastCols || safeRows != lastRows
        val firstCustomSize = !hasCustomPtySize
        hasCustomPtySize = true
        if (!dimsChanged && !firstCustomSize) return
        lastCols = safeCols
        lastRows = safeRows
        scope.launch(uiSyncDispatcher) {
            if (dimsChanged) {
                emulator.resize(safeCols, safeRows)
            }
            refreshTerminalSnapshot()
        }
        if (dimsChanged) {
            scope.launch(writeDispatcher) {
                try {
                    shellChannel?.setPtySize(safeCols, safeRows, 0, 0)
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun appendNoticeOnce(ansiMessage: String) {
        if (reviveNoticeShown || ansiMessage.isBlank()) return
        reviveNoticeShown = true
        scope.launch(uiSyncDispatcher) {
            appendSystemLine(ansiMessage)
        }
    }

    fun clearTerminal() {
        scope.launch(uiSyncDispatcher) {
            emulator.clear()
            refreshTerminalSnapshot()
        }
    }

    fun dismissAuthLink() {
        _snapshot.update { it.copy(detectedAuthLink = null) }
    }

    fun submitInteractivePrompt(answers: List<String>?) {
        val requestId = _snapshot.value.pendingInteractivePrompt?.requestId ?: return
        submitInteractivePrompt(requestId, answers)
    }

    fun submitInteractivePrompt(requestId: String, answers: List<String>?) {
        synchronized(promptLock) {
            if (_snapshot.value.pendingInteractivePrompt?.requestId != requestId) return
            val deferred = pendingPromptDeferred
            pendingPromptDeferred = null
            _snapshot.update { it.copy(pendingInteractivePrompt = null) }
            deferred?.complete(answers)
        }
    }

    fun deployKey(publicKeyLine: String, onResult: (Boolean, String) -> Unit) {
        val cmd = SshKeyManager.buildInstallPublicKeyCommand(publicKeyLine)
        if (_snapshot.value.state == SessionState.CONNECTED && shellChannel?.isConnected == true) {
            sendText("$cmd\n")
            onResult(true, "已向当前会话发送公钥写入指令")
        } else {
            onResult(false, "请先建立 SSH Shell 会话再一键部署公钥")
        }
    }

    private suspend fun runConnectionLoop(gen: Long) {
        var attempt = 0
        while (isAttemptActive(gen, currentCoroutineContext().isActive)) {
            val currentHost = hostProfile
            _snapshot.update {
                it.copy(
                    state = if (attempt == 0) SessionState.CONNECTING else SessionState.RECONNECTING,
                    statusDetail = if (attempt == 0) "正在连接 ${currentHost.host}:${currentHost.port}..." else "第 $attempt 次重连中...",
                    reconnectAttempts = attempt
                )
            }
            appendSystemLine("\r\n\u001B[36m[LinkTerm] 连接 ${currentHost.username}@${currentHost.host}:${currentHost.port}...\u001B[0m\r\n")

            val label = sessionLabel(currentHost)
            var unexpectedFailureMessage: String? = null
            try {
                establishAndStreamSession(currentHost, gen)
                if (isAttemptActive(gen, currentCoroutineContext().isActive)) {
                    emulator.exitAltScreenIfActive()
                    unexpectedFailureMessage = if (currentHost.localForwardSpec.isBlank()) {
                        "远端会话已关闭 (EOF)"
                    } else {
                        "SSH 隧道已断开"
                    }
                    repository.appendLog("WARN", "SSH", "$label: $unexpectedFailureMessage")
                    appendSystemLine("\r\n\u001B[33m[$unexpectedFailureMessage]\u001B[0m\r\n")
                }
            } catch (e: Exception) {
                if (isAttemptActive(gen, currentCoroutineContext().isActive)) {
                    if (e is HostKeyVerificationException || e is LocalForwardSetupException) {
                        automaticReconnectBlocked = true
                    }
                    emulator.exitAltScreenIfActive()
                    unexpectedFailureMessage = e.message ?: e::class.java.simpleName
                    repository.appendLog("WARN", "SSH", "$label: 连接中断 - $unexpectedFailureMessage")
                    appendSystemLine("\r\n\u001B[31m[连接异常: $unexpectedFailureMessage]\u001B[0m\r\n")
                }
            } finally {
                if (connectionGen.get() == gen) {
                    keepAliveJob?.cancel()
                }
            }

            if (!isAttemptActive(gen, currentCoroutineContext().isActive) ||
                !currentHost.autoReconnect || automaticReconnectBlocked
            ) {
                if (!userInitiatedStop && connectionGen.get() == gen && currentCoroutineContext().isActive) {
                    _snapshot.update {
                        it.copy(
                            state = SessionState.ERROR,
                            statusDetail = unexpectedFailureMessage ?: "连接已断开",
                            connectedSinceMillis = null
                        )
                    }
                }
                break
            }

            attempt++
            val backoffMs = when (attempt) {
                1 -> 2_000L
                2 -> 4_000L
                3 -> 8_000L
                4 -> 15_000L
                else -> 30_000L
            }
            if (isAttemptActive(gen, currentCoroutineContext().isActive)) {
                _snapshot.update {
                    it.copy(
                        state = SessionState.RECONNECTING,
                        statusDetail = "等待 ${backoffMs / 1000}s 后自动重连 ($unexpectedFailureMessage)",
                        connectedSinceMillis = null,
                        reconnectAttempts = attempt
                    )
                }
            }
            delay(backoffMs)
        }
    }

    private suspend fun establishAndStreamSession(host: HostProfile, gen: Long) = withContext(Dispatchers.IO) {
        val localForward = host.localForwardSpec.takeIf { it.isNotBlank() }?.let {
            LocalPortForward.parse(it) ?: throw LocalForwardSetupException("本地端口转发格式无效，请编辑主机配置")
        }
        emulator.exitAltScreenIfActive()
        emulator.resetParserState()
        val jsch = JSch()
        configureIdentities(jsch, host)
        val attemptJob = currentCoroutineContext()[Job]!!
        jsch.hostKeyRepository = PinnedHostKeyRepository(repository.knownHostKeys, host.host, host.port) { key ->
            awaitUserAnswers(
                title = "首次连接：确认主机指纹",
                instruction = "主机：${key.endpoint}\n算法：${key.algorithm}\n" +
                    "指纹：${key.fingerprintSha256}\n\n" +
                    "请通过可信渠道与服务器指纹核对。确认后保存指纹；后续密钥变化将阻止连接。",
                prompts = emptyList(),
                echo = emptyList(),
                gen = gen,
                attemptJob = attemptJob,
                confirmButtonText = "信任并连接"
            ) != null
        }

        val session = jsch.getSession(host.username, host.host, host.port)
        session.hostKeyAlias = KnownHostKeyStore.endpoint(host.host, host.port)
        val rttFactory = RttSocketFactory()
        session.setSocketFactory(rttFactory)
        var channel: ChannelShell? = null
        var outStream: OutputStream? = null
        var forwardRegistered = false
        try {
            jschSession = session
            session.setConfig("StrictHostKeyChecking", "yes")
            session.setConfig(
                "PreferredAuthentications",
                when (host.authType) {
                    AuthType.PUBLIC_KEY -> "publickey,keyboard-interactive,password"
                    AuthType.INTERACTIVE_OR_TAILSCALE -> "keyboard-interactive,publickey,password"
                    AuthType.PASSWORD -> "password,keyboard-interactive,publickey"
                }
            )
            session.setConfig("Compression", "none")
            session.setConfig("TCPKeepAlive", "yes")

            val savedPassword = VaultCrypto.decrypt(host.encryptedPassword)
            if (savedPassword.isNotEmpty() && host.authType != AuthType.PUBLIC_KEY) {
                session.setPassword(savedPassword.toByteArray(StandardCharsets.UTF_8))
            }
            val aliveSec = host.keepAliveIntervalSec.coerceIn(15, 300)
            // Use a tolerant ServerAlive budget (~15 min) without overriding SO_TIMEOUT below ServerAliveInterval,
            // so brief screen-off / OEM freezer pauses do not tear down an otherwise healthy TCP session.
            val serverAliveMs = maxOf(aliveSec, 45) * 1000
            session.setServerAliveInterval(serverAliveMs)
            session.setServerAliveCountMax(20)

            session.userInfo = createSshUserInfo(host, savedPassword, gen, attemptJob)

            // Tailscale Check Mode / interactive SSO may take up to ~2 minutes while user authorizes in browser
            session.connect(90_000)
            if (!isAttemptActive(gen, currentCoroutineContext().isActive)) return@withContext

            val inStream = if (localForward == null) {
                val shell = session.openChannel("shell") as ChannelShell
                channel = shell
                shellChannel = shell
                shell.setPty(true)
                shell.setPtyType("xterm-256color")
                shell.setPtySize(lastCols, lastRows, 0, 0)
                val input = shell.inputStream
                outStream = shell.outputStream
                shellOut = outStream
                shell.connect(15_000)
                input
            } else {
                try {
                    session.setPortForwardingL(
                        "127.0.0.1", localForward.localPort, localForward.destinationHost, localForward.destinationPort
                    )
                    forwardRegistered = true
                } catch (e: JSchException) {
                    throw LocalForwardSetupException("无法监听手机 ${localForward.localAddress}：${e.message}", e)
                }
                null
            }
            if (!isAttemptActive(gen, currentCoroutineContext().isActive)) return@withContext

            val connectedChannel = channel
            val onlineLabel = localForward?.let { "转发中 · ${it.localAddress}" } ?: "在线"
            val connectedAt = System.currentTimeMillis()
            _snapshot.update {
                it.copy(
                    state = SessionState.CONNECTED,
                    statusDetail = "$onlineLabel · 心跳 ${aliveSec}s",
                    connectedSinceMillis = connectedAt,
                    reconnectAttempts = 0,
                    pendingInteractivePrompt = null
                )
            }
            val label = sessionLabel(host)
            repository.appendLog("INFO", "SSH", "$label: SSH 会话已建立")
            if (localForward != null) {
                appendSystemLine("\r\n[本地转发已开启：手机 ${localForward.localAddress} → SSH 目标 ${localForward.destinationHost}:${localForward.destinationPort}]\r\n")
            }

            // Start heartbeat RTT probe at a gentle cadence to populate chart telemetry without flooding idle sockets.
            keepAliveJob?.cancel()
            val probeIntervalMs = maxOf(aliveSec, 30) * 1000L
            keepAliveJob = scope.launch {
                var firstProbe = true
                while (isActive && session.isConnected && connectedChannel?.isConnected != false && connectionGen.get() == gen) {
                    delay(if (firstProbe) 1_500L else probeIntervalMs)
                    firstProbe = false
                    if (!session.isConnected || connectedChannel?.isConnected == false || connectionGen.get() != gen) break
                    try {
                        // sendKeepAliveMsg() returns right after the local socket write, so measure the time
                        // until the server's reply actually arrives on the wire (real network RTT).
                        val sentAt = System.nanoTime()
                        session.sendKeepAliveMsg()
                        val deadline = sentAt + RTT_REPLY_TIMEOUT_NANOS
                        var rtt: Long? = null
                        while (isActive && System.nanoTime() < deadline) {
                            rtt = rttFactory.inboundDelayMsSince(sentAt)
                            if (rtt != null) break
                            delay(2L)
                        }
                        if (rtt != null) {
                            _snapshot.update {
                                it.copy(
                                    lastRttMs = rtt,
                                    statusDetail = "$onlineLabel · ${rtt}ms"
                                )
                            }
                            repository.appendRttSample(rttMs = rtt, activeSessions = 1)
                        } else if (isActive) {
                            repository.appendLog("WARN", "KeepAlive", "$label: 心跳 ${RTT_REPLY_TIMEOUT_NANOS / 1_000_000_000L}s 内未收到服务器回应")
                        }
                    } catch (e: Exception) {
                        repository.appendLog("WARN", "KeepAlive", "$label: 心跳异常 ${e.message}")
                        if (!session.isConnected) break
                    }
                }
            }

            // Execute optional auto-attach command (e.g., tmux attach)
            if (connectedChannel != null && host.autoAttachCommand.isNotBlank()) {
                scope.launch {
                    delay(280)
                    if (connectedChannel.isConnected && connectionGen.get() == gen) {
                        sendText(host.autoAttachCommand.trim() + "\n")
                    }
                }
            }

            if (inStream == null) {
                while (isAttemptActive(gen, currentCoroutineContext().isActive) && session.isConnected) {
                    delay(500)
                }
                return@withContext
            }

            val buffer = ByteArray(8192)
            var lastSnapshotAt = 0L
            var dirtySnapshot = false
            while (isAttemptActive(gen, currentCoroutineContext().isActive)) {
                val read = inStream.read(buffer)
                if (read < 0) break
                if (read > 0 && isAttemptActive(gen, currentCoroutineContext().isActive)) {
                    val decoded = emulator.feedBytes(buffer, read)
                    scanTextForAuthUrls(decoded)
                    val now = SystemClock.elapsedRealtime()
                    if (inStream.available() == 0 || now - lastSnapshotAt >= 40L) {
                        refreshTerminalSnapshot()
                        lastSnapshotAt = now
                        dirtySnapshot = false
                    } else {
                        dirtySnapshot = true
                    }
                }
            }
            if (dirtySnapshot && isAttemptActive(gen, currentCoroutineContext().isActive)) {
                refreshTerminalSnapshot()
            }
        } finally {
            if (forwardRegistered && localForward != null) {
                try {
                    session.delPortForwardingL(localForward.localPort)
                } catch (_: JSchException) {
                    // JSch may already have removed the listener while disconnecting the transport.
                }
            }
            try {
                channel?.disconnect()
            } catch (_: Throwable) {
            }
            try {
                session.disconnect()
            } catch (_: Throwable) {
            }
            if (shellOut === outStream) shellOut = null
            if (shellChannel === channel) shellChannel = null
            if (jschSession === session) jschSession = null
        }
    }

    private fun configureIdentities(jsch: JSch, host: HostProfile) {
        val keyId = host.keyId ?: return
        val keyRecord = repository.keys.value.firstOrNull { it.id == keyId } ?: return
        val privateKeyPem = VaultCrypto.decrypt(keyRecord.encryptedPrivateKey)
        if (privateKeyPem.isBlank()) return
        val passphrase = VaultCrypto.decrypt(host.encryptedPassphrase)
            .takeIf { it.isNotEmpty() }
            ?.toByteArray(StandardCharsets.UTF_8)
        jsch.addIdentity(
            keyRecord.name,
            privateKeyPem.toByteArray(StandardCharsets.UTF_8),
            keyRecord.publicKeyOpenSsh.toByteArray(StandardCharsets.UTF_8),
            passphrase
        )
    }

    private fun createSshUserInfo(host: HostProfile, savedPassword: String, gen: Long, attemptJob: Job): UserInfo {
        var usedSavedPassword = false
        return object : UserInfo, UIKeyboardInteractive {
            override fun getPassphrase(): String = VaultCrypto.decrypt(host.encryptedPassphrase)
            override fun getPassword(): String = savedPassword

            override fun promptPassword(message: String?): Boolean {
                if (savedPassword.isNotEmpty() && !usedSavedPassword) {
                    usedSavedPassword = true
                    return true
                }
                val answer = awaitUserAnswers(
                    title = "需要密码 (${host.host})",
                    instruction = message ?: "请输入 SSH 登录密码",
                    prompts = listOf("Password:"),
                    echo = listOf(false),
                    gen = gen,
                    attemptJob = attemptJob
                ) ?: return false
                jschSession?.setPassword(answer.firstOrNull().orEmpty().toByteArray(StandardCharsets.UTF_8))
                return true
            }

            override fun promptPassphrase(message: String?): Boolean {
                val savedPassphrase = VaultCrypto.decrypt(host.encryptedPassphrase)
                if (savedPassphrase.isNotEmpty()) return true
                val answer = awaitUserAnswers(
                    title = "私钥口令 (${host.host})",
                    instruction = message ?: "请输入私钥 Passphrase",
                    prompts = listOf("Passphrase:"),
                    echo = listOf(false),
                    gen = gen,
                    attemptJob = attemptJob
                ) ?: return false
                return answer.isNotEmpty()
            }

            override fun promptYesNo(message: String?): Boolean {
                return awaitUserAnswers(
                    title = "SSH 连接确认 (${host.host})",
                    instruction = message.orEmpty(),
                    prompts = emptyList(),
                    echo = emptyList(),
                    gen = gen,
                    attemptJob = attemptJob,
                    confirmButtonText = "确认"
                ) != null
            }

            override fun showMessage(message: String?) {
                if (message.isNullOrBlank()) return
                appendSystemLine("\r\n$message\r\n")
                scanTextForAuthUrls(message, isAuthHandshake = true)
            }

            override fun promptKeyboardInteractive(
                destination: String?,
                name: String?,
                instruction: String?,
                prompt: Array<out String>?,
                echo: BooleanArray?
            ): Array<String>? {
                val promptsList = prompt?.toList().orEmpty()
                val echoList = echo?.toList() ?: List(promptsList.size) { true }
                val combinedText = buildString {
                    if (!name.isNullOrBlank()) appendLine(name)
                    if (!instruction.isNullOrBlank()) appendLine(instruction)
                    promptsList.forEach { appendLine(it) }
                }
                if (combinedText.isNotBlank()) {
                    appendSystemLine("\r\n$combinedText")
                    scanTextForAuthUrls(combinedText, isAuthHandshake = true)
                }

                // Tailscale check mode often sends banner/URL with 0 prompts or expects empty response while polling
                if (promptsList.isEmpty()) {
                    return emptyArray()
                }
                if (promptsList.size == 1 && promptsList[0].contains("password", ignoreCase = true) &&
                    savedPassword.isNotEmpty() && !usedSavedPassword
                ) {
                    usedSavedPassword = true
                    return arrayOf(savedPassword)
                }

                val answers = awaitUserAnswers(
                    title = name?.ifBlank { null } ?: "交互式认证 (${host.host})",
                    instruction = instruction.orEmpty(),
                    prompts = promptsList,
                    echo = echoList,
                    gen = gen,
                    attemptJob = attemptJob
                )
                return answers?.toTypedArray()
            }
        }
    }

    private fun awaitUserAnswers(
        title: String,
        instruction: String,
        prompts: List<String>,
        echo: List<Boolean>,
        gen: Long,
        attemptJob: Job,
        confirmButtonText: String = "提交"
    ): List<String>? {
        val deferred = CompletableDeferred<List<String>?>(attemptJob)
        val request = InteractivePromptRequest(
            title = title,
            instruction = instruction,
            prompts = prompts,
            echo = echo,
            confirmButtonText = confirmButtonText
        )
        synchronized(promptLock) {
            if (!isAttemptActive(gen, attemptJob.isActive)) {
                deferred.cancel()
                return null
            }
            pendingPromptDeferred = deferred
            _snapshot.update {
                it.copy(
                    state = SessionState.AWAITING_INPUT,
                    statusDetail = if (prompts.isEmpty()) "等待确认连接" else "等待输入认证信息",
                    pendingInteractivePrompt = request
                )
            }
        }
        try {
            val answers = runBlocking {
                withTimeoutOrNull(120_000L) { deferred.await() }
            }
            return if (isAttemptActive(gen, attemptJob.isActive)) answers else null
        } finally {
            synchronized(promptLock) {
                if (_snapshot.value.pendingInteractivePrompt?.requestId == request.requestId) {
                    pendingPromptDeferred = null
                    _snapshot.update { it.copy(pendingInteractivePrompt = null) }
                }
            }
            deferred.cancel()
        }
    }

    private fun scanTextForAuthUrls(chunk: String, isAuthHandshake: Boolean = false) {
        if (chunk.isBlank()) return
        synchronized(authRollingBuffer) {
            authRollingBuffer.append(chunk)
            if (authRollingBuffer.length > 2048) {
                authRollingBuffer.delete(0, authRollingBuffer.length - 1024)
            }
            val detected = LinkDetector.detectAuthLink(authRollingBuffer.toString())
            if (detected != null) {
                val previousUrl = _snapshot.value.detectedAuthLink?.url
                _snapshot.update {
                    it.copy(
                        detectedAuthLink = detected,
                        state = if (isAuthHandshake) SessionState.AWAITING_OAUTH else it.state,
                        statusDetail = if (isAuthHandshake) "等待浏览器完成 ${detected.providerLabel}" else it.statusDetail
                    )
                }
                if (detected.url != previousUrl && detected.url != lastAutoOpenedUrl) {
                    repository.appendLog("INFO", "OAuth", "捕获 ${detected.providerLabel}: ${detected.url}")
                    if (repository.uiPreferences.value.autoOpenOAuthUrl) {
                        lastAutoOpenedUrl = detected.url
                        onAutoOpenAuthUrl(detected.url)
                    }
                }
            }
        }
    }

    private fun appendSystemLine(text: String) {
        emulator.feedText(text)
        refreshTerminalSnapshot()
    }

    private fun refreshTerminalSnapshot() {
        synchronized(snapshotApplyLock) {
            val snap = emulator.snapshotState()
            if (snap.sequence < appliedMutationSeq) return
            appliedMutationSeq = snap.sequence
            _snapshot.update {
                it.copy(
                    terminalLines = snap.lines,
                    terminalRevision = it.terminalRevision + 1L,
                    windowTitle = snap.windowTitle,
                    ptyCols = lastCols,
                    ptyRows = lastRows,
                    hasCustomPtySize = hasCustomPtySize
                )
            }
        }
    }

    private fun closeTransportAsync() {
        val oldChannel = shellChannel
        val oldSession = jschSession
        shellOut = null
        shellChannel = null
        jschSession = null
        if (oldChannel == null && oldSession == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                oldChannel?.disconnect()
            } catch (_: Throwable) {
            }
            try {
                oldSession?.disconnect()
            } catch (_: Throwable) {
            }
        }
    }
}

class SshSessionManager(
    private val context: Context,
    private val repository: AppRepository
) {
    private val appContext = context.applicationContext
    private val managerLock = Any()
    private val controllers = ConcurrentHashMap<String, SshSessionController>()
    private val collectorJobs = ConcurrentHashMap<String, Job>()
    private val sessionSeq = AtomicInteger(0)

    private val _snapshots = MutableStateFlow<Map<String, SessionUiSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<String, SessionUiSnapshot>> = _snapshots.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _activeHostId = MutableStateFlow<String?>(null)
    val activeHostId: StateFlow<String?> = _activeHostId.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val autoOpenCallback: (String) -> Unit = { url ->
        scope.launch {
            val launchContext = (appContext as? LinkTerminalApp)?.currentResumedActivity() ?: appContext
            LinkDetector.openUrlInBrowser(launchContext, url)
        }
    }

    private fun sessionPriorityRank(state: SessionState): Int = when (state) {
        SessionState.CONNECTED -> 0
        SessionState.AWAITING_OAUTH, SessionState.AWAITING_INPUT -> 1
        SessionState.CONNECTING, SessionState.RECONNECTING -> 2
        SessionState.DISCONNECTED -> 3
        SessionState.ERROR -> 4
        SessionState.IDLE -> 5
    }

    private fun findPrimaryControllerForHostLocked(hostId: String): SshSessionController? {
        val active = _activeSessionId.value?.let { controllers[it] }
        if (active != null && active.hostProfile.id == hostId) return active
        return controllers.values
            .filter { it.hostProfile.id == hostId }
            .minWithOrNull(
                compareBy<SshSessionController> { sessionPriorityRank(it.snapshot.value.state) }
                    .thenBy { it.sessionIndex }
            )
    }

    private fun registerControllerLocked(
        sessionId: String,
        sessionIndex: Int,
        host: HostProfile
    ): SshSessionController {
        controllers[sessionId]?.let { return it }
        val created = SshSessionController(
            context = appContext,
            repository = repository,
            onAutoOpenAuthUrl = autoOpenCallback,
            initialHost = host,
            sessionId = sessionId,
            sessionIndex = sessionIndex
        )
        controllers[sessionId] = created
        _snapshots.update { it + (sessionId to created.snapshot.value) }
        collectorJobs[sessionId] = scope.launch {
            created.snapshot.collect { snap ->
                if (controllers[sessionId] === created) {
                    _snapshots.update { it + (sessionId to snap) }
                }
            }
        }
        return created
    }

    fun getControllerBySessionId(sessionId: String): SshSessionController? = controllers[sessionId]

    fun updateHostProfiles(updated: HostProfile) {
        synchronized(managerLock) {
            controllers.values
                .filter { it.hostProfile.id == updated.id }
                .forEach { it.updateHostProfile(updated) }
        }
    }

    fun getOrCreateController(host: HostProfile): SshSessionController = synchronized(managerLock) {
        controllers.values
            .filter { it.hostProfile.id == host.id }
            .forEach { it.updateHostProfile(host) }
        findPrimaryControllerForHostLocked(host.id)?.let { return@synchronized it }
        registerControllerLocked(sessionId = host.id, sessionIndex = 1, host = host)
    }

    fun createNewSession(host: HostProfile): SshSessionController {
        val created = synchronized(managerLock) {
            val existingForHost = controllers.values.filter { it.hostProfile.id == host.id }
            val nextIndex = (existingForHost.maxOfOrNull { it.sessionIndex } ?: 0) + 1
            val newSessionId = if (controllers[host.id] == null && nextIndex == 1) {
                host.id
            } else {
                "${host.id}#${sessionSeq.incrementAndGet()}"
            }
            registerControllerLocked(
                sessionId = newSessionId,
                sessionIndex = nextIndex,
                host = host
            ).also {
                _activeHostId.value = host.id
                _activeSessionId.value = it.sessionId
            }
        }
        created.connect()
        return created
    }

    fun openOrSelectHost(host: HostProfile) {
        val shouldConnect = synchronized(managerLock) {
            val hostControllers = controllers.values.filter { it.hostProfile.id == host.id }
            if (hostControllers.isEmpty() || hostControllers.all { it.snapshot.value.state == SessionState.IDLE }) {
                true
            } else {
                _activeHostId.value = host.id
                _activeSessionId.value = findPrimaryControllerForHostLocked(host.id)?.sessionId
                false
            }
        }
        if (shouldConnect) {
            openAndConnect(host)
        }
    }

    fun resolvePreferredHost(hosts: List<HostProfile>, requestedHostId: String? = null): HostProfile? {
        val (activeSessionHostId, activeHostId, liveHostIds) = synchronized(managerLock) {
            Triple(
                _activeSessionId.value?.let { controllers[it]?.hostProfile?.id },
                _activeHostId.value,
                controllers.values
                    .filter { it.snapshot.value.state.isActiveChannel() || it.snapshot.value.state == SessionState.RECONNECTING }
                    .map { it.hostProfile.id }
                    .toSet()
            )
        }
        return resolveHostSelection(
            hosts = hosts,
            requestedHostId = requestedHostId,
            activeSessionHostId = activeSessionHostId,
            activeHostId = activeHostId,
            liveSessionHostIds = liveHostIds
        )
    }

    fun openAndConnect(host: HostProfile) {
        val controller = synchronized(managerLock) {
            getOrCreateController(host).also {
                _activeHostId.value = host.id
                _activeSessionId.value = it.sessionId
            }
        }
        controller.connect()
    }

    fun selectHost(hostId: String) {
        synchronized(managerLock) {
            _activeHostId.value = hostId
            _activeSessionId.value = findPrimaryControllerForHostLocked(hostId)?.sessionId
        }
    }

    fun selectSession(sessionId: String) {
        synchronized(managerLock) {
            val controller = controllers[sessionId] ?: return
            _activeSessionId.value = sessionId
            _activeHostId.value = controller.hostProfile.id
        }
    }

    fun closeSession(sessionId: String) {
        val removed = synchronized(managerLock) {
            val ctrl = controllers.remove(sessionId) ?: return
            val hostId = ctrl.hostProfile.id
            collectorJobs.remove(sessionId)?.cancel()
            _snapshots.update { it - sessionId }
            if (_activeSessionId.value == sessionId) {
                val sameHostFallback = controllers.values
                    .filter { it.hostProfile.id == hostId }
                    .minByOrNull { it.sessionIndex }
                val fallback = sameHostFallback ?: controllers.values.firstOrNull()
                _activeSessionId.value = fallback?.sessionId
                if (fallback != null) {
                    _activeHostId.value = fallback.hostProfile.id
                }
            }
            ctrl
        }
        removed.destroy()
    }

    fun disconnectHost(hostId: String) {
        val list = synchronized(managerLock) {
            controllers.values.filter { it.hostProfile.id == hostId }
        }
        list.forEach { it.disconnect() }
    }

    fun forgetHostKey(host: String, port: Int) {
        val endpoint = KnownHostKeyStore.endpoint(host, port)
        val list = synchronized(managerLock) {
            controllers.values.filter {
                KnownHostKeyStore.endpoint(it.hostProfile.host, it.hostProfile.port) == endpoint
            }
        }
        list.forEach { it.disconnect() }
        repository.knownHostKeys.forget(host, port)
    }

    fun disconnectAll() {
        val list = synchronized(managerLock) { controllers.values.toList() }
        list.forEach { it.disconnect() }
    }

    fun reconnectAll() {
        val list = synchronized(managerLock) { controllers.values.toList() }
        list.forEach { controller ->
            val state = controller.snapshot.value.state
            if (shouldReconnectOnAction(state, controller.hostProfile.pinInDaemon, controller.automaticReconnectBlocked)) {
                controller.connect(forceRestart = state == SessionState.RECONNECTING)
            }
        }
    }

    fun restorePinnedHosts(reviveReason: String? = null) {
        repository.hosts.value
            .filter { it.pinInDaemon }
            .forEach { host ->
                val controller = getOrCreateController(host)
                val shouldNotifyRevive = !reviveReason.isNullOrBlank() &&
                    controller.snapshot.value.state == SessionState.IDLE
                if (shouldNotifyRevive) {
                    controller.appendNoticeOnce("\r\n\u001B[33m$reviveReason\u001B[0m\r\n")
                }
                if (!controller.automaticReconnectBlocked) controller.connect()
            }
    }

    fun connectAll() {
        repository.hosts.value.forEach { host ->
            getOrCreateController(host).connect()
        }
    }

    fun onNetworkAvailable() {
        val list = synchronized(managerLock) { controllers.values.toList() }
        list.forEach { it.onNetworkAvailable() }
    }

    fun removeHostController(hostId: String) {
        val removedList = synchronized(managerLock) {
            val targets = controllers.values.filter { it.hostProfile.id == hostId }
            val removedIds = targets.map { it.sessionId }
            targets.forEach { ctrl ->
                controllers.remove(ctrl.sessionId)
                collectorJobs.remove(ctrl.sessionId)?.cancel()
            }
            if (removedIds.isNotEmpty()) {
                _snapshots.update { it - removedIds.toSet() }
            }
            if (_activeHostId.value == hostId || (_activeSessionId.value in removedIds)) {
                val fallback = controllers.values.firstOrNull()
                _activeSessionId.value = fallback?.sessionId
                _activeHostId.value = fallback?.hostProfile?.id
            }
            targets
        }
        removedList.forEach { it.destroy() }
    }

    fun activeConnectedCount(): Int =
        _snapshots.value.values.count {
            it.state.isActiveChannel() || it.state == SessionState.RECONNECTING
        }

    fun averageRttMs(): Long? {
        val rtts = _snapshots.value.values.mapNotNull { it.lastRttMs }
        return if (rtts.isEmpty()) null else rtts.average().toLong()
    }

    companion object {
        internal fun shouldReconnectOnAction(
            state: SessionState,
            pinInDaemon: Boolean,
            automaticReconnectBlocked: Boolean = false
        ): Boolean = !automaticReconnectBlocked && when (state) {
            SessionState.CONNECTED,
            SessionState.CONNECTING,
            SessionState.AWAITING_INPUT,
            SessionState.AWAITING_OAUTH -> false
            SessionState.RECONNECTING,
            SessionState.DISCONNECTED,
            SessionState.ERROR -> true
            SessionState.IDLE -> pinInDaemon
        }

        internal fun resolveHostSelection(
            hosts: List<HostProfile>,
            requestedHostId: String?,
            activeSessionHostId: String?,
            activeHostId: String?,
            liveSessionHostIds: Set<String>
        ): HostProfile? {
            if (hosts.isEmpty()) return null
            if (requestedHostId != null) {
                return hosts.firstOrNull { it.id == requestedHostId } ?: hosts.firstOrNull()
            }
            return activeSessionHostId?.let { id -> hosts.firstOrNull { it.id == id } }
                ?: activeHostId?.let { id -> hosts.firstOrNull { it.id == id } }
                ?: hosts.firstOrNull { it.id in liveSessionHostIds }
                ?: hosts.firstOrNull()
        }
    }
}
