package com.xusu.sshlinkterminal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeEngineTest {

    @Test
    fun `boostAccentColor elevates dark moody wallpaper swatches to luminous accent`() {
        val darkTeal = Color(0xFF103038)
        val boosted = boostAccentColor(darkTeal)
        assertTrue("Expected boosted accent luminance (${boosted.luminance()}) to exceed 0.25", boosted.luminance() > 0.25f)
    }

    @Test
    fun `deepenExtractedAccent deepens accents while keeping distinct extracted hues`() {
        val warmCoral = Color(0xFFF97316)
        val emerald = Color(0xFF34D399)
        val royalViolet = Color(0xFFA78BFA)

        val deepCoral = deepenExtractedAccent(warmCoral, depth = 0.35f)
        val shallowCoral = deepenExtractedAccent(warmCoral, depth = 0.05f)
        val deepEmerald = deepenExtractedAccent(emerald, depth = 0.35f)
        val deepViolet = deepenExtractedAccent(royalViolet, depth = 0.35f)

        assertTrue("Higher depth should darken extracted accent", deepCoral.luminance() < shallowCoral.luminance())
        assertTrue("Coral dominant channel must remain red", deepCoral.red > deepCoral.green && deepCoral.red > deepCoral.blue)
        assertTrue("Emerald dominant channel must remain green", deepEmerald.green > deepEmerald.red && deepEmerald.green > deepEmerald.blue)
        assertTrue("Violet dominant channel must remain blue", deepViolet.blue > deepViolet.red && deepViolet.blue > deepViolet.green)
    }

    @Test
    fun `contrastOnColor picks crisp white on dark background and deep ink on bright accent`() {
        val onBrightCyan = contrastOnColor(Color(0xFF5EEAD4))
        val onDeepNavy = contrastOnColor(Color(0xFF0A101E))
        val onDeepCrimson = contrastOnColor(deepenExtractedAccent(Color(0xFFEF4444), depth = 0.35f))
        assertTrue("Expected dark ink foreground on bright cyan", onBrightCyan.luminance() < 0.1f)
        assertTrue("Expected crisp light foreground on deep navy", onDeepNavy.luminance() > 0.8f)
        assertTrue("Expected crisp light foreground on deep crimson accent", onDeepCrimson.luminance() > 0.8f)
    }
}
