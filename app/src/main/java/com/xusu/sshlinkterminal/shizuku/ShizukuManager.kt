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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            daemonService = IShizukuDaemonService.Stub.asInterface(service)
            isBound = daemonService != null
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

    suspend fun applyHardeningAndWatchdog(silent: Boolean = false): GuardStatus = withContext(Dispatchers.IO) {
        if (!isShizukuPermissionGranted()) {
            return@withContext refreshStatus()
        }
        if (daemonService == null) {
            bindDaemonService()
            return@withContext refreshStatus()
        }
        try {
            val rawReport = daemonService?.applyKeepAliveHardening(appContext.packageName) ?: "{}"
            val serviceClass = "com.xusu.sshlinkterminal.service.TerminalForegroundService"
            daemonService?.startWatchdog(appContext.packageName, serviceClass)
            val json = JSONObject(rawReport)
            val ok = json.optBoolean("ok", false)
            if (!silent) {
                repository.appendLog(
                    if (ok) "INFO" else "WARN",
                    "Shizuku",
                    if (ok) "Doze 白名单、Standby Active、AppOps 放行与守护看门狗已全部启用"
                    else "部分加固命令返回非零: $rawReport"
                )
            }
        } catch (t: Throwable) {
            repository.appendLog("ERROR", "Shizuku", "执行 Shizuku 加固失败: ${t.message}")
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

        if (permGranted && daemonService != null) {
            try {
                val raw = daemonService?.queryHardeningStatus(appContext.packageName) ?: "{}"
                val json = JSONObject(raw)
                dozeWhitelisted = json.optBoolean("dozeWhitelisted", false)
                standbyActive = json.optBoolean("standbyActive", false)
                appOpsAllowed = json.optBoolean("appOpsAllowed", false)
                watchdogActive = json.optBoolean("watchdogActive", false)
                val bucket = json.optString("standbyBucket", "?")
                reportSummary = "Bucket=$bucket | Doze=${dozeWhitelisted} | AppOps=${appOpsAllowed}"
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
