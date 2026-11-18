package com.xusu.sshlinkterminal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

/** MIME types commonly reported for .ttf/.otf/.ttc files; many file managers fall back to octet-stream. */
internal val FONT_FILE_MIME_TYPES = arrayOf(
    "font/ttf",
    "font/otf",
    "font/sfnt",
    "font/collection",
    "application/x-font-ttf",
    "application/x-font-otf",
    "application/x-font-truetype",
    "application/x-font-opentype",
    "application/font-sfnt",
    "application/vnd.ms-opentype",
    "application/octet-stream"
)

/**
 * SAF `ACTION_OPEN_DOCUMENT` restricted to font MIME types and opened in the Downloads folder.
 * Passing a plain star-slash-star filter makes some ROMs (e.g. ColorOS) show the media/photo picker instead of the file browser.
 */
class OpenFontDocumentContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, FONT_FILE_MIME_TYPES)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            runCatching {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download")
                )
            }
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}

/**
 * Fallback: `ACTION_GET_CONTENT` wrapped in a system chooser so the user can pick any installed
 * file manager (system Files, MT Manager, ...) when the default document picker misbehaves.
 */
class ChooseFontFileContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        val inner = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, FONT_FILE_MIME_TYPES)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
        return Intent.createChooser(inner, "选择字体文件 (.ttf / .otf)")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}
