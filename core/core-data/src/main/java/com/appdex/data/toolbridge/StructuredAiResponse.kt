package com.appdex.data.toolbridge

// ═══════════════════════════════════════════════════════════════
// Structured AI Response — Parsed from LLM output
// ═══════════════════════════════════════════════════════════════
// AI never replies with walls of text.
// AI replies with: Summary → Reason → Risk → Recommendation → Action Cards
// ═══════════════════════════════════════════════════════════════

/**
 * Structured AI response — parsed from raw LLM output.
 */
data class StructuredAiResponse(
    val summary: String,
    val reason: String? = null,
    val risk: String? = null,
    val recommendation: String? = null,
    val actionCards: List<ActionCardData> = emptyList(),
    val technicalDetails: String? = null,
    val rawContent: String
)

/**
 * Parse a raw AI response into a structured format.
 * 
 * The AI is instructed to use the following format:
 * 
 * **总结**: One-line summary
 * **原因**: Detailed explanation
 * **风险**: Risk assessment
 * **建议**: Next steps
 * [ACTION:title|description|iconType|route]
 * **技术详情**: Technical details
 */
fun parseStructuredResponse(content: String): StructuredAiResponse {
    // Extract action cards first
    val actionCards = ToolBridge.parseActionCards(content)
    val withoutCards = ToolBridge.stripActionCards(content)

    // Parse structured sections
    val summary = extractSection(withoutCards, "总结", "Summary")?.trim()
        ?: withoutCards.lines().firstOrNull { it.isNotBlank() }?.trim()
        ?: ""

    val reason = extractSection(withoutCards, "原因", "Reason")
    val risk = extractSection(withoutCards, "风险", "Risk")
    val recommendation = extractSection(withoutCards, "建议", "Recommendation")
    val technicalDetails = extractSection(withoutCards, "技术详情", "Technical Details")

    return StructuredAiResponse(
        summary = summary,
        reason = reason?.trim(),
        risk = risk?.trim(),
        recommendation = recommendation?.trim(),
        actionCards = actionCards,
        technicalDetails = technicalDetails?.trim(),
        rawContent = content
    )
}

/**
 * Extract a section from the AI response.
 * Sections are marked with **Section Name**: or Section Name: headers.
 */
private fun extractSection(text: String, vararg sectionNames: String): String? {
    val lines = text.lines()
    val sectionNamePattern = sectionNames.joinToString("|") { name ->
        Regex.escape(name)
    }
    // Match patterns like "**总结**:" or "总结:" or "**总结**："
    val pattern = Regex("(?:\\*\\*)?(?:$sectionNamePattern)(?:\\*\\*)?\\s*[:：]\\s*(.*?)(?=(?:\\*\\*)?(?:$sectionNamePattern|总结|原因|风险|建议|技术详情|Summary|Reason|Risk|Recommendation|Technical Details)(?:\\*\\*)?\\s*[:：]|\\[ACTION:|\$)", RegexOption.DOT_MATCHES_ALL)

    val match = pattern.find(text)
    return match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
}
