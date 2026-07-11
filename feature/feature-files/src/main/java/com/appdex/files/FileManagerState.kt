package com.appdex.files

import com.appdex.arch.MviState
import com.appdex.model.Bookmark
import com.appdex.model.FileItem
import com.appdex.model.FileOperation

data class FileManagerState(
    val currentPath: String = "/storage/emulated/0",
    val files: List<FileItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val showHidden: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val operationProgress: FileOperation? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : MviState {
    val hasSelection: Boolean get() = selectedPaths.isNotEmpty()
    val selectedFiles: List<FileItem> get() = files.filter { it.path in selectedPaths }
}
