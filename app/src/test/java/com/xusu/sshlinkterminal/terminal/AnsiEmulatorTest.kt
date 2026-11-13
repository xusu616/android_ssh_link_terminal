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
}
