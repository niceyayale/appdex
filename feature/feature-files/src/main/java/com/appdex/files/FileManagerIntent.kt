package com.appdex.files

import com.appdex.arch.MviIntent

sealed interface FileManagerIntent : MviIntent {
    data class NavigateTo(val path: String) : FileManagerIntent
    data class ToggleSelection(val path: String) : FileManagerIntent
    data class CopyFiles(val sources: List<String>, val target: String) : FileManagerIntent
    data class MoveFiles(val sources: List<String>, val target: String) : FileManagerIntent
    data class DeleteFiles(val paths: List<String>) : FileManagerIntent
    data class RenameFile(val path: String, val newName: String) : FileManagerIntent
    data class SearchFiles(val query: String, val regex: Boolean) : FileManagerIntent
    data class CompressFiles(val paths: List<String>, val target: String) : FileManagerIntent
    data class ExtractArchive(val path: String, val target: String) : FileManagerIntent
    data class AddBookmark(val name: String, val path: String) : FileManagerIntent
    data class RemoveBookmark(val path: String) : FileManagerIntent
    data object Refresh : FileManagerIntent
    data object ToggleHiddenFiles : FileManagerIntent
    data object ClearSelection : FileManagerIntent
    data object NavigateUp : FileManagerIntent
}
