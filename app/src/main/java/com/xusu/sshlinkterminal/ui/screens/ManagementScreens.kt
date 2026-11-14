package com.xusu.sshlinkterminal.ui.screens

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xusu.sshlinkterminal.BuildConfig
import com.xusu.sshlinkterminal.crypto.SshKeyManager
import com.xusu.sshlinkterminal.data.AppLogEntry
import com.xusu.sshlinkterminal.data.GuardStatus
import com.xusu.sshlinkterminal.data.MacroButton
import com.xusu.sshlinkterminal.data.MacroCategory
import com.xusu.sshlinkterminal.data.SshKeyRecord
import com.xusu.sshlinkterminal.data.UiPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyVaultScreen(
    keys: List<SshKeyRecord>,
    onBack: () -> Unit,
    onAddKey: (SshKeyRecord) -> Unit,
    onDeleteKey: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH 密钥库", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showGenerateDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("生成 Ed25519")
                }
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("导入私钥")
                }
            }

            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无私钥。建议生成一枚 Ed25519 密钥，连接后可在终端顶部一键写入 ~/.ssh/authorized_keys。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(keys, key = { it.id }) { key ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${key.name} · ${key.algorithm}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        clipboard.setText(AnnotatedString(key.publicKeyOpenSsh))
                                        Toast.makeText(context, "已复制公钥", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "复制公钥")
                                    }
                                    IconButton(onClick = { onDeleteKey(key.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除密钥")
                                    }
                                }
                                Text(
                                    key.fingerprintSha256,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    key.publicKeyOpenSsh,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        var name by remember { mutableStateOf("Pixel-Ed25519") }
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("生成 Ed25519 密钥对") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("密钥名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val mat = SshKeyManager.generateEd25519Key(name.trim().ifBlank { "Ed25519" })
                    onAddKey(mat.record)
                    showGenerateDialog = false
                }) { Text("生成") }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) { Text("取消") }
            }
        )
    }

    if (showImportDialog) {
        var name by remember { mutableStateOf("Imported-Key") }
        var pemText by remember { mutableStateOf("") }
        var passphrase by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入 SSH 私钥") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pemText,
                        onValueChange = { pemText = it },
                        label = { Text("粘贴私钥内容 (PEM / OpenSSH)") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("私钥口令（无则留空）") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val mat = SshKeyManager.importPrivateKey(
                            name = name.trim().ifBlank { "Imported" },
                            privateKeyText = pemText,
                            passphrase = passphrase.ifEmpty { null }
                        )
                        onAddKey(mat.record)
                        showImportDialog = false
                    } catch (e: Exception) {
                        errorMsg = e.message ?: "私钥解析失败"
                    }
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroEditorScreen(
    macros: List<MacroButton>,
    initialEditMacro: MacroButton? = null,
    onBack: () -> Unit,
    onSaveMacro: (MacroButton) -> Unit,
    onDeleteMacro: (String) -> Unit,
    onMoveMacro: (String, Int) -> Unit
) {
    var editing by remember(initialEditMacro) { mutableStateOf(initialEditMacro) }
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外置快捷按钮定制", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加按钮")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(macros, key = { it.id }) { macro ->
                val pillColor = Color(macro.colorHex)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = pillColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, pillColor),
                            modifier = Modifier.size(16.dp)
                        ) {}
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(macro.label, fontWeight = FontWeight.Bold)
                            Text(
                                text = macro.payload.replace("\u0003", "^C").replace("\u0004", "^D") +
                                    if (macro.appendNewline) " [⏎]" else "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onMoveMacro(macro.id, -1) }) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "上移")
                        }
                        IconButton(onClick = { onMoveMacro(macro.id, 1) }) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "下移")
                        }
                        IconButton(onClick = { editing = macro }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { onDeleteMacro(macro.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        MacroEditDialog(
            initial = editing,
            onDismiss = {
                showCreate = false
                editing = null
            },
            onSave = { macro ->
                onSaveMacro(macro)
                showCreate = false
                editing = null
            }
        )
    }
}

@Composable
fun MacroEditDialog(
    initial: MacroButton?,
    onDismiss: () -> Unit,
    onSave: (MacroButton) -> Unit
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var payload by remember { mutableStateOf(initial?.payload ?: "") }
    var appendNewline by remember { mutableStateOf(initial?.appendNewline ?: true) }
    var selectedColor by remember { mutableStateOf(initial?.colorHex ?: 0xFF2DD4BFL) }
    val paletteColors = listOf(0xFF2DD4BFL, 0xFFA78BFAL, 0xFF38BDF8L, 0xFF34D399L, 0xFFFBBF24L, 0xFFF87171L, 0xFFFB923CL)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建外置快捷按钮" else "编辑外置按钮") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("按钮显示名（如 agy / tmux a）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { payload = it },
                    label = { Text("发送内容（命令或转义符）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = appendNewline, onCheckedChange = { appendNewline = it })
                    Text("自动追加回车发送", style = MaterialTheme.typography.bodySmall)
                }
                Text("胶囊配色", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    paletteColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .clickable { selectedColor = hex }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && payload.isNotEmpty(),
                onClick = {
                    onSave(
                        MacroButton(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            label = label.trim(),
                            payload = payload,
                            appendNewline = appendNewline,
                            colorHex = selectedColor,
                            category = initial?.category ?: MacroCategory.COMMAND,
                            sortOrder = initial?.sortOrder ?: 99
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    prefs: UiPreferences,
    guardStatus: GuardStatus,
    onBack: () -> Unit,
    onUpdatePrefs: ((UiPreferences) -> UiPreferences) -> Unit,
    onPickWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onPickTerminalFont: () -> Unit,
    onRequestShizuku: () -> Unit,
    onApplyShizukuHarden: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观、壁纸与链接策略", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onPickWallpaper, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("选择自定义壁纸")
                }
                OutlinedButton(onClick = onResetWallpaper) {
                    Text("默认极光图")
                }
            }

            PrefSwitchRow(
                title = "启用壁纸与 Haze 毛玻璃质感",
                subtitle = "参考 shizuku-ftp，在主屏与终端透出壁纸纹理",
                checked = prefs.showWallpaper,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(showWallpaper = checked) } }
            )
            PrefSwitchRow(
                title = "使用系统 Material 3 动态取色",
                subtitle = "关闭则自动从当前壁纸 Palette 提取主题配色",
                checked = prefs.useM3Color,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(useM3Color = checked) } }
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "壁纸取色加深深度: ${(prefs.accentDepth * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (prefs.useM3Color) {
                        "当前处于系统 M3 动态取色；关闭上方开关后生效（源码: ThemeEngine.kt -> deepenExtractedAccent）"
                    } else {
                        "保留壁纸原色相并加深饱和质感（源码: ThemeEngine.kt -> deepenExtractedAccent）"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = prefs.accentDepth.coerceIn(0f, 0.70f),
                    onValueChange = { v -> onUpdatePrefs { it.copy(accentDepth = v) } },
                    valueRange = 0f..0.70f,
                    enabled = !prefs.useM3Color
                )
            }
            PrefSwitchRow(
                title = "检测到 OAuth / Tailscale 链接自动打开浏览器",
                subtitle = "识别 login.tailscale.com、Google/Antigravity OAuth 时自动唤起 Custom Tabs",
                checked = prefs.autoOpenOAuthUrl,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(autoOpenOAuthUrl = checked) } }
            )
            PrefSwitchRow(
                title = "Shizuku 就绪时自动应用防杀加固",
                subtitle = "自动设置 Doze 白名单、Standby Active 与看门狗",
                checked = prefs.autoHardenWithShizuku,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(autoHardenWithShizuku = checked) } }
            )

            // Shizuku Optional Permission & Status Card
            val shizukuStatusLabel = when {
                !guardStatus.shizukuInstalled -> "未安装 Shizuku（可选，不影响基础 SSH）"
                !guardStatus.shizukuRunning -> "Shizuku 已安装，但服务尚未启动"
                !guardStatus.shizukuPermissionGranted -> "Shizuku 运行中，尚未授予本应用权限"
                else -> "Shizuku 已授权（看门狗: ${if (guardStatus.watchdogActive) "运行中" else "待激活"}）"
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = if (guardStatus.shizukuPermissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shizuku 权限与后台加固", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = shizukuStatusLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "不使用 Shizuku 也能正常连接与使用终端；授权后可额外启用 Doze 白名单、Standby Active 与进程看门狗。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            guardStatus.shizukuRunning && !guardStatus.shizukuPermissionGranted -> {
                                Button(onClick = onRequestShizuku) {
                                    Text("请求 Shizuku 授权")
                                }
                            }
                            guardStatus.shizukuPermissionGranted -> {
                                FilledTonalButton(onClick = onApplyShizukuHarden) {
                                    Text("立即执行防杀加固")
                                }
                            }
                            else -> {
                                OutlinedButton(onClick = onRequestShizuku) {
                                    Text("检测 / 请求 Shizuku 授权")
                                }
                            }
                        }
                    }
                }
            }
            PrefSwitchRow(
                title = "开机自启恢复常驻守护主机",
                subtitle = "收到 BOOT_COMPLETED 后自动恢复勾选常驻的主机连接",
                checked = prefs.autoStartOnBoot,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(autoStartOnBoot = checked) } }
            )
            PrefSwitchRow(
                title = "弹簧物理动效 & 按钮触感反馈",
                subtitle = "外置快捷键与侧栏抽屉启用弹簧动画和震动",
                checked = prefs.springAnimation,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(springAnimation = checked, hapticFeedback = checked) } }
            )
            PrefSwitchRow(
                title = "延迟图表坐标轴切换动画",
                subtitle = "切换 M / H / D 时间窗时启用坐标轴压缩与展开动画",
                checked = prefs.chartAxisAnimation,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(chartAxisAnimation = checked) } }
            )
            PrefSwitchRow(
                title = "避让顶部挖孔屏安全区",
                subtitle = "开启后主屏 Hero 卡片自动向下避让摄像头挖孔",
                checked = prefs.topComponentPressedDown,
                onCheckedChange = { checked -> onUpdatePrefs { it.copy(topComponentPressedDown = checked) } }
            )

            HorizontalDivider()

            // Terminal Typography & Viewport Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("终端字体与排版", fontWeight = FontWeight.Bold)
                    PrefSwitchRow(
                        title = "终端使用等宽字体",
                        subtitle = "开启后按等宽网格对齐 ASCII 图形；关闭则使用系统默认字体自然排版",
                        checked = prefs.terminalUseMonospace,
                        onCheckedChange = { checked -> onUpdatePrefs { it.copy(terminalUseMonospace = checked) } }
                    )
                    if (prefs.terminalUseMonospace) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = prefs.terminalFontPreset == "AUTO",
                                onClick = { onUpdatePrefs { it.copy(terminalFontPreset = "AUTO") } },
                                label = { Text("系统真实等宽") }
                            )
                            FilterChip(
                                selected = prefs.terminalFontPreset == "SYSTEM_MONO",
                                onClick = { onUpdatePrefs { it.copy(terminalFontPreset = "SYSTEM_MONO") } },
                                label = { Text("系统 Monospace") }
                            )
                            if (prefs.terminalCustomFontPath != null) {
                                FilterChip(
                                    selected = prefs.terminalFontPreset == "CUSTOM",
                                    onClick = { onUpdatePrefs { it.copy(terminalFontPreset = "CUSTOM") } },
                                    label = { Text(prefs.terminalCustomFontName ?: "自定义字体") }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onPickTerminalFont) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("选择自定义等宽字体 (.ttf/.otf)")
                            }
                            val customPath = prefs.terminalCustomFontPath
                            if (customPath != null) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching { File(customPath).delete() }
                                        onUpdatePrefs {
                                            it.copy(
                                                terminalFontPreset = "AUTO",
                                                terminalCustomFontPath = null,
                                                terminalCustomFontName = null
                                            )
                                        }
                                    }
                                ) {
                                    Text("移除")
                                }
                            }
                        }
                    }

                    val previewFamily = remember(
                        prefs.terminalUseMonospace,
                        prefs.terminalFontPreset,
                        prefs.terminalCustomFontPath
                    ) {
                        when {
                            !prefs.terminalUseMonospace -> FontFamily.Default
                            prefs.terminalFontPreset == "CUSTOM" && prefs.terminalCustomFontPath != null -> {
                                runCatching {
                                    FontFamily(Typeface.createFromFile(File(prefs.terminalCustomFontPath)))
                                }.getOrDefault(FontFamily.Monospace)
                            }
                            else -> FontFamily.Monospace
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF090D16)
                    ) {
                        Text(
                            text = "Windows PowerShell | 192.168.1.222 | 中文对齐 1234",
                            color = Color(0xFFE2E8F0),
                            fontFamily = previewFamily,
                            fontSize = prefs.terminalFontSizeSp.coerceIn(8f, 22f).sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    PrefSwitchRow(
                        title = "终端允许左右横向滚动",
                        subtitle = "开启后保持至少 80 列不折行挤压，长命令行可直接左右滑动",
                        checked = prefs.terminalHorizontalScroll,
                        onCheckedChange = { checked -> onUpdatePrefs { it.copy(terminalHorizontalScroll = checked) } }
                    )

                    Text(
                        text = "终端默认字号: ${String.format(Locale.US, "%.1f", prefs.terminalFontSizeSp)} sp",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = prefs.terminalFontSizeSp.coerceIn(8f, 22f),
                        onValueChange = { v ->
                            val snapped = ((v * 2f).toInt() / 2f).coerceIn(8f, 22f)
                            onUpdatePrefs { it.copy(terminalFontSizeSp = snapped) }
                        },
                        valueRange = 8f..22f,
                        steps = 27
                    )
                }
            }

            Text("Haze 毛玻璃模糊半径: ${prefs.blurRadius.toInt()} dp", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = prefs.blurRadius,
                onValueChange = { v -> onUpdatePrefs { it.copy(blurRadius = v) } },
                valueRange = 4f..32f
            )

            Text("终端背景压暗遮罩: ${(prefs.terminalScrimAlpha * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = prefs.terminalScrimAlpha,
                onValueChange = { v -> onUpdatePrefs { it.copy(terminalScrimAlpha = v) } },
                valueRange = 0.45f..0.95f
            )
        }
    }
}

@Composable
private fun PrefSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsAndAboutScreen(
    logs: List<AppLogEntry>,
    onBack: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var releaseStatus by remember { mutableStateOf("当前版本 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("守护日志与 GitLab 版本", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onClearLogs) { Text("清空") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SSH Link Terminal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(releaseStatus, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            enabled = !checking,
                            onClick = {
                                checking = true
                                scope.launch {
                                    releaseStatus = checkGitLabRelease()
                                    checking = false
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (checking) "检查中..." else "检查 GitLab Releases")
                        }
                        OutlinedButton(onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://gitlab.com/xusu616/android_ssh_link_terminal")
                            )
                            context.startActivity(intent)
                        }) {
                            Text("打开 GitLab")
                        }
                    }
                }
            }

            Text("运行日志 (${logs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs.asReversed()) { entry ->
                    val badgeColor = when (entry.level) {
                        "ERROR" -> Color(0xFFF87171)
                        "WARN" -> Color(0xFFFBBF24)
                        else -> Color(0xFF38BDF8)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${timeFmt.format(Date(entry.timestampMillis))} [${entry.tag}] ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = entry.message,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun checkGitLabRelease(): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://gitlab.com/api/v4/projects/xusu616%2Fandroid_ssh_link_terminal/releases")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            return@withContext "GitLab API 状态 $code（可能尚未发布 Release，当前 v${BuildConfig.VERSION_NAME}）"
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(body)
        if (arr.length() == 0) {
            "当前已是最新 v${BuildConfig.VERSION_NAME}（GitLab 暂无 Release）"
        } else {
            val tag = arr.getJSONObject(0).optString("tag_name", "unknown")
            "GitLab 最新版本: $tag · 当前 v${BuildConfig.VERSION_NAME}"
        }
    } catch (e: Exception) {
        "检查失败: ${e.message}"
    }
}
