package com.appdex.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Parses ANSI escape sequences and converts to AnnotatedString with colors.
 * Supports basic 16 colors and 256-color mode.
 */
object AnsiColorParser {

    private val colorMap = mapOf(
        0 to Color(0xFF000000),   // Black
        1 to Color(0xFFCC0000),   // Red
        2 to Color(0xFF4E9A06),   // Green
        3 to Color(0xFFC4A000),   // Yellow
        4 to Color(0xFF3465A4),   // Blue
        5 to Color(0xFF75507B),   // Magenta
        6 to Color(0xFF06989A),   // Cyan
        7 to Color(0xFFD3D7CF),   // White
        8 to Color(0xFF555753),   // Bright Black (Gray)
        9 to Color(0xFFEF2929),   // Bright Red
        10 to Color(0xFF8AE234),  // Bright Green
        11 to Color(0xFFFCE94F),  // Bright Yellow
        12 to Color(0xFF729FCF),  // Bright Blue
        13 to Color(0xFFAD7FA8),  // Bright Magenta
        14 to Color(0xFF34E2E2),  // Bright Cyan
        15 to Color(0xFFEEEEEC)   // Bright White
    )

    private val ansiRegex = Regex("\u001B\\[([0-9;]*)m")

    fun parse(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var lastEnd = 0
            var currentColor: Color? = null
            var isBold = false

            for (match in ansiRegex.findAll(text)) {
                // Append text before escape
                if (match.range.first > lastEnd) {
                    val segment = text.substring(lastEnd, match.range.first)
                    val style = buildStyle(currentColor, isBold, defaultColor)
                    if (style != null) {
                        pushStyle(style)
                        append(segment)
                        pop()
                    } else {
                        append(segment)
                    }
                }

                // Parse color code
                val codes = match.groupValues[1].split(";")
                for (codeStr in codes) {
                    val code = codeStr.toIntOrNull() ?: 0
                    when {
                        code == 0 -> { currentColor = null; isBold = false }
                        code == 1 -> isBold = true
                        code == 2 -> isBold = false
                        code in 30..37 -> currentColor = colorMap[code - 30]
                        code in 90..97 -> currentColor = colorMap[code - 90 + 8]
                        code == 38 -> {
                            // Extended color: 38;5;n or 38;2;r;g;b
                            // Handled below in multi-code parsing
                        }
                        code == 39 -> currentColor = null
                    }
                }

                // Handle 256-color and RGB: look for 38;5;N or 48;5;N patterns
                val fullCode = match.groupValues[1]
                if (fullCode.startsWith("38;5;")) {
                    val n = fullCode.substringAfter("38;5;").substringBefore(";").toIntOrNull()
                    if (n != null) {
                        currentColor = colorMap256(n)
                    }
                } else if (fullCode.startsWith("38;2;")) {
                    val parts = fullCode.split(";")
                    if (parts.size >= 5) {
                        val r = parts[2].toIntOrNull() ?: 0
                        val g = parts[3].toIntOrNull() ?: 0
                        val b = parts[4].toIntOrNull() ?: 0
                        currentColor = Color(r, g, b)
                    }
                }

                lastEnd = match.range.last + 1
            }

            // Append remaining text
            if (lastEnd < text.length) {
                val segment = text.substring(lastEnd)
                val style = buildStyle(currentColor, isBold, defaultColor)
                if (style != null) {
                    pushStyle(style)
                    append(segment)
                    pop()
                } else {
                    append(segment)
                }
            }
        }
    }

    private fun buildStyle(color: Color?, bold: Boolean, default: Color): SpanStyle? {
        if (color == null && !bold) return null
        return SpanStyle(
            color = color ?: default,
            fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null
        )
    }

    private fun colorMap256(n: Int): Color {
        return when {
            n < 16 -> colorMap[n] ?: Color.White
            n < 232 -> {
                // 6x6x6 color cube
                val offset = n - 16
                val r = offset / 36
                val g = (offset % 36) / 6
                val b = offset % 6
                Color(
                    if (r == 0) 0 else (r * 40 + 55),
                    if (g == 0) 0 else (g * 40 + 55),
                    if (b == 0) 0 else (b * 40 + 55)
                )
            }
            else -> {
                // Grayscale
                val v = 8 + (n - 232) * 10
                Color(v, v, v)
            }
        }
    }
}
