package com.appdex.editor

import com.appdex.arch.MviState
import com.appdex.ui.components.CopilotInsight

data class EditorState(
    val filePath: String? = null,
    val fileName: String? = null,
    val content: String = "",
    val encoding: String = "UTF-8",
    val isModified: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Copilot state
    val copilotInsights: List<CopilotInsight> = emptyList(),
    val isCopilotLoading: Boolean = false,
    val isCopilotVisible: Boolean = false
) : MviState
