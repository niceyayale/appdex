package com.appdex.editor

import com.appdex.arch.MviIntent

sealed interface EditorIntent : MviIntent {
    data class OpenFile(val path: String) : EditorIntent
    data class UpdateContent(val content: String) : EditorIntent
    data object Save : EditorIntent
    data object ToggleCopilot : EditorIntent
    data object RequestCopilotInsights : EditorIntent
    data object DismissCopilot : EditorIntent
}
