package com.xusu.sshlinkterminal.ssh

import android.content.Context
import android.os.SystemClock
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

data class SessionUiSnapshot(
    val hostId: String,
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
    val windowTitle: String = ""
)

class SshSessionController(
    private val context: Context,
    private val repository: AppRepository,
    private val onAutoOpenAuthUrl: (String) -> Unit,
    initialHost: HostProfile
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val emulator = AnsiEmulator(initialCols = 80, initialRows = 28)

    @Volatile
    var hostProfile: HostProfile = initialHost
        private set

    private val _snapshot = MutableStateFlow(
        SessionUiSnapshot(
            hostId = initialHost.id,
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
    private var lastAutoOpenedUrl: String? = null

    @Volatile
    private var lastCols = 80

    @Volatile
    private var lastRows = 28

    private var pendingPromptDeferred: CompletableDeferred<List<String>?>? = null
    private val authRollingBuffer = StringBuilder()

    fun updateHostProfile(updated: HostProfile) {
        hostProfile = updated
        _snapshot.value = _snapshot.value.copy(
            hostName = updated.name,
            userAtHost = "${updated.username}@${updated.host}:${updated.port}"
        )
    }

    fun connect(forceRestart: Boolean = false) {
        if (!forceRestart && (_snapshot.value.state == SessionState.CONNECTED ||
                _snapshot.value.state == SessionState.CONNECTING ||
                _snapshot.value.state == SessionState.AWAITING_OAUTH)
        ) {
            return
        }
        TerminalForegroundService.ensureStarted(context)
        userInitiatedStop = false
        connectionJob?.cancel()
        keepAliveJob?.cancel()
        connectionJob = scope.launch {
            runConnectionLoop()
        }
    }

    fun disconnect() {
        userInitiatedStop = true
        pendingPromptDeferred?.cancel()
        pendingPromptDeferred = null
        keepAliveJob?.cancel()
        connectionJob?.cancel()
        closeTransportOnly()
        appendSystemLine("\r\n\u001B[33m[已手动断开连接]\u001B[0m\r\n")
        _snapshot.value = _snapshot.value.copy(
            state = SessionState.DISCONNECTED,
            statusDetail = "已断开",
            connectedSinceMillis = null,
            pendingInteractivePrompt = null
        )
    }

    fun onNetworkAvailable() {
        val state = _snapshot.value.state
        if (!userInitiatedStop && hostProfile.autoReconnect &&
            (state == SessionState.RECONNECTING || state == SessionState.ERROR || state == SessionState.DISCONNECTED)
        ) {
            repository.appendLog("INFO", "SSH", "${hostProfile.name}: 检测到网络恢复，立即尝试重连")
            connect(forceRestart = true)
        }
    }

    fun sendBytes(bytes: ByteArray) {
        scope.launch {
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
        if (safeCols == lastCols && safeRows == lastRows) return
        lastCols = safeCols
        lastRows = safeRows
        emulator.resize(safeCols, safeRows)
        refreshTerminalSnapshot()
        scope.launch {
            try {
                shellChannel?.setPtySize(safeCols, safeRows, 0, 0)
            } catch (_: Throwable) {
            }
        }
    }

    fun clearTerminal() {
        emulator.clear()
        refreshTerminalSnapshot()
    }

    fun dismissAuthLink() {
        _snapshot.value = _snapshot.value.copy(detectedAuthLink = null)
    }

    fun submitInteractivePrompt(answers: List<String>?) {
        val deferred = pendingPromptDeferred
        pendingPromptDeferred = null
        _snapshot.value = _snapshot.value.copy(pendingInteractivePrompt = null)
        deferred?.complete(answers)
    }

    fun deployKey(publicKeyLine: String, onResult: (Boolean, String) -> Unit) {
        val cmd = SshKeyManager.buildInstallPublicKeyCommand(publicKeyLine)
        if (_snapshot.value.state == SessionState.CONNECTED) {
            sendText("$cmd\n")
            onResult(true, "已向当前会话发送公钥写入指令")
        } else {
            onResult(false, "请先建立 SSH 连接后再一键部署公钥")
        }
    }

    private suspend fun runConnectionLoop() {
        var attempt = 0
        while (!userInitiatedStop && scope.isActive) {
            val currentHost = hostProfile
            _snapshot.value = _snapshot.value.copy(
                state = if (attempt == 0) SessionState.CONNECTING else SessionState.RECONNECTING,
                statusDetail = if (attempt == 0) "正在连接 ${currentHost.host}:${currentHost.port}..." else "第 $attempt 次重连中...",
                reconnectAttempts = attempt
            )
            appendSystemLine("\r\n\u001B[36m[LinkTerm] 连接 ${currentHost.username}@${currentHost.host}:${currentHost.port}...\u001B[0m\r\n")

            var unexpectedFailureMessage: String? = null
            try {
                establishAndStreamSession(currentHost)
                if (!userInitiatedStop) {
                    unexpectedFailureMessage = "远端会话已关闭"
                }
            } catch (e: Exception) {
                if (!userInitiatedStop) {
                    unexpectedFailureMessage = e.message ?: e::class.java.simpleName
                    repository.appendLog("WARN", "SSH", "${currentHost.name}: 连接中断 - $unexpectedFailureMessage")
                    appendSystemLine("\r\n\u001B[31m[连接异常: $unexpectedFailureMessage]\u001B[0m\r\n")
                }
            } finally {
                keepAliveJob?.cancel()
                closeTransportOnly()
            }

            if (userInitiatedStop || !currentHost.autoReconnect) {
                if (!userInitiatedStop) {
                    _snapshot.value = _snapshot.value.copy(
                        state = SessionState.ERROR,
                        statusDetail = unexpectedFailureMessage ?: "连接已断开",
                        connectedSinceMillis = null
                    )
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
            _snapshot.value = _snapshot.value.copy(
                state = SessionState.RECONNECTING,
                statusDetail = "等待 ${backoffMs / 1000}s 后自动重连 ($unexpectedFailureMessage)",
                connectedSinceMillis = null,
                reconnectAttempts = attempt
            )
            delay(backoffMs)
        }
    }

    private suspend fun establishAndStreamSession(host: HostProfile) = withContext(Dispatchers.IO) {
        val jsch = JSch()
        configureIdentities(jsch, host)

        val session = jsch.getSession(host.username, host.host, host.port)
        jschSession = session
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig(
            "PreferredAuthentications",
            when (host.authType) {
                AuthType.PUBLIC_KEY -> "publickey,keyboard-interactive,password"
                AuthType.INTERACTIVE_OR_TAILSCALE -> "keyboard-interactive,publickey,password"
                AuthType.PASSWORD -> "password,keyboard-interactive,publickey"
            }
        )
        session.setConfig("Compression", "none")

        val savedPassword = VaultCrypto.decrypt(host.encryptedPassword)
        if (savedPassword.isNotEmpty() && host.authType != AuthType.PUBLIC_KEY) {
            session.setPassword(savedPassword.toByteArray(StandardCharsets.UTF_8))
        }
        val aliveSec = host.keepAliveIntervalSec.coerceIn(5, 120)
        session.setServerAliveInterval(aliveSec * 1000)
        session.setServerAliveCountMax(4)
        session.timeout = 20_000

        session.userInfo = createSshUserInfo(host, savedPassword)

        // Tailscale Check Mode / interactive SSO may take up to ~2 minutes while user authorizes in browser
        session.connect(90_000)

        val channel = session.openChannel("shell") as ChannelShell
        shellChannel = channel
        channel.setPty(true)
        channel.setPtyType("xterm-256color")
        channel.setPtySize(lastCols, lastRows, 0, 0)

        val inStream = channel.inputStream
        shellOut = channel.outputStream
        channel.connect(15_000)

        val connectedAt = System.currentTimeMillis()
        _snapshot.value = _snapshot.value.copy(
            state = SessionState.CONNECTED,
            statusDetail = "在线 · 心跳 ${aliveSec}s",
            connectedSinceMillis = connectedAt,
            reconnectAttempts = 0,
            pendingInteractivePrompt = null
        )
        repository.appendLog("INFO", "SSH", "${host.name}: SSH 会话已建立")

        // Start heartbeat RTT probe
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && session.isConnected) {
                delay(aliveSec * 1000L)
                if (!session.isConnected) break
                val start = SystemClock.elapsedRealtime()
                try {
                    session.sendKeepAliveMsg()
                    val rtt = (SystemClock.elapsedRealtime() - start).coerceAtLeast(1L)
                    _snapshot.value = _snapshot.value.copy(
                        lastRttMs = rtt,
                        statusDetail = "在线 · ${rtt}ms"
                    )
                    repository.appendRttSample(rttMs = rtt, activeSessions = 1)
                } catch (e: Exception) {
                    repository.appendLog("WARN", "KeepAlive", "${host.name}: 心跳失败 ${e.message}")
                    break
                }
            }
        }

        // Execute optional auto-attach command (e.g., tmux attach)
        if (host.autoAttachCommand.isNotBlank()) {
            scope.launch {
                delay(280)
                if (channel.isConnected) {
                    sendText(host.autoAttachCommand.trim() + "\n")
                }
            }
        }

        val buffer = ByteArray(8192)
        while (scope.isActive && !userInitiatedStop) {
            val read = inStream.read(buffer)
            if (read < 0) break
            if (read > 0) {
                val decoded = emulator.feedBytes(buffer, read)
                scanTextForAuthUrls(decoded)
                refreshTerminalSnapshot()
            }
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

    private fun createSshUserInfo(host: HostProfile, savedPassword: String): UserInfo {
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
                    echo = listOf(false)
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
                    echo = listOf(false)
                ) ?: return false
                return answer.isNotEmpty()
            }

            override fun promptYesNo(message: String?): Boolean {
                if (!message.isNullOrBlank()) {
                    appendSystemLine("\r\n\u001B[36m$message\u001B[0m\r\n")
                    scanTextForAuthUrls(message)
                }
                return true
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
                    echo = echoList
                )
                return answers?.toTypedArray()
            }
        }
    }

    private fun awaitUserAnswers(
        title: String,
        instruction: String,
        prompts: List<String>,
        echo: List<Boolean>
    ): List<String>? {
        val deferred = CompletableDeferred<List<String>?>()
        pendingPromptDeferred = deferred
        _snapshot.value = _snapshot.value.copy(
            state = SessionState.AWAITING_INPUT,
            statusDetail = "等待输入认证信息",
            pendingInteractivePrompt = InteractivePromptRequest(
                title = title,
                instruction = instruction,
                prompts = prompts,
                echo = echo
            )
        )
        return runBlocking {
            withTimeoutOrNull(120_000L) {
                deferred.await()
            }
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
                _snapshot.value = _snapshot.value.copy(
                    detectedAuthLink = detected,
                    state = if (isAuthHandshake) SessionState.AWAITING_OAUTH else _snapshot.value.state,
                    statusDetail = if (isAuthHandshake) "等待浏览器完成 ${detected.providerLabel}" else _snapshot.value.statusDetail
                )
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
        val lines = emulator.snapshotLines()
        _snapshot.value = _snapshot.value.copy(
            terminalLines = lines,
            terminalRevision = _snapshot.value.terminalRevision + 1L,
            windowTitle = emulator.windowTitle
        )
    }

    private fun closeTransportOnly() {
        try {
            shellOut = null
            shellChannel?.disconnect()
            shellChannel = null
        } catch (_: Throwable) {
        }
        try {
            jschSession?.disconnect()
            jschSession = null
        } catch (_: Throwable) {
        }
    }
}

class SshSessionManager(
    private val context: Context,
    private val repository: AppRepository
) {
    private val appContext = context.applicationContext
    private val controllers = ConcurrentHashMap<String, SshSessionController>()

    private val _snapshots = MutableStateFlow<Map<String, SessionUiSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<String, SessionUiSnapshot>> = _snapshots.asStateFlow()

    private val _activeHostId = MutableStateFlow<String?>(null)
    val activeHostId: StateFlow<String?> = _activeHostId.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val autoOpenCallback: (String) -> Unit = { url ->
        scope.launch {
            LinkDetector.openUrlInBrowser(appContext, url)
        }
    }

    fun getOrCreateController(host: HostProfile): SshSessionController {
        val existing = controllers[host.id]
        if (existing != null) {
            existing.updateHostProfile(host)
            return existing
        }
        val created = SshSessionController(
            context = appContext,
            repository = repository,
            onAutoOpenAuthUrl = autoOpenCallback,
            initialHost = host
        )
        controllers[host.id] = created
        scope.launch {
            created.snapshot.collect { snap ->
                _snapshots.value = _snapshots.value.toMutableMap().apply { put(host.id, snap) }
            }
        }
        return created
    }

    fun openAndConnect(host: HostProfile) {
        _activeHostId.value = host.id
        getOrCreateController(host).connect()
    }

    fun selectHost(hostId: String) {
        _activeHostId.value = hostId
    }

    fun disconnectHost(hostId: String) {
        controllers[hostId]?.disconnect()
    }

    fun disconnectAll() {
        controllers.values.forEach { it.disconnect() }
    }

    fun reconnectAll() {
        controllers.values.forEach { controller ->
            if (controller.hostProfile.pinInDaemon || controller.snapshot.value.state != SessionState.IDLE) {
                controller.connect(forceRestart = true)
            }
        }
    }

    fun restorePinnedHosts() {
        repository.hosts.value
            .filter { it.pinInDaemon }
            .forEach { host ->
                getOrCreateController(host).connect()
            }
    }

    fun onNetworkAvailable() {
        controllers.values.forEach { it.onNetworkAvailable() }
    }

    fun removeHostController(hostId: String) {
        controllers.remove(hostId)?.disconnect()
        _snapshots.value = _snapshots.value.toMutableMap().apply { remove(hostId) }
        if (_activeHostId.value == hostId) {
            _activeHostId.value = _snapshots.value.keys.firstOrNull()
        }
    }

    fun activeConnectedCount(): Int =
        _snapshots.value.values.count {
            it.state == SessionState.CONNECTED ||
                it.state == SessionState.CONNECTING ||
                it.state == SessionState.AWAITING_OAUTH ||
                it.state == SessionState.RECONNECTING
        }

    fun averageRttMs(): Long? {
        val rtts = _snapshots.value.values.mapNotNull { it.lastRttMs }
        return if (rtts.isEmpty()) null else rtts.average().toLong()
    }
}
