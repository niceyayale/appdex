package com.appdex.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.appdex.ui.theme.AmberGold
import com.appdex.ui.theme.AuroraGreen
import com.appdex.ui.theme.NebulaBlue
import com.appdex.ui.theme.OnAuroraGreenContainer
import com.appdex.ui.theme.OnNebulaBlueContainer
import com.appdex.ui.theme.TextMuted
import com.appdex.ui.theme.TextPrimary

/**
 * Lightweight syntax highlighter for code editing.
 * Supports: Kotlin, Java, XML, JSON, Python, JS, HTML, CSS, SQL, Shell, C/C++, Go, Rust.
 */
object SyntaxHighlighter {

    data class HighlightRule(
        val regex: Regex,
        val style: SpanStyle
    )

    // ── Color palette (referencing AppDex DesignSystem) ──
    private val keywordColor = AmberGold
    private val stringColor = AuroraGreen
    private val numberColor = Color(0xFFB794F6)        // Soft purple (syntax-specific, not in DesignSystem)
    private val commentColor = TextMuted
    private val annotationColor = NebulaBlue
    private val tagColor = NebulaBlue
    private val attrColor = AmberGold
    private val funcColor = OnNebulaBlueContainer
    private val typeColor = OnAuroraGreenContainer
    private val plainColor = TextPrimary

    // ── Language rules ──
    private val kotlinRules = listOf(
        HighlightRule(Regex("//[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("\"\"\"[\\s\\S]*?\"\"\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:val|var|fun|class|object|interface|enum|sealed|data|companion|override|private|public|protected|internal|abstract|final|open|init|constructor|this|super|return|if|else|when|for|while|do|break|continue|in|is|as|import|package|typealias|suspend|inline|noinline|crossinline|reified|operator|infix|tailrec|external|annotation|lateinit|const|vararg|abstract|by|get|set|field|out|in|where|catch|finally|throw|try)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("@[A-Za-z_][A-Za-z0-9_]*"), SpanStyle(color = annotationColor)),
        HighlightRule(Regex("\\b[A-Z][A-Za-z0-9_]*\\b"), SpanStyle(color = typeColor)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?[fFlLdD]?\\b"), SpanStyle(color = numberColor)),
        HighlightRule(Regex("#[A-Za-z_][A-Za-z0-9_]*"), SpanStyle(color = annotationColor))
    )

    private val javaRules = listOf(
        HighlightRule(Regex("//[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:public|private|protected|static|final|void|class|interface|enum|extends|implements|new|this|super|return|if|else|for|while|do|break|continue|switch|case|default|try|catch|finally|throw|throws|import|package|abstract|synchronized|volatile|transient|native|strictfp|instanceof|boolean|byte|char|short|int|long|float|double|null|true|false|const|goto)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("@[A-Za-z_][A-Za-z0-9_]*"), SpanStyle(color = annotationColor)),
        HighlightRule(Regex("\\b[A-Z][A-Za-z0-9_]*\\b"), SpanStyle(color = typeColor)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?[fFlLdD]?\\b"), SpanStyle(color = numberColor))
    )

    private val xmlRules = listOf(
        HighlightRule(Regex("<!--[^-]*-->"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("</?[A-Za-z][A-Za-z0-9_\\-.:]*"), SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("/?>"), SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("[A-Za-z_][A-Za-z0-9_\\-.:]*(?==)"), SpanStyle(color = attrColor)),
        HighlightRule(Regex("\"[^\"]*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'[^']*'"), SpanStyle(color = stringColor))
    )

    private val jsonRules = listOf(
        HighlightRule(Regex("\"[^\"]*\"(?=\\s*:)"), SpanStyle(color = attrColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\"[^\"]*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:true|false|null)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"), SpanStyle(color = numberColor))
    )

    private val pythonRules = listOf(
        HighlightRule(Regex("#[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("\"\"\"[\\s\\S]*?\"\"\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'''[\\s\\S]*?'''"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("[rfbu]*\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("[rfbu]*'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:def|class|return|if|elif|else|for|while|break|continue|in|not|is|and|or|import|from|as|try|except|finally|raise|with|lambda|yield|global|nonlocal|pass|del|assert|async|await|self|cls|True|False|None)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("@[A-Za-z_][A-Za-z0-9_.]*"), SpanStyle(color = annotationColor)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?[jJ]?\\b"), SpanStyle(color = numberColor))
    )

    private val jsRules = listOf(
        HighlightRule(Regex("//[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("`(?:[^`\\\\]|\\\\.)*`"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:var|let|const|function|return|if|else|for|while|do|break|continue|switch|case|default|try|catch|finally|throw|new|delete|typeof|instanceof|in|of|class|extends|super|this|import|export|from|async|await|yield|void|null|undefined|true|false)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?\\b"), SpanStyle(color = numberColor))
    )

    private val sqlRules = listOf(
        HighlightRule(Regex("--[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\"(?:[^\"]|\"\")*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|ALTER|DROP|INDEX|VIEW|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP|BY|HAVING|ORDER|LIMIT|OFFSET|AS|AND|OR|NOT|IN|LIKE|BETWEEN|IS|NULL|DISTINCT|UNION|ALL|CASE|WHEN|THEN|ELSE|END|COUNT|SUM|AVG|MIN|MAX|PRIMARY|KEY|FOREIGN|REFERENCES|CONSTRAINT|DEFAULT|CHECK|UNIQUE|CASCADE|BEGIN|COMMIT|ROLLBACK|TRANSACTION)\\b", RegexOption.IGNORE_CASE), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?\\b"), SpanStyle(color = numberColor))
    )

    private val shellRules = listOf(
        HighlightRule(Regex("#[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'[^']*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\$(?:\\{[^}]*\\}|[A-Za-z_][A-Za-z0-9_]*)"), SpanStyle(color = annotationColor)),
        HighlightRule(Regex("\\b(?:if|then|else|elif|fi|for|in|do|done|while|case|esac|function|return|exit|echo|printf|read|export|local|declare|unset|source|alias|cd|pwd|ls|cat|grep|sed|awk|find|cp|mv|rm|mkdir|rmdir|chmod|chown|sudo|apt|yum|brew|git|npm|pip)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold))
    )

    private val htmlRules = listOf(
        HighlightRule(Regex("<!--[^-]*-->"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("</?[A-Za-z][A-Za-z0-9]*"), SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("/?>"), SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("[A-Za-z_-]+(?==)"), SpanStyle(color = attrColor)),
        HighlightRule(Regex("\"[^\"]*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'[^']*'"), SpanStyle(color = stringColor))
    )

    private val cssRules = listOf(
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("[.#]?[A-Za-z_][A-Za-z0-9_-]*(?=\\s*\\{)"), SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("[A-Za-z-]+(?=:)"), SpanStyle(color = attrColor)),
        HighlightRule(Regex(":[^;{}]+"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("#[0-9A-Fa-f]{3,8}\\b"), SpanStyle(color = numberColor)),
        HighlightRule(Regex("\\b\\d+(?:px|em|rem|%|pt|vh|vw|deg|s|ms)?\\b"), SpanStyle(color = numberColor))
    )

    private val goRules = listOf(
        HighlightRule(Regex("//[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("`[^`]*`"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:func|var|const|type|struct|interface|map|chan|package|import|return|if|else|for|range|switch|case|default|break|continue|go|defer|select|fallthrough|goto|type|nil|true|false)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?\\b"), SpanStyle(color = numberColor))
    )

    private val rustRules = listOf(
        HighlightRule(Regex("//[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:fn|let|mut|const|static|struct|enum|trait|impl|pub|use|mod|crate|self|Self|super|return|if|else|match|for|while|loop|break|continue|in|ref|move|as|where|unsafe|async|await|dyn|abstract|become|box|do|final|macro|override|priv|try|typeof|unsized|virtual|yield)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?[fFiIuU8 16 32 64 128]*\\b"), SpanStyle(color = numberColor))
    )

    private val cRules = listOf(
        HighlightRule(Regex("//[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
        HighlightRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("#[A-Za-z_]+"), SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b(?:int|char|float|double|void|long|short|unsigned|signed|const|static|extern|register|volatile|auto|struct|union|enum|typedef|return|if|else|for|while|do|break|continue|switch|case|default|goto|sizeof|inline|restrict|bool|true|false|NULL)\\b"), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?[fFlLuU]*\\b"), SpanStyle(color = numberColor))
    )

    private val yamlRules = listOf(
        HighlightRule(Regex("#[^\\n]*"), SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)),
        HighlightRule(Regex("[A-Za-z_][A-Za-z0-9_-]*(?=:)"), SpanStyle(color = attrColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex(":\\s*[^\\n#]+"), SpanStyle(color = stringColor)),
        HighlightRule(Regex("\\b(?:true|false|null|yes|no|on|off)\\b", RegexOption.IGNORE_CASE), SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)),
        HighlightRule(Regex("\\b\\d+(?:\\.\\d+)?\\b"), SpanStyle(color = numberColor))
    )

    // ── Language detection ──
    fun getLanguage(extension: String): SyntaxLanguage {
        return when (extension.lowercase()) {
            "kt", "kts" -> SyntaxLanguage.KOTLIN
            "java" -> SyntaxLanguage.JAVA
            "xml", "svg" -> SyntaxLanguage.XML
            "json" -> SyntaxLanguage.JSON
            "py" -> SyntaxLanguage.PYTHON
            "js", "jsx" -> SyntaxLanguage.JAVASCRIPT
            "ts", "tsx" -> SyntaxLanguage.TYPESCRIPT
            "html", "htm" -> SyntaxLanguage.HTML
            "css", "scss" -> SyntaxLanguage.CSS
            "sql" -> SyntaxLanguage.SQL
            "sh", "bash", "zsh" -> SyntaxLanguage.SHELL
            "c", "h" -> SyntaxLanguage.C
            "cpp", "cc", "cxx", "hpp", "hxx" -> SyntaxLanguage.CPP
            "go" -> SyntaxLanguage.GO
            "rs" -> SyntaxLanguage.RUST
            "yaml", "yml" -> SyntaxLanguage.YAML
            else -> SyntaxLanguage.NONE
        }
    }

    private fun getRules(language: SyntaxLanguage): List<HighlightRule>? {
        return when (language) {
            SyntaxLanguage.KOTLIN -> kotlinRules
            SyntaxLanguage.JAVA -> javaRules
            SyntaxLanguage.XML -> xmlRules
            SyntaxLanguage.JSON -> jsonRules
            SyntaxLanguage.PYTHON -> pythonRules
            SyntaxLanguage.JAVASCRIPT, SyntaxLanguage.TYPESCRIPT -> jsRules
            SyntaxLanguage.HTML -> htmlRules
            SyntaxLanguage.CSS -> cssRules
            SyntaxLanguage.SQL -> sqlRules
            SyntaxLanguage.SHELL -> shellRules
            SyntaxLanguage.C, SyntaxLanguage.CPP -> cRules
            SyntaxLanguage.GO -> goRules
            SyntaxLanguage.RUST -> rustRules
            SyntaxLanguage.YAML -> yamlRules
            SyntaxLanguage.NONE -> null
        }
    }

    fun highlight(text: String, language: SyntaxLanguage): AnnotatedString {
        val rules = getRules(language) ?: return AnnotatedString(text)

        return buildAnnotatedString {
            // Collect all matches with their positions
            data class Match(val start: Int, val end: Int, val style: SpanStyle)

            val allMatches = mutableListOf<Match>()
            for (rule in rules) {
                for (m in rule.regex.findAll(text)) {
                    allMatches.add(Match(m.range.first, m.range.last + 1, rule.style))
                }
            }
            // Sort by start position, remove overlaps (first match wins)
            allMatches.sortBy { it.start }

            var lastEnd = 0
            val merged = mutableListOf<Match>()
            for (m in allMatches) {
                if (m.start >= lastEnd) {
                    merged.add(m)
                    lastEnd = m.end
                }
            }

            // Build the annotated string
            for (m in merged) {
                if (m.start > lastEnd) {
                    append(text.substring(lastEnd, m.start))
                }
                withStyle(m.style) {
                    append(text.substring(m.start, m.end))
                }
                lastEnd = m.end
            }
            if (lastEnd < text.length) {
                append(text.substring(lastEnd))
            }
        }
    }

    fun highlight(text: String, extension: String): AnnotatedString {
        return highlight(text, getLanguage(extension))
    }
}
