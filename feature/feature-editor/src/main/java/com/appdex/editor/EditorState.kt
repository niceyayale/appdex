package com.appdex.editor

import com.appdex.arch.MviState

data class EditorState(
    val filePath: String? = null,
    val fileName: String? = null,
    val content: String = "",
    val encoding: String = "UTF-8",
    val isModified: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) : MviState
