package com.xusu.sshlinkterminal.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hosts = MutableStateFlow(loadHosts())
    val hosts: StateFlow<List<HostProfile>> = _hosts.asStateFlow()

    private val _keys = MutableStateFlow(loadKeys())
    val keys: StateFlow<List<SshKeyRecord>> = _keys.asStateFlow()

    private val _macros = MutableStateFlow(loadMacros())
    val macros: StateFlow<List<MacroButton>> = _macros.asStateFlow()

    private val _uiPreferences = MutableStateFlow(loadUiPreferences())
    val uiPreferences: StateFlow<UiPreferences> = _uiPreferences.asStateFlow()

    private val _rttSamples = MutableStateFlow<List<RttSample>>(emptyList())
    val rttSamples: StateFlow<List<RttSample>> = _rttSamples.asStateFlow()

    private val _logs = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val logs: StateFlow<List<AppLogEntry>> = _logs.asStateFlow()

    fun upsertHost(host: HostProfile) {
        val current = _hosts.value.toMutableList()
        val index = current.indexOfFirst { it.id == host.id }
        if (index >= 0) current[index] = host else current.add(0, host)
        _hosts.value = current
        saveHosts(current)
    }

    fun deleteHost(hostId: String) {
        val current = _hosts.value.filterNot { it.id == hostId }
        _hosts.value = current
        saveHosts(current)
    }

    fun addKey(key: SshKeyRecord) {
        val current = listOf(key) + _keys.value.filterNot { it.id == key.id }
        _keys.value = current
        saveKeys(current)
    }

    fun deleteKey(keyId: String) {
        val current = _keys.value.filterNot { it.id == keyId }
        _keys.value = current
        saveKeys(current)
    }

    fun upsertMacro(macro: MacroButton) {
        val current = _macros.value.toMutableList()
        val index = current.indexOfFirst { it.id == macro.id }
        if (index >= 0) current[index] = macro else current.add(macro.copy(sortOrder = current.size))
        val sorted = current.sortedBy { it.sortOrder }.mapIndexed { idx, item -> item.copy(sortOrder = idx) }
        _macros.value = sorted
        saveMacros(sorted)
    }

    fun deleteMacro(macroId: String) {
        val sorted = _macros.value
            .filterNot { it.id == macroId }
            .mapIndexed { idx, item -> item.copy(sortOrder = idx) }
        _macros.value = sorted
        saveMacros(sorted)
    }

    fun moveMacro(macroId: String, delta: Int) {
        val list = _macros.value.toMutableList()
        val idx = list.indexOfFirst { it.id == macroId }
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, list.lastIndex)
        if (target == idx) return
        val item = list.removeAt(idx)
        list.add(target, item)
        val sorted = list.mapIndexed { i, m -> m.copy(sortOrder = i) }
        _macros.value = sorted
        saveMacros(sorted)
    }

    fun updateUiPreferences(transform: (UiPreferences) -> UiPreferences) {
        val next = transform(_uiPreferences.value)
        _uiPreferences.value = next
        saveUiPreferences(next)
    }

    fun appendRttSample(rttMs: Long, activeSessions: Int) {
        val nowSec = System.currentTimeMillis() / 1000L
        val sample = RttSample(nowSec, rttMs.coerceAtLeast(0L), activeSessions)
        val current = _rttSamples.value
        val next = if (current.lastOrNull()?.timestampSec == nowSec) {
            current.dropLast(1) + sample
        } else {
            (current + sample).takeLast(720)
        }
        _rttSamples.value = next
    }

    fun appendLog(level: String, tag: String, message: String) {
        val next = (_logs.value + AppLogEntry(level = level, tag = tag, message = message)).takeLast(300)
        _logs.value = next
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun hasPromptedShizukuPermission(): Boolean =
        prefs.getBoolean(KEY_SHIZUKU_PROMPTED, false)

    fun markShizukuPermissionPrompted() {
        prefs.edit { putBoolean(KEY_SHIZUKU_PROMPTED, true) }
    }

    private fun loadHosts(): List<HostProfile> {
        val raw = prefs.getString(KEY_HOSTS, null) ?: return defaultHosts()
        return try {
            val arr = JSONArray(raw)
            val parsed = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HostProfile(
                    id = obj.getString("id"),
                    name = obj.optString("name", obj.getString("host")),
                    host = obj.getString("host"),
                    port = obj.optInt("port", 22),
                    username = obj.optString("username", "root"),
                    authType = AuthType.entries.firstOrNull { it.name == obj.optString("authType") } ?: AuthType.PASSWORD,
                    encryptedPassword = obj.optString("encryptedPassword", "").ifEmpty { null },
                    keyId = obj.optString("keyId", "").ifEmpty { null },
                    encryptedPassphrase = obj.optString("encryptedPassphrase", "").ifEmpty { null },
                    autoAttachCommand = obj.optString("autoAttachCommand", ""),
                    keepAliveIntervalSec = obj.optInt("keepAliveIntervalSec", 15),
                    autoReconnect = obj.optBoolean("autoReconnect", true),
                    pinInDaemon = obj.optBoolean("pinInDaemon", true),
                    tag = obj.optString("tag", "LAN")
                )
            }
            if (!prefs.getBoolean(KEY_MIGRATED_LEGACY_TMUX_V1, false)) {
                var changed = false
                val cleaned = parsed.map { host ->
                    if (host.autoAttachCommand.trim() == LEGACY_AUTO_ATTACH_CMD) {
                        changed = true
                        host.copy(autoAttachCommand = "")
                    } else {
                        host
                    }
                }
                if (changed) {
                    saveHosts(cleaned)
                }
                prefs.edit { putBoolean(KEY_MIGRATED_LEGACY_TMUX_V1, true) }
                cleaned
            } else {
                parsed
            }
        } catch (_: Exception) {
            defaultHosts()
        }
    }

    private fun saveHosts(list: List<HostProfile>) {
        val arr = JSONArray()
        list.forEach { host ->
            arr.put(JSONObject().apply {
                put("id", host.id)
                put("name", host.name)
                put("host", host.host)
                put("port", host.port)
                put("username", host.username)
                put("authType", host.authType.name)
                put("encryptedPassword", host.encryptedPassword ?: "")
                put("keyId", host.keyId ?: "")
                put("encryptedPassphrase", host.encryptedPassphrase ?: "")
                put("autoAttachCommand", host.autoAttachCommand)
                put("keepAliveIntervalSec", host.keepAliveIntervalSec)
                put("autoReconnect", host.autoReconnect)
                put("pinInDaemon", host.pinInDaemon)
                put("tag", host.tag)
            })
        }
        prefs.edit { putString(KEY_HOSTS, arr.toString()) }
    }

    private fun loadKeys(): List<SshKeyRecord> {
        val raw = prefs.getString(KEY_KEYS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SshKeyRecord(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    algorithm = obj.optString("algorithm", "Ed25519"),
                    fingerprintSha256 = obj.optString("fingerprintSha256", ""),
                    publicKeyOpenSsh = obj.optString("publicKeyOpenSsh", ""),
                    encryptedPrivateKey = obj.optString("encryptedPrivateKey", ""),
                    hasPassphrase = obj.optBoolean("hasPassphrase", false),
                    createdAtMillis = obj.optLong("createdAtMillis", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveKeys(list: List<SshKeyRecord>) {
        val arr = JSONArray()
        list.forEach { key ->
            arr.put(JSONObject().apply {
                put("id", key.id)
                put("name", key.name)
                put("algorithm", key.algorithm)
                put("fingerprintSha256", key.fingerprintSha256)
                put("publicKeyOpenSsh", key.publicKeyOpenSsh)
                put("encryptedPrivateKey", key.encryptedPrivateKey)
                put("hasPassphrase", key.hasPassphrase)
                put("createdAtMillis", key.createdAtMillis)
            })
        }
        prefs.edit { putString(KEY_KEYS, arr.toString()) }
    }

    private fun loadMacros(): List<MacroButton> {
        val raw = prefs.getString(KEY_MACROS, null) ?: return defaultMacros()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                MacroButton(
                    id = obj.getString("id"),
                    label = obj.getString("label"),
                    payload = obj.getString("payload"),
                    appendNewline = obj.optBoolean("appendNewline", true),
                    colorHex = obj.optLong("colorHex", 0xFF2DD4BFL),
                    category = MacroCategory.entries.firstOrNull { it.name == obj.optString("category") }
                        ?: MacroCategory.COMMAND,
                    sortOrder = obj.optInt("sortOrder", i)
                )
            }.sortedBy { it.sortOrder }
        } catch (_: Exception) {
            defaultMacros()
        }
    }

    private fun saveMacros(list: List<MacroButton>) {
        val arr = JSONArray()
        list.forEach { macro ->
            arr.put(JSONObject().apply {
                put("id", macro.id)
                put("label", macro.label)
                put("payload", macro.payload)
                put("appendNewline", macro.appendNewline)
                put("colorHex", macro.colorHex)
                put("category", macro.category.name)
                put("sortOrder", macro.sortOrder)
            })
        }
        prefs.edit { putString(KEY_MACROS, arr.toString()) }
    }

    private fun loadUiPreferences(): UiPreferences {
        return UiPreferences(
            useM3Color = prefs.getBoolean("ui_use_m3", false),
            showWallpaper = prefs.getBoolean("ui_show_wallpaper", true),
            wallpaperUri = prefs.getString("ui_wallpaper_uri", null),
            blurRadius = prefs.getFloat("ui_blur_radius", 18f),
            terminalScrimAlpha = prefs.getFloat("ui_terminal_scrim", 0.72f),
            terminalFontSizeSp = prefs.getFloat("ui_terminal_font_sp", 11.5f),
            terminalUseMonospace = prefs.getBoolean("ui_terminal_use_mono", true),
            terminalFontPreset = prefs.getString("ui_terminal_font_preset", "AUTO") ?: "AUTO",
            terminalCustomFontPath = prefs.getString("ui_terminal_custom_font_path", null),
            terminalCustomFontName = prefs.getString("ui_terminal_custom_font_name", null),
            terminalHorizontalScroll = prefs.getBoolean("ui_terminal_hscroll", true),
            accentDepth = prefs.getFloat("ui_accent_depth", 0.28f),
            autoOpenOAuthUrl = prefs.getBoolean("ui_auto_open_oauth", true),
            springAnimation = prefs.getBoolean("ui_spring_anim", true),
            hapticFeedback = prefs.getBoolean("ui_haptic", true),
            autoHardenWithShizuku = prefs.getBoolean("ui_auto_harden_shizuku", true),
            autoStartOnBoot = prefs.getBoolean("ui_auto_start_boot", true),
            topComponentPressedDown = prefs.getBoolean("ui_top_pressed_down", true),
            chartAxisAnimation = prefs.getBoolean("ui_chart_axis_anim", true)
        )
    }

    private fun saveUiPreferences(value: UiPreferences) {
        prefs.edit {
            putBoolean("ui_use_m3", value.useM3Color)
            putBoolean("ui_show_wallpaper", value.showWallpaper)
            putString("ui_wallpaper_uri", value.wallpaperUri)
            putFloat("ui_blur_radius", value.blurRadius)
            putFloat("ui_terminal_scrim", value.terminalScrimAlpha)
            putFloat("ui_terminal_font_sp", value.terminalFontSizeSp)
            putBoolean("ui_terminal_use_mono", value.terminalUseMonospace)
            putString("ui_terminal_font_preset", value.terminalFontPreset)
            putString("ui_terminal_custom_font_path", value.terminalCustomFontPath)
            putString("ui_terminal_custom_font_name", value.terminalCustomFontName)
            putBoolean("ui_terminal_hscroll", value.terminalHorizontalScroll)
            putFloat("ui_accent_depth", value.accentDepth)
            putBoolean("ui_auto_open_oauth", value.autoOpenOAuthUrl)
            putBoolean("ui_spring_anim", value.springAnimation)
            putBoolean("ui_haptic", value.hapticFeedback)
            putBoolean("ui_auto_harden_shizuku", value.autoHardenWithShizuku)
            putBoolean("ui_auto_start_boot", value.autoStartOnBoot)
            putBoolean("ui_top_pressed_down", value.topComponentPressedDown)
            putBoolean("ui_chart_axis_anim", value.chartAxisAnimation)
        }
    }

    private fun defaultHosts(): List<HostProfile> = listOf(
        HostProfile(
            id = "preset-lan-demo",
            name = "内网工作站 (示例)",
            host = "192.168.1.222",
            port = 22,
            username = "root",
            authType = AuthType.PASSWORD,
            autoAttachCommand = "",
            keepAliveIntervalSec = 15,
            autoReconnect = true,
            pinInDaemon = true,
            tag = "LAN"
        ),
        HostProfile(
            id = "preset-tailscale-demo",
            name = "Tailscale 节点 (OAuth 兼容)",
            host = "100.64.0.1",
            port = 22,
            username = "root",
            authType = AuthType.INTERACTIVE_OR_TAILSCALE,
            autoAttachCommand = "",
            keepAliveIntervalSec = 15,
            autoReconnect = true,
            pinInDaemon = true,
            tag = "Tailscale"
        )
    )

    private fun defaultMacros(): List<MacroButton> = listOf(
        MacroButton(id = "m-tmux", label = "tmux a", payload = "tmux new-session -A -s main", appendNewline = true, colorHex = 0xFF2DD4BFL, sortOrder = 0),
        MacroButton(id = "m-agy", label = "agy", payload = "agy", appendNewline = true, colorHex = 0xFFA78BFAL, sortOrder = 1),
        MacroButton(id = "m-ts", label = "tailscale", payload = "tailscale status", appendNewline = true, colorHex = 0xFF38BDF8L, sortOrder = 2),
        MacroButton(id = "m-ls", label = "ls -la", payload = "ls -la", appendNewline = true, colorHex = 0xFF34D399L, sortOrder = 3),
        MacroButton(id = "m-htop", label = "htop", payload = "htop", appendNewline = true, colorHex = 0xFFFBBF24L, sortOrder = 4),
        MacroButton(id = "m-ctrlc", label = "Ctrl+C", payload = "\u0003", appendNewline = false, colorHex = 0xFFF87171L, category = MacroCategory.CONTROL, sortOrder = 5),
        MacroButton(id = "m-ctrld", label = "Ctrl+D", payload = "\u0004", appendNewline = false, colorHex = 0xFFFB923CL, category = MacroCategory.CONTROL, sortOrder = 6),
        MacroButton(id = "m-clear", label = "clear", payload = "clear", appendNewline = true, colorHex = 0xFF94A3B8L, sortOrder = 7)
    )

    companion object {
        private const val PREFS_NAME = "ssh_link_terminal_store"
        private const val KEY_HOSTS = "hosts_json"
        private const val KEY_KEYS = "keys_json"
        private const val KEY_MACROS = "macros_json"
        private const val KEY_SHIZUKU_PROMPTED = "shizuku_permission_prompted"
        private const val KEY_MIGRATED_LEGACY_TMUX_V1 = "migrated_legacy_tmux_v1"
        private const val LEGACY_AUTO_ATTACH_CMD = "tmux new-session -A -s main || true"
    }
}
