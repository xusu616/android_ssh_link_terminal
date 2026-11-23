package com.xusu.sshlinkterminal.terminal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.xusu.sshlinkterminal.data.DetectedAuthLink

object LinkDetector {

    private val URL_REGEX = Regex("""https?://[^\s<>"'`{}\\|^\]\u2500-\u257F]+""", RegexOption.IGNORE_CASE)
    private val TRAILING_PUNCT = charArrayOf('.', ',', ';', ':', ')', ']', '>', '!', '?', '\'', '"')

    data class UrlMatch(
        val url: String,
        val startIndex: Int,
        val endIndexExclusive: Int
    )

    fun extractUrls(text: String): List<UrlMatch> {
        if (text.isEmpty()) return emptyList()
        return URL_REGEX.findAll(text).mapNotNull { match ->
            var end = match.range.last + 1
            var raw = match.value
            while (raw.isNotEmpty() && raw.last() in TRAILING_PUNCT) {
                // Keep balanced ')' if '(' exists in the URL
                if (raw.last() == ')' && raw.count { it == '(' } >= raw.count { it == ')' }) {
                    break
                }
                raw = raw.dropLast(1)
                end--
            }
            if (raw.length > 10) {
                UrlMatch(url = raw, startIndex = match.range.first, endIndexExclusive = end)
            } else {
                null
            }
        }.toList()
    }

    fun detectAuthLink(text: String): DetectedAuthLink? {
        val matches = extractUrls(text)
        if (matches.isEmpty()) return null
        val lowerText = text.lowercase()
        val hasAuthContext = lowerText.contains("authenticate") ||
            lowerText.contains("oauth") ||
            lowerText.contains("visit:") ||
            lowerText.contains("open this url") ||
            lowerText.contains("authorize") ||
            lowerText.contains("sign in") ||
            lowerText.contains("login") ||
            lowerText.contains("verification") ||
            lowerText.contains("验证") ||
            lowerText.contains("授权")

        for (match in matches) {
            val lowerUrl = match.url.lowercase()
            val provider = classifyAuthProvider(lowerUrl, hasAuthContext)
            if (provider != null) {
                return DetectedAuthLink(url = match.url, providerLabel = provider)
            }
        }
        return null
    }

    private fun classifyAuthProvider(lowerUrl: String, hasAuthContext: Boolean): String? {
        return when {
            lowerUrl.contains("login.tailscale.com") -> "Tailscale SSH / 节点验证"
            lowerUrl.contains("accounts.google.com") -> "Google / Antigravity OAuth"
            lowerUrl.contains("github.com/login") -> "GitHub 设备授权"
            lowerUrl.contains("login.microsoftonline.com") -> "Microsoft Entra 授权"
            hasAuthContext && (
                lowerUrl.contains("oauth") ||
                    lowerUrl.contains("auth") ||
                    lowerUrl.contains("login") ||
                    lowerUrl.contains("sso") ||
                    lowerUrl.contains("device") ||
                    lowerUrl.contains("verify") ||
                    lowerUrl.contains("check_")
                ) -> "OAuth / 登录验证"
            else -> null
        }
    }

    fun openUrlInBrowser(context: Context, url: String): Boolean {
        val uri = try {
            Uri.parse(url.trim())
        } catch (_: Exception) {
            return false
        }
        val needsNewTask = context !is Activity
        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            if (needsNewTask) {
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(fallback)
                true
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "未找到可用的浏览器打开链接", Toast.LENGTH_SHORT).show()
                false
            } catch (_: Exception) {
                Toast.makeText(context, "打开链接失败", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}
