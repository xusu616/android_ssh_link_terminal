package com.xusu.sshlinkterminal.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `responds to DA1 DA2 XTVERSION DSR DECRQM and OSC color queries immediately`() {
        val replies = mutableListOf<String>()
        val emulator = AnsiEmulator(
            initialCols = 40,
            initialRows = 10,
            onTerminalResponse = { replies += it }
        )

        emulator.feedText("ab")
        emulator.feedText("\u001B[c")
        emulator.feedText("\u001B[0c")
        emulator.feedText("\u001BZ")
        emulator.feedText("\u001B[>c")
        emulator.feedText("\u001B[>0q")
        emulator.feedText("\u001B[5n")
        emulator.feedText("\u001B[6n")
        emulator.feedText("\u001B[?6n")
        emulator.feedText("\u001B[?2026\$p")
        emulator.feedText("\u001B]10;?\u0007")
        emulator.feedText("\u001B]11;?\u001B\\")

        assertEquals(
            listOf(
                "\u001B[?62;22c",
                "\u001B[?62;22c",
                "\u001B[?62;22c",
                "\u001B[>0;10;1c",
                "\u001BP>|LinkTerm(1.0.8)\u001B\\",
                "\u001B[0n",
                "\u001B[1;3R",
                "\u001B[?1;3R",
                "\u001B[?2026;0\$y",
                "\u001B]10;rgb:e2e2/e8e8/f0f0\u001B\\",
                "\u001B]11;rgb:0707/0b0b/1212\u001B\\"
            ),
            replies
        )
    }

    @Test
    fun `handles charset designation DCS APC colon SGR and ignores private equals less CSI`() {
        val replies = mutableListOf<String>()
        val emulator = AnsiEmulator(
            initialCols = 40,
            initialRows = 6,
            onTerminalResponse = { replies += it }
        )

        emulator.feedText("\u001B(Bhi\u001B)0")
        emulator.feedText("\u001BP+q544e\u001B\\")
        emulator.feedText("\u001B_Gi=1\u001B\\")
        emulator.feedText("\u001B[=c\u001B[<u")
        emulator.feedText("\u001B[4:3;38:2::255:128:64mOK\u001B[0m")
        emulator.feedText("\u001B]10;?;?\u0007")

        val lines = emulator.snapshotLines()
        assertEquals("hiOK", lines[0].plainText)
        val okSpan = lines[0].spans.last()
        assertEquals("OK", okSpan.text)
        assertTrue(okSpan.style.underline)
        assertEquals(0xFFFF8040.toInt(), okSpan.style.fgRgb)
        assertEquals(
            listOf(
                "\u001B]10;rgb:e2e2/e8e8/f0f0\u001B\\",
                "\u001B]11;rgb:0707/0b0b/1212\u001B\\"
            ),
            replies
        )

        // Interrupted escape / incomplete UTF-8 followed by clear() recovers cleanly
        emulator.feedBytes(byteArrayOf(0x1B, '['.code.toByte(), 0xE4.toByte()))
        emulator.clear()
        emulator.feedText("clean")
        assertEquals("clean", emulator.snapshotLines()[0].plainText)
    }

    @Test
    fun `wraps high surrogate cleanly at line end without splitting surrogate pair`() {
        val emulator = AnsiEmulator(initialCols = 20, initialRows = 8)
        val prefix = "a".repeat(19)
        emulator.feedText(prefix + "\uD83D\uDE00")
        val lines = emulator.snapshotLines().map { it.plainText }
        assertEquals(2, lines.size)
        assertEquals(prefix, lines[0])
        assertEquals("\uD83D\uDE00", lines[1])
    }

    @Test
    fun `exits alternate screen before reconnect banner and increments snapshot sequence monotonically`() {
        val emulator = AnsiEmulator(initialCols = 50, initialRows = 8)
        val seq0 = emulator.snapshotState().sequence
        emulator.feedText("shell-before-tui\r\n")
        val seq1 = emulator.snapshotState().sequence
        assertTrue(seq1 > seq0)

        emulator.feedText("\u001B[?1049h\u001B[2Jagy-running-tui")
        assertEquals(listOf("agy-running-tui"), emulator.snapshotLines().map { it.plainText })

        assertTrue(emulator.exitAltScreenIfActive())
        assertFalse(emulator.exitAltScreenIfActive())
        val seq2 = emulator.snapshotState().sequence
        assertTrue(seq2 > seq1)

        emulator.feedText("[远端会话已关闭 (EOF)]\r\n")
        val recovered = emulator.snapshotLines().map { it.plainText }
        assertTrue("Expected primary screen restored: $recovered", recovered.any { it.contains("shell-before-tui") })
        assertTrue("Expected EOF notice preserved: $recovered", recovered.any { it.contains("EOF") })
    }
}
