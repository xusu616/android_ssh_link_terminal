package com.xusu.sshlinkterminal.terminal

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class TerminalStyle(
    val fgRgb: Int? = null,
    val bgRgb: Int? = null,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val inverse: Boolean = false
)

data class TerminalSpan(
    val text: String,
    val style: TerminalStyle,
    val linkUrl: String? = null
)

data class TerminalRenderLine(
    val spans: List<TerminalSpan>,
    val plainText: String
)

private data class Cell(
    var ch: Char = ' ',
    var style: TerminalStyle = TerminalStyle(),
    var oscLink: String? = null
)

/**
 * Lightweight, chunk-safe UTF-8 VT100/ANSI terminal emulator supporting scrollback, alternate screen
 * (`tmux`/`vim`/`htop`), 16/256/truecolor SGR, OSC titles/links, and clickable URL extraction.
 */
class AnsiEmulator(
    initialCols: Int = 80,
    initialRows: Int = 24,
    private val maxScrollback: Int = 1800
) {
    var cols: Int = initialCols.coerceIn(20, 240)
        private set
    var rows: Int = initialRows.coerceIn(8, 120)
        private set

    var windowTitle: String = ""
        private set

    private var primaryScreen = createScreen(rows, cols)
    private var altScreen = createScreen(rows, cols)
    private var usingAltScreen = false
    private val scrollback = ArrayDeque<Array<Cell>>()

    private var cursorRow = 0
    private var cursorCol = 0
    private var savedCursorRow = 0
    private var savedCursorCol = 0
    private var currentStyle = TerminalStyle()
    private var currentOscLink: String? = null
    private var scrollTop = 0
    private var scrollBottom = rows - 1

    private enum class ParserState { TEXT, ESC, CSI, OSC, OSC_ESC }

    private var state = ParserState.TEXT
    private val csiBuf = StringBuilder()
    private val oscBuf = StringBuilder()

    // Chunk-safe UTF-8 decoder state
    private val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    private var pendingBytes = ByteArray(0)

    private val lock = Any()

    fun resize(newCols: Int, newRows: Int): Unit = synchronized(lock) {
        val targetCols = newCols.coerceIn(20, 240)
        val targetRows = newRows.coerceIn(8, 120)
        if (targetCols == cols && targetRows == rows) return@synchronized

        primaryScreen = resizeGrid(primaryScreen, targetRows, targetCols)
        altScreen = resizeGrid(altScreen, targetRows, targetCols)
        cols = targetCols
        rows = targetRows
        scrollTop = 0
        scrollBottom = rows - 1
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
    }

    fun feedBytes(bytes: ByteArray, length: Int = bytes.size): String {
        val combined = if (pendingBytes.isEmpty()) {
            bytes.copyOf(length)
        } else {
            ByteArray(pendingBytes.size + length).also {
                System.arraycopy(pendingBytes, 0, it, 0, pendingBytes.size)
                System.arraycopy(bytes, 0, it, pendingBytes.size, length)
            }
        }
        val inBuf = ByteBuffer.wrap(combined)
        val outBuf = CharBuffer.allocate(combined.size * 2 + 4)
        utf8Decoder.decode(inBuf, outBuf, false)
        pendingBytes = if (inBuf.hasRemaining()) {
            ByteArray(inBuf.remaining()).also { inBuf.get(it) }
        } else {
            ByteArray(0)
        }
        outBuf.flip()
        val decoded = outBuf.toString()
        if (decoded.isNotEmpty()) {
            feedText(decoded)
        }
        return decoded
    }

    fun feedText(text: String) = synchronized(lock) {
        for (ch in text) {
            processChar(ch)
        }
    }

    fun clear() = synchronized(lock) {
        scrollback.clear()
        primaryScreen = createScreen(rows, cols)
        altScreen = createScreen(rows, cols)
        usingAltScreen = false
        cursorRow = 0
        cursorCol = 0
        scrollTop = 0
        scrollBottom = rows - 1
        currentStyle = TerminalStyle()
    }

    fun snapshotLines(): List<TerminalRenderLine> = synchronized(lock) {
        val allRows = ArrayList<Array<Cell>>(scrollback.size + rows)
        if (!usingAltScreen) {
            allRows.addAll(scrollback)
        }
        val active = activeScreen()
        var lastNonEmpty = rows - 1
        while (lastNonEmpty > cursorRow && isBlankRow(active[lastNonEmpty])) {
            lastNonEmpty--
        }
        for (r in 0..lastNonEmpty) {
            allRows.add(active[r])
        }
        return allRows.map { rowToRenderLine(it) }
    }

    private fun activeScreen(): Array<Array<Cell>> = if (usingAltScreen) altScreen else primaryScreen

    private fun createScreen(rCount: Int, cCount: Int): Array<Array<Cell>> =
        Array(rCount) { Array(cCount) { Cell() } }

    private fun resizeGrid(old: Array<Array<Cell>>, newRows: Int, newCols: Int): Array<Array<Cell>> {
        val next = createScreen(newRows, newCols)
        val copyRows = minOf(old.size, newRows)
        for (r in 0 until copyRows) {
            val copyCols = minOf(old[r].size, newCols)
            for (c in 0 until copyCols) {
                next[r][c] = old[r][c].copy()
            }
        }
        return next
    }

    private fun processChar(ch: Char) {
        when (state) {
            ParserState.TEXT -> when (ch) {
                '\u001B' -> state = ParserState.ESC
                '\r' -> cursorCol = 0
                '\n' -> lineFeed()
                '\b' -> if (cursorCol > 0) cursorCol--
                '\t' -> {
                    val nextTab = ((cursorCol / 8) + 1) * 8
                    cursorCol = nextTab.coerceAtMost(cols - 1)
                }
                '\u0007' -> Unit // BEL
                else -> if (ch >= ' ') putChar(ch)
            }

            ParserState.ESC -> when (ch) {
                '[' -> {
                    csiBuf.setLength(0)
                    state = ParserState.CSI
                }
                ']' -> {
                    oscBuf.setLength(0)
                    state = ParserState.OSC
                }
                '7' -> {
                    savedCursorRow = cursorRow
                    savedCursorCol = cursorCol
                    state = ParserState.TEXT
                }
                '8' -> {
                    cursorRow = savedCursorRow.coerceIn(0, rows - 1)
                    cursorCol = savedCursorCol.coerceIn(0, cols - 1)
                    state = ParserState.TEXT
                }
                'M' -> {
                    reverseIndex()
                    state = ParserState.TEXT
                }
                'c' -> {
                    clear()
                    state = ParserState.TEXT
                }
                '(', ')' -> state = ParserState.TEXT // Charset designation ignored cleanly
                else -> state = ParserState.TEXT
            }

            ParserState.CSI -> {
                if (ch.code in 0x40..0x7E) {
                    executeCsi(csiBuf.toString(), ch)
                    state = ParserState.TEXT
                } else {
                    csiBuf.append(ch)
                }
            }

            ParserState.OSC -> when (ch) {
                '\u0007' -> {
                    executeOsc(oscBuf.toString())
                    state = ParserState.TEXT
                }
                '\u001B' -> state = ParserState.OSC_ESC
                else -> oscBuf.append(ch)
            }

            ParserState.OSC_ESC -> {
                if (ch == '\\') {
                    executeOsc(oscBuf.toString())
                    state = ParserState.TEXT
                } else {
                    oscBuf.append('\u001B').append(ch)
                    state = ParserState.OSC
                }
            }
        }
    }

    private fun putChar(ch: Char) {
        if (cursorCol >= cols) {
            cursorCol = 0
            lineFeed()
        }
        val row = activeScreen()[cursorRow]
        row[cursorCol] = Cell(ch = ch, style = currentStyle, oscLink = currentOscLink)
        cursorCol++
    }

    private fun lineFeed() {
        if (cursorRow >= scrollBottom) {
            scrollUp(1)
        } else {
            cursorRow = (cursorRow + 1).coerceAtMost(rows - 1)
        }
    }

    private fun reverseIndex() {
        if (cursorRow <= scrollTop) {
            scrollDown(1)
        } else {
            cursorRow = (cursorRow - 1).coerceAtLeast(0)
        }
    }

    private fun scrollUp(lines: Int) {
        val screen = activeScreen()
        repeat(lines.coerceAtLeast(1)) {
            if (!usingAltScreen && scrollTop == 0) {
                if (scrollback.size >= maxScrollback) {
                    scrollback.removeFirst()
                }
                scrollback.addLast(Array(cols) { idx -> screen[0][idx].copy() })
            }
            for (r in scrollTop until scrollBottom) {
                screen[r] = screen[r + 1]
            }
            screen[scrollBottom] = Array(cols) { Cell() }
        }
    }

    private fun scrollDown(lines: Int) {
        val screen = activeScreen()
        repeat(lines.coerceAtLeast(1)) {
            for (r in scrollBottom downTo scrollTop + 1) {
                screen[r] = screen[r - 1]
            }
            screen[scrollTop] = Array(cols) { Cell() }
        }
    }

    private fun executeCsi(rawParams: String, finalChar: Char) {
        val isPrivate = rawParams.startsWith("?")
        val clean = if (isPrivate) rawParams.substring(1) else rawParams
        val params = if (clean.isEmpty()) emptyList() else clean.split(';').map { it.toIntOrNull() ?: 0 }

        if (isPrivate) {
            when (finalChar) {
                'h' -> params.forEach { p ->
                    if (p == 1049 || p == 1047 || p == 47) switchToAltScreen(true)
                }
                'l' -> params.forEach { p ->
                    if (p == 1049 || p == 1047 || p == 47) switchToAltScreen(false)
                }
            }
            return
        }

        when (finalChar) {
            'A' -> cursorRow = (cursorRow - (params.firstOrNull()?.takeIf { it > 0 } ?: 1)).coerceAtLeast(0)
            'B' -> cursorRow = (cursorRow + (params.firstOrNull()?.takeIf { it > 0 } ?: 1)).coerceAtMost(rows - 1)
            'C' -> cursorCol = (cursorCol + (params.firstOrNull()?.takeIf { it > 0 } ?: 1)).coerceAtMost(cols - 1)
            'D' -> cursorCol = (cursorCol - (params.firstOrNull()?.takeIf { it > 0 } ?: 1)).coerceAtLeast(0)
            'E' -> {
                cursorRow = (cursorRow + (params.firstOrNull()?.takeIf { it > 0 } ?: 1)).coerceAtMost(rows - 1)
                cursorCol = 0
            }
            'F' -> {
                cursorRow = (cursorRow - (params.firstOrNull()?.takeIf { it > 0 } ?: 1)).coerceAtLeast(0)
                cursorCol = 0
            }
            'G', '`' -> {
                val targetCol = (params.firstOrNull()?.takeIf { it > 0 } ?: 1) - 1
                cursorCol = targetCol.coerceIn(0, cols - 1)
            }
            'd' -> {
                val targetRow = (params.firstOrNull()?.takeIf { it > 0 } ?: 1) - 1
                cursorRow = targetRow.coerceIn(0, rows - 1)
            }
            'H', 'f' -> {
                val r = (params.getOrNull(0)?.takeIf { it > 0 } ?: 1) - 1
                val c = (params.getOrNull(1)?.takeIf { it > 0 } ?: 1) - 1
                cursorRow = r.coerceIn(0, rows - 1)
                cursorCol = c.coerceIn(0, cols - 1)
            }
            'J' -> eraseDisplay(params.firstOrNull() ?: 0)
            'K' -> eraseLine(params.firstOrNull() ?: 0)
            'L' -> insertLines(params.firstOrNull()?.takeIf { it > 0 } ?: 1)
            'M' -> deleteLines(params.firstOrNull()?.takeIf { it > 0 } ?: 1)
            'P' -> deleteChars(params.firstOrNull()?.takeIf { it > 0 } ?: 1)
            'X' -> eraseChars(params.firstOrNull()?.takeIf { it > 0 } ?: 1)
            'r' -> {
                val top = ((params.getOrNull(0)?.takeIf { it > 0 } ?: 1) - 1).coerceIn(0, rows - 1)
                val bottom = ((params.getOrNull(1)?.takeIf { it > 0 } ?: rows) - 1).coerceIn(0, rows - 1)
                if (top < bottom) {
                    scrollTop = top
                    scrollBottom = bottom
                    cursorRow = 0
                    cursorCol = 0
                }
            }
            'm' -> applySgr(if (params.isEmpty()) listOf(0) else params)
            's' -> {
                savedCursorRow = cursorRow
                savedCursorCol = cursorCol
            }
            'u' -> {
                cursorRow = savedCursorRow.coerceIn(0, rows - 1)
                cursorCol = savedCursorCol.coerceIn(0, cols - 1)
            }
        }
    }

    private fun switchToAltScreen(enable: Boolean) {
        if (enable && !usingAltScreen) {
            savedCursorRow = cursorRow
            savedCursorCol = cursorCol
            altScreen = createScreen(rows, cols)
            usingAltScreen = true
            cursorRow = 0
            cursorCol = 0
            scrollTop = 0
            scrollBottom = rows - 1
        } else if (!enable && usingAltScreen) {
            usingAltScreen = false
            cursorRow = savedCursorRow.coerceIn(0, rows - 1)
            cursorCol = savedCursorCol.coerceIn(0, cols - 1)
            scrollTop = 0
            scrollBottom = rows - 1
        }
    }

    private fun eraseDisplay(mode: Int) {
        val screen = activeScreen()
        when (mode) {
            0 -> {
                eraseLine(0)
                for (r in (cursorRow + 1) until rows) {
                    screen[r] = Array(cols) { Cell() }
                }
            }
            1 -> {
                for (r in 0 until cursorRow) {
                    screen[r] = Array(cols) { Cell() }
                }
                eraseLine(1)
            }
            2, 3 -> {
                for (r in 0 until rows) {
                    screen[r] = Array(cols) { Cell() }
                }
                if (mode == 3 && !usingAltScreen) scrollback.clear()
            }
        }
    }

    private fun eraseLine(mode: Int) {
        val row = activeScreen()[cursorRow]
        when (mode) {
            0 -> for (c in cursorCol until cols) row[c] = Cell()
            1 -> for (c in 0..cursorCol.coerceAtMost(cols - 1)) row[c] = Cell()
            2 -> for (c in 0 until cols) row[c] = Cell()
        }
    }

    private fun insertLines(count: Int) {
        val screen = activeScreen()
        if (cursorRow < scrollTop || cursorRow > scrollBottom) return
        repeat(count) {
            for (r in scrollBottom downTo cursorRow + 1) {
                screen[r] = screen[r - 1]
            }
            screen[cursorRow] = Array(cols) { Cell() }
        }
    }

    private fun deleteLines(count: Int) {
        val screen = activeScreen()
        if (cursorRow < scrollTop || cursorRow > scrollBottom) return
        repeat(count) {
            for (r in cursorRow until scrollBottom) {
                screen[r] = screen[r + 1]
            }
            screen[scrollBottom] = Array(cols) { Cell() }
        }
    }

    private fun deleteChars(count: Int) {
        val row = activeScreen()[cursorRow]
        val shift = count.coerceAtMost(cols - cursorCol)
        for (c in cursorCol until cols - shift) {
            row[c] = row[c + shift]
        }
        for (c in (cols - shift) until cols) {
            row[c] = Cell()
        }
    }

    private fun eraseChars(count: Int) {
        val row = activeScreen()[cursorRow]
        val end = (cursorCol + count).coerceAtMost(cols)
        for (c in cursorCol until end) {
            row[c] = Cell()
        }
    }

    private fun applySgr(codes: List<Int>) {
        var i = 0
        while (i < codes.size) {
            when (val code = codes[i]) {
                0 -> currentStyle = TerminalStyle()
                1 -> currentStyle = currentStyle.copy(bold = true)
                4 -> currentStyle = currentStyle.copy(underline = true)
                7 -> currentStyle = currentStyle.copy(inverse = true)
                22 -> currentStyle = currentStyle.copy(bold = false)
                24 -> currentStyle = currentStyle.copy(underline = false)
                27 -> currentStyle = currentStyle.copy(inverse = false)
                in 30..37 -> currentStyle = currentStyle.copy(fgRgb = ANSI_16_COLORS[code - 30])
                39 -> currentStyle = currentStyle.copy(fgRgb = null)
                in 40..47 -> currentStyle = currentStyle.copy(bgRgb = ANSI_16_COLORS[code - 40])
                49 -> currentStyle = currentStyle.copy(bgRgb = null)
                in 90..97 -> currentStyle = currentStyle.copy(fgRgb = ANSI_16_COLORS[code - 90 + 8])
                in 100..107 -> currentStyle = currentStyle.copy(bgRgb = ANSI_16_COLORS[code - 100 + 8])
                38, 48 -> {
                    val isFg = code == 38
                    val mode = codes.getOrNull(i + 1)
                    if (mode == 5 && i + 2 < codes.size) {
                        val color = xterm256Color(codes[i + 2])
                        currentStyle = if (isFg) currentStyle.copy(fgRgb = color) else currentStyle.copy(bgRgb = color)
                        i += 2
                    } else if (mode == 2 && i + 4 < codes.size) {
                        val r = codes[i + 2].coerceIn(0, 255)
                        val g = codes[i + 3].coerceIn(0, 255)
                        val b = codes[i + 4].coerceIn(0, 255)
                        val rgb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        currentStyle = if (isFg) currentStyle.copy(fgRgb = rgb) else currentStyle.copy(bgRgb = rgb)
                        i += 4
                    }
                }
            }
            i++
        }
    }

    private fun executeOsc(payload: String) {
        val idx = payload.indexOf(';')
        if (idx <= 0) return
        val code = payload.substring(0, idx)
        val value = payload.substring(idx + 1)
        when (code) {
            "0", "2" -> windowTitle = value
            "8" -> {
                val sep = value.indexOf(';')
                val url = if (sep >= 0) value.substring(sep + 1) else value
                currentOscLink = url.ifBlank { null }
            }
        }
    }

    private fun isBlankRow(row: Array<Cell>): Boolean =
        row.all { it.ch == ' ' && it.style.bgRgb == null && !it.style.inverse }

    private fun rowToRenderLine(row: Array<Cell>): TerminalRenderLine {
        var lastCol = row.lastIndex
        while (lastCol >= 0 && row[lastCol].ch == ' ' && row[lastCol].style.bgRgb == null && !row[lastCol].style.inverse) {
            lastCol--
        }
        if (lastCol < 0) return TerminalRenderLine(emptyList(), "")

        val sb = StringBuilder(lastCol + 1)
        for (c in 0..lastCol) {
            sb.append(row[c].ch)
        }
        val plain = sb.toString()
        val detectedLinks = LinkDetector.extractUrls(plain)

        fun linkAt(col: Int): String? {
            row[col].oscLink?.let { return it }
            return detectedLinks.firstOrNull { col in it.startIndex until it.endIndexExclusive }?.url
        }

        val spans = mutableListOf<TerminalSpan>()
        var runStart = 0
        var runStyle = row[0].style
        var runLink = linkAt(0)
        for (c in 1..lastCol) {
            val st = row[c].style
            val lk = linkAt(c)
            if (st != runStyle || lk != runLink) {
                spans.add(TerminalSpan(plain.substring(runStart, c), runStyle, runLink))
                runStart = c
                runStyle = st
                runLink = lk
            }
        }
        spans.add(TerminalSpan(plain.substring(runStart, lastCol + 1), runStyle, runLink))
        return TerminalRenderLine(spans = spans, plainText = plain)
    }

    companion object {
        private val ANSI_16_COLORS = intArrayOf(
            0xFF1E293B.toInt(), // Black
            0xFFF87171.toInt(), // Red
            0xFF4ADE80.toInt(), // Green
            0xFFFACC15.toInt(), // Yellow
            0xFF60A5FA.toInt(), // Blue
            0xFFC084FC.toInt(), // Magenta
            0xFF2DD4BF.toInt(), // Cyan
            0xFFE2E8F0.toInt(), // White
            0xFF64748B.toInt(), // Bright Black
            0xFFFCA5A5.toInt(), // Bright Red
            0xFF86EFAC.toInt(), // Bright Green
            0xFFFDE047.toInt(), // Bright Yellow
            0xFF93C5FD.toInt(), // Bright Blue
            0xFFD8B4FE.toInt(), // Bright Magenta
            0xFF5EEAD4.toInt(), // Bright Cyan
            0xFFFFFFFF.toInt()  // Bright White
        )

        private fun xterm256Color(index: Int): Int {
            val idx = index.coerceIn(0, 255)
            if (idx < 16) return ANSI_16_COLORS[idx]
            if (idx >= 232) {
                val gray = 8 + (idx - 232) * 10
                return (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
            val cube = idx - 16
            val r = cubeComp(cube / 36)
            val g = cubeComp((cube % 36) / 6)
            val b = cubeComp(cube % 6)
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun cubeComp(v: Int): Int = if (v == 0) 0 else 55 + v * 40
    }
}
