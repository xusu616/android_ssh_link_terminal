package com.xusu.sshlinkterminal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
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
import com.xusu.sshlinkterminal.terminal.LinkDetector
import com.xusu.sshlinkterminal.terminal.TerminalRenderLine
import com.xusu.sshlinkterminal.ui.components.ExternalButtonDeck
import kotlinx.coroutines.launch

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
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
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

    // Hidden capture text buffer so normal Android IME typing feeds SSH directly
    var imeBuffer by remember { mutableStateOf("") }

    fun sendWithModifiers(raw: String) {
        val hostId = currentHost?.id ?: return
        if (raw.isEmpty()) return
        val transformed = if ((ctrlLocked || altLocked) && raw.length == 1) {
            val ch = raw[0]
            var result = raw
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
            raw
        }
        onSendText(hostId, transformed)
    }

    val accentColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant
    val lines = currentSnapshot?.terminalLines.orEmpty()
    val revision = currentSnapshot?.terminalRevision ?: 0L

    val isAtBottom = remember(listState.firstVisibleItemIndex, listState.layoutInfo.totalItemsCount) {
        val total = listState.layoutInfo.totalItemsCount
        total == 0 || (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= total - 2
    }

    LaunchedEffect(revision, lines.size) {
        if (lines.isNotEmpty() && isAtBottom) {
            listState.scrollToItem(lines.lastIndex)
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

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF1E293B).copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, if (selected) accentColor else Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectHost(host) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = host.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                snap?.lastRttMs?.let { rtt ->
                                    Spacer(Modifier.width(5.dp))
                                    Text("${rtt}ms", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { onFontScaleChange((uiPreferences.terminalFontSizeSp - 1f).coerceAtLeast(10f)) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.TextDecrease, contentDescription = "缩小字号", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onFontScaleChange((uiPreferences.terminalFontSizeSp + 1f).coerceAtMost(22f)) },
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
            val authLink = currentSnapshot?.detectedAuthLink
            AnimatedVisibility(
                visible = authLink != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                if (authLink != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0F293A).copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "检测到 ${authLink.providerLabel}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onDismissAuthLink(currentHost.id) }) {
                                    Text("忽略", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                            Text(
                                text = authLink.url,
                                color = Color(0xFF7DD3FC),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { LinkDetector.openUrlInBrowser(context, authLink.url) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF38BDF8),
                                        contentColor = Color(0xFF082F49)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("一键浏览器授权", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { clipboard.setText(AnnotatedString(authLink.url)) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFF7DD3FC), modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("复制链接", color = Color(0xFF7DD3FC), fontSize = 12.sp)
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
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            ) {
                val density = LocalDensity.current
                val fontSp = uiPreferences.terminalFontSizeSp
                val charWidthPx = with(density) { (fontSp * 0.62f).sp.toPx() }
                val lineHeightPx = with(density) { (fontSp * 1.38f).sp.toPx() }
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }

                LaunchedEffect(currentHost?.id, widthPx, heightPx, fontSp) {
                    val hostId = currentHost?.id ?: return@LaunchedEffect
                    val cols = (widthPx / charWidthPx).toInt().coerceIn(32, 200)
                    val rows = (heightPx / lineHeightPx).toInt().coerceIn(12, 80)
                    onResizePty(hostId, cols, rows)
                }

                // Invisible IME input capture field
                BasicTextField(
                    value = imeBuffer,
                    onValueChange = { next ->
                        if (next.length > imeBuffer.length) {
                            val added = next.substring(imeBuffer.length)
                            sendWithModifiers(added)
                        } else if (next.length < imeBuffer.length) {
                            val delCount = imeBuffer.length - next.length
                            repeat(delCount) { sendWithModifiers("\u007F") }
                        }
                        imeBuffer = ""
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

                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        itemsIndexed(lines, key = { idx, _ -> idx }) { _, line ->
                            TerminalLineText(
                                line = line,
                                fontSizeSp = fontSp,
                                onOpenUrl = { url -> LinkDetector.openUrlInBrowser(context, url) }
                            )
                        }
                    }
                }

                if (!isAtBottom && lines.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch { listState.animateScrollToItem(lines.lastIndex) }
                        },
                        containerColor = accentColor,
                        contentColor = colorBag.darkMuted,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "回到底部")
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

            // Optional buffered command line input bar (ideal for long pasted commands / Chinese)
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
                        placeholder = { Text("输入完整命令后发送...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            currentHost?.let { host ->
                                onSendText(host.id, quickCommandLine + "\r")
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
}

@Composable
private fun TerminalLineText(
    line: TerminalRenderLine,
    fontSizeSp: Float,
    onOpenUrl: (String) -> Unit
) {
    if (line.spans.isEmpty()) {
        Spacer(Modifier.height((fontSizeSp * 1.25f).dp))
        return
    }
    val defaultFg = Color(0xFFE2E8F0)
    val linkFg = Color(0xFF38BDF8)

    val annotated = buildAnnotatedString {
        line.spans.forEach { span ->
            val rawFg = span.style.fgRgb?.let { Color(it) } ?: defaultFg
            val rawBg = span.style.bgRgb?.let { Color(it) }
            val fg = when {
                span.linkUrl != null -> linkFg
                span.style.inverse -> rawBg ?: Color(0xFF0F172A)
                else -> rawFg
            }
            val bg = when {
                span.style.inverse -> rawFg
                rawBg != null -> rawBg
                else -> Color.Unspecified
            }

            val start = length
            append(span.text)
            val end = length

            addStyle(
                style = SpanStyle(
                    color = fg,
                    background = bg,
                    fontWeight = if (span.style.bold) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (span.style.underline || span.linkUrl != null) TextDecoration.Underline else TextDecoration.None
                ),
                start = start,
                end = end
            )

            if (span.linkUrl != null) {
                addLink(
                    clickable = LinkAnnotation.Clickable(
                        tag = "url",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkFg,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        ),
                        linkInteractionListener = { onOpenUrl(span.linkUrl) }
                    ),
                    start = start,
                    end = end
                )
            }
        }
    }

    Text(
        text = annotated,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.38f).sp
    )
}
