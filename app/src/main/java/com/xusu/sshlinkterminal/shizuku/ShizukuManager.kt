package com.xusu.sshlinkterminal.shizuku

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.xusu.sshlinkterminal.data.AppRepository
import com.xusu.sshlinkterminal.data.GuardStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import rikka.shizuku.Shizuku

class ShizukuManager(
    private val context: Context,
    private val repository: AppRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _guardStatus = MutableStateFlow(GuardStatus())
    val guardStatus: StateFlow<GuardStatus> = _guardStatus.asStateFlow()

    @Volatile
    private var daemonService: IShizukuDaemonService? = null

    @Volatile
    private var isBound = false

    @Volatile
    private var foregroundRunningFlag = false

    @Volatile
    private var pendingBindWaiter: CompletableDeferred<IShizukuDaemonService?>? = null

    private val serviceArgs: Shizuku.UserServiceArgs by lazy {
        val debuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, ShizukuDaemonService::class.java.name)
        )
            .daemon(true)
            .processNameSuffix("shizuku_daemon")
            .debuggable(debuggable)
            .version(1)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val svc = IShizukuDaemonService.Stub.asInterface(service)
            daemonService = svc
            isBound = svc != null
            pendingBindWaiter?.complete(svc)
            pendingBindWaiter = null
            repository.appendLog("INFO", "Shizuku", "Shizuku 守护服务已连接 (daemon=true)")
            scope.launch {
                if (repository.uiPreferences.value.autoHardenWithShizuku) {
                    applyHardeningAndWatchdog(silent = true)
                } else {
                    refreshStatus()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            daemonService = null
            isBound = false
            pendingBindWaiter?.complete(null)
            pendingBindWaiter = null
            repository.appendLog("WARN", "Shizuku", "Shizuku 守护服务连接断开")
            scope.launch { refreshStatus() }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        repository.appendLog("INFO", "Shizuku", "检测到 Shizuku Binder 就绪")
        scope.launch {
            refreshStatus()
            if (isShizukuPermissionGranted() && repository.uiPreferences.value.autoHardenWithShizuku) {
                bindDaemonService()
            }
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        daemonService = null
        isBound = false
        repository.appendLog("WARN", "Shizuku", "Shizuku Binder 已离线")
        scope.launch { refreshStatus() }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            repository.appendLog("INFO", "Shizuku", if (granted) "Shizuku 权限已授予" else "Shizuku 权限被拒绝")
            scope.launch {
                refreshStatus()
                if (granted) {
                    bindDaemonService()
                }
            }
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
        }
        scope.launch { refreshStatus() }
    }

    fun setForegroundServiceRunning(running: Boolean) {
        foregroundRunningFlag = running
        _guardStatus.value = _guardStatus.value.copy(foregroundServiceRunning = running)
    }

    fun isShizukuInstalled(): Boolean {
        return try {
            appContext.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun isShizukuPermissionGranted(): Boolean {
        return try {
            isShizukuRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestShizukuPermission(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11()) return false
            if (isShizukuPermissionGranted()) {
                bindDaemonService()
                true
            } else {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                true
            }
        } catch (t: Throwable) {
            repository.appendLog("ERROR", "Shizuku", "请求 Shizuku 权限失败: ${t.message}")
            false
        }
    }

    fun bindDaemonService() {
        try {
            if (!isShizukuPermissionGranted()) return
            if (!isBound || daemonService == null) {
                Shizuku.bindUserService(serviceArgs, serviceConnection)
            }
        } catch (t: Throwable) {
            repository.appendLog("ERROR", "Shizuku", "绑定 UserService 失败: ${t.message}")
        }
    }

    fun sendHeartbeat() {
        try {
            daemonService?.heartbeat()
        } catch (_: Throwable) {
            daemonService = null
            isBound = false
        }
    }

    fun stopWatchdog() {
        try {
            daemonService?.stopWatchdog()
        } catch (_: Throwable) {
        }
        _guardStatus.value = _guardStatus.value.copy(watchdogActive = false)
    }

    private suspend fun ensureDaemonService(timeoutMs: Long = 4_000L): IShizukuDaemonService? {
        daemonService?.let { return it }
        if (!isShizukuPermissionGranted()) return null
        val waiter = CompletableDeferred<IShizukuDaemonService?>()
        pendingBindWaiter = waiter
        bindDaemonService()
        return withTimeoutOrNull(timeoutMs) { waiter.await() } ?: daemonService
    }

    suspend fun applyHardeningAndWatchdog(silent: Boolean = false): GuardStatus = withContext(Dispatchers.IO) {
        if (!isShizukuPermissionGranted()) {
            val status = refreshStatus()
            return@withContext status.copy(lastReport = "请先授予 Shizuku 权限后再执行加固")
        }
        val svc = ensureDaemonService()
        if (svc == null) {
            val status = refreshStatus()
            return@withContext status.copy(lastReport = "Shizuku Daemon 尚未连接成功，请稍等或重启 Shizuku 后重试")
        }
        try {
            val rawReport = svc.applyKeepAliveHardening(appContext.packageName) ?: "{}"
            val serviceClass = "com.xusu.sshlinkterminal.service.TerminalForegroundService"
            svc.startWatchdog(appContext.packageName, serviceClass)
            val json = JSONObject(rawReport)
            val ok = json.optBoolean("ok", false)
            if (!silent) {
                repository.appendLog(
                    if (ok) "INFO" else "WARN",
                    "Shizuku",
                    if (ok) "Doze 白名单、Standby、AppOps 放行与守护看门狗已全部启用"
                    else "部分加固命令返回非零: $rawReport"
                )
            }
        } catch (t: Throwable) {
            daemonService = null
            isBound = false
            repository.appendLog("ERROR", "Shizuku", "执行 Shizuku 加固失败: ${t.message}")
            return@withContext refreshStatus().copy(lastReport = "执行 Shizuku 加固异常: ${t.message}")
        }
        refreshStatus()
    }

    suspend fun refreshStatus(): GuardStatus = withContext(Dispatchers.IO) {
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = pm.isIgnoringBatteryOptimizations(appContext.packageName)
        val installed = isShizukuInstalled()
        val running = isShizukuRunning()
        val permGranted = isShizukuPermissionGranted()

        var dozeWhitelisted: Boolean? = null
        var standbyActive: Boolean? = null
        var appOpsAllowed: Boolean? = null
        var watchdogActive = false
        var reportSummary = ""

        val svc = daemonService
        if (permGranted && svc != null) {
            try {
                val raw = svc.queryHardeningStatus(appContext.packageName) ?: "{}"
                val json = JSONObject(raw)
                dozeWhitelisted = json.optBoolean("dozeWhitelisted", false) || batteryIgnored
                standbyActive = json.optBoolean("standbyActive", false)
                appOpsAllowed = json.optBoolean("appOpsAllowed", false)
                watchdogActive = json.optBoolean("watchdogActive", false)
                val bucket = json.optString("standbyBucket", "?")
                val bucketLabel = when (bucket) {
                    "5" -> "5(已豁免)"
                    "10" -> "10(Active)"
                    else -> bucket
                }
                val watchdogLabel = if (watchdogActive) "看门狗运行中" else "看门狗未开启"
                reportSummary = "Bucket=$bucketLabel | Doze=$dozeWhitelisted | AppOps=$appOpsAllowed | $watchdogLabel"
            } catch (_: Throwable) {
            }
        }

        val status = GuardStatus(
            notificationsGranted = notifGranted,
            batteryOptimizationIgnored = batteryIgnored,
            shizukuInstalled = installed,
            shizukuRunning = running,
            shizukuPermissionGranted = permGranted,
            dozeWhitelisted = dozeWhitelisted,
            standbyBucketActive = standbyActive,
            appOpsBackgroundAllowed = appOpsAllowed,
            watchdogActive = watchdogActive,
            foregroundServiceRunning = foregroundRunningFlag,
            lastReport = reportSummary
        )
        _guardStatus.value = status
        status
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1409
    }
}
