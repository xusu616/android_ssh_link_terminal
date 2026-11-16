package com.xusu.sshlinkterminal.terminal

import android.content.Context
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

enum class FontPackRole {
    ICON_FALLBACK,
    FULL_MONO_FONT
}

data class NerdFontPackSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val approxSizeLabel: String,
    val fileName: String,
    val role: FontPackRole,
    val urls: List<String>,
    val archiveEntryName: String? = null
)

object NerdFontPacks {
    const val FALLBACK_PACK_ID = "nerd_symbols_mono"

    val catalog: List<NerdFontPackSpec> = listOf(
        NerdFontPackSpec(
            id = FALLBACK_PACK_ID,
            title = "Nerd Fonts Symbols 图标补充包（推荐）",
            subtitle = "含文件夹、Arch/Linux、Git、时钟、Powerline 等全套终端图标；叠加在现有字体上自动填补框框",
            approxSizeLabel = "~2.3 MB",
            fileName = "nerd_symbols_mono.ttf",
            role = FontPackRole.ICON_FALLBACK,
            urls = listOf(
                "https://cdn.jsdelivr.net/gh/ryanoasis/nerd-fonts@v3.3.0/patched-fonts/NerdFontsSymbolsOnly/SymbolsNerdFontMono-Regular.ttf",
                "https://raw.githubusercontent.com/ryanoasis/nerd-fonts/v3.3.0/patched-fonts/NerdFontsSymbolsOnly/SymbolsNerdFontMono-Regular.ttf",
                "https://github.com/ryanoasis/nerd-fonts/releases/latest/download/NerdFontsSymbolsOnly.zip"
            ),
            archiveEntryName = "SymbolsNerdFontMono-Regular.ttf"
        ),
        NerdFontPackSpec(
            id = "jetbrains_mono_nf",
            title = "JetBrainsMono Nerd Font Mono",
            subtitle = "高清晰度开发等宽字体，原生内嵌完整 Nerd Fonts 图标，安装后可一键设为终端主字体",
            approxSizeLabel = "~2.8 MB",
            fileName = "jetbrains_mono_nf.ttf",
            role = FontPackRole.FULL_MONO_FONT,
            urls = listOf(
                "https://cdn.jsdelivr.net/gh/ryanoasis/nerd-fonts@v3.3.0/patched-fonts/JetBrainsMono/Ligatures/Regular/JetBrainsMonoNerdFontMono-Regular.ttf",
                "https://raw.githubusercontent.com/ryanoasis/nerd-fonts/v3.3.0/patched-fonts/JetBrainsMono/Ligatures/Regular/JetBrainsMonoNerdFontMono-Regular.ttf"
            )
        ),
        NerdFontPackSpec(
            id = "meslo_lgm_nf",
            title = "MesloLG Nerd Font Mono",
            subtitle = "Powerlevel10k / Tide / Starship 官方推荐等宽字体，兼容各类箭头与状态栏符号",
            approxSizeLabel = "~2.6 MB",
            fileName = "meslo_lgm_nf.ttf",
            role = FontPackRole.FULL_MONO_FONT,
            urls = listOf(
                "https://cdn.jsdelivr.net/gh/ryanoasis/nerd-fonts@v3.3.0/patched-fonts/Meslo/M/Regular/MesloLGMNerdFontMono-Regular.ttf",
                "https://raw.githubusercontent.com/ryanoasis/nerd-fonts/v3.3.0/patched-fonts/Meslo/M/Regular/MesloLGMNerdFontMono-Regular.ttf"
            )
        )
    )

    fun fontsDir(context: Context): File = File(context.filesDir, "fonts").apply { mkdirs() }

    fun packFile(context: Context, spec: NerdFontPackSpec): File = File(fontsDir(context), spec.fileName)

    fun isPackInstalled(context: Context, spec: NerdFontPackSpec): Boolean {
        val file = packFile(context, spec)
        return file.isFile && file.canRead() && file.length() > 1024L && hasValidSfntHeader(file)
    }

    fun installedSizeBytes(context: Context, spec: NerdFontPackSpec): Long {
        val file = packFile(context, spec)
        return if (file.isFile) file.length() else 0L
    }

    fun resolveIconFallbackFile(context: Context, configuredPath: String?): File? {
        if (!configuredPath.isNullOrBlank()) {
            val file = File(configuredPath)
            if (file.isFile && file.canRead() && file.length() > 1024L) return file
        }
        val defaultSpec = catalog.firstOrNull { it.role == FontPackRole.ICON_FALLBACK } ?: return null
        val defaultFile = packFile(context, defaultSpec)
        return if (defaultFile.isFile && defaultFile.canRead() && defaultFile.length() > 1024L) {
            defaultFile
        } else {
            null
        }
    }

    fun hasValidSfntHeader(file: File): Boolean {
        if (!file.isFile || file.length() < 16L) return false
        return runCatching {
            val header = ByteArray(4)
            file.inputStream().use { input ->
                if (input.read(header) < 4) return@runCatching false
            }
            val tag = String(header, Charsets.US_ASCII)
            val isSfntTrueType = header[0] == 0.toByte() &&
                header[1] == 1.toByte() &&
                header[2] == 0.toByte() &&
                header[3] == 0.toByte()
            isSfntTrueType || tag == "OTTO" || tag == "ttcf" || tag == "true" || tag == "typ1"
        }.getOrDefault(false)
    }

    fun isValidAndroidFontFile(file: File): Boolean {
        if (!hasValidSfntHeader(file)) return false
        return runCatching { Typeface.createFromFile(file) != null }.getOrDefault(false)
    }

    internal fun extractEntryFromZipStream(
        input: InputStream,
        entryFileName: String,
        destFile: File
    ): Boolean {
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory &&
                    (entry.name == entryFileName || entry.name.endsWith("/$entryFileName"))
                ) {
                    destFile.outputStream().use { out ->
                        zis.copyTo(out)
                    }
                    return destFile.length() > 1024L
                }
                entry = zis.nextEntry
            }
        }
        return false
    }

    suspend fun installPack(
        context: Context,
        spec: NerdFontPackSpec,
        onProgress: (Float?) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = fontsDir(context)
        val dest = File(dir, spec.fileName)
        val tmp = File(dir, "${spec.fileName}.tmp")
        var lastError: Throwable? = null

        for (rawUrl in spec.urls) {
            tmp.delete()
            var conn: HttpURLConnection? = null
            try {
                onProgress(0f)
                conn = openFollowingRedirects(rawUrl)
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code")
                }
                val totalBytes = conn.contentLengthLong
                val isZip = rawUrl.endsWith(".zip", ignoreCase = true)
                conn.inputStream.use { input ->
                    if (isZip) {
                        onProgress(null)
                        val targetName = spec.archiveEntryName
                            ?: throw IllegalStateException("缺少 ZIP 目标字体名")
                        if (!extractEntryFromZipStream(input, targetName, tmp)) {
                            throw IllegalStateException("压缩包内未找到 $targetName")
                        }
                    } else {
                        copyStreamWithProgress(input, tmp, totalBytes, onProgress)
                    }
                }
                if (!isValidAndroidFontFile(tmp)) {
                    throw IllegalStateException("字体文件校验失败")
                }
                if (dest.exists()) dest.delete()
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                onProgress(1f)
                return@withContext Result.success(dest)
            } catch (e: Throwable) {
                lastError = e
                tmp.delete()
            } finally {
                conn?.disconnect()
            }
        }
        Result.failure(lastError ?: IllegalStateException("下载失败"))
    }

    fun uninstallPack(context: Context, spec: NerdFontPackSpec): Boolean {
        val file = packFile(context, spec)
        return !file.exists() || file.delete()
    }

    private fun openFollowingRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "LinkTerm-Android")
            }
            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 ||
                status == 308
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("重定向缺少 Location")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("重定向次数过多")
    }

    private fun copyStreamWithProgress(
        input: InputStream,
        dest: File,
        totalBytes: Long,
        onProgress: (Float?) -> Unit
    ) {
        val buf = ByteArray(16_384)
        var downloaded = 0L
        dest.outputStream().use { out ->
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                if (read == 0) continue
                out.write(buf, 0, read)
                downloaded += read
                if (totalBytes > 0L) {
                    onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.98f))
                } else {
                    onProgress(null)
                }
            }
        }
    }
}
