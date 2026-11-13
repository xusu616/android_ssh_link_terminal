package com.xusu.sshlinkterminal.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette
import com.xusu.sshlinkterminal.R
import com.xusu.sshlinkterminal.data.ColorBag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LinkTerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    hideStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!LocalInspectionMode.current && view.context is Activity) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        if (hideStatusBar) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
            controller.isAppearanceLightStatusBars = !darkTheme
        }
    }

    val colorScheme = when {
        dynamicColor && !LocalInspectionMode.current && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFF5EEAD4),
            secondary = Color(0xFF38BDF8),
            tertiary = Color(0xFFC4B5FD),
            background = Color(0xFF090D16),
            surface = Color(0xFF0F172A)
        )
        else -> lightColorScheme(
            primary = Color(0xFF0F766E),
            secondary = Color(0xFF0284C7),
            tertiary = Color(0xFF6D28D9)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(content = content)
    }
}

@Composable
fun WallpaperControlsTheme(
    colorBag: ColorBag,
    content: @Composable () -> Unit
) {
    val base = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val accent = if (dark) colorBag.vibrant else colorBag.muted
    val onAccent = if (dark) colorBag.darkMuted else colorBag.lightMuted
    val colors = if (colorBag.useM3Color) {
        base
    } else {
        base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = colorBag.vibrant,
            onPrimaryContainer = colorBag.darkMuted,
            secondary = colorBag.muted,
            onSecondary = onAccent,
            secondaryContainer = colorBag.vibrant.copy(alpha = 0.85f),
            onSecondaryContainer = colorBag.darkMuted,
            tertiary = colorBag.lightMuted,
            onTertiary = colorBag.darkMuted,
            surfaceTint = accent
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun getDisplayCutoutTopPadding(): Dp =
    WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()

@Composable
fun getDeviceCornerRadius(): Dp {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !LocalInspectionMode.current) {
        val windowInsets = LocalView.current.rootWindowInsets
        val roundedCorner = windowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
        roundedCorner?.radius?.let { with(LocalDensity.current) { it.toDp() } } ?: 28.dp
    } else {
        28.dp
    }
}

data class LoadedHeroWallpaper(
    val imageBitmap: ImageBitmap,
    val colorBag: ColorBag
)

suspend fun loadHeroWallpaperAndPalette(
    context: Context,
    wallpaperUri: String?,
    useM3Color: Boolean
): LoadedHeroWallpaper = withContext(Dispatchers.IO) {
    val bitmap = loadBitmapFromUriOrDefault(context, wallpaperUri)
    val palette = Palette.from(bitmap).generate()
    val vibrant = Color(palette.getVibrantColor(0xFF5EEAD4.toInt()))
    val darkMuted = Color(palette.getDarkMutedColor(0xFF0F172A.toInt()))
    val lightMuted = Color(palette.getLightMutedColor(0xFFA7F3D0.toInt()))
    val muted = Color(palette.getMutedColor(0xFF38BDF8.toInt()))
    LoadedHeroWallpaper(
        imageBitmap = bitmap.asImageBitmap(),
        colorBag = ColorBag(
            vibrant = vibrant,
            darkMuted = darkMuted,
            lightMuted = lightMuted,
            muted = muted,
            useM3Color = useM3Color
        )
    )
}

private fun loadBitmapFromUriOrDefault(context: Context, uriString: String?): Bitmap {
    if (!uriString.isNullOrBlank()) {
        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val decoded = BitmapFactory.decodeStream(stream, null, options)
                if (decoded != null) return decoded
            }
        } catch (_: Exception) {
        }
    }
    val drawable = ContextCompat.getDrawable(context, R.drawable.hero_cyber_mesh)
    val width = 800
    val height = 600
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable?.setBounds(0, 0, width, height)
    drawable?.draw(canvas)
    return bmp
}
