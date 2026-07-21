package com.appdex.syntax

import kotlinx.serialization.json.Json

class SyntaxEngine {

    private val json = Json { ignoreUnknownKeys = true }
    private val grammars = mutableMapOf<String, SyntaxGrammar>()

    fun loadGrammar(jsonString: String): SyntaxGrammar {
        val def = json.decodeFromString<SyntaxDefinition>(jsonString)
        val compiled = def.patterns.map { pattern ->
            CompiledPattern(
                name = pattern.name,
                regex = Regex(pattern.match, RegexOption.MULTILINE),
                colorKey = pattern.color
            )
        }
        return SyntaxGrammar(def.name, def.extensions, compiled, def.colors).also { grammar ->
            def.extensions.forEach { ext ->
                grammars[ext.lowercase()] = grammar
            }
        }
    }

    fun getGrammarForExtension(ext: String): SyntaxGrammar? {
        return grammars[ext.lowercase()]
    }

    fun tokenize(text: String, grammar: SyntaxGrammar): List<SyntaxToken> {
        val tokens = mutableListOf<SyntaxToken>()
        for (pattern in grammar.compiledPatterns) {
            val color = grammar.colors[pattern.colorKey] ?: "#FFFFFF"
            for (match in pattern.regex.findAll(text)) {
                tokens.add(SyntaxToken(
                    start = match.range.first,
                    end = match.range.last + 1,
                    type = pattern.name,
                    color = color
                ))
            }
        }
        return tokens.sortedBy { it.start }
    }
}
