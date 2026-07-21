package com.appdex.syntax

import kotlinx.serialization.Serializable

@Serializable
data class SyntaxDefinition(
    val name: String,
    val extensions: List<String>,
    val patterns: List<SyntaxPattern>,
    val colors: Map<String, String>
)

@Serializable
data class SyntaxPattern(
    val name: String,
    val match: String,
    val color: String
)

data class CompiledPattern(
    val name: String,
    val regex: Regex,
    val colorKey: String
)

data class SyntaxGrammar(
    val name: String,
    val extensions: List<String>,
    val compiledPatterns: List<CompiledPattern>,
    val colors: Map<String, String>
)

data class SyntaxToken(
    val start: Int,
    val end: Int,
    val type: String,
    val color: String
)

enum class SyntaxLanguage {
    NONE, KOTLIN, JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT,
    XML, JSON, YAML, HTML, CSS, SQL, SHELL, C, CPP, GO, RUST
}
