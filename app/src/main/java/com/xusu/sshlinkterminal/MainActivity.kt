package com.xusu.sshlinkterminal

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xusu.sshlinkterminal.data.ColorBag
import com.xusu.sshlinkterminal.data.MacroButton
import com.xusu.sshlinkterminal.data.SessionState
import com.xusu.sshlinkterminal.service.TerminalForegroundService
import com.xusu.sshlinkterminal.ui.components.GuardStatusOverlay
import com.xusu.sshlinkterminal.ui.glass.NarrowRailWidth
import com.xusu.sshlinkterminal.ui.glass.ResponsiveSideMenuLayout
import com.xusu.sshlinkterminal.ui.glass.SideMenuItem
import com.xusu.sshlinkterminal.ui.glass.rememberSideMenuState
import com.xusu.sshlinkterminal.ui.screens.AppearanceSettingsScreen
import com.xusu.sshlinkterminal.ui.screens.DashboardScreen
import com.xusu.sshlinkterminal.ui.screens.KeyVaultScreen
import com.xusu.sshlinkterminal.ui.screens.LogsAndAboutScreen
import com.xusu.sshlinkterminal.ui.screens.MacroEditorScreen
import com.xusu.sshlinkterminal.ui.screens.TerminalScreen
import com.xusu.sshlinkterminal.ui.theme.LinkTerminalTheme
import com.xusu.sshlinkterminal.ui.theme.LoadedHeroWallpaper
import com.xusu.sshlinkterminal.ui.theme.WallpaperControlsTheme
import com.xusu.sshlinkterminal.ui.theme.loadHeroWallpaperAndPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Routes {
    const val DASHBOARD = "dashboard"
    const val TERMINAL = "terminal"
    const val KEYS = "keys"
    const val MACROS = "macros"
    const val SETTINGS = "settings"
    const val LOGS = "logs"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LinkTerminalApp

        setContent {
            val uiPrefs by app.repository.uiPreferences.collectAsState()
            val hosts by app.repository.hosts.collectAsState()
            val keys by app.repository.keys.collectAsState()
            val macros by app.repository.macros.collectAsState()
            val sessions by app.sshManager.snapshots.collectAsState()
            val activeHostId by app.sshManager.activeHostId.collectAsState()
            val rttSamples by app.repository.rttSamples.collectAsState()
            val guardStatus by app.shizukuManager.guardStatus.collectAsState()
            val logs by app.repository.logs.collectAsState()

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            var loadedHero by remember { mutableStateOf<LoadedHeroWallpaper?>(null) }
            LaunchedEffect(uiPrefs.wallpaperUri, uiPrefs.useM3Color, uiPrefs.accentDepth) {
                loadedHero = loadHeroWallpaperAndPalette(
                    context = context,
                    wallpaperUri = uiPrefs.wallpaperUri,
                    useM3Color = uiPrefs.useM3Color,
                    accentDepth = uiPrefs.accentDepth
                )
            }
            val colorBag = loadedHero?.colorBag ?: ColorBag.Default.copy(useM3Color = uiPrefs.useM3Color)

            var showGuardCard by remember { mutableStateOf(false) }
            var editingMacroFromTerminal by remember { mutableStateOf<MacroButton?>(null) }

            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                scope.launch { app.shizukuManager.refreshStatus() }
            }

            val wallpaperPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                    }
                    app.repository.updateUiPreferences { it.copy(wallpaperUri = uri.toString(), showWallpaper = true) }
                }
            }

            val fontPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val imported = withContext(Dispatchers.IO) {
                            importTerminalFontFile(context, uri)
                        }
                        if (imported != null) {
                            val (destPath, displayName) = imported
                            app.repository.updateUiPreferences {
                                it.copy(
                                    terminalUseMonospace = true,
                                    terminalFontPreset = "CUSTOM",
                                    terminalCustomFontPath = destPath,
                                    terminalCustomFontName = displayName
                                )
                            }
                            Toast.makeText(context, "已载入字体: $displayName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "无法解析所选字体（请选 .ttf / .otf）", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            var isResumed by remember { mutableStateOf(false) }
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            isResumed = true
                            scope.launch { app.shizukuManager.refreshStatus() }
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            isResumed = false
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // First-run proactive Shizuku permission prompt (only once when Shizuku is running & ungranted)
            LaunchedEffect(isResumed, guardStatus.shizukuRunning, guardStatus.shizukuPermissionGranted) {
                if (isResumed &&
                    guardStatus.shizukuRunning &&
                    !guardStatus.shizukuPermissionGranted &&
                    !app.repository.hasPromptedShizukuPermission()
                ) {
                    delay(450L)
                    if (!app.shizukuManager.isShizukuPermissionGranted() &&
                        !app.repository.hasPromptedShizukuPermission()
                    ) {
                        app.repository.markShizukuPermissionPrompted()
                        app.shizukuManager.requestShizukuPermission()
                    }
                }
            }

            fun requestShizukuWithFeedback() {
                scope.launch {
                    val status = app.shizukuManager.refreshStatus()
                    when {
                        !status.shizukuInstalled -> {
                            Toast.makeText(
                                context,
                                "未检测到 Shizuku App（不使用也可正常用 SSH）",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        !status.shizukuRunning -> {
                            Toast.makeText(
                                context,
                                "Shizuku 服务尚未启动，请先在 Shizuku 中启动服务",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        status.shizukuPermissionGranted -> {
                            Toast.makeText(context, "Shizuku 已获得授权", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            app.repository.markShizukuPermissionPrompted()
                            val launched = app.shizukuManager.requestShizukuPermission()
                            if (!launched) {
                                Toast.makeText(
                                    context,
                                    "无法拉起 Shizuku 授权框，请检查 Shizuku 状态",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

            fun applyShizukuHardeningWithFeedback() {
                scope.launch {
                    val result = app.shizukuManager.applyHardeningAndWatchdog(silent = false)
                    val ok = result.dozeWhitelisted == true &&
                        result.appOpsBackgroundAllowed == true &&
                        result.watchdogActive
                    val msg = when {
                        ok -> "加固生效: ${result.lastReport}"
                        result.lastReport.isNotBlank() -> result.lastReport
                        else -> "Shizuku 加固未完成，请确认 Shizuku 权限与服务状态"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }

            val (timeText, batteryPct, charging) = rememberClockAndBattery(context)

            fun openBatteryWhitelistSettings() {
                try {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }

            fun openAppDetailSettings() {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }

            LinkTerminalTheme(darkTheme = true, dynamicColor = uiPrefs.useM3Color) {
                WallpaperControlsTheme(colorBag = colorBag) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Routes.DASHBOARD) {
                            val sideMenuState = rememberSideMenuState()

                            val drawerItems = remember {
                                listOf(
                                    SideMenuItem(Routes.TERMINAL, "假终端工作台", "外置快捷键与 OAuth 桥接", Icons.Rounded.Terminal),
                                    SideMenuItem(Routes.KEYS, "SSH 密钥库", "生成 Ed25519 与公钥部署", Icons.Rounded.Key),
                                    SideMenuItem(Routes.MACROS, "外置按钮定制", "管理命令行与彩色宏胶囊", Icons.Rounded.Tune),
                                    SideMenuItem(Routes.SETTINGS, "外观与壁纸实验室", "Haze 模糊、取色与启动策略", Icons.Rounded.Palette),
                                    SideMenuItem(Routes.LOGS, "守护日志与更新", "诊断记录与 GitLab 版本检查", Icons.Rounded.Description)
                                )
                            }

                            ResponsiveSideMenuLayout(
                                state = sideMenuState,
                                railPadding = NarrowRailWidth,
                                blurRadius = uiPrefs.blurRadius,
                                colorBag = colorBag,
                                springAnimation = uiPrefs.springAnimation,
                                showWallpaper = uiPrefs.showWallpaper,
                                onNavigate = { route -> navController.navigate(route) },
                                drawerHeader = {
                                    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                                        Text(
                                            "SSH Link Terminal",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            "Shizuku 守护 · 防断连 · 链接直跳",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                },
                                drawerItems = drawerItems,
                                mainContent = { startPadding ->
                                    DashboardScreen(
                                        heroBitmap = loadedHero?.imageBitmap,
                                        colorBag = colorBag,
                                        uiPreferences = uiPrefs,
                                        timeText = timeText,
                                        batteryProgress = batteryPct,
                                        isCharging = charging,
                                        guardStatus = guardStatus,
                                        hosts = hosts,
                                        keys = keys,
                                        sessions = sessions,
                                        rttSamples = rttSamples,
                                        onToggleMenu = sideMenuState::toggle,
                                        onToggleGlass = {
                                            app.repository.updateUiPreferences { it.copy(showWallpaper = !it.showWallpaper) }
                                        },
                                        onOpenWallpaperPicker = { wallpaperPicker.launch(arrayOf("image/*")) },
                                        onOpenGuardCard = {
                                            scope.launch { app.shizukuManager.refreshStatus() }
                                            showGuardCard = true
                                        },
                                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                        onOpenTerminalScreen = { hostId ->
                                            val target = hostId?.let { id -> hosts.firstOrNull { it.id == id } } ?: hosts.firstOrNull()
                                            if (target != null && sessions[target.id] == null) {
                                                app.sshManager.openAndConnect(target)
                                            } else if (target != null) {
                                                app.sshManager.selectHost(target.id)
                                            }
                                            navController.navigate(Routes.TERMINAL)
                                        },
                                        onConnectHost = { host -> app.sshManager.openAndConnect(host) },
                                        onDisconnectHost = { hostId -> app.sshManager.disconnectHost(hostId) },
                                        onToggleGuardianAll = {
                                            val hasRunning = guardStatus.foregroundServiceRunning || sessions.values.any {
                                                it.state == SessionState.CONNECTED ||
                                                    it.state == SessionState.CONNECTING ||
                                                    it.state == SessionState.RECONNECTING ||
                                                    it.state == SessionState.AWAITING_OAUTH ||
                                                    it.state == SessionState.AWAITING_INPUT
                                            }
                                            if (hasRunning) {
                                                app.sshManager.disconnectAll()
                                                app.shizukuManager.stopWatchdog()
                                                context.stopService(Intent(context, TerminalForegroundService::class.java))
                                            } else {
                                                TerminalForegroundService.ensureStarted(
                                                    context,
                                                    TerminalForegroundService.ACTION_START_GUARDIAN
                                                )
                                                app.sshManager.connectAll()
                                                if (app.shizukuManager.isShizukuPermissionGranted() && uiPrefs.autoHardenWithShizuku) {
                                                    app.shizukuManager.bindDaemonService()
                                                }
                                            }
                                        },
                                        onSaveHost = { host ->
                                            app.repository.upsertHost(host)
                                            app.sshManager.getOrCreateController(host)
                                        },
                                        onDeleteHost = { hostId ->
                                            app.sshManager.removeHostController(hostId)
                                            app.repository.deleteHost(hostId)
                                        },
                                        modifier = Modifier.padding(start = (startPadding - NarrowRailWidth).coerceAtLeast(0.dp))
                                    )
                                },
                                detailContent = { route, _, onBackDetail ->
                                    when (route) {
                                        Routes.KEYS -> KeyVaultScreen(
                                            keys = keys,
                                            onBack = onBackDetail,
                                            onAddKey = app.repository::addKey,
                                            onDeleteKey = app.repository::deleteKey
                                        )
                                        Routes.MACROS -> MacroEditorScreen(
                                            macros = macros,
                                            initialEditMacro = editingMacroFromTerminal,
                                            onBack = {
                                                editingMacroFromTerminal = null
                                                onBackDetail()
                                            },
                                            onSaveMacro = app.repository::upsertMacro,
                                            onDeleteMacro = app.repository::deleteMacro,
                                            onMoveMacro = app.repository::moveMacro
                                        )
                                        Routes.SETTINGS -> AppearanceSettingsScreen(
                                            prefs = uiPrefs,
                                            guardStatus = guardStatus,
                                            onBack = onBackDetail,
                                            onUpdatePrefs = app.repository::updateUiPreferences,
                                            onPickWallpaper = { wallpaperPicker.launch(arrayOf("image/*")) },
                                            onResetWallpaper = {
                                                app.repository.updateUiPreferences { it.copy(wallpaperUri = null) }
                                            },
                                            onPickTerminalFont = { fontPicker.launch(arrayOf("*/*")) },
                                            onRequestShizuku = ::requestShizukuWithFeedback,
                                            onApplyShizukuHarden = ::applyShizukuHardeningWithFeedback
                                        )
                                        Routes.LOGS -> LogsAndAboutScreen(
                                            logs = logs,
                                            onBack = onBackDetail,
                                            onClearLogs = app.repository::clearLogs
                                        )
                                        else -> {}
                                    }
                                }
                            )

                            GuardStatusOverlay(
                                visible = showGuardCard,
                                guardStatus = guardStatus,
                                colorBag = colorBag,
                                onDismiss = { showGuardCard = false },
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestBatteryWhitelist = { openBatteryWhitelistSettings() },
                                onRequestShizuku = ::requestShizukuWithFeedback,
                                onApplyShizukuHarden = ::applyShizukuHardeningWithFeedback,
                                onOpenAppSettings = { openAppDetailSettings() }
                            )
                        }

                        composable(Routes.TERMINAL) {
                            TerminalScreen(
                                heroBitmap = loadedHero?.imageBitmap,
                                colorBag = colorBag,
                                uiPreferences = uiPrefs,
                                hosts = hosts,
                                keys = keys,
                                activeHostId = activeHostId,
                                sessions = sessions,
                                macros = macros,
                                onBack = { navController.popBackStack() },
                                onSelectHost = { host ->
                                    app.sshManager.selectHost(host.id)
                                    if (sessions[host.id] == null) {
                                        app.sshManager.openAndConnect(host)
                                    }
                                },
                                onReconnectHost = { host ->
                                    app.sshManager.getOrCreateController(host).connect(forceRestart = true)
                                },
                                onSendText = { hostId, text ->
                                    hosts.firstOrNull { it.id == hostId }?.let { host ->
                                        app.sshManager.getOrCreateController(host).sendText(text)
                                    }
                                },
                                onResizePty = { hostId, cols, rows ->
                                    hosts.firstOrNull { it.id == hostId }?.let { host ->
                                        app.sshManager.getOrCreateController(host).resizePty(cols, rows)
                                    }
                                },
                                onClearTerminal = { hostId ->
                                    hosts.firstOrNull { it.id == hostId }?.let { host ->
                                        app.sshManager.getOrCreateController(host).clearTerminal()
                                    }
                                },
                                onDismissAuthLink = { hostId ->
                                    hosts.firstOrNull { it.id == hostId }?.let { host ->
                                        app.sshManager.getOrCreateController(host).dismissAuthLink()
                                    }
                                },
                                onSubmitInteractivePrompt = { hostId, answers ->
                                    hosts.firstOrNull { it.id == hostId }?.let { host ->
                                        app.sshManager.getOrCreateController(host).submitInteractivePrompt(answers)
                                    }
                                },
                                onDeployPublicKey = { hostId, pubKey ->
                                    hosts.firstOrNull { it.id == hostId }?.let { host ->
                                        app.sshManager.getOrCreateController(host).deployKey(pubKey) { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onFontScaleChange = { newSp ->
                                    app.repository.updateUiPreferences { it.copy(terminalFontSizeSp = newSp) }
                                },
                                onOpenMacroEditor = {
                                    editingMacroFromTerminal = null
                                    navController.navigate(Routes.MACROS)
                                },
                                onEditMacro = { macro ->
                                    editingMacroFromTerminal = macro
                                    navController.navigate(Routes.MACROS)
                                }
                            )
                        }

                        composable(Routes.KEYS) {
                            KeyVaultScreen(
                                keys = keys,
                                onBack = { navController.popBackStack() },
                                onAddKey = app.repository::addKey,
                                onDeleteKey = app.repository::deleteKey
                            )
                        }

                        composable(Routes.MACROS) {
                            MacroEditorScreen(
                                macros = macros,
                                initialEditMacro = editingMacroFromTerminal,
                                onBack = {
                                    editingMacroFromTerminal = null
                                    navController.popBackStack()
                                },
                                onSaveMacro = app.repository::upsertMacro,
                                onDeleteMacro = app.repository::deleteMacro,
                                onMoveMacro = app.repository::moveMacro
                            )
                        }

                        composable(Routes.SETTINGS) {
                            AppearanceSettingsScreen(
                                prefs = uiPrefs,
                                guardStatus = guardStatus,
                                onBack = { navController.popBackStack() },
                                onUpdatePrefs = app.repository::updateUiPreferences,
                                onPickWallpaper = { wallpaperPicker.launch(arrayOf("image/*")) },
                                onResetWallpaper = {
                                    app.repository.updateUiPreferences { it.copy(wallpaperUri = null) }
                                },
                                onPickTerminalFont = { fontPicker.launch(arrayOf("*/*")) },
                                onRequestShizuku = ::requestShizukuWithFeedback,
                                onApplyShizukuHarden = ::applyShizukuHardeningWithFeedback
                            )
                        }

                        composable(Routes.LOGS) {
                            LogsAndAboutScreen(
                                logs = logs,
                                onBack = { navController.popBackStack() },
                                onClearLogs = app.repository::clearLogs
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberClockAndBattery(context: Context): Triple<String, Float, Boolean> {
    var timeText by remember { mutableStateOf("00:00") }
    var batteryPct by remember { mutableFloatStateOf(1f) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            timeText = fmt.format(Date())
            delay(15_000L)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) {
                    batteryPct = level.toFloat() / scale.toFloat()
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Throwable) {
            }
        }
    }

    return Triple(timeText, batteryPct, isCharging)
}

private fun importTerminalFontFile(context: Context, uri: Uri): Pair<String, String>? {
    val resolver = context.contentResolver
    var rawName = "custom_font.ttf"
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) {
                rawName = cursor.getString(idx).orEmpty().ifBlank { rawName }
            }
        }
    }
    val safeName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val dir = File(context.filesDir, "fonts").apply { mkdirs() }
    val dest = File(dir, "terminal_$safeName")
    val copied = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.length() > 16L
    }.getOrDefault(false)
    if (!copied) {
        dest.delete()
        return null
    }

    // Validate TTF / OTF / TTC SFNT header magic bytes before accepting the file.
    val validHeader = runCatching {
        val header = ByteArray(4)
        dest.inputStream().use { it.read(header) }
        val tag = String(header, Charsets.US_ASCII)
        val isSfntTrueType = header[0] == 0.toByte() && header[1] == 1.toByte() && header[2] == 0.toByte() && header[3] == 0.toByte()
        isSfntTrueType || tag == "OTTO" || tag == "ttcf" || tag == "true" || tag == "typ1"
    }.getOrDefault(false)
    if (!validHeader) {
        dest.delete()
        return null
    }

    val loaded = runCatching { Typeface.createFromFile(dest) }.getOrNull()
    if (loaded == null) {
        dest.delete()
        return null
    }
    return dest.absolutePath to rawName
}
