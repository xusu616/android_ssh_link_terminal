package com.xusu.sshlinkterminal.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class AnsiEmulatorTest {

    @Test
    fun `decodes chunk-split UTF-8 CJK characters accurately`() {
        val emulator = AnsiEmulator(initialCols = 40, initialRows = 10)
        val raw = "连接成功: 中文终端".toByteArray(StandardCharsets.UTF_8)
        // Split right in the middle of a multi-byte UTF-8 sequence
        val splitIdx = 11
        emulator.feedBytes(raw.copyOfRange(0, splitIdx))
        emulator.feedBytes(raw.copyOfRange(splitIdx, raw.size))

        val lines = emulator.snapshotLines()
        assertEquals(1, lines.size)
        assertEquals("连接成功: 中文终端", lines[0].plainText)
    }

    @Test
    fun `parses SGR colors and extracts clickable URLs`() {
        val emulator = AnsiEmulator(initialCols = 100, initialRows = 10)
        emulator.feedText("\u001B[32mVisit:\u001B[0m https://login.tailscale.com/a/check_test99\r\n")

        val lines = emulator.snapshotLines()
        assertTrue(lines.isNotEmpty())
        val firstLine = lines[0]
        assertEquals("Visit: https://login.tailscale.com/a/check_test99", firstLine.plainText)
        assertTrue(firstLine.spans.any { it.linkUrl == "https://login.tailscale.com/a/check_test99" })
    }

    @Test
    fun `supports alternate screen buffer restore`() {
        val emulator = AnsiEmulator(initialCols = 40, initialRows = 6)
        emulator.feedText("primary-line\r\n")
        emulator.feedText("\u001B[?1049h\u001B[2Jhtop-screen")
        assertEquals("htop-screen", emulator.snapshotLines()[0].plainText)

        emulator.feedText("\u001B[?1049l")
        assertEquals("primary-line", emulator.snapshotLines()[0].plainText)
    }

    @Test
    fun `shrinking primary screen height preserves bottom prompt via scrollback`() {
        val emulator = AnsiEmulator(initialCols = 40, initialRows = 8)
        for (i in 1..6) {
            emulator.feedText("line-$i\r\n")
        }
        emulator.feedText("Enter code: ")

        // Shrink viewport height (e.g. OAuth banner or IME opened)
        emulator.resize(40, 4)

        val lines = emulator.snapshotLines().map { it.plainText }
        assertTrue("Expected earlier scrollback lines preserved: $lines", lines.contains("line-1"))
        assertTrue("Expected bottom prompt preserved instead of clipped: $lines", lines.any { it.contains("Enter code:") })
    }

    @Test
    fun `handles mixed ASCII and CJK wide characters cleanly`() {
        val emulator = AnsiEmulator(initialCols = 20, initialRows = 4)
        assertTrue(AnsiEmulator.isWideChar('中'))
        emulator.feedText("AB中文CD")

        val lines = emulator.snapshotLines()
        assertEquals(1, lines.size)
        assertEquals("AB中文CD", lines[0].plainText)
    }

    @Test
    fun `clamps extreme CSI movement and erase parameters without overflow`() {
        val emulator = AnsiEmulator(initialCols = 20, initialRows = 6)
        emulator.feedText("hello")
        emulator.feedText("\u001B[2147483647B\u001B[2147483647C\u001B[2147483647P!")
        val lines = emulator.snapshotLines().map { it.plainText }
        assertTrue(lines.any { it.contains("!") })
    }
}
