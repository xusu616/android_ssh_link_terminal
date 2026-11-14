package com.xusu.sshlinkterminal.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.xusu.sshlinkterminal.terminal.TerminalRenderLine
import com.xusu.sshlinkterminal.ui.components.ExternalButtonDeck
import com.xusu.sshlinkterminal.ui.theme.contrastOnColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
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
    sessions: Map<String, SessionUiSnapshot>,
    macros: List<MacroButton>,
    onBack: () -> Unit,
    onSelectHost: (HostProfile) -> Unit,
    onReconnectHost: (HostProfile) -> Unit,
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
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val quickBarFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentHost = hosts.firstOrNull { it.id == activeHostId } ?: hosts.firstOrNull()
    val currentSnapshot = currentHost?.let { sessions[it.id] }

    var deckExpanded by remember { mutableStateOf(true) }
    var ctrlLocked by remember { mutableStateOf(false) }
    var altLocked by remember { mutableStateOf(false) }
    var showDeployKeyDialog by remember { mutableStateOf(false) }
    var quickCommandLine by remember { mutableStateOf("") }
    var showQuickBar by remember { mutableStateOf(false) }
    var copySheetText by remember { mutableStateOf<String?>(null) }

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

    fun sendWithModifiers(raw: String) {
        val hostId = currentHost?.id ?: return
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
        onSendText(hostId, transformed)
    }

    val rawAccent = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant
    val accentColor = if (!colorBag.useM3Color && rawAccent.luminance() < 0.14f) colorBag.lightMuted else rawAccent
    val lines = currentSnapshot?.terminalLines.orEmpty()
    val revision = currentSnapshot?.terminalRevision ?: 0L
    val authLink = currentSnapshot?.detectedAuthLink
    val metrics = remember(
        uiPreferences.terminalFontSizeSp,
        uiPreferences.terminalUseMonospace,
        uiPreferences.terminalFontPreset,
        uiPreferences.terminalCustomFontPath,
        density
    ) {
        measureTerminalMetrics(uiPreferences, density)
    }

    var hostMenuExpanded by remember { mutableStateOf(false) }

    var followBottom by remember(currentHost?.id) { mutableStateOf(true) }
    LaunchedEffect(listState, currentHost?.id) {
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

    LaunchedEffect(revision, lines.size) {
        if (lines.isNotEmpty() && followBottom) {
            val totalItems = listState.layoutInfo.totalItemsCount
            val visibleItems = listState.layoutInfo.visibleItemsInfo.size
            if (totalItems > visibleItems || visibleItems == 0) {
                listState.scrollToItem(lines.lastIndex, Int.MAX_VALUE)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                    val activeSnap = currentHost?.let { sessions[it.id] }
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
                        onDismissRequest = { hostMenuExpanded = false }
                    ) {
                        hosts.forEach { host ->
                            val snap = sessions[host.id]
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
                    onClick = { currentHost?.let(onReconnectHost) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "重连", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { currentHost?.let { onClearTerminal(it.id) } },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = "清屏", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
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
                                    onClick = { onDismissAuthLink(currentHost.id) },
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
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                var lastSentCols by remember(currentHost?.id) { mutableIntStateOf(-1) }
                var lastSentRows by remember(currentHost?.id) { mutableIntStateOf(-1) }

                // Keep remote PTY rows stable while IME or OAuth banner opens/closes so CLIs (like agy) do not SIGWINCH-refresh.
                LaunchedEffect(currentHost?.id, cols, rawRows, isImeVisible, hasOverlayBar) {
                    val hostId = currentHost?.id ?: return@LaunchedEffect
                    delay(180L)
                    val targetRows = when {
                        lastSentRows <= 0 -> rawRows
                        !isImeVisible && !hasOverlayBar -> rawRows
                        else -> lastSentRows
                    }
                    if (cols != lastSentCols || targetRows != lastSentRows) {
                        lastSentCols = cols
                        lastSentRows = targetRows
                        onResizePty(hostId, cols, targetRows)
                    }
                }

                // Only adjust scroll when an OAuth banner, quick bar, or IME actually occludes the viewport.
                LaunchedEffect(heightPx.toInt(), authLink != null, showQuickBar, isImeVisible) {
                    if ((authLink != null || showQuickBar || isImeVisible) && lines.isNotEmpty() && followBottom) {
                        val contentPx = lines.size * metrics.lineHeightPx + verticalPadPx
                        if (contentPx > heightPx) {
                            listState.scrollToItem(lines.lastIndex, Int.MAX_VALUE)
                        }
                    }
                }

                // Invisible IME input capture field
                BasicTextField(
                    value = imeValue,
                    onValueChange = { next ->
                        if (next.composition != null) {
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
                    keyboardOptions = KeyboardOptions(
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
                                when (event.key) {
                                    Key.Enter -> {
                                        sendWithModifiers("\r")
                                        true
                                    }
                                    Key.Backspace -> {
                                        sendWithModifiers("\u007F")
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

                val hScrollState = rememberScrollState()
                val requiredContentWidthPx = remember(revision, lines.size, cols, metrics, widthPx, horizontalPadPx) {
                    if (!uiPreferences.terminalHorizontalScroll) {
                        widthPx
                    } else {
                        val lineMaxPx = lines.takeLast(250).maxOfOrNull { line ->
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

                Box(modifier = viewportContainerModifier) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 14.dp),
                        modifier = (if (uiPreferences.terminalHorizontalScroll) {
                            Modifier
                                .width(contentWidthDp)
                                .fillMaxHeight()
                        } else {
                            Modifier.fillMaxSize()
                        }).pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { focusTerminalIme() },
                                onLongPress = {
                                    val allVisible = lines.joinToString("\n") { it.plainText }.trim()
                                    if (allVisible.isNotEmpty()) {
                                        copySheetText = allVisible
                                    }
                                }
                            )
                        }
                    ) {
                        itemsIndexed(lines, key = { idx, _ -> idx }) { _, line ->
                            TerminalCanvasRow(
                                line = line,
                                metrics = metrics,
                                onTapTerminal = { focusTerminalIme() },
                                onOpenUrl = { url -> LinkDetector.openUrlInBrowser(context, url) },
                                onLongPressLine = { lineText ->
                                    copySheetText = lineText.ifBlank {
                                        lines.joinToString("\n") { it.plainText }.trim()
                                    }
                                }
                            )
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
                            .clickable { onReconnectHost(currentHost) }
                    ) {
                        Text(
                            text = "${currentSnapshot.statusDetail} · 点击立即重连",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

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
                                currentHost?.let { host ->
                                    onSendText(host.id, quickCommandLine.replace("\r\n", "\r").replace("\n", "\r") + "\r")
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
                            currentHost?.let { host ->
                                onSendText(host.id, quickCommandLine.replace("\r\n", "\r").replace("\n", "\r") + "\r")
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
                    currentHost?.let { host ->
                        val payload = if (macro.appendNewline) "${macro.payload}\r" else macro.payload
                        onSendText(host.id, payload)
                    }
                },
                onEditMacro = onEditMacro,
                onOpenMacroEditor = onOpenMacroEditor
            )
        }
    }

    // Keyboard-Interactive SSH Auth Prompt Dialog (OTP / Password / Pin)
    val pendingPrompt = currentSnapshot?.pendingInteractivePrompt
    if (pendingPrompt != null) {
        val answers = remember(pendingPrompt.requestId) {
            mutableStateListOf<String>().apply {
                repeat(pendingPrompt.prompts.size) { add("") }
            }
        }
        AlertDialog(
            onDismissRequest = { onSubmitInteractivePrompt(currentHost.id, null) },
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
                TextButton(onClick = { onSubmitInteractivePrompt(currentHost.id, answers.toList()) }) {
                    Text("提交")
                }
            },
            dismissButton = {
                TextButton(onClick = { onSubmitInteractivePrompt(currentHost.id, null) }) {
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
                                        onDeployPublicKey(currentHost.id, key.publicKeyOpenSsh)
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

    // Long-press Terminal Copy Dialog (keeps terminal surface conflict-free for taps & links)
    copySheetText?.let { textToCopy ->
        AlertDialog(
            onDismissRequest = { copySheetText = null },
            title = { Text("终端文本选择与复制") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = textToCopy,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        copySheetText = null
                    }
                ) {
                    Text("复制全部")
                }
            },
            dismissButton = {
                TextButton(onClick = { copySheetText = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

private data class TerminalMetrics(
    val useMonospace: Boolean,
    val fontPx: Float,
    val cellWidthPx: Float,
    val lineHeightPx: Float,
    val baselineOffsetPx: Float,
    val normalPaint: Paint,
    val boldPaint: Paint
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

private fun measureTerminalMetrics(
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
        boldPaint = boldPaint
    )
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
    geomCellWidthPx: Float
): Float {
    var width = 0f
    var i = 0
    while (i < text.length) {
        if (isGeometricTerminalGlyph(text[i])) {
            width += geomCellWidthPx
            i++
        } else {
            val runStart = i
            while (i < text.length && !isGeometricTerminalGlyph(text[i])) {
                i++
            }
            width += paint.measureText(text, runStart, i)
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
            totalPx += measureProportionalSpanWidthPx(span.text, paint, metrics.cellWidthPx)
        }
        totalPx
    }
}

@Composable
private fun TerminalCanvasRow(
    line: TerminalRenderLine,
    metrics: TerminalMetrics,
    onTapTerminal: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onLongPressLine: (String) -> Unit
) {
    val density = LocalDensity.current
    val rowHeightDp = with(density) { metrics.lineHeightPx.toDp() }
    val linkRegions = remember(line, metrics) {
        buildList {
            var curX = 0f
            line.spans.forEach { span ->
                val spanW = if (metrics.useMonospace) {
                    countSpanDisplayCols(span.text) * metrics.cellWidthPx
                } else {
                    val paint = if (span.style.bold) metrics.boldPaint else metrics.normalPaint
                    measureProportionalSpanWidthPx(span.text, paint, metrics.cellWidthPx)
                }
                if (span.linkUrl != null && spanW > 0f) {
                    add(TerminalLinkRegion(startX = curX, endX = curX + spanW, url = span.linkUrl))
                }
                curX += spanW
            }
        }
    }

    val defaultFgArgb = 0xFFE2E8F0.toInt()
    val inverseFgFallbackArgb = 0xFF0F172A.toInt()
    val linkFgArgb = 0xFF38BDF8.toInt()
    val linkColor = Color(linkFgArgb)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeightDp)
            .pointerInput(line, metrics, linkRegions) {
                detectTapGestures(
                    onTap = { offset ->
                        val hit = linkRegions.firstOrNull { offset.x in it.startX..it.endX }
                        if (hit != null) {
                            onOpenUrl(hit.url)
                        } else {
                            onTapTerminal()
                        }
                    },
                    onLongPress = {
                        onLongPressLine(line.plainText)
                    }
                )
            }
    ) {
        if (line.spans.isEmpty()) return@Canvas

        val cellW = metrics.cellWidthPx
        val rowH = size.height
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
                val spanWidthPx = measureProportionalSpanWidthPx(span.text, paint, cellW)

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
                    if (ch.code in 0x2580..0x259F) {
                        drawBlockElementCell(ch.code, cursorX, cellW, rowH, fgColor)
                        cursorX += cellW
                        i++
                    } else if (ch.code in 0x2500..0x257F) {
                        if (!drawBoxDrawingCell(ch.code, cursorX, cellW, rowH, fgColor)) {
                            nativeCanvas.drawText(span.text, i, i + 1, cursorX, metrics.baselineOffsetPx, paint)
                        }
                        cursorX += cellW
                        i++
                    } else {
                        val runStart = i
                        while (i < span.text.length && !isGeometricTerminalGlyph(span.text[i])) {
                            i++
                        }
                        nativeCanvas.drawText(span.text, runStart, i, cursorX, metrics.baselineOffsetPx, paint)
                        cursorX += paint.measureText(span.text, runStart, i)
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
