package com.xusu.sshlinkterminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xusu.sshlinkterminal.LinkTerminalApp
import com.xusu.sshlinkterminal.MainActivity
import com.xusu.sshlinkterminal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TerminalForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var networkCallbackRegistered = false

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val app = application as LinkTerminalApp
            app.sshManager.onNetworkAvailable()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val app = application as LinkTerminalApp
        app.shizukuManager.setForegroundServiceRunning(true)
        acquireLocks()
        registerNetworkMonitor()
        startHeartbeatLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 14+ requires startForeground() synchronously on main thread
        startForeground(NOTIFICATION_ID, buildNotification())

        val app = application as LinkTerminalApp
        when (intent?.action) {
            ACTION_STOP_GUARDIAN -> {
                app.sshManager.disconnectAll()
                app.shizukuManager.stopWatchdog()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RECONNECT_ALL -> {
                app.sshManager.reconnectAll()
            }
            ACTION_RESTORE_PINNED, ACTION_WATCHDOG_REVIVE -> {
                app.repository.appendLog("INFO", "Daemon", "守护进程拉起并恢复常驻会话")
                app.sshManager.restorePinnedHosts()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        val app = application as LinkTerminalApp
        app.shizukuManager.setForegroundServiceRunning(false)
        heartbeatJob?.cancel()
        unregisterNetworkMonitor()
        releaseLocks()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            val app = application as LinkTerminalApp
            while (isActive) {
                app.shizukuManager.sendHeartbeat()
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification())
                delay(15_000L)
            }
        }
    }

    private fun acquireLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LinkTerm::SshKeepAliveLock").apply {
                setReferenceCounted(false)
                acquire()
            }
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wm.createWifiLock(mode, "LinkTerm::WifiKeepAlive").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {
        }
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Throwable) {
        }
    }

    private fun registerNetworkMonitor() {
        if (networkCallbackRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            networkCallbackRegistered = true
        } catch (_: Throwable) {
        }
    }

    private fun unregisterNetworkMonitor() {
        if (!networkCallbackRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            networkCallbackRegistered = false
        } catch (_: Throwable) {
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val app = application as LinkTerminalApp
        val activeCount = app.sshManager.activeConnectedCount()
        val avgRtt = app.sshManager.averageRttMs()
        val guard = app.shizukuManager.guardStatus.value

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reconnectIntent = Intent(this, TerminalForegroundService::class.java).apply {
            action = ACTION_RECONNECT_ALL
        }
        val reconnectPending = PendingIntent.getService(
            this,
            11,
            reconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TerminalForegroundService::class.java).apply {
            action = ACTION_STOP_GUARDIAN
        }
        val stopPending = PendingIntent.getService(
            this,
            12,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (activeCount > 0) {
            "SSH Link 守护中 · $activeCount 个活跃会话"
        } else {
            "SSH Link 守护待命"
        }
        val detail = buildString {
            append(if (avgRtt != null) "心跳 ${avgRtt}ms" else "等待连接")
            append(if (guard.watchdogActive) " · Shizuku 守护已武装" else " · 前台保活")
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_terminal_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.action_reconnect_all), reconnectPending)
            .addAction(0, getString(R.string.action_stop_guardian), stopPending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ssh_link_guardian_channel"
        private const val NOTIFICATION_ID = 2207
        const val ACTION_START_GUARDIAN = "ACTION_START_GUARDIAN"
        const val ACTION_STOP_GUARDIAN = "ACTION_STOP_GUARDIAN"
        const val ACTION_RECONNECT_ALL = "ACTION_RECONNECT_ALL"
        const val ACTION_RESTORE_PINNED = "ACTION_RESTORE_PINNED"
        const val ACTION_WATCHDOG_REVIVE = "ACTION_WATCHDOG_REVIVE"

        fun ensureStarted(context: Context, action: String = ACTION_START_GUARDIAN) {
            val intent = Intent(context, TerminalForegroundService::class.java).apply {
                this.action = action
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val app = context.applicationContext as? LinkTerminalApp ?: return
            if (app.repository.uiPreferences.value.autoStartOnBoot &&
                app.repository.hosts.value.any { it.pinInDaemon }
            ) {
                TerminalForegroundService.ensureStarted(
                    context,
                    TerminalForegroundService.ACTION_RESTORE_PINNED
                )
            }
        }
    }
}
