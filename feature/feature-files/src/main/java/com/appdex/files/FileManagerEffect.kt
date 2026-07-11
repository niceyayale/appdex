package com.appdex.files

import com.appdex.arch.MviEffect

sealed interface FileManagerEffect : MviEffect {
    data class ShowToast(val message: String) : FileManagerEffect
    data class OpenFile(val path: String) : FileManagerEffect
    data class OpenEditor(val path: String) : FileManagerEffect
    data class OpenApkAnalyzer(val path: String) : FileManagerEffect
    data class NavigateToPath(val path: String) : FileManagerEffect
    data object FileDeleted : FileManagerEffect
    data object CopyComplete : FileManagerEffect
    data object MoveComplete : FileManagerEffect
    data object CompressComplete : FileManagerEffect
    data object ExtractComplete : FileManagerEffect
    data class Error(val message: String) : FileManagerEffect
}
