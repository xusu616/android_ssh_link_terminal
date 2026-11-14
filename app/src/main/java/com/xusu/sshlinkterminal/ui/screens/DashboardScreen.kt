package com.xusu.sshlinkterminal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xusu.sshlinkterminal.crypto.VaultCrypto
import com.xusu.sshlinkterminal.data.AuthType
import com.xusu.sshlinkterminal.data.ChartWindowRule
import com.xusu.sshlinkterminal.data.ColorBag
import com.xusu.sshlinkterminal.data.GuardStatus
import com.xusu.sshlinkterminal.data.HostProfile
import com.xusu.sshlinkterminal.data.RttSample
import com.xusu.sshlinkterminal.data.SessionState
import com.xusu.sshlinkterminal.data.SshKeyRecord
import com.xusu.sshlinkterminal.data.UiPreferences
import com.xusu.sshlinkterminal.ssh.SessionUiSnapshot
import com.xusu.sshlinkterminal.ui.chart.SshLatencyChart
import com.xusu.sshlinkterminal.ui.glass.GuardianControlButton
import com.xusu.sshlinkterminal.ui.glass.NarrowGlassRail
import com.xusu.sshlinkterminal.ui.glass.NarrowRailWidth
import com.xusu.sshlinkterminal.ui.glass.ThreeStateWindowSwitch
import com.xusu.sshlinkterminal.ui.glass.glassHaze
import com.xusu.sshlinkterminal.ui.theme.contrastOnColor
import com.xusu.sshlinkterminal.ui.theme.getDeviceCornerRadius
import com.xusu.sshlinkterminal.ui.theme.getDisplayCutoutTopPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.util.Locale

@Composable
fun DashboardScreen(
    heroBitmap: ImageBitmap?,
    colorBag: ColorBag,
    uiPreferences: UiPreferences,
    timeText: String,
    batteryProgress: Float,
    isCharging: Boolean,
    guardStatus: GuardStatus,
    hosts: List<HostProfile>,
    keys: List<SshKeyRecord>,
    sessions: Map<String, SessionUiSnapshot>,
    rttSamples: List<RttSample>,
    onToggleMenu: () -> Unit,
    onToggleGlass: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onOpenGuardCard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTerminalScreen: (String?) -> Unit,
    onConnectHost: (HostProfile) -> Unit,
    onDisconnectHost: (String) -> Unit,
    onToggleGuardianAll: () -> Unit,
    onSaveHost: (HostProfile) -> Unit,
    onDeleteHost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = rememberHazeState()
    var chartWindow by remember { mutableStateOf(ChartWindowRule.MINUTE) }
    var editingHost by remember { mutableStateOf<HostProfile?>(null) }
    var showCreateHost by remember { mutableStateOf(false) }

    val activeSessionsCount = sessions.values.count {
        it.state == SessionState.CONNECTED || it.state == SessionState.AWAITING_OAUTH
    }
    val hasRunningConnections = sessions.values.any {
        it.state == SessionState.CONNECTED ||
            it.state == SessionState.CONNECTING ||
            it.state == SessionState.RECONNECTING ||
            it.state == SessionState.AWAITING_OAUTH ||
            it.state == SessionState.AWAITING_INPUT
    }
    val guardHealthy = guardStatus.notificationsGranted &&
        guardStatus.batteryOptimizationIgnored &&
        guardStatus.shizukuPermissionGranted
    val accentColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant
    val outlineAccent = if (!colorBag.useM3Color && accentColor.luminance() < 0.14f) {
        colorBag.lightMuted
    } else {
        accentColor
    }
    val secondaryColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.secondary else colorBag.muted
    val guardianActive = guardStatus.foregroundServiceRunning || hasRunningConnections

    Box(modifier = modifier.fillMaxSize()) {
        // Full-bleed Wallpaper Background for Glass Effect
        if (uiPreferences.showWallpaper && heroBitmap != null) {
            Image(
                bitmap = heroBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.25f),
                            0.45f to Color(0xFF090D16).copy(alpha = 0.72f),
                            1f to Color(0xFF090D16).copy(alpha = 0.94f)
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // Main Foreground Content (Narrow Rail + Scrollable Hero & Cards)
        Row(Modifier.fillMaxSize()) {
            NarrowGlassRail(
                timeText = timeText,
                batteryProgress = batteryProgress,
                isCharging = isCharging,
                glassEnabled = uiPreferences.showWallpaper,
                guardHealthy = guardHealthy,
                colorBag = colorBag,
                onToggleMenu = onToggleMenu,
                onToggleGlass = onToggleGlass,
                onOpenTerminal = { onOpenTerminalScreen(null) },
                onOpenGuardCard = onOpenGuardCard,
                onOpenSettings = onOpenSettings
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val cornerRadius = getDeviceCornerRadius()
                val cutoutTop = if (uiPreferences.topComponentPressedDown) getDisplayCutoutTopPadding() else 8.dp
                val isWideLandscape = maxWidth > maxHeight && maxHeight < 580.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(end = 12.dp, bottom = 84.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(cutoutTop.coerceAtLeast(8.dp)))

                    // Hero Banner Card (Long-press to change wallpaper, inspired by shizuku-ftp)
                    if (!isWideLandscape) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(188.dp)
                                .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius, bottomStart = 24.dp, bottomEnd = 24.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = { onOpenWallpaperPicker() })
                                }
                        ) {
                            if (heroBitmap != null) {
                                Image(
                                    bitmap = heroBitmap,
                                    contentDescription = "长按更换壁纸",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.55f to Color(0xFF090D16).copy(alpha = 0.28f),
                                            1f to Color(0xFF090D16).copy(alpha = 0.88f)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = (if (guardHealthy) Color(0xFF4ADE80) else outlineAccent).copy(alpha = 0.20f),
                                        border = BorderStroke(1.dp, (if (guardHealthy) Color(0xFF4ADE80) else outlineAccent).copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = if (guardStatus.watchdogActive) "SHIZUKU 守护已武装" else "LINK TERMINAL",
                                            color = if (guardHealthy) Color(0xFF86EFAC) else outlineAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (activeSessionsCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF38BDF8).copy(alpha = 0.22f)
                                        ) {
                                            Text(
                                                text = "$activeSessionsCount 会话在线",
                                                color = Color(0xFF7DD3FC),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "SSH Link Terminal",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "长按更换壁纸 · 自动捕捉 OAuth / Tailscale 链接",
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Guardian Master Control + RTT Monitor Card
                    FrostedGlassCard(
                        hazeState = hazeState,
                        blurRadius = uiPreferences.blurRadius,
                        useGlass = uiPreferences.showWallpaper
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "连接心跳与延迟监控",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val latestRtt = rttSamples.lastOrNull()?.rttMs
                                    val avgRtt = if (rttSamples.isNotEmpty()) {
                                        rttSamples.map { it.rttMs }.average()
                                    } else {
                                        null
                                    }
                                    Text(
                                        text = if (latestRtt != null && avgRtt != null) {
                                            "实时 ${latestRtt}ms · 均值 ${String.format(Locale.US, "%.1f", avgRtt)}ms"
                                        } else {
                                            "开启连接后实时采集 SSH 心跳"
                                        },
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                ThreeStateWindowSwitch(
                                    state = chartWindow,
                                    onStateChange = { chartWindow = it },
                                    colorBag = colorBag
                                )
                            }

                            SshLatencyChart(
                                samples = rttSamples,
                                windowRule = chartWindow,
                                accentColor = outlineAccent,
                                secondaryColor = secondaryColor,
                                animateAxisSwitch = uiPreferences.chartAxisAnimation,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GuardianControlButton(
                                    isRunning = guardianActive,
                                    activeLabel = "断开所有连接",
                                    inactiveLabel = "开启所有连接",
                                    accentColor = outlineAccent,
                                    onClick = onToggleGuardianAll,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = { onOpenTerminalScreen(null) },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("终端", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    // Host Profiles Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SSH 主机列表 (${hosts.size})",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showCreateHost = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = outlineAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("新增主机", color = outlineAccent)
                        }
                    }

                    hosts.forEach { host ->
                        val snap = sessions[host.id]
                        HostProfileCard(
                            host = host,
                            snapshot = snap,
                            accentColor = accentColor,
                            hazeState = hazeState,
                            blurRadius = uiPreferences.blurRadius,
                            useGlass = uiPreferences.showWallpaper,
                            onOpenTerminal = {
                                onConnectHost(host)
                                onOpenTerminalScreen(host.id)
                            },
                            onDisconnect = { onDisconnectHost(host.id) },
                            onEdit = { editingHost = host },
                            onDelete = { onDeleteHost(host.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateHost = true },
            containerColor = accentColor,
            contentColor = contrastOnColor(accentColor),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "添加主机")
        }
    }

    if (showCreateHost || editingHost != null) {
        HostEditorDialog(
            initial = editingHost,
            keys = keys,
            onDismiss = {
                showCreateHost = false
                editingHost = null
            },
            onSave = { saved ->
                onSaveHost(saved)
                showCreateHost = false
                editingHost = null
            }
        )
    }
}

@Composable
private fun FrostedGlassCard(
    hazeState: HazeState,
    blurRadius: Float,
    useGlass: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (useGlass) {
                    Modifier.glassHaze(
                        hazeState = hazeState,
                        blurIntensity = blurRadius,
                        darkTint = Color(0xFF0F172A).copy(alpha = 0.56f),
                        darkFallback = Color(0xFF0F172A).copy(alpha = 0.85f)
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                }
            )
    ) {
        content()
    }
}

@Composable
private fun HostProfileCard(
    host: HostProfile,
    snapshot: SessionUiSnapshot?,
    accentColor: Color,
    hazeState: HazeState,
    blurRadius: Float,
    useGlass: Boolean,
    onOpenTerminal: () -> Unit,
    onDisconnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val state = snapshot?.state ?: SessionState.IDLE
    val isLive = state == SessionState.CONNECTED || state == SessionState.AWAITING_OAUTH
    val dotColor by animateColorAsState(
        targetValue = when (state) {
            SessionState.CONNECTED -> Color(0xFF4ADE80)
            SessionState.AWAITING_OAUTH, SessionState.AWAITING_INPUT -> Color(0xFF38BDF8)
            SessionState.CONNECTING, SessionState.RECONNECTING -> Color(0xFFFBBF24)
            SessionState.ERROR -> Color(0xFFF87171)
            else -> Color(0xFF64748B)
        },
        label = "hostStateDot"
    )

    FrostedGlassCard(
        hazeState = hazeState,
        blurRadius = blurRadius,
        useGlass = useGlass,
        modifier = Modifier.clickable(onClick = onOpenTerminal)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = host.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${host.username}@${host.host}:${host.port}",
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = host.tag,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = snapshot?.statusDetail ?: buildString {
                        append(
                            when (host.authType) {
                                AuthType.PASSWORD -> "已存加密密码"
                                AuthType.PUBLIC_KEY -> "Ed25519/私钥"
                                AuthType.INTERACTIVE_OR_TAILSCALE -> "Tailscale/交互"
                            }
                        )
                        append(" · 心跳 ${host.keepAliveIntervalSec}s")
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    if (isLive || state == SessionState.RECONNECTING || state == SessionState.CONNECTING) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.7f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("断开", color = Color(0xFFE57373), fontSize = 12.sp)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onOpenTerminal,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("连接", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEditorDialog(
    initial: HostProfile?,
    keys: List<SshKeyRecord>,
    onDismiss: () -> Unit,
    onSave: (HostProfile) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var portText by remember { mutableStateOf((initial?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(initial?.username ?: "root") }
    var authType by remember { mutableStateOf(initial?.authType ?: AuthType.PASSWORD) }
    var password by remember { mutableStateOf(VaultCrypto.decrypt(initial?.encryptedPassword)) }
    var selectedKeyId by remember { mutableStateOf(initial?.keyId ?: keys.firstOrNull()?.id) }
    var autoAttach by remember { mutableStateOf(initial?.autoAttachCommand ?: "") }
    var keepAliveSec by remember { mutableStateOf((initial?.keepAliveIntervalSec ?: 15).toString()) }
    var autoReconnect by remember { mutableStateOf(initial?.autoReconnect ?: true) }
    var pinInDaemon by remember { mutableStateOf(initial?.pinInDaemon ?: true) }
    var tag by remember { mutableStateOf(initial?.tag ?: "LAN") }
    var keyMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建 SSH 主机" else "编辑 SSH 主机") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("别名（如：家用服务器）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("主机 IP / Tailscale 域名") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter(Char::isDigit) },
                        label = { Text("端口") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("标签", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("LAN", "Tailscale", "Prod", "Lab").forEach { option ->
                        FilterChip(
                            selected = tag == option,
                            onClick = { tag = option },
                            label = { Text(option) }
                        )
                    }
                }

                Text("认证方式", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = authType == AuthType.PASSWORD,
                        onClick = { authType = AuthType.PASSWORD },
                        label = { Text("加密保存密码") }
                    )
                    FilterChip(
                        selected = authType == AuthType.PUBLIC_KEY,
                        onClick = { authType = AuthType.PUBLIC_KEY },
                        label = { Text("SSH 私钥") }
                    )
                    FilterChip(
                        selected = authType == AuthType.INTERACTIVE_OR_TAILSCALE,
                        onClick = { authType = AuthType.INTERACTIVE_OR_TAILSCALE },
                        label = { Text("Tailscale / 交互") }
                    )
                }

                if (authType != AuthType.PUBLIC_KEY) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("登录密码（Keystore AES-GCM 加密保存）") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (authType == AuthType.PUBLIC_KEY) {
                    if (keys.isEmpty()) {
                        Text(
                            "尚未创建密钥，请先在侧栏「SSH 密钥库」生成 Ed25519 密钥",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = keyMenuExpanded,
                            onExpandedChange = { keyMenuExpanded = !keyMenuExpanded }
                        ) {
                            val selectedName = keys.firstOrNull { it.id == selectedKeyId }?.name ?: "选择私钥"
                            OutlinedTextField(
                                value = selectedName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("选择私钥") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = keyMenuExpanded,
                                onDismissRequest = { keyMenuExpanded = false }
                            ) {
                                keys.forEach { key ->
                                    DropdownMenuItem(
                                        text = { Text("${key.name} (${key.algorithm})") },
                                        onClick = {
                                            selectedKeyId = key.id
                                            keyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = autoAttach,
                    onValueChange = { autoAttach = it },
                    label = { Text("登录后自动附着命令（可选）") },
                    placeholder = { Text("留空则直接进入默认 Shell；Linux 可填 tmux") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = autoAttach.trim() == "tmux new-session -A -s main",
                        onClick = { autoAttach = "tmux new-session -A -s main" },
                        label = { Text("填入 Linux tmux") }
                    )
                    if (autoAttach.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = { autoAttach = "" },
                            label = { Text("清空") }
                        )
                    }
                }
                OutlinedTextField(
                    value = keepAliveSec,
                    onValueChange = { keepAliveSec = it.filter(Char::isDigit) },
                    label = { Text("KeepAlive 心跳间隔 (秒)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoReconnect, onCheckedChange = { autoReconnect = it })
                    Text("断线/切网自动重连", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pinInDaemon, onCheckedChange = { pinInDaemon = it })
                    Text("在守护进程/开机恢复时常驻保活", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            ButtonTextAction(
                text = "保存",
                enabled = host.isNotBlank() && username.isNotBlank(),
                onClick = {
                    val finalProfile = HostProfile(
                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim().ifBlank { host.trim() },
                        host = host.trim(),
                        port = (portText.toIntOrNull() ?: 22).coerceIn(1, 65535),
                        username = username.trim(),
                        authType = authType,
                        encryptedPassword = password.takeIf { it.isNotEmpty() }?.let { VaultCrypto.encrypt(it) },
                        keyId = if (authType == AuthType.PUBLIC_KEY) selectedKeyId else initial?.keyId,
                        autoAttachCommand = autoAttach.trim(),
                        keepAliveIntervalSec = (keepAliveSec.toIntOrNull() ?: 15).coerceIn(5, 120),
                        autoReconnect = autoReconnect,
                        pinInDaemon = pinInDaemon,
                        tag = tag
                    )
                    onSave(finalProfile)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ButtonTextAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) { Text(text) }
}
