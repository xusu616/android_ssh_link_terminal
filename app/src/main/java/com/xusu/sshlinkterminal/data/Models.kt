package com.xusu.sshlinkterminal.data

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class AuthType {
    PASSWORD,
    PUBLIC_KEY,
    INTERACTIVE_OR_TAILSCALE
}

enum class SessionState {
    IDLE,
    CONNECTING,
    AWAITING_OAUTH,
    AWAITING_INPUT,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    ERROR;

    fun isActiveChannel(): Boolean = when (this) {
        CONNECTED,
        CONNECTING,
        AWAITING_INPUT,
        AWAITING_OAUTH -> true
        else -> false
    }
}

enum class MacroCategory {
    CONTROL,
    COMMAND
}

data class HostProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val encryptedPassword: String? = null,
    val keyId: String? = null,
    val encryptedPassphrase: String? = null,
    val autoAttachCommand: String = "",
    val keepAliveIntervalSec: Int = 15,
    val autoReconnect: Boolean = true,
    val pinInDaemon: Boolean = true,
    val tag: String = "LAN",
    val localForwardSpec: String = ""
)

data class SshKeyRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val algorithm: String,
    val fingerprintSha256: String,
    val publicKeyOpenSsh: String,
    val encryptedPrivateKey: String,
    val hasPassphrase: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class MacroButton(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val payload: String,
    val appendNewline: Boolean = true,
    val colorHex: Long = 0xFF2DD4BFL,
    val category: MacroCategory = MacroCategory.COMMAND,
    val sortOrder: Int = 0
)

data class UiPreferences(
    val useM3Color: Boolean = false,
    val showWallpaper: Boolean = true,
    val wallpaperUri: String? = null,
    val blurRadius: Float = 18f,
    val terminalScrimAlpha: Float = 0.72f,
    val terminalFontSizeSp: Float = 11.5f,
    val terminalUseMonospace: Boolean = true,
    val terminalFontPreset: String = "AUTO",
    val terminalCustomFontPath: String? = null,
    val terminalCustomFontName: String? = null,
    val terminalNerdIconsEnabled: Boolean = true,
    val terminalNerdIconsPath: String? = null,
    val terminalHorizontalScroll: Boolean = true,
    val accentDepth: Float = 0.28f,
    val autoOpenOAuthUrl: Boolean = true,
    val springAnimation: Boolean = true,
    val hapticFeedback: Boolean = true,
    val autoHardenWithShizuku: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val topComponentPressedDown: Boolean = true,
    val chartAxisAnimation: Boolean = true,
    val terminalNonTextInput: Boolean = true,
    val hideStatusBar: Boolean = false
)

data class ColorBag(
    val vibrant: Color,
    val darkMuted: Color,
    val lightMuted: Color,
    val muted: Color,
    val useM3Color: Boolean
) {
    companion object {
        val Default = ColorBag(
            vibrant = Color(0xFF5EEAD4),
            darkMuted = Color(0xFF0F172A),
            lightMuted = Color(0xFFA7F3D0),
            muted = Color(0xFF38BDF8),
            useM3Color = false
        )
    }
}

data class RttSample(
    val timestampSec: Long,
    val rttMs: Long,
    val activeSessions: Int
)

enum class ChartWindowRule {
    MINUTE,
    HOUR,
    DAY
}

data class DetectedAuthLink(
    val url: String,
    val providerLabel: String,
    val detectedAtMillis: Long = System.currentTimeMillis()
)

data class InteractivePromptRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val title: String,
    val instruction: String,
    val prompts: List<String>,
    val echo: List<Boolean>,
    val confirmButtonText: String = "提交"
)

data class GuardStatus(
    val notificationsGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuPermissionGranted: Boolean = false,
    val dozeWhitelisted: Boolean? = null,
    val standbyBucketActive: Boolean? = null,
    val appOpsBackgroundAllowed: Boolean? = null,
    val watchdogActive: Boolean = false,
    val foregroundServiceRunning: Boolean = false,
    val lastReport: String = ""
)

data class AppLogEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String
)
