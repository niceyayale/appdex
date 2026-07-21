package com.appdex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.ui.theme.AppXTheme

/**
 * Lightweight Markdown renderer for AI chat messages.
 * Supports: headings, bold, code blocks, inline code, lists, blockquotes.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    val blocks = parseMarkdownBlocks(text)

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 16.sp
                        2 -> 14.sp
                        3 -> 13.sp
                        else -> 12.sp
                    }
                    Text(
                        text = block.content,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.surfaceAlt.copy(alpha = 0.6f))
                            .border(1.dp, c.borderLight, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = block.content,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = c.auroraGreen,
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Spacer(modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .background(c.amberGold.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = block.content,
                            fontSize = 11.sp,
                            color = c.textSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (block.ordered) "${block.index}." else "•",
                            fontSize = 11.sp,
                            color = c.amberGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatInlineMarkdown(block.content, c),
                            fontSize = 11.sp,
                            color = c.textPrimary,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.content, c),
                        fontSize = 12.sp,
                        color = c.textPrimary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    data class ListItem(val content: String, val ordered: Boolean, val index: Int) : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0
    var orderedIndex = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeBuilder.appendLine(lines[i])
                i++
            }
            i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(language, codeBuilder.toString().trimEnd()))
            orderedIndex = 0
            continue
        }

        // Heading
        val headingMatch = Regex("^(#{1,4})\\s+(.+)").find(line)
        if (headingMatch != null) {
            blocks.add(MarkdownBlock.Heading(
                level = headingMatch.groupValues[1].length,
                content = headingMatch.groupValues[2].trim()
            ))
            orderedIndex = 0
            i++
            continue
        }

        // Blockquote
        if (line.trim().startsWith("> ")) {
            blocks.add(MarkdownBlock.Quote(line.trim().removePrefix("> ").trim()))
            orderedIndex = 0
            i++
            continue
        }

        // Ordered list item
        val orderedMatch = Regex("^(\\d+)\\.\\s+(.+)").find(line.trim())
        if (orderedMatch != null) {
            orderedIndex++
            blocks.add(MarkdownBlock.ListItem(
                content = orderedMatch.groupValues[2].trim(),
                ordered = true,
                index = orderedMatch.groupValues[1].toIntOrNull() ?: orderedIndex
            ))
            i++
            continue
        }

        // Unordered list item
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ") || line.trim().startsWith("• ")) {
            val content = line.trim().drop(2).trim()
            blocks.add(MarkdownBlock.ListItem(content, ordered = false, index = 0))
            orderedIndex = 0
            i++
            continue
        }

        // Empty line
        if (line.isBlank()) {
            orderedIndex = 0
            i++
            continue
        }

        // Paragraph (accumulate consecutive non-empty lines)
        val paraBuilder = StringBuilder(line)
        i++
        while (i < lines.size && lines[i].isNotBlank() &&
               !lines[i].trim().startsWith("#") &&
               !lines[i].trim().startsWith("```") &&
               !lines[i].trim().startsWith("> ") &&
               !lines[i].trim().startsWith("- ") &&
               !lines[i].trim().startsWith("* ") &&
               !Regex("^(\\d+)\\.\\s+").containsMatchIn(lines[i].trim())
        ) {
            paraBuilder.append("\n").append(lines[i])
            i++
        }
        blocks.add(MarkdownBlock.Paragraph(paraBuilder.toString()))
        orderedIndex = 0
    }

    return blocks
}

private fun formatInlineMarkdown(text: String, c: com.appdex.ui.theme.AppXColors): AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            // Inline code `code`
            val codeMatch = Regex("`([^`]+)`").find(remaining)
            // Bold **text**
            val boldMatch = Regex("\\*\\*([^*]+)\\*\\*").find(remaining)

            val nextMatch = listOfNotNull(codeMatch, boldMatch).minByOrNull { it.range.first }

            if (nextMatch == null) {
                append(remaining)
                break
            }

            if (nextMatch.range.first > 0) {
                append(remaining.substring(0, nextMatch.range.first))
            }

            when (nextMatch) {
                codeMatch -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = c.auroraGreen,
                        background = c.surfaceAlt.copy(alpha = 0.5f)
                    )) {
                        append(codeMatch.groupValues[1])
                    }
                }
                boldMatch -> {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )) {
                        append(boldMatch.groupValues[1])
                    }
                }
            }

            remaining = remaining.substring(nextMatch.range.last + 1)
        }
    }
}

