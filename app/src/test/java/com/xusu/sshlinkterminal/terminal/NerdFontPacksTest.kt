package com.xusu.sshlinkterminal.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NerdFontPacksTest {

    @Test
    fun `catalog includes optional fallback symbols and full mono nerd packs`() {
        assertTrue(NerdFontPacks.catalog.any { it.id == NerdFontPacks.FALLBACK_PACK_ID && it.role == FontPackRole.ICON_FALLBACK })
        assertTrue(NerdFontPacks.catalog.any { it.role == FontPackRole.FULL_MONO_FONT })
        assertTrue(NerdFontPacks.catalog.all { it.urls.isNotEmpty() && it.fileName.endsWith(".ttf") })
    }

    @Test
    fun `validates SFNT magic bytes and extracts font entry cleanly from zip`() {
        val tempDir = Files.createTempDirectory("nerd_pack_test").toFile()
        try {
            val validBytes = ByteArray(2048).apply {
                this[0] = 0x00
                this[1] = 0x01
                this[2] = 0x00
                this[3] = 0x00
            }
            val badFile = File(tempDir, "bad.ttf").apply { writeBytes("<html>not font</html>".toByteArray()) }
            assertFalse(NerdFontPacks.hasValidSfntHeader(badFile))

            val zipBytes = ByteArrayOutputStream().use { bos ->
                ZipOutputStream(bos).use { zos ->
                    zos.putNextEntry(ZipEntry("nested/SymbolsNerdFontMono-Regular.ttf"))
                    zos.write(validBytes)
                    zos.closeEntry()
                }
                bos.toByteArray()
            }
            val extracted = File(tempDir, "extracted.ttf")
            val ok = NerdFontPacks.extractEntryFromZipStream(
                ByteArrayInputStream(zipBytes),
                "SymbolsNerdFontMono-Regular.ttf",
                extracted
            )
            assertTrue(ok)
            assertTrue(NerdFontPacks.hasValidSfntHeader(extracted))
            assertEquals(2048L, extracted.length())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
