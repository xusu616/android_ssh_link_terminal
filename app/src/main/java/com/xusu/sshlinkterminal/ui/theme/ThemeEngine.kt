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
import androidx.compose.ui.graphics.luminance
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
    val onAccent = contrastOnColor(accent)
    val secondaryContainer = if (dark) colorBag.vibrant.copy(alpha = 0.90f) else colorBag.muted.copy(alpha = 0.90f)
    val onSecondaryContainer = contrastOnColor(if (dark) colorBag.vibrant else colorBag.muted)
    val colors = if (colorBag.useM3Color) {
        base
    } else {
        base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = colorBag.vibrant,
            onPrimaryContainer = contrastOnColor(colorBag.vibrant),
            secondary = colorBag.muted,
            onSecondary = contrastOnColor(colorBag.muted),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = colorBag.lightMuted,
            onTertiary = contrastOnColor(colorBag.lightMuted),
            surfaceTint = accent
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * Returns a high-contrast foreground color (deep dark ink or crisp off-white) for a given background.
 */
fun contrastOnColor(background: Color): Color {
    return if (background.luminance() > 0.30f) Color(0xFF07111E) else Color(0xFFF8FAFC)
}

/**
 * Deepens an extracted wallpaper accent color while strictly preserving its extracted hue.
 *
 * Manual tuning guide:
 * - Increase [depth] (0f..0.70f, default 0.28f) for darker, richer jewel-toned buttons and fills.
 * - Adjust the lightness bounds inside coerceIn(minL, maxL) below to shift overall brightness.
 */
internal fun deepenExtractedAccent(
    color: Color,
    depth: Float = 0.28f
): Color {
    val hsl = colorToHsl(color)
    val clampedDepth = depth.coerceIn(0f, 0.70f)
    if (hsl[1] > 0.06f) {
        hsl[1] = (hsl[1] * (1f + clampedDepth * 0.45f)).coerceIn(0.46f, 0.94f)
    }
    val targetL = hsl[2] * (1f - clampedDepth * 0.62f)
    val maxL = (0.58f - clampedDepth * 0.22f).coerceIn(0.30f, 0.58f)
    hsl[2] = targetL.coerceIn(0.24f, maxL)
    return hslToColor(hsl, alpha = color.alpha)
}

internal fun boostAccentColor(
    color: Color,
    minLightness: Float = 0.64f,
    maxLightness: Float = 0.78f,
    minSaturation: Float = 0.56f
): Color {
    val hsl = colorToHsl(color)
    if (hsl[1] > 0.08f) {
        hsl[1] = hsl[1].coerceAtLeast(minSaturation)
    }
    hsl[2] = hsl[2].coerceIn(minLightness, maxLightness)
    return hslToColor(hsl, alpha = color.alpha)
}

internal fun clampDarkSurfaceColor(color: Color, maxLightness: Float = 0.16f): Color {
    val hsl = colorToHsl(color)
    hsl[2] = hsl[2].coerceAtMost(maxLightness)
    return hslToColor(hsl, alpha = color.alpha)
}

internal fun clampLightAccentColor(color: Color, minLightness: Float = 0.78f): Color {
    val hsl = colorToHsl(color)
    hsl[2] = hsl[2].coerceAtLeast(minLightness)
    return hslToColor(hsl, alpha = color.alpha)
}

private fun colorToHsl(color: Color): FloatArray {
    val r = color.red.coerceIn(0f, 1f)
    val g = color.green.coerceIn(0f, 1f)
    val b = color.blue.coerceIn(0f, 1f)
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val l = (max + min) / 2f
    val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == r -> ((g - b) / d + (if (g < b) 6f else 0f)) * 60f
        max == g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }
    return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hslToColor(hsl: FloatArray, alpha: Float = 1f): Color {
    val h = ((hsl[0] % 360f) + 360f) % 360f
    val s = hsl[1].coerceIn(0f, 1f)
    val l = hsl[2].coerceIn(0f, 1f)
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
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
    useM3Color: Boolean,
    accentDepth: Float = 0.28f
): LoadedHeroWallpaper = withContext(Dispatchers.IO) {
    val bitmap = loadBitmapFromUriOrDefault(context, wallpaperUri)
    val paletteBitmap = scaleDownForPalette(bitmap, maxSide = 192)
    val palette = try {
        Palette.from(paletteBitmap).maximumColorCount(24).generate()
    } finally {
        if (paletteBitmap !== bitmap && !paletteBitmap.isRecycled) {
            paletteBitmap.recycle()
        }
    }

    val bestChromaticSwatch = palette.swatches
        .filter { it.hsl[1] >= 0.22f && it.hsl[2] in 0.15f..0.85f }
        .maxByOrNull { it.population * (0.5f + it.hsl[1]) }

    val primarySwatch = palette.vibrantSwatch
        ?: bestChromaticSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.dominantSwatch

    val primaryHue = primarySwatch?.hsl?.getOrNull(0)
    val distinctSecondary = if (primaryHue != null) {
        palette.swatches
            .filter {
                val dh = kotlin.math.abs(it.hsl[0] - primaryHue)
                val hueDist = kotlin.math.min(dh, 360f - dh)
                it.hsl[1] >= 0.20f && it.hsl[2] in 0.14f..0.85f && hueDist >= 35f
            }
            .maxByOrNull { it.population * (0.5f + it.hsl[1]) }
    } else null

    val primaryRaw = primarySwatch?.rgb ?: 0xFF0F766E.toInt()
    val secondaryRaw = distinctSecondary?.rgb
        ?: palette.darkVibrantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: palette.vibrantSwatch?.rgb
        ?: 0xFF0284C7.toInt()
    val darkMutedRaw = palette.darkMutedSwatch?.rgb ?: 0xFF0F172A.toInt()
    val lightMutedRaw = palette.lightVibrantSwatch?.rgb
        ?: palette.lightMutedSwatch?.rgb
        ?: primaryRaw

    val vibrant = deepenExtractedAccent(Color(primaryRaw), depth = accentDepth)
    val muted = deepenExtractedAccent(Color(secondaryRaw), depth = (accentDepth * 0.72f).coerceIn(0f, 0.70f))
    val darkMuted = clampDarkSurfaceColor(Color(darkMutedRaw), maxLightness = 0.14f)
    val lightMuted = boostAccentColor(Color(lightMutedRaw), minLightness = 0.66f, maxLightness = 0.80f, minSaturation = 0.50f)

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

private fun scaleDownForPalette(source: Bitmap, maxSide: Int = 192): Bitmap {
    val longest = maxOf(source.width, source.height).coerceAtLeast(1)
    if (longest <= maxSide) return source
    val scale = maxSide.toFloat() / longest.toFloat()
    val w = (source.width * scale).toInt().coerceAtLeast(1)
    val h = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true)
}

private fun loadBitmapFromUriOrDefault(context: Context, uriString: String?): Bitmap {
    if (!uriString.isNullOrBlank()) {
        try {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest > 0 && longest / sampleSize > 1440) {
                sampleSize *= 2
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
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
