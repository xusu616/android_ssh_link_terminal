package com.xusu.sshlinkterminal.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xusu.sshlinkterminal.data.ColorBag
import com.xusu.sshlinkterminal.data.HostProfile
import com.xusu.sshlinkterminal.data.MacroButton
import com.xusu.sshlinkterminal.data.SessionState
import com.xusu.sshlinkterminal.data.SshKeyRecord
import com.xusu.sshlinkterminal.data.UiPreferences
import com.xusu.sshlinkterminal.ssh.SessionUiSnapshot
import com.xusu.sshlinkterminal.terminal.AnsiEmulator
import com.xusu.sshlinkterminal.terminal.LinkDetector
import com.xusu.sshlinkterminal.terminal.NerdFontPacks
import com.xusu.sshlinkterminal.terminal.TerminalRenderLine
import com.xusu.sshlinkterminal.ui.components.ExternalButtonDeck
import com.xusu.sshlinkterminal.ui.glass.GlassSideMenuWidth
import com.xusu.sshlinkterminal.ui.glass.bouncySpringSpec
import com.xusu.sshlinkterminal.ui.glass.glassHaze
import com.xusu.sshlinkterminal.ui.theme.contrastOnColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil

private const val IME_SENTINEL = " "

@Composable
fun TerminalScreen(
    heroBitmap: ImageBitmap?,
    colorBag: ColorBag,
    uiPreferences: UiPreferences,
    hosts: List<HostProfile>,
    keys: List<SshKeyRecord>,
    activeHostId: String?,
    activeSessionId: String?,
    sessions: Map<String, SessionUiSnapshot>,
    macros: List<MacroButton>,
    onBack: () -> Unit,
    onSelectHost: (HostProfile) -> Unit,
    onSelectSession: (String) -> Unit,
    onNewSession: (HostProfile) -> Unit,
    onCloseSession: (String) -> Unit,
    onReconnectSession: (String, HostProfile) -> Unit,
    onSendText: (String, String) -> Unit,
    onResizePty: (String, Int, Int) -> Unit,
    onClearTerminal: (String) -> Unit,
    onDismissAuthLink: (String) -> Unit,
    onSubmitInteractivePrompt: (String, List<String>?) -> Unit,
    onDeployPublicKey: (String, String) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onOpenMacroEditor: () -> Unit,
    onEditMacro: (MacroButton) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val quickBarFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val activeSnapCandidate = activeSessionId?.let { sessions[it] }
    val currentHost = activeSnapCandidate?.let { snap -> hosts.firstOrNull { it.id == snap.hostId } }
        ?: hosts.firstOrNull { it.id == activeHostId }
        ?: hosts.firstOrNull()
    val hostSessions = remember(sessions, currentHost?.id) {
        val hid = currentHost?.id ?: return@remember emptyList()
        sessions.values.filter { it.hostId == hid }.sortedBy { it.sessionIndex }
    }
    val currentSnapshot = remember(activeSessionId, sessions, currentHost?.id, hostSessions) {
        activeSessionId?.let { sessions[it] }?.takeIf { currentHost == null || it.hostId == currentHost.id }
            ?: hostSessions.firstOrNull()
    }
    val currentSessionKey = currentSnapshot?.sessionId
    val activeActionKey = currentSessionKey
    val listState = remember(activeActionKey ?: currentHost?.id) { LazyListState() }

    val drawerHazeState = rememberHazeState()
    val drawerWidthPx = with(density) { GlassSideMenuWidth.toPx() }
    val edgeStripWidthPx = with(density) { 34.dp.toPx() }
    val drawerAnimatedFraction = remember { Animatable(0f) }
    var drawerDragFraction by remember { mutableStateOf<Float?>(null) }
    var edgeDrawerArmed by remember { mutableStateOf(false) }
    val effectiveDrawerFraction = drawerDragFraction ?: drawerAnimatedFraction.value
    val isDrawerOpen = effectiveDrawerFraction > 0.01f

    fun animateDrawerTo(target: Float, snapFrom: Float? = null) {
        drawerDragFraction = null
        val spec = if (target > 0.5f && uiPreferences.springAnimation) {
            bouncySpringSpec<Float>()
        } else {
            tween(220)
        }
        scope.launch {
            if (snapFrom != null) drawerAnimatedFraction.snapTo(snapFrom)
            drawerAnimatedFraction.animateTo(target, animationSpec = spec)
        }
    }

    var deckExpanded by remember { mutableStateOf(true) }
    var ctrlLocked by remember { mutableStateOf(false) }
    var altLocked by remember { mutableStateOf(false) }
    var showDeployKeyDialog by remember { mutableStateOf(false) }
    var quickCommandLine by remember { mutableStateOf("") }
    var showQuickBar by remember { mutableStateOf(false) }
    var selection by remember(activeActionKey) { mutableStateOf<TerminalSelection?>(null) }

    BackHandler(enabled = selection != null && !isDrawerOpen) {
        selection = null
    }
    BackHandler(enabled = isDrawerOpen) {
        animateDrawerTo(0f)
    }

    // Hidden capture text buffer with sentinel space so IME backspace, Chinese composition, and paste work reliably
    var imeValue by remember {
        mutableStateOf(TextFieldValue(text = IME_SENTINEL, selection = TextRange(IME_SENTINEL.length)))
    }

    fun resetImeBuffer() {
        imeValue = TextFieldValue(text = IME_SENTINEL, selection = TextRange(IME_SENTINEL.length))
    }

    fun focusTerminalIme() {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun copySelectionAndClear(sel: TerminalSelection, currentLines: List<TerminalRenderLine>) {
        val copied = extractSelectedText(currentLines, sel)
        if (copied.isNotEmpty()) {
            clipboard.setText(AnnotatedString(copied))
            Toast.makeText(context, "已复制 ${copied.length} 个字符", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "所选区域为空", Toast.LENGTH_SHORT).show()
        }
        selection = null
    }

    fun sendWithModifiers(raw: String) {
        val targetKey = activeActionKey ?: return
        if (raw.isEmpty()) return
        val normalized = raw.replace("\r\n", "\r").replace("\n", "\r")
        val transformed = if ((ctrlLocked || altLocked) && normalized.length == 1) {
            val ch = normalized[0]
            var result = normalized
            if (ctrlLocked) {
                val upper = ch.uppercaseChar()
                if (upper in '@'..'_' || ch in 'a'..'z') {
                    val code = upper.code - '@'.code
                    result = code.toChar().toString()
                }
                ctrlLocked = false
            }
            if (altLocked) {
                result = "\u001B$result"
                altLocked = false
            }
            result
        } else {
            normalized
        }
        onSendText(targetKey, transformed)
    }

    val rawAccent = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant
    val accentColor = if (!colorBag.useM3Color && rawAccent.luminance() < 0.14f) colorBag.lightMuted else rawAccent
    val lines = currentSnapshot?.terminalLines.orEmpty()
    val revision = currentSnapshot?.terminalRevision ?: 0L
    val authLink = currentSnapshot?.detectedAuthLink
    val metrics = remember(
        context,
        uiPreferences.terminalFontSizeSp,
        uiPreferences.terminalUseMonospace,
        uiPreferences.terminalFontPreset,
        uiPreferences.terminalCustomFontPath,
        uiPreferences.terminalNerdIconsEnabled,
        uiPreferences.terminalNerdIconsPath,
        density
    ) {
        measureTerminalMetrics(context, uiPreferences, density)
    }

    var hostMenuExpanded by remember { mutableStateOf(false) }

    var followBottom by remember(activeActionKey) { mutableStateOf(true) }
    LaunchedEffect(listState, activeActionKey) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()
            val atBottom = total == 0 || (lastVisible != null && lastVisible.index >= total - 1 &&
                lastVisible.offset + lastVisible.size <= info.viewportEndOffset + 12)
            Pair(listState.isScrollInProgress, atBottom)
        }.collect { (scrolling, atBottom) ->
            if (atBottom) {
                followBottom = true
            } else if (scrolling) {
                followBottom = false
            }
        }
    }

    LaunchedEffect(authLink?.url) {
        if (authLink != null) {
            followBottom = true
        }
    }

    var prevSnapshotHead by remember(activeActionKey) { mutableStateOf<String?>(null) }
    var prevLineCount by remember(activeActionKey) { mutableIntStateOf(0) }
    LaunchedEffect(revision, lines.size) {
        val curHead = lines.firstOrNull()?.plainText
        val cur = selection
        if (cur != null) {
            if (lines.isEmpty() || cur.end.lineIdx >= lines.size) {
                selection = null
            } else if (prevLineCount == lines.size && lines.size >= 1800 && prevSnapshotHead != null && curHead != prevSnapshotHead) {
                val shiftedAnchor = cur.anchor.lineIdx - 1
                val shiftedActive = cur.active.lineIdx - 1
                selection = if (minOf(shiftedAnchor, shiftedActive) < 0) {
                    null
                } else {
                    TerminalSelection(
                        anchor = cur.anchor.copy(lineIdx = shiftedAnchor),
                        active = cur.active.copy(lineIdx = shiftedActive)
                    )
                }
            }
        }
        prevSnapshotHead = curHead
        prevLineCount = lines.size
    }

    LaunchedEffect(activeActionKey, revision, lines.size) {
        if (lines.isNotEmpty() && followBottom) {
            val totalItems = listState.layoutInfo.totalItemsCount
            val visibleItems = listState.layoutInfo.visibleItemsInfo.size
            if (totalItems > visibleItems || visibleItems == 0) {
                listState.scrollToItem(lines.lastIndex, 4_096)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(drawerHazeState)
        ) {
            // Wallpaper Bleed-Through Backdrop
            if (uiPreferences.showWallpaper && heroBitmap != null) {
                Image(
                    bitmap = heroBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF060911).copy(alpha = uiPreferences.terminalScrimAlpha.coerceIn(0.45f, 0.96f)))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF070B12))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
            // Top Cyber-Glass Session Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1120).copy(alpha = 0.84f))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回主页", tint = Color.White)
                }

                Box(modifier = Modifier.weight(1f)) {
                    val activeSnap = currentSnapshot
                    val activeDotColor = when (activeSnap?.state) {
                        SessionState.CONNECTED -> Color(0xFF4ADE80)
                        SessionState.AWAITING_OAUTH, SessionState.AWAITING_INPUT -> Color(0xFF38BDF8)
                        SessionState.CONNECTING, SessionState.RECONNECTING -> Color(0xFFFBBF24)
                        SessionState.ERROR -> Color(0xFFF87171)
                        else -> Color(0xFF64748B)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hosts.isNotEmpty()) accentColor.copy(alpha = 0.22f) else Color(0xFF1E293B).copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (hosts.isNotEmpty()) accentColor else Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = hosts.isNotEmpty()) { hostMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(activeDotColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = currentHost?.name ?: "暂无主机",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (hostSessions.size > 1 && activeSnap != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.30f),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = "#${activeSnap.sessionIndex}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            activeSnap?.lastRttMs?.let { rtt ->
                                Spacer(Modifier.width(4.dp))
                                Text("${rtt}ms", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                            }
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "切换主机",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = hostMenuExpanded,
                        onDismissRequest = { hostMenuExpanded = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (currentHost != null) {
                            DropdownMenuItem(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                text = {
                                    Text(
                                        text = "新建同主机对话 (${currentHost.name})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    hostMenuExpanded = false
                                    onNewSession(currentHost)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "打开会话侧边栏",
                                        fontSize = 12.5.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Terminal,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(17.dp)
                                    )
                                },
                                onClick = {
                                    hostMenuExpanded = false
                                    animateDrawerTo(1f)
                                }
                            )
                            HorizontalDivider()
                        }
                        hosts.forEach { host ->
                            val snaps = sessions.values.filter { it.hostId == host.id }
                            val snap = snaps.firstOrNull {
                                it.state == SessionState.CONNECTED || it.state == SessionState.AWAITING_OAUTH
                            } ?: snaps.firstOrNull()
                            val selected = host.id == currentHost?.id
                            val dotColor = when (snap?.state) {
                                SessionState.CONNECTED -> Color(0xFF4ADE80)
                                SessionState.AWAITING_OAUTH, SessionState.AWAITING_INPUT -> Color(0xFF38BDF8)
                                SessionState.CONNECTING, SessionState.RECONNECTING -> Color(0xFFFBBF24)
                                SessionState.ERROR -> Color(0xFFF87171)
                                else -> Color(0xFF64748B)
                            }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = host.name,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (snaps.size > 1) {
                                                Spacer(Modifier.width(5.dp))
                                                Text("${snaps.size}会话", color = accentColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                            snap?.lastRttMs?.let { rtt ->
                                                Spacer(Modifier.width(6.dp))
                                                Text("${rtt}ms", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            }
                                        }
                                        Text(
                                            text = "${host.username}@${host.host}:${host.port}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    hostMenuExpanded = false
                                    onSelectHost(host)
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onFontScaleChange((uiPreferences.terminalFontSizeSp - 0.5f).coerceAtLeast(8f)) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.TextDecrease, contentDescription = "缩小字号", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onFontScaleChange((uiPreferences.terminalFontSizeSp + 0.5f).coerceAtMost(22f)) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.TextIncrease, contentDescription = "放大字号", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { showDeployKeyDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.VpnKey, contentDescription = "部署公钥", tint = accentColor, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        currentHost?.let { host ->
                            onReconnectSession(activeActionKey ?: host.id, host)
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "重连", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { currentSessionKey?.let(onClearTerminal) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = "清屏", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = ::focusTerminalIme,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.Keyboard, contentDescription = "唤起输入法", tint = accentColor, modifier = Modifier.size(19.dp))
                }
            }

            // Smart OAuth / Tailscale URL Detection Pill
            AnimatedVisibility(
                visible = authLink != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                if (authLink != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F293A).copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "检测到 ${authLink.providerLabel}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { currentSessionKey?.let(onDismissAuthLink) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("忽略", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = authLink.url,
                                color = Color(0xFF7DD3FC),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { LinkDetector.openUrlInBrowser(context, authLink.url) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF38BDF8),
                                        contentColor = Color(0xFF082F49)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("一键浏览器授权", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(authLink.url))
                                        Toast.makeText(context, "已复制授权链接", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFF7DD3FC), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("复制链接", color = Color(0xFF7DD3FC), fontSize = 11.5.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val pasted = clipboard.getText()?.text?.trim().orEmpty()
                                        if (pasted.isNotEmpty()) {
                                            quickCommandLine = pasted
                                            showQuickBar = true
                                            scope.launch {
                                                delay(80L)
                                                quickBarFocusRequester.requestFocus()
                                                keyboardController?.show()
                                            }
                                        } else {
                                            showQuickBar = true
                                            Toast.makeText(context, "剪贴板暂无内容，可直接在下方输入", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF5EEAD4).copy(alpha = 0.7f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = Color(0xFF5EEAD4), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("粘贴授权码", color = Color(0xFF5EEAD4), fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Terminal Output Viewport + Auto-PTY Sizing
            val exclusionBandHeightPx = with(density) { 180.dp.toPx() }
            val exclusionWidthPx = with(density) { 36.dp.toPx() }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .systemGestureExclusion { coords ->
                        val h = coords.size.height.toFloat()
                        val bandH = minOf(h, exclusionBandHeightPx)
                        val top = ((h - bandH) * 0.55f).coerceAtLeast(0f)
                        Rect(0f, top, exclusionWidthPx, top + bandH)
                    }
            ) {
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val horizontalPadPx = with(density) { 20.dp.toPx() }
                val verticalPadPx = with(density) { 18.dp.toPx() }
                val viewportCols = ((widthPx - horizontalPadPx).coerceAtLeast(metrics.cellWidthPx * 20f) / metrics.cellWidthPx)
                    .toInt().coerceIn(32, 200)
                val cols = if (uiPreferences.terminalHorizontalScroll) {
                    maxOf(viewportCols, 80)
                } else {
                    viewportCols
                }.coerceIn(32, 200)
                val rawRows = ((heightPx - verticalPadPx).coerceAtLeast(metrics.lineHeightPx * 8f) / metrics.lineHeightPx)
                    .toInt().coerceIn(10, 80)

                val isImeVisible = WindowInsets.ime.getBottom(density) > 0
                val hasOverlayBar = authLink != null || showQuickBar

                var lastSentCols by remember(currentSessionKey) { mutableIntStateOf(-1) }
                var lastSentRows by remember(currentSessionKey) { mutableIntStateOf(-1) }

                // Keep remote PTY rows stable while IME or OAuth banner opens/closes so CLIs (like agy) do not SIGWINCH-refresh.
                LaunchedEffect(currentSessionKey, cols, rawRows, isImeVisible, hasOverlayBar) {
                    val targetSessionId = currentSessionKey ?: return@LaunchedEffect
                    delay(180L)
                    val targetRows = when {
                        lastSentRows <= 0 -> rawRows
                        !isImeVisible && !hasOverlayBar -> rawRows
                        else -> lastSentRows
                    }
                    if (cols != lastSentCols || targetRows != lastSentRows) {
                        lastSentCols = cols
                        lastSentRows = targetRows
                        onResizePty(targetSessionId, cols, targetRows)
                    }
                }

                // Only adjust scroll when an OAuth banner, quick bar, or IME actually occludes the viewport.
                LaunchedEffect(heightPx.toInt(), authLink != null, showQuickBar, isImeVisible) {
                    if ((authLink != null || showQuickBar || isImeVisible) && lines.isNotEmpty() && followBottom) {
                        val contentPx = lines.size * metrics.lineHeightPx + verticalPadPx
                        if (contentPx > heightPx) {
                            listState.scrollToItem(lines.lastIndex, 4_096)
                        }
                    }
                }

                // Invisible IME input capture field (optionally forces Termux-like TYPE_NULL raw input)
                @OptIn(ExperimentalComposeUiApi::class)
                val terminalInputInterceptor = remember(uiPreferences.terminalNonTextInput) {
                    PlatformTextInputInterceptor { request, nextHandler ->
                        val wrappedRequest = PlatformTextInputMethodRequest { outAttributes ->
                            val conn = request.createInputConnection(outAttributes)
                            if (uiPreferences.terminalNonTextInput) {
                                outAttributes.inputType = InputType.TYPE_NULL
                                outAttributes.imeOptions = outAttributes.imeOptions or
                                    EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                                    EditorInfo.IME_FLAG_NO_FULLSCREEN
                            }
                            conn
                        }
                        nextHandler.startInputMethod(wrappedRequest)
                    }
                }

                @OptIn(ExperimentalComposeUiApi::class)
                InterceptPlatformTextInput(interceptor = terminalInputInterceptor) {
                    BasicTextField(
                        value = imeValue,
                        onValueChange = { next ->
                            if (!uiPreferences.terminalNonTextInput && next.composition != null) {
                                imeValue = next
                            } else {
                                when {
                                    next.text.isEmpty() -> sendWithModifiers("\u007F")
                                    next.text.startsWith(IME_SENTINEL) -> {
                                        val added = next.text.substring(IME_SENTINEL.length)
                                        if (added.isNotEmpty()) {
                                            sendWithModifiers(added)
                                        }
                                    }
                                    else -> sendWithModifiers(next.text)
                                }
                                resetImeBuffer()
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (uiPreferences.terminalNonTextInput) KeyboardType.Ascii else KeyboardType.Text,
                            imeAction = ImeAction.Send,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { sendWithModifiers("\r") }
                        ),
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f)
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    val native = event.nativeKeyEvent
                                    if (native.isCtrlPressed || native.isAltPressed) {
                                        val keyChar = when {
                                            native.keyCode in android.view.KeyEvent.KEYCODE_A..android.view.KeyEvent.KEYCODE_Z ->
                                                ('a' + (native.keyCode - android.view.KeyEvent.KEYCODE_A)).toString()
                                            native.unicodeChar > 0 -> (native.unicodeChar and 0xFFFF).toChar().toString()
                                            else -> ""
                                        }
                                        if (keyChar.isNotEmpty()) {
                                            var seq = keyChar
                                            if (native.isCtrlPressed) {
                                                val upper = seq[0].uppercaseChar()
                                                if (upper in '@'..'_' || seq[0] in 'a'..'z') {
                                                    seq = (upper.code - '@'.code).toChar().toString()
                                                }
                                            }
                                            if (native.isAltPressed) {
                                                seq = "\u001B$seq"
                                            }
                                            activeActionKey?.let { onSendText(it, seq) }
                                            ctrlLocked = false
                                            altLocked = false
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    when (event.key) {
                                        Key.Enter -> {
                                            sendWithModifiers("\r")
                                            true
                                        }
                                        Key.Backspace -> {
                                            sendWithModifiers("\u007F")
                                            true
                                        }
                                        Key.Delete -> {
                                            sendWithModifiers("\u001B[3~")
                                            true
                                        }
                                        Key.MoveHome -> {
                                            sendWithModifiers("\u001B[H")
                                            true
                                        }
                                        Key.MoveEnd -> {
                                            sendWithModifiers("\u001B[F")
                                            true
                                        }
                                        Key.Tab -> {
                                            sendWithModifiers("\t")
                                            true
                                        }
                                        Key.Escape -> {
                                            sendWithModifiers("\u001B")
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            sendWithModifiers("\u001B[A")
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            sendWithModifiers("\u001B[B")
                                            true
                                        }
                                        Key.DirectionLeft -> {
                                            sendWithModifiers("\u001B[D")
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            sendWithModifiers("\u001B[C")
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                    )
                }

                val hScrollState = rememberScrollState()
                val requiredContentWidthPx = remember(revision, lines.size, cols, metrics, widthPx, horizontalPadPx) {
                    if (!uiPreferences.terminalHorizontalScroll) {
                        widthPx
                    } else {
                        val lineMaxPx = lines.takeLast(800).maxOfOrNull { line ->
                            estimateTerminalLineWidthPx(line, metrics)
                        } ?: 0f
                        val minTarget = if (metrics.useMonospace) {
                            maxOf(cols * metrics.cellWidthPx, lineMaxPx) + horizontalPadPx
                        } else {
                            lineMaxPx + horizontalPadPx
                        }
                        maxOf(widthPx, minTarget)
                    }
                }
                val contentWidthDp = with(density) { requiredContentWidthPx.toDp() }
                val viewportContainerModifier = if (uiPreferences.terminalHorizontalScroll) {
                    Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScrollState)
                } else {
                    Modifier.fillMaxSize()
                }

                // Termux-style left-edge long-press + slide right gesture host wrapper (pinned outside horizontalScroll)
                val fastSwipeThresholdPx = with(density) { 2.5.dp.toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(drawerWidthPx, edgeStripWidthPx, uiPreferences.hapticFeedback) {
                            val holdTriggerMs = 180L
                            awaitEachGesture {
                                if ((drawerDragFraction ?: drawerAnimatedFraction.value) >= 0.5f) {
                                    return@awaitEachGesture
                                }
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                if (down.position.x > edgeStripWidthPx) return@awaitEachGesture

                                var abortedBeforeHold = false
                                val holdOutcome = withTimeoutOrNull(holdTriggerMs) {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed || change.isConsumed) {
                                            abortedBeforeHold = true
                                            break
                                        }
                                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                            abortedBeforeHold = true
                                            break
                                        }
                                    }
                                }
                                if (abortedBeforeHold || holdOutcome != null) return@awaitEachGesture

                                edgeDrawerArmed = true
                                if (uiPreferences.hapticFeedback) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                val peekFraction = 0.06f

                                var dragging = false
                                var dragStartX = down.position.x
                                var prevX = down.position.x
                                var lastStepDx = 0f

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            if (change != null && !dragging) {
                                                change.consume()
                                            }
                                            break
                                        }
                                        val totalDx = change.position.x - down.position.x
                                        val totalDy = change.position.y - down.position.y
                                        if (!dragging) {
                                            if (abs(totalDy) > viewConfiguration.touchSlop * 1.4f &&
                                                abs(totalDy) > abs(totalDx)
                                            ) {
                                                break
                                            }
                                            if (totalDx > viewConfiguration.touchSlop * 0.45f) {
                                                dragging = true
                                                dragStartX = change.position.x
                                                prevX = change.position.x
                                                drawerDragFraction = peekFraction
                                                change.consume()
                                            }
                                        } else {
                                            change.consume()
                                            lastStepDx = change.position.x - prevX
                                            prevX = change.position.x
                                            drawerDragFraction = (peekFraction + (change.position.x - dragStartX) / drawerWidthPx)
                                                .coerceIn(0f, 1f)
                                        }
                                    }
                                } finally {
                                    val finalFrac = if (dragging) (drawerDragFraction ?: 0f) else 0f
                                    val snapOpen = dragging && (
                                        finalFrac >= 0.25f ||
                                            (finalFrac >= 0.12f && lastStepDx > fastSwipeThresholdPx)
                                        )
                                    edgeDrawerArmed = false
                                    if (dragging) {
                                        animateDrawerTo(target = if (snapOpen) 1f else 0f, snapFrom = finalFrac)
                                    } else {
                                        drawerDragFraction = null
                                    }
                                }
                            }
                        }
                ) {
                    Box(modifier = viewportContainerModifier) {
                        val horizontalContentPadPx = with(density) { 10.dp.toPx() }
                        val selectionTapTolerancePx = with(density) { 12.dp.toPx() }
                        val selectionHighlightColor = accentColor.copy(alpha = 0.36f)
                        var selectionContainerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                        val containerModifier = if (uiPreferences.terminalHorizontalScroll) {
                            Modifier
                                .width(contentWidthDp)
                                .fillMaxHeight()
                        } else {
                            Modifier.fillMaxSize()
                        }

                        Box(
                            modifier = containerModifier.onGloballyPositioned { selectionContainerCoords = it }
                        ) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 14.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(lines, metrics, selection != null) {
                                        detectTapGestures(
                                            onTap = {
                                                if (edgeDrawerArmed || isDrawerOpen) return@detectTapGestures
                                                if (selection != null) {
                                                    selection = null
                                                } else {
                                                    focusTerminalIme()
                                                }
                                            },
                                            onLongPress = {
                                                if (edgeDrawerArmed || isDrawerOpen) return@detectTapGestures
                                                if (lines.isNotEmpty()) {
                                                    val targetIdx = lines.indexOfLast { it.plainText.isNotBlank() }
                                                        .takeIf { it >= 0 } ?: lines.lastIndex
                                                    val textLen = lines[targetIdx].plainText.length
                                                    val endIdx = (textLen - 1).coerceAtLeast(0)
                                                    selection = TerminalSelection(
                                                        anchor = TerminalTextPos(targetIdx, 0),
                                                        active = TerminalTextPos(targetIdx, endIdx)
                                                    )
                                                    if (uiPreferences.hapticFeedback) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                itemsIndexed(lines, key = { idx, _ -> idx }) { lineIdx, line ->
                                    TerminalCanvasRow(
                                        lineIdx = lineIdx,
                                        line = line,
                                        metrics = metrics,
                                        selection = selection,
                                        selectionColor = selectionHighlightColor,
                                        handleColor = accentColor,
                                        tapTolerancePx = selectionTapTolerancePx,
                                        onTapRow = { insideSelection ->
                                            if (edgeDrawerArmed || isDrawerOpen) return@TerminalCanvasRow
                                            val activeSel = selection
                                            if (activeSel != null) {
                                                if (insideSelection) {
                                                    copySelectionAndClear(activeSel, lines)
                                                } else {
                                                    selection = null
                                                }
                                            } else {
                                                focusTerminalIme()
                                            }
                                        },
                                        onOpenUrl = { url ->
                                            if (!edgeDrawerArmed && !isDrawerOpen) {
                                                LinkDetector.openUrlInBrowser(context, url)
                                            }
                                        },
                                        onLongPressRow = { charIdx ->
                                            if (edgeDrawerArmed || isDrawerOpen) return@TerminalCanvasRow
                                            val range = findInitialSelectionBounds(line.plainText, charIdx)
                                            selection = TerminalSelection(
                                                anchor = TerminalTextPos(lineIdx, range.first),
                                                active = TerminalTextPos(lineIdx, range.last)
                                            )
                                            if (uiPreferences.hapticFeedback) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    )
                                }
                            }

                            // Termux-style draggable start / end teardrop selection handles
                            val currentSelection = selection
                            if (currentSelection != null && lines.isNotEmpty()) {
                                val layoutInfo = listState.layoutInfo
                                val visibleItems = layoutInfo.visibleItemsInfo
                                val latestLines by rememberUpdatedState(lines)
                                val latestSelection by rememberUpdatedState(currentSelection)
                                val containerW = (selectionContainerCoords?.size?.width ?: 0).toFloat()
                                val containerH = (selectionContainerCoords?.size?.height ?: 0).toFloat()

                                fun resolvePosAtContainerPoint(pointX: Float, pointY: Float): TerminalTextPos? {
                                    val vis = listState.layoutInfo.visibleItemsInfo
                                    if (vis.isEmpty() || latestLines.isEmpty()) return null
                                    val topOffset = listState.layoutInfo.viewportStartOffset
                                    val targetItem = vis.firstOrNull { item ->
                                        val topY = (item.offset - topOffset).toFloat()
                                        pointY in topY..(topY + item.size.toFloat())
                                    } ?: if (pointY < (vis.first().offset - topOffset)) vis.first() else vis.last()
                                    val lineIndex = targetItem.index.coerceIn(0, latestLines.lastIndex)
                                    val targetLine = latestLines[lineIndex]
                                    val bounds = measureLineCharBoundsPx(targetLine, metrics)
                                    val localX = (pointX - horizontalContentPadPx).coerceAtLeast(0f)
                                    val charIdx = charIndexAtX(bounds, targetLine.plainText.length, localX)
                                    return TerminalTextPos(lineIndex, charIdx)
                                }

                                val startPos = currentSelection.start
                                val endPos = currentSelection.end
                                val startItem = visibleItems.firstOrNull { it.index == startPos.lineIdx }
                                val endItem = visibleItems.firstOrNull { it.index == endPos.lineIdx }

                                if (startItem != null && startPos.lineIdx in lines.indices) {
                                    val startLine = lines[startPos.lineIdx]
                                    val startBounds = remember(startLine, metrics) {
                                        measureLineCharBoundsPx(startLine, metrics)
                                    }
                                    val safeStartIdx = snapPosToCharBoundary(startLine.plainText, startPos.charIdx, isEnd = false)
                                    val startX = horizontalContentPadPx +
                                        startBounds[safeStartIdx.coerceIn(0, startBounds.lastIndex)]
                                    val startTopY = (startItem.offset - layoutInfo.viewportStartOffset).toFloat()
                                    val startBottomY = startTopY + startItem.size.toFloat()
                                    TerminalSelectionHandle(
                                        isStart = true,
                                        anchorX = startX,
                                        rowTopY = startTopY,
                                        rowBottomY = startBottomY,
                                        containerCoords = selectionContainerCoords,
                                        containerWidthPx = containerW,
                                        containerHeightPx = containerH,
                                        color = accentColor,
                                        onDragPoint = { dragX, dragY ->
                                            val curSel = latestSelection
                                            val rawPos = resolvePosAtContainerPoint(dragX, dragY) ?: return@TerminalSelectionHandle
                                            val endText = latestLines.getOrNull(rawPos.lineIdx)?.plainText.orEmpty()
                                            val snapped = rawPos.copy(charIdx = snapPosToCharBoundary(endText, rawPos.charIdx, isEnd = false))
                                            val clamped = minOf(snapped, curSel.end)
                                            selection = TerminalSelection(anchor = clamped, active = curSel.end)
                                        }
                                    )
                                }

                                if (endItem != null && endPos.lineIdx in lines.indices) {
                                    val endLine = lines[endPos.lineIdx]
                                    val endBounds = remember(endLine, metrics) {
                                        measureLineCharBoundsPx(endLine, metrics)
                                    }
                                    val safeEndIdx = snapPosToCharBoundary(endLine.plainText, endPos.charIdx, isEnd = true)
                                    val endCharSlot = if (endLine.plainText.isEmpty()) 0 else (safeEndIdx + 1).coerceIn(0, endBounds.lastIndex)
                                    val endX = horizontalContentPadPx + if (endLine.plainText.isEmpty()) metrics.cellWidthPx else endBounds[endCharSlot]
                                    val endTopY = (endItem.offset - layoutInfo.viewportStartOffset).toFloat()
                                    val endBottomY = endTopY + endItem.size.toFloat()
                                    TerminalSelectionHandle(
                                        isStart = false,
                                        anchorX = endX,
                                        rowTopY = endTopY,
                                        rowBottomY = endBottomY,
                                        containerCoords = selectionContainerCoords,
                                        containerWidthPx = containerW,
                                        containerHeightPx = containerH,
                                        color = accentColor,
                                        onDragPoint = { dragX, dragY ->
                                            val curSel = latestSelection
                                            val rawPos = resolvePosAtContainerPoint(dragX, dragY) ?: return@TerminalSelectionHandle
                                            val endText = latestLines.getOrNull(rawPos.lineIdx)?.plainText.orEmpty()
                                            val snapped = rawPos.copy(charIdx = snapPosToCharBoundary(endText, rawPos.charIdx, isEnd = true))
                                            val clamped = maxOf(snapped, curSel.start)
                                            selection = TerminalSelection(anchor = curSel.start, active = clamped)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Subtle left-edge handle indicator + hold-then-slide peek tooltip
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 2.dp)
                            .width(if (edgeDrawerArmed) 4.dp else 2.5.dp)
                            .height(if (edgeDrawerArmed) 52.dp else 36.dp)
                            .clip(CircleShape)
                            .background(
                                if (edgeDrawerArmed) accentColor
                                else Color.White.copy(alpha = 0.22f)
                            )
                    )
                    if (edgeDrawerArmed && (drawerDragFraction ?: 0f) <= 0.12f) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, accentColor),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = "向右滑动打开会话栏",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (currentSnapshot == null && currentHost != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                Text(
                                    text = "${currentHost.name} 当前暂无开启的终端会话",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "长按左侧边缘向右滑动可随时打开会话栏",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onNewSession(currentHost) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = contrastOnColor(accentColor)
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("新建会话", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!followBottom && lines.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = {
                            followBottom = true
                            scope.launch { listState.animateScrollToItem(lines.lastIndex) }
                        },
                        containerColor = accentColor,
                        contentColor = contrastOnColor(accentColor),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "回到底部")
                    }
                }

                // Inline reconnect status badge if connection dropped
                val snapState = currentSnapshot?.state
                if (currentHost != null && (snapState == SessionState.RECONNECTING || snapState == SessionState.ERROR || snapState == SessionState.DISCONNECTED)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, if (snapState == SessionState.RECONNECTING) Color(0xFFFBBF24) else Color(0xFFF87171)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onReconnectSession(activeActionKey ?: currentHost.id, currentHost) }
                    ) {
                        Text(
                            text = "${currentSnapshot.statusDetail} · 点击立即重连",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                val activeSel = selection
                if (activeSel != null && lines.isNotEmpty()) {
                    val selectedLen = remember(lines, activeSel) {
                        extractSelectedText(lines, activeSel).length
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, accentColor),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "已选 $selectedLen 字 · 点击选区复制",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { copySelectionAndClear(activeSel, lines) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "全选",
                                color = accentColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val lastIdx = lines.lastIndex
                                        val lastEnd = (lines[lastIdx].plainText.length - 1).coerceAtLeast(0)
                                        selection = TerminalSelection(
                                            anchor = TerminalTextPos(0, 0),
                                            active = TerminalTextPos(lastIdx, lastEnd)
                                        )
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "取消",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selection = null }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    // Quick toggle pill for line-mode input box (handy for Chinese IME or multi-word commands)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQuickBar = !showQuickBar }
                    ) {
                        Text(
                            text = if (showQuickBar) "收起输入栏" else "命令行输入",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Optional buffered command line input bar (ideal for long pasted commands / OAuth codes / Chinese)
            AnimatedVisibility(visible = showQuickBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickCommandLine,
                        onValueChange = { quickCommandLine = it },
                        placeholder = { Text("输入完整命令或粘贴授权码后发送...", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                activeActionKey?.let { key ->
                                    onSendText(key, quickCommandLine.replace("\r\n", "\r").replace("\n", "\r") + "\r")
                                    quickCommandLine = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(quickBarFocusRequester)
                    )
                    IconButton(
                        onClick = {
                            activeActionKey?.let { key ->
                                onSendText(key, quickCommandLine.replace("\r\n", "\r").replace("\n", "\r") + "\r")
                                quickCommandLine = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送", tint = accentColor)
                    }
                }
            }

            // Collapsible External Physical Keys + Custom Macro Pills Deck
            ExternalButtonDeck(
                expanded = deckExpanded,
                ctrlLocked = ctrlLocked,
                altLocked = altLocked,
                hapticEnabled = uiPreferences.hapticFeedback,
                macros = macros,
                accentColor = accentColor,
                onToggleExpanded = { deckExpanded = !deckExpanded },
                onToggleCtrl = { ctrlLocked = !ctrlLocked },
                onToggleAlt = { altLocked = !altLocked },
                onSendSequence = { seq -> sendWithModifiers(seq) },
                onRunMacro = { macro ->
                    activeActionKey?.let { key ->
                        val payload = if (macro.appendNewline) "${macro.payload}\r" else macro.payload
                        onSendText(key, payload)
                    }
                },
                onEditMacro = onEditMacro,
                onOpenMacroEditor = onOpenMacroEditor
            )
            }
        }

        if (effectiveDrawerFraction > 0.005f) {
            TerminalSessionSidebarOverlay(
                fraction = effectiveDrawerFraction,
                drawerWidthPx = drawerWidthPx,
                hazeState = drawerHazeState,
                blurRadius = uiPreferences.blurRadius,
                showWallpaper = uiPreferences.showWallpaper,
                accentColor = accentColor,
                currentHost = currentHost,
                hostSessions = hostSessions,
                allHosts = hosts,
                allSessions = sessions,
                activeSessionKey = currentSessionKey,
                onFractionDrag = { deltaPx ->
                    val current = drawerDragFraction ?: drawerAnimatedFraction.value
                    drawerDragFraction = (current + deltaPx / drawerWidthPx).coerceIn(0f, 1f)
                },
                onFinishDrag = { lastDeltaPx ->
                    val current = drawerDragFraction ?: drawerAnimatedFraction.value
                    val keepOpen = current >= 0.65f || (current >= 0.45f && lastDeltaPx > -2f)
                    animateDrawerTo(target = if (keepOpen) 1f else 0f, snapFrom = current)
                },
                onDismiss = { animateDrawerTo(0f) },
                onSelectSession = { sid ->
                    onSelectSession(sid)
                    animateDrawerTo(0f)
                },
                onCloseSession = { sid -> onCloseSession(sid) },
                onNewSession = { host ->
                    onNewSession(host)
                    animateDrawerTo(0f)
                },
                onSelectHost = { host ->
                    onSelectHost(host)
                    animateDrawerTo(0f)
                },
                onFocusKeyboard = {
                    animateDrawerTo(0f)
                    focusTerminalIme()
                },
                onMissingHostNotice = {
                    Toast.makeText(context, "请先在主页添加主机配置", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Keyboard-Interactive SSH Auth Prompt Dialog (OTP / Password / Pin)
    val pendingPrompt = currentSnapshot?.pendingInteractivePrompt
    val promptTargetKey = activeActionKey
    if (pendingPrompt != null && promptTargetKey != null) {
        val answers = remember(pendingPrompt.requestId) {
            mutableStateListOf<String>().apply {
                repeat(pendingPrompt.prompts.size) { add("") }
            }
        }
        AlertDialog(
            onDismissRequest = { onSubmitInteractivePrompt(promptTargetKey, null) },
            title = { Text(pendingPrompt.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendingPrompt.instruction.isNotBlank()) {
                        Text(pendingPrompt.instruction, style = MaterialTheme.typography.bodySmall)
                    }
                    pendingPrompt.prompts.forEachIndexed { idx, promptLabel ->
                        val echo = pendingPrompt.echo.getOrElse(idx) { true }
                        OutlinedTextField(
                            value = answers.getOrElse(idx) { "" },
                            onValueChange = { if (idx < answers.size) answers[idx] = it },
                            label = { Text(promptLabel) },
                            visualTransformation = if (echo) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (echo) KeyboardType.Text else KeyboardType.Password
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSubmitInteractivePrompt(promptTargetKey, answers.toList()) }) {
                    Text("提交")
                }
            },
            dismissButton = {
                TextButton(onClick = { onSubmitInteractivePrompt(promptTargetKey, null) }) {
                    Text("取消")
                }
            }
        )
    }

    // One-Tap Public Key Deploy Dialog
    if (showDeployKeyDialog && currentHost != null) {
        AlertDialog(
            onDismissRequest = { showDeployKeyDialog = false },
            title = { Text("一键部署公钥到 ${currentHost.host}") },
            text = {
                if (keys.isEmpty()) {
                    Text("尚未生成 SSH 密钥，请先在侧栏「SSH 密钥库」生成 Ed25519 密钥。")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("点击任意公钥，将自动写入远端 ~/.ssh/authorized_keys 实现免密登录：", style = MaterialTheme.typography.bodySmall)
                        keys.forEach { key ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDeployPublicKey(activeActionKey ?: currentHost.id, key.publicKeyOpenSsh)
                                        showDeployKeyDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${key.name} (${key.algorithm})", fontWeight = FontWeight.Bold)
                                    Text(key.fingerprintSha256, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeployKeyDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun TerminalSessionSidebarOverlay(
    fraction: Float,
    drawerWidthPx: Float,
    hazeState: HazeState,
    blurRadius: Float,
    showWallpaper: Boolean,
    accentColor: Color,
    currentHost: HostProfile?,
    hostSessions: List<SessionUiSnapshot>,
    allHosts: List<HostProfile>,
    allSessions: Map<String, SessionUiSnapshot>,
    activeSessionKey: String?,
    onFractionDrag: (Float) -> Unit,
    onFinishDrag: (Float) -> Unit,
    onDismiss: () -> Unit,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onNewSession: (HostProfile) -> Unit,
    onSelectHost: (HostProfile) -> Unit,
    onFocusKeyboard: () -> Unit,
    onMissingHostNotice: () -> Unit
) {
    val latestOnFractionDrag by rememberUpdatedState(onFractionDrag)
    val latestOnFinishDrag by rememberUpdatedState(onFinishDrag)
    val drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Dark backdrop scrim + swipe-left-to-close support
        var lastScrimDelta by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.44f * fraction.coerceIn(0f, 1f)))
                .pointerInput(drawerWidthPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            lastScrimDelta = dragAmount
                            latestOnFractionDrag(dragAmount)
                        },
                        onDragEnd = { latestOnFinishDrag(lastScrimDelta) },
                        onDragCancel = { latestOnFinishDrag(lastScrimDelta) }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        var lastDrawerDelta by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .width(GlassSideMenuWidth)
                .fillMaxHeight()
                .offset { IntOffset((-drawerWidthPx * (1f - fraction.coerceIn(0f, 1f))).roundToInt(), 0) }
                .clip(drawerShape)
                .then(
                    if (showWallpaper) {
                        Modifier.glassHaze(
                            hazeState = hazeState,
                            blurIntensity = blurRadius.coerceAtLeast(22f),
                            darkTint = Color(0xFF0A101E).copy(alpha = 0.80f),
                            darkFallback = Color(0xFF0B1120).copy(alpha = 0.95f)
                        )
                    } else {
                        Modifier.background(Color(0xFF0B1120).copy(alpha = 0.97f))
                    }
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.18f),
                        1f to Color.White.copy(alpha = 0.06f)
                    ),
                    shape = drawerShape
                )
                .pointerInput(drawerWidthPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            lastDrawerDelta = dragAmount
                            latestOnFractionDrag(dragAmount)
                        },
                        onDragEnd = { latestOnFinishDrag(lastDrawerDelta) },
                        onDragCancel = { latestOnFinishDrag(lastDrawerDelta) }
                    )
                }
                // Option rows consume their own clicks; tapping blank/header areas dismisses the drawer.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TERMINAL SESSIONS",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentHost?.name ?: "SSH 工作区",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentHost != null) {
                            Text(
                                text = "${currentHost.username}@${currentHost.host}:${currentHost.port}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "收起侧栏",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentHost != null) "${currentHost.name} · 会话 (${hostSessions.size})" else "会话列表",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (hostSessions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "当前主机尚未开启对话，点击下方「New Session」可立即发起连接。",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        hostSessions.forEach { snap ->
                            val isSelected = snap.sessionId == activeSessionKey
                            val dotColor = sessionStateDotColor(snap.state)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) accentColor else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectSession(snap.sessionId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "会话 #${snap.sessionIndex}",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = accentColor.copy(alpha = 0.30f)
                                                ) {
                                                    Text(
                                                        text = "当前",
                                                        color = Color.White,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            snap.lastRttMs?.let { rtt ->
                                                Spacer(Modifier.width(6.dp))
                                                Text("${rtt}ms", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = snap.windowTitle.ifBlank { snap.statusDetail },
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCloseSession(snap.sessionId) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "关闭会话 #${snap.sessionIndex}",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val otherHosts = remember(allHosts, currentHost?.id) {
                        allHosts.filter { it.id != currentHost?.id }
                    }
                    if (otherHosts.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "其他主机",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        otherHosts.forEach { otherHost ->
                            val otherSnaps = allSessions.values.filter { it.hostId == otherHost.id }
                            val bestSnap = otherSnaps.firstOrNull {
                                it.state == SessionState.CONNECTED || it.state == SessionState.AWAITING_OAUTH
                            } ?: otherSnaps.firstOrNull()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectHost(otherHost) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(sessionStateDotColor(bestSnap?.state))
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = otherHost.name,
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (otherSnaps.isNotEmpty()) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "${otherSnaps.size}会话",
                                                    color = accentColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${otherHost.username}@${otherHost.host}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onNewSession(otherHost) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "在 ${otherHost.name} 新建会话",
                                            tint = accentColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Termux-style pinned bottom action strip: Keyboard toggle + New Session on current host
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onFocusKeyboard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = "键盘",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (currentHost != null) {
                                onNewSession(currentHost)
                            } else {
                                onMissingHostNotice()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = contrastOnColor(accentColor)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "New Session",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun sessionStateDotColor(state: SessionState?): Color = when (state) {
    SessionState.CONNECTED -> Color(0xFF4ADE80)
    SessionState.AWAITING_OAUTH, SessionState.AWAITING_INPUT -> Color(0xFF38BDF8)
    SessionState.CONNECTING, SessionState.RECONNECTING -> Color(0xFFFBBF24)
    SessionState.ERROR -> Color(0xFFF87171)
    else -> Color(0xFF64748B)
}

private data class TerminalTextPos(
    val lineIdx: Int,
    val charIdx: Int
) : Comparable<TerminalTextPos> {
    override fun compareTo(other: TerminalTextPos): Int =
        if (lineIdx != other.lineIdx) lineIdx.compareTo(other.lineIdx) else charIdx.compareTo(other.charIdx)
}

private data class TerminalSelection(
    val anchor: TerminalTextPos,
    val active: TerminalTextPos
) {
    val start: TerminalTextPos get() = minOf(anchor, active)
    val end: TerminalTextPos get() = maxOf(anchor, active)
}

private fun measureLineCharBoundsPx(
    line: TerminalRenderLine,
    metrics: TerminalMetrics
): FloatArray {
    val plain = line.plainText
    val bounds = FloatArray(plain.length + 1)
    if (plain.isEmpty()) return bounds
    if (metrics.useMonospace) {
        var curX = 0f
        var i = 0
        while (i < plain.length) {
            bounds[i] = curX
            val ch = plain[i]
            val isSurrogatePair = Character.isHighSurrogate(ch) &&
                i + 1 < plain.length &&
                Character.isLowSurrogate(plain[i + 1])
            if (isSurrogatePair) {
                val w = 2f * metrics.cellWidthPx
                bounds[i + 1] = curX + w * 0.5f
                curX += w
                bounds[i + 2] = curX
                i += 2
            } else {
                val w = (if (AnsiEmulator.isWideChar(ch)) 2f else 1f) * metrics.cellWidthPx
                curX += w
                bounds[i + 1] = curX
                i++
            }
        }
        return bounds
    }

    var curX = 0f
    var globalIdx = 0
    line.spans.forEach { span ->
        val paint = if (span.style.bold) metrics.boldPaint else metrics.normalPaint
        var i = 0
        while (i < span.text.length && globalIdx < plain.length) {
            val ch = span.text[i]
            val isSurrogatePair = Character.isHighSurrogate(ch) &&
                i + 1 < span.text.length &&
                Character.isLowSurrogate(span.text[i + 1])
            val step = if (isSurrogatePair) 2 else 1
            val cp = if (isSurrogatePair) Character.toCodePoint(ch, span.text[i + 1]) else ch.code

            if (!isSurrogatePair && isGeometricTerminalGlyph(ch)) {
                bounds[globalIdx] = curX
                curX += metrics.cellWidthPx
                globalIdx++
                bounds[globalIdx] = curX
                i++
            } else if (shouldUseFallbackIconPaint(span.text, i, i + step, cp, paint, metrics)) {
                bounds[globalIdx] = curX
                val w = metrics.iconPaint!!.measureText(span.text, i, i + step)
                if (isSurrogatePair && globalIdx + 1 <= plain.length) {
                    bounds[globalIdx + 1] = curX + w * 0.5f
                }
                curX += w
                globalIdx = (globalIdx + step).coerceAtMost(plain.length)
                bounds[globalIdx] = curX
                i += step
            } else {
                val runStart = i
                val runStartX = curX
                val runEnd = findNormalTextRunEnd(span.text, runStart, paint, metrics)
                i = runEnd
                var k = runStart
                while (k < runEnd && globalIdx < plain.length) {
                    bounds[globalIdx] = curX
                    val c = span.text[k]
                    val pair = Character.isHighSurrogate(c) &&
                        k + 1 < runEnd &&
                        Character.isLowSurrogate(span.text[k + 1])
                    val s = if (pair) 2 else 1
                    val nextX = runStartX + paint.measureText(span.text, runStart, k + s)
                    if (pair && globalIdx + 1 <= plain.length) {
                        bounds[globalIdx + 1] = curX + (nextX - curX) * 0.5f
                    }
                    curX = nextX
                    globalIdx = (globalIdx + s).coerceAtMost(plain.length)
                    bounds[globalIdx] = curX
                    k += s
                }
            }
        }
    }
    return bounds
}

private fun snapPosToCharBoundary(plain: String, rawIdx: Int, isEnd: Boolean): Int {
    if (plain.isEmpty()) return 0
    val idx = rawIdx.coerceIn(0, plain.lastIndex)
    if (!isEnd && idx > 0 && Character.isLowSurrogate(plain[idx]) && Character.isHighSurrogate(plain[idx - 1])) {
        return idx - 1
    }
    if (isEnd && idx < plain.lastIndex && Character.isHighSurrogate(plain[idx]) && Character.isLowSurrogate(plain[idx + 1])) {
        return idx + 1
    }
    return idx
}

private fun charIndexAtX(
    bounds: FloatArray,
    textLength: Int,
    xPx: Float
): Int {
    if (textLength <= 0 || bounds.size < 2) return 0
    if (xPx <= bounds[0]) return 0
    val maxIdx = (textLength - 1).coerceAtMost(bounds.size - 2)
    for (i in 0..maxIdx) {
        val mid = (bounds[i] + bounds[i + 1]) * 0.5f
        if (xPx < mid) return i
    }
    return maxIdx
}

private fun findInitialSelectionBounds(text: String, charIdx: Int): IntRange {
    if (text.isEmpty()) return 0..0
    val safeIdx = snapPosToCharBoundary(text, charIdx, isEnd = false)
    if (text[safeIdx].isWhitespace()) {
        val end = snapPosToCharBoundary(text, safeIdx, isEnd = true)
        return safeIdx..end
    }
    var start = safeIdx
    while (start > 0 && !text[start - 1].isWhitespace()) {
        start--
    }
    var end = snapPosToCharBoundary(text, safeIdx, isEnd = true)
    while (end < text.lastIndex && !text[end + 1].isWhitespace()) {
        end++
    }
    start = snapPosToCharBoundary(text, start, isEnd = false)
    end = snapPosToCharBoundary(text, end, isEnd = true)
    return start..end
}

private fun extractSelectedText(
    lines: List<TerminalRenderLine>,
    selection: TerminalSelection
): String {
    if (lines.isEmpty()) return ""
    val start = selection.start
    val end = selection.end
    val firstLine = start.lineIdx.coerceIn(0, lines.lastIndex)
    val lastLine = end.lineIdx.coerceIn(firstLine, lines.lastIndex)
    return buildString {
        for (lineIdx in firstLine..lastLine) {
            if (lineIdx > firstLine) append('\n')
            val plain = lines[lineIdx].plainText
            if (plain.isEmpty()) continue
            val rawS = if (lineIdx == start.lineIdx) start.charIdx.coerceIn(0, plain.lastIndex) else 0
            val rawE = if (lineIdx == end.lineIdx) end.charIdx.coerceIn(0, plain.lastIndex) else plain.lastIndex
            val s = snapPosToCharBoundary(plain, rawS, isEnd = false)
            val e = snapPosToCharBoundary(plain, rawE, isEnd = true)
            if (s <= e) {
                append(plain, s, e + 1)
            }
        }
    }
}

@Composable
private fun TerminalSelectionHandle(
    isStart: Boolean,
    anchorX: Float,
    rowTopY: Float,
    rowBottomY: Float,
    containerCoords: LayoutCoordinates?,
    containerWidthPx: Float,
    containerHeightPx: Float,
    color: Color,
    onDragPoint: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val hitSizeDp = 44.dp
    val hitSizePx = with(density) { hitSizeDp.toPx() }
    val preferredX = anchorX + (if (isStart) -hitSizePx * 0.65f else -hitSizePx * 0.35f)
    val boxX = if (containerWidthPx > hitSizePx) {
        preferredX.coerceIn(0f, containerWidthPx - hitSizePx)
    } else {
        preferredX.coerceAtLeast(0f)
    }
    val flipAbove = containerHeightPx > 0f &&
        (rowBottomY + hitSizePx > containerHeightPx) &&
        (rowTopY >= hitSizePx * 0.65f)
    val rawBoxY = if (flipAbove) rowTopY - hitSizePx else rowBottomY
    val boxY = if (containerHeightPx > hitSizePx) {
        rawBoxY.coerceIn(0f, containerHeightPx - hitSizePx)
    } else {
        rawBoxY.coerceAtLeast(0f)
    }
    val tipX = (anchorX - boxX).coerceIn(4f, hitSizePx - 4f)

    val latestOnDragPoint by rememberUpdatedState(onDragPoint)
    val latestAnchorX by rememberUpdatedState(anchorX)
    val latestRowCenterY by rememberUpdatedState(((rowTopY + rowBottomY) * 0.5f).coerceAtLeast(0f))
    val latestContainerCoords by rememberUpdatedState(containerCoords)
    var handleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = boxX.roundToInt(),
                    y = boxY.roundToInt()
                )
            }
            .size(hitSizeDp)
            .onGloballyPositioned { handleCoords = it }
            .pointerInput(isStart) {
                var grabOffsetX = 0f
                var grabOffsetY = 0f
                detectDragGestures(
                    onDragStart = { localStart ->
                        val cCoords = latestContainerCoords
                        val hCoords = handleCoords
                        if (cCoords != null && hCoords != null && cCoords.isAttached && hCoords.isAttached) {
                            val pInContainer = cCoords.localPositionOf(hCoords, localStart)
                            grabOffsetX = pInContainer.x - latestAnchorX
                            grabOffsetY = pInContainer.y - latestRowCenterY
                        } else {
                            grabOffsetX = 0f
                            grabOffsetY = 0f
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val cCoords = latestContainerCoords
                        val hCoords = handleCoords
                        if (cCoords != null && hCoords != null && cCoords.isAttached && hCoords.isAttached) {
                            val pInContainer = cCoords.localPositionOf(hCoords, change.position)
                            latestOnDragPoint(pInContainer.x - grabOffsetX, pInContainer.y - grabOffsetY)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val knobRadius = 9.dp.toPx()
            val cx = if (isStart) {
                (tipX - knobRadius * 0.45f).coerceIn(knobRadius + 2f, size.width - knobRadius - 2f)
            } else {
                (tipX + knobRadius * 0.45f).coerceIn(knobRadius + 2f, size.width - knobRadius - 2f)
            }
            val tipY = if (flipAbove) size.height else 0f
            val cy = if (flipAbove) {
                size.height - knobRadius - 5.dp.toPx()
            } else {
                knobRadius + 5.dp.toPx()
            }
            val shoulderY = if (flipAbove) cy + knobRadius * 0.65f else cy - knobRadius * 0.65f
            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(cx + knobRadius * 0.7f, shoulderY)
                lineTo(cx - knobRadius * 0.7f, shoulderY)
                close()
            }
            drawPath(path = path, color = color)
            drawCircle(color = color, radius = knobRadius, center = Offset(cx, cy))
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = knobRadius * 0.38f, center = Offset(cx, cy))
        }
    }
}

internal data class TerminalMetrics(
    val useMonospace: Boolean,
    val fontPx: Float,
    val cellWidthPx: Float,
    val lineHeightPx: Float,
    val baselineOffsetPx: Float,
    val normalPaint: Paint,
    val boldPaint: Paint,
    val iconPaint: Paint?,
    val preferPrimaryOwnGlyph: Boolean
)

private data class TerminalLinkRegion(
    val startX: Float,
    val endX: Float,
    val url: String
)

private val MONOSPACE_SYSTEM_FONT_CANDIDATES = listOf(
    "/system/fonts/DroidSansMono.ttf",
    "/system/fonts/RobotoMono-Regular.ttf",
    "/system/fonts/NotoSansMono-Regular.ttf",
    "/system/fonts/CutiveMono.ttf"
)

private fun resolveTerminalTypefaces(prefs: UiPreferences): Pair<Typeface, Typeface> {
    if (!prefs.terminalUseMonospace) {
        return Typeface.DEFAULT to Typeface.DEFAULT_BOLD
    }
    if (prefs.terminalFontPreset == "CUSTOM" && !prefs.terminalCustomFontPath.isNullOrBlank()) {
        val custom = runCatching {
            val file = File(prefs.terminalCustomFontPath)
            if (file.isFile && file.canRead()) Typeface.createFromFile(file) else null
        }.getOrNull()
        if (custom != null) {
            return custom to Typeface.create(custom, Typeface.BOLD)
        }
    }
    if (prefs.terminalFontPreset == "SYSTEM_MONO") {
        return Typeface.MONOSPACE to Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    // AUTO: probe authentic fixed-width fonts on disk so OEM ROMs do not map Typeface.MONOSPACE to proportional fonts.
    val probed = MONOSPACE_SYSTEM_FONT_CANDIDATES.firstNotNullOfOrNull { path ->
        runCatching {
            val file = File(path)
            if (file.isFile && file.canRead()) Typeface.createFromFile(file) else null
        }.getOrNull()
    }
    val base = probed ?: Typeface.MONOSPACE
    return base to Typeface.create(base, Typeface.BOLD)
}

internal fun measureTerminalMetrics(
    context: Context,
    prefs: UiPreferences,
    density: androidx.compose.ui.unit.Density
): TerminalMetrics {
    val fontPx = with(density) { prefs.terminalFontSizeSp.coerceIn(8f, 22f).sp.toPx() }
    val (normalTypeface, boldTypeface) = resolveTerminalTypefaces(prefs)
    val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = normalTypeface
        textSize = fontPx
    }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTypeface
        textSize = fontPx
    }
    val iconPaint = if (prefs.terminalNerdIconsEnabled) {
        val iconFile = NerdFontPacks.resolveIconFallbackFile(context, prefs.terminalNerdIconsPath)
        val iconTf = iconFile?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() }
        iconTf?.let {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = it
                textSize = fontPx
            }
        }
    } else {
        null
    }
    val cellWidthPx = normalPaint.measureText("M").coerceAtLeast(fontPx * 0.56f)
    val fm = normalPaint.fontMetrics
    val rawHeight = ceil((fm.descent - fm.ascent + fm.leading).toDouble()).toFloat()
    val lineHeightPx = rawHeight.coerceAtLeast(fontPx * 1.24f)
    val extraLeading = (lineHeightPx - (fm.descent - fm.ascent)) * 0.5f
    val baselineOffsetPx = -fm.ascent + extraLeading
    return TerminalMetrics(
        useMonospace = prefs.terminalUseMonospace,
        fontPx = fontPx,
        cellWidthPx = cellWidthPx,
        lineHeightPx = lineHeightPx,
        baselineOffsetPx = baselineOffsetPx,
        normalPaint = normalPaint,
        boldPaint = boldPaint,
        iconPaint = iconPaint,
        preferPrimaryOwnGlyph = prefs.terminalFontPreset == "CUSTOM"
    )
}

internal fun isNerdIconCodePoint(cp: Int): Boolean =
    cp in 0xE000..0xF8FF ||
        cp in 0xF0000..0x10FFFF ||
        cp in 0x23FB..0x23FE ||
        cp == 0x2B58

internal fun shouldUseFallbackIconPaint(
    text: String,
    start: Int,
    end: Int,
    codePoint: Int,
    primaryPaint: Paint,
    metrics: TerminalMetrics
): Boolean {
    val iconPaint = metrics.iconPaint ?: return false
    if (codePoint < 0x2000) return false
    val cluster = text.substring(start, end)
    if (isNerdIconCodePoint(codePoint)) {
        if (metrics.preferPrimaryOwnGlyph && primaryPaint.hasGlyph(cluster)) return false
        return iconPaint.hasGlyph(cluster)
    }
    return !primaryPaint.hasGlyph(cluster) && iconPaint.hasGlyph(cluster)
}

private fun findNormalTextRunEnd(
    text: String,
    start: Int,
    primaryPaint: Paint,
    metrics: TerminalMetrics
): Int {
    var i = start
    while (i < text.length) {
        val c = text[i]
        val pair = Character.isHighSurrogate(c) &&
            i + 1 < text.length &&
            Character.isLowSurrogate(text[i + 1])
        val step = if (pair) 2 else 1
        val code = if (pair) Character.toCodePoint(c, text[i + 1]) else c.code
        if ((!pair && isGeometricTerminalGlyph(c)) ||
            shouldUseFallbackIconPaint(text, i, i + step, code, primaryPaint, metrics)
        ) {
            break
        }
        i += step
    }
    return i
}

private fun isGeometricTerminalGlyph(ch: Char): Boolean {
    val code = ch.code
    return code in 0x2500..0x259F
}

private fun countSpanDisplayCols(text: String): Int {
    var cols = 0
    var idx = 0
    while (idx < text.length) {
        val ch = text[idx]
        if (Character.isHighSurrogate(ch) && idx + 1 < text.length && Character.isLowSurrogate(text[idx + 1])) {
            cols += 2
            idx += 2
        } else {
            cols += if (AnsiEmulator.isWideChar(ch)) 2 else 1
            idx++
        }
    }
    return cols
}

private fun measureProportionalSpanWidthPx(
    text: String,
    paint: Paint,
    metrics: TerminalMetrics
): Float {
    var width = 0f
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        val isSurrogatePair = Character.isHighSurrogate(ch) &&
            i + 1 < text.length &&
            Character.isLowSurrogate(text[i + 1])
        val step = if (isSurrogatePair) 2 else 1
        val cp = if (isSurrogatePair) Character.toCodePoint(ch, text[i + 1]) else ch.code

        if (!isSurrogatePair && isGeometricTerminalGlyph(ch)) {
            width += metrics.cellWidthPx
            i++
        } else if (shouldUseFallbackIconPaint(text, i, i + step, cp, paint, metrics)) {
            width += metrics.iconPaint!!.measureText(text, i, i + step)
            i += step
        } else {
            val runStart = i
            val runEnd = findNormalTextRunEnd(text, runStart, paint, metrics)
            i = runEnd
            width += paint.measureText(text, runStart, runEnd)
        }
    }
    return width
}

private fun estimateTerminalLineWidthPx(
    line: TerminalRenderLine,
    metrics: TerminalMetrics
): Float {
    if (line.spans.isEmpty()) return 0f
    return if (metrics.useMonospace) {
        var totalCols = 0
        line.spans.forEach { totalCols += countSpanDisplayCols(it.text) }
        totalCols * metrics.cellWidthPx
    } else {
        var totalPx = 0f
        line.spans.forEach { span ->
            val paint = if (span.style.bold) metrics.boldPaint else metrics.normalPaint
            totalPx += measureProportionalSpanWidthPx(span.text, paint, metrics)
        }
        totalPx
    }
}

@Composable
private fun TerminalCanvasRow(
    lineIdx: Int,
    line: TerminalRenderLine,
    metrics: TerminalMetrics,
    selection: TerminalSelection?,
    selectionColor: Color,
    handleColor: Color,
    tapTolerancePx: Float,
    onTapRow: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    onLongPressRow: (Int) -> Unit
) {
    val density = LocalDensity.current
    val rowHeightDp = with(density) { metrics.lineHeightPx.toDp() }
    val charBounds = remember(line, metrics) {
        measureLineCharBoundsPx(line, metrics)
    }
    val linkRegions = remember(line, metrics) {
        buildList {
            var curX = 0f
            line.spans.forEach { span ->
                val spanW = if (metrics.useMonospace) {
                    countSpanDisplayCols(span.text) * metrics.cellWidthPx
                } else {
                    val paint = if (span.style.bold) metrics.boldPaint else metrics.normalPaint
                    measureProportionalSpanWidthPx(span.text, paint, metrics)
                }
                if (span.linkUrl != null && spanW > 0f) {
                    add(TerminalLinkRegion(startX = curX, endX = curX + spanW, url = span.linkUrl))
                }
                curX += spanW
            }
        }
    }

    val selRangePx = remember(lineIdx, line, selection, charBounds, metrics) {
        if (selection == null || lineIdx !in selection.start.lineIdx..selection.end.lineIdx) {
            null
        } else {
            val plain = line.plainText
            val sChar = if (lineIdx == selection.start.lineIdx) selection.start.charIdx else 0
            val eChar = if (lineIdx == selection.end.lineIdx) selection.end.charIdx else (plain.length - 1).coerceAtLeast(0)
            if (plain.isEmpty()) {
                0f to metrics.cellWidthPx
            } else {
                val clampedS = snapPosToCharBoundary(plain, sChar, isEnd = false)
                val clampedE = snapPosToCharBoundary(plain, eChar.coerceAtLeast(clampedS), isEnd = true)
                charBounds[clampedS] to charBounds[(clampedE + 1).coerceIn(0, charBounds.lastIndex)]
            }
        }
    }

    val defaultFgArgb = 0xFFE2E8F0.toInt()
    val inverseFgFallbackArgb = 0xFF0F172A.toInt()
    val linkFgArgb = 0xFF38BDF8.toInt()
    val linkColor = Color(linkFgArgb)
    val hasSelection by rememberUpdatedState(selection != null)
    val latestLine by rememberUpdatedState(line)
    val latestLinkRegions by rememberUpdatedState(linkRegions)
    val latestCharBounds by rememberUpdatedState(charBounds)
    val latestSelRangePx by rememberUpdatedState(selRangePx)
    val latestOnTapRow by rememberUpdatedState(onTapRow)
    val latestOnOpenUrl by rememberUpdatedState(onOpenUrl)
    val latestOnLongPressRow by rememberUpdatedState(onLongPressRow)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeightDp)
            .pointerInput(lineIdx, metrics) {
                detectTapGestures(
                    onTap = { offset ->
                        if (hasSelection) {
                            val activeRange = latestSelRangePx
                            val inside = activeRange != null &&
                                offset.x in (activeRange.first - tapTolerancePx)..(activeRange.second + tapTolerancePx)
                            latestOnTapRow(inside)
                        } else {
                            val hit = latestLinkRegions.firstOrNull { offset.x in it.startX..it.endX }
                            if (hit != null) {
                                latestOnOpenUrl(hit.url)
                            } else {
                                latestOnTapRow(false)
                            }
                        }
                    },
                    onLongPress = { offset ->
                        val charIdx = charIndexAtX(latestCharBounds, latestLine.plainText.length, offset.x)
                        latestOnLongPressRow(charIdx)
                    }
                )
            }
    ) {
        val rowH = size.height
        fun drawRowSelectionOverlay() {
            val range = selRangePx ?: return
            val sx = range.first
            val widthPx = if (line.plainText.isEmpty()) metrics.cellWidthPx else (range.second - sx).coerceAtLeast(2f)
            val ex = sx + widthPx
            drawRect(
                color = selectionColor,
                topLeft = Offset(sx, 0f),
                size = Size(widthPx, rowH)
            )
            val activeSel = selection ?: return
            val edgeWidth = 2.dp.toPx().coerceAtLeast(2f)
            if (lineIdx == activeSel.start.lineIdx) {
                drawLine(
                    color = handleColor,
                    start = Offset(sx, 0f),
                    end = Offset(sx, rowH),
                    strokeWidth = edgeWidth
                )
            }
            if (lineIdx == activeSel.end.lineIdx) {
                drawLine(
                    color = handleColor,
                    start = Offset(ex, 0f),
                    end = Offset(ex, rowH),
                    strokeWidth = edgeWidth
                )
            }
        }
        if (line.spans.isEmpty()) {
            drawRowSelectionOverlay()
            return@Canvas
        }
        drawTerminalRenderLine(line, metrics)
        drawRowSelectionOverlay()
    }
}

@Composable
internal fun TerminalPreviewLineRow(
    line: TerminalRenderLine,
    metrics: TerminalMetrics,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val rowHeightDp = with(density) { metrics.lineHeightPx.toDp() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeightDp)
    ) {
        drawTerminalRenderLine(line, metrics)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTerminalRenderLine(
    line: TerminalRenderLine,
    metrics: TerminalMetrics
) {
    val defaultFgArgb = 0xFFE2E8F0.toInt()
    val inverseFgFallbackArgb = 0xFF0F172A.toInt()
    val linkFgArgb = 0xFF38BDF8.toInt()
    val linkColor = Color(linkFgArgb)
    val rowH = size.height
    val cellW = metrics.cellWidthPx
    val nativeCanvas = drawContext.canvas.nativeCanvas
    var col = 0
    var cursorX = 0f

    line.spans.forEach { span ->
        val rawFg = span.style.fgRgb ?: defaultFgArgb
        val rawBg = span.style.bgRgb
        val fgArgb = when {
            span.linkUrl != null -> linkFgArgb
            span.style.inverse -> rawBg ?: inverseFgFallbackArgb
            else -> rawFg
        }
        val bgArgb = when {
            span.style.inverse -> rawFg
            rawBg != null -> rawBg
            else -> null
        }
        val fgColor = Color(fgArgb)
        val paint = if (span.style.bold) metrics.boldPaint else metrics.normalPaint
        paint.color = fgArgb

        if (metrics.useMonospace) {
            val startCol = col
            val spanCols = countSpanDisplayCols(span.text)
            val endCol = startCol + spanCols

            if (bgArgb != null && spanCols > 0) {
                val x0 = startCol * cellW
                val x1 = endCol * cellW
                drawRect(
                    color = Color(bgArgb),
                    topLeft = Offset(x0, 0f),
                    size = Size((x1 - x0) + 0.6f, rowH)
                )
            }

            var curCol = startCol
            var i = 0
            while (i < span.text.length) {
                val ch = span.text[i]
                val isSurrogatePair = Character.isHighSurrogate(ch) &&
                    i + 1 < span.text.length &&
                    Character.isLowSurrogate(span.text[i + 1])
                val step = if (isSurrogatePair) 2 else 1
                val colWidth = when {
                    isSurrogatePair -> 2
                    AnsiEmulator.isWideChar(ch) -> 2
                    else -> 1
                }
                val cellX = curCol * cellW
                val totalCellW = colWidth * cellW

                when {
                    ch == ' ' || ch == '\u00A0' -> Unit
                    !isSurrogatePair && ch.code in 0x2580..0x259F -> {
                        drawBlockElementCell(ch.code, cellX, totalCellW, rowH, fgColor)
                    }
                    !isSurrogatePair && ch.code in 0x2500..0x257F -> {
                        if (!drawBoxDrawingCell(ch.code, cellX, totalCellW, rowH, fgColor)) {
                            drawGridAlignedText(
                                canvas = nativeCanvas,
                                text = span.text,
                                start = i,
                                end = i + step,
                                cellX = cellX,
                                targetWidth = totalCellW,
                                baselineY = metrics.baselineOffsetPx,
                                paint = paint
                            )
                        }
                    }
                    else -> {
                        val cp = if (isSurrogatePair) Character.toCodePoint(ch, span.text[i + 1]) else ch.code
                        val glyphPaint = if (shouldUseFallbackIconPaint(span.text, i, i + step, cp, paint, metrics)) {
                            metrics.iconPaint!!.also { it.color = fgArgb }
                        } else {
                            paint
                        }
                        drawGridAlignedText(
                            canvas = nativeCanvas,
                            text = span.text,
                            start = i,
                            end = i + step,
                            cellX = cellX,
                            targetWidth = totalCellW,
                            baselineY = metrics.baselineOffsetPx,
                            paint = glyphPaint
                        )
                    }
                }
                curCol += colWidth
                i += step
            }

            if ((span.style.underline || span.linkUrl != null) && spanCols > 0) {
                val underlineY = (metrics.baselineOffsetPx + metrics.fontPx * 0.14f).coerceAtMost(rowH - 1f)
                drawLine(
                    color = if (span.linkUrl != null) linkColor else fgColor,
                    start = Offset(startCol * cellW, underlineY),
                    end = Offset(endCol * cellW, underlineY),
                    strokeWidth = (metrics.fontPx * 0.07f).coerceAtLeast(1.2f)
                )
            }

            col = endCol
        } else {
            val spanStartX = cursorX
            val spanWidthPx = measureProportionalSpanWidthPx(span.text, paint, metrics)

            if (bgArgb != null && spanWidthPx > 0f) {
                drawRect(
                    color = Color(bgArgb),
                    topLeft = Offset(spanStartX, 0f),
                    size = Size(spanWidthPx + 0.6f, rowH)
                )
            }

            var i = 0
            while (i < span.text.length) {
                val ch = span.text[i]
                val isSurrogatePair = Character.isHighSurrogate(ch) &&
                    i + 1 < span.text.length &&
                    Character.isLowSurrogate(span.text[i + 1])
                val step = if (isSurrogatePair) 2 else 1
                val cp = if (isSurrogatePair) Character.toCodePoint(ch, span.text[i + 1]) else ch.code

                if (!isSurrogatePair && ch.code in 0x2580..0x259F) {
                    drawBlockElementCell(ch.code, cursorX, cellW, rowH, fgColor)
                    cursorX += cellW
                    i++
                } else if (!isSurrogatePair && ch.code in 0x2500..0x257F) {
                    if (!drawBoxDrawingCell(ch.code, cursorX, cellW, rowH, fgColor)) {
                        nativeCanvas.drawText(span.text, i, i + 1, cursorX, metrics.baselineOffsetPx, paint)
                    }
                    cursorX += cellW
                    i++
                } else if (shouldUseFallbackIconPaint(span.text, i, i + step, cp, paint, metrics)) {
                    val iconP = metrics.iconPaint!!.also { it.color = fgArgb }
                    nativeCanvas.drawText(span.text, i, i + step, cursorX, metrics.baselineOffsetPx, iconP)
                    cursorX += iconP.measureText(span.text, i, i + step)
                    i += step
                } else {
                    val runStart = i
                    val runEnd = findNormalTextRunEnd(span.text, runStart, paint, metrics)
                    i = runEnd
                    nativeCanvas.drawText(span.text, runStart, runEnd, cursorX, metrics.baselineOffsetPx, paint)
                    cursorX += paint.measureText(span.text, runStart, runEnd)
                }
            }

            if ((span.style.underline || span.linkUrl != null) && cursorX > spanStartX) {
                val underlineY = (metrics.baselineOffsetPx + metrics.fontPx * 0.14f).coerceAtMost(rowH - 1f)
                drawLine(
                    color = if (span.linkUrl != null) linkColor else fgColor,
                    start = Offset(spanStartX, underlineY),
                    end = Offset(cursorX, underlineY),
                    strokeWidth = (metrics.fontPx * 0.07f).coerceAtLeast(1.2f)
                )
            }
        }
    }
}

private fun drawGridAlignedText(
    canvas: android.graphics.Canvas,
    text: String,
    start: Int,
    end: Int,
    cellX: Float,
    targetWidth: Float,
    baselineY: Float,
    paint: Paint
) {
    val measured = paint.measureText(text, start, end)
    if (measured > targetWidth * 1.04f && measured > 0f) {
        paint.textScaleX = (targetWidth / measured).coerceAtLeast(0.5f)
        canvas.drawText(text, start, end, cellX, baselineY, paint)
        paint.textScaleX = 1f
    } else {
        val xOffset = if (measured in 0.1f..targetWidth) (targetWidth - measured) * 0.5f else 0f
        canvas.drawText(text, start, end, cellX + xOffset, baselineY, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlockElementCell(
    code: Int,
    x: Float,
    w: Float,
    h: Float,
    color: Color
) {
    fun rectFraction(left: Float, top: Float, right: Float, bottom: Float, alphaMul: Float = 1f) {
        val rx = x + w * left
        val ry = h * top
        val rw = w * (right - left) + 0.6f
        val rh = h * (bottom - top) + 0.6f
        val c = if (alphaMul < 1f) color.copy(alpha = color.alpha * alphaMul) else color
        drawRect(color = c, topLeft = Offset(rx, ry), size = Size(rw, rh))
    }

    when (code) {
        0x2580 -> rectFraction(0f, 0f, 1f, 0.5f) // ▀
        in 0x2581..0x2588 -> {
            val eighth = (code - 0x2580) / 8f
            rectFraction(0f, 1f - eighth, 1f, 1f)
        }
        in 0x2589..0x258F -> {
            val eighth = (0x2590 - code) / 8f
            rectFraction(0f, 0f, eighth, 1f)
        }
        0x2590 -> rectFraction(0.5f, 0f, 1f, 1f) // ▐
        0x2591 -> rectFraction(0f, 0f, 1f, 1f, alphaMul = 0.25f) // ░
        0x2592 -> rectFraction(0f, 0f, 1f, 1f, alphaMul = 0.50f) // ▒
        0x2593 -> rectFraction(0f, 0f, 1f, 1f, alphaMul = 0.75f) // ▓
        0x2594 -> rectFraction(0f, 0f, 1f, 0.125f) // ▔
        0x2595 -> rectFraction(0.875f, 0f, 1f, 1f) // ▕
        else -> {
            val mask = when (code) {
                0x2596 -> 0b0100 // ▖ BL
                0x2597 -> 0b1000 // ▗ BR
                0x2598 -> 0b0001 // ▘ TL
                0x2599 -> 0b1101 // ▙ TL+BL+BR
                0x259A -> 0b1001 // ▚ TL+BR
                0x259B -> 0b0111 // ▛ TL+TR+BL
                0x259C -> 0b1011 // ▜ TL+TR+BR
                0x259D -> 0b0010 // ▝ TR
                0x259E -> 0b0110 // ▞ TR+BL
                0x259F -> 0b1110 // ▟ TR+BL+BR
                else -> 0
            }
            if ((mask and 0b0001) != 0) rectFraction(0f, 0f, 0.5f, 0.5f)
            if ((mask and 0b0010) != 0) rectFraction(0.5f, 0f, 1f, 0.5f)
            if ((mask and 0b0100) != 0) rectFraction(0f, 0.5f, 0.5f, 1f)
            if ((mask and 0b1000) != 0) rectFraction(0.5f, 0.5f, 1f, 1f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoxDrawingCell(
    code: Int,
    x: Float,
    w: Float,
    h: Float,
    color: Color
): Boolean {
    val cx = x + w * 0.5f
    val cy = h * 0.5f
    val thin = (w * 0.14f).coerceAtLeast(1.4f)
    val thick = (w * 0.28f).coerceAtLeast(2.8f)

    fun arms(left: Float = 0f, right: Float = 0f, up: Float = 0f, down: Float = 0f) {
        if (left > 0f && right > 0f && left == right && up == 0f && down == 0f) {
            drawLine(color, Offset(x, cy), Offset(x + w, cy), strokeWidth = left)
            return
        }
        if (up > 0f && down > 0f && up == down && left == 0f && right == 0f) {
            drawLine(color, Offset(cx, 0f), Offset(cx, h), strokeWidth = up)
            return
        }
        if (left > 0f) drawLine(color, Offset(x, cy), Offset(cx, cy), strokeWidth = left, cap = StrokeCap.Square)
        if (right > 0f) drawLine(color, Offset(cx, cy), Offset(x + w, cy), strokeWidth = right, cap = StrokeCap.Square)
        if (up > 0f) drawLine(color, Offset(cx, 0f), Offset(cx, cy), strokeWidth = up, cap = StrokeCap.Square)
        if (down > 0f) drawLine(color, Offset(cx, cy), Offset(cx, h), strokeWidth = down, cap = StrokeCap.Square)
    }

    when (code) {
        0x2500 -> arms(left = thin, right = thin) // ─
        0x2501 -> arms(left = thick, right = thick) // ━
        0x2502 -> arms(up = thin, down = thin) // │
        0x2503 -> arms(up = thick, down = thick) // ┃
        0x250C, 0x256D -> arms(right = thin, down = thin) // ┌ ╭
        0x250F -> arms(right = thick, down = thick) // ┏
        0x2510, 0x256E -> arms(left = thin, down = thin) // ┐ ╮
        0x2513 -> arms(left = thick, down = thick) // ┓
        0x2514, 0x2570 -> arms(right = thin, up = thin) // └ ╰
        0x2517 -> arms(right = thick, up = thick) // ┗
        0x2518, 0x256F -> arms(left = thin, up = thin) // ┘ ╯
        0x251B -> arms(left = thick, up = thick) // ┛
        0x251C -> arms(up = thin, down = thin, right = thin) // ├
        0x2523 -> arms(up = thick, down = thick, right = thick) // ┣
        0x2524 -> arms(up = thin, down = thin, left = thin) // ┤
        0x252B -> arms(up = thick, down = thick, left = thick) // ┫
        0x252C -> arms(left = thin, right = thin, down = thin) // ┬
        0x2533 -> arms(left = thick, right = thick, down = thick) // ┳
        0x2534 -> arms(left = thin, right = thin, up = thin) // ┴
        0x253B -> arms(left = thick, right = thick, up = thick) // ┻
        0x253C -> arms(left = thin, right = thin, up = thin, down = thin) // ┼
        0x254B -> arms(left = thick, right = thick, up = thick, down = thick) // ╋
        0x2574 -> arms(left = thin) // ╴
        0x2575 -> arms(up = thin) // ╵
        0x2576 -> arms(right = thin) // ╶
        0x2577 -> arms(down = thin) // ╷
        0x2550 -> {
            val d = (thin * 1.1f).coerceAtLeast(1.8f)
            drawLine(color, Offset(x, cy - d), Offset(x + w, cy - d), strokeWidth = thin * 0.85f)
            drawLine(color, Offset(x, cy + d), Offset(x + w, cy + d), strokeWidth = thin * 0.85f)
        }
        0x2551 -> {
            val d = (thin * 1.1f).coerceAtLeast(1.8f)
            drawLine(color, Offset(cx - d, 0f), Offset(cx - d, h), strokeWidth = thin * 0.85f)
            drawLine(color, Offset(cx + d, 0f), Offset(cx + d, h), strokeWidth = thin * 0.85f)
        }
        else -> return false
    }
    return true
}
