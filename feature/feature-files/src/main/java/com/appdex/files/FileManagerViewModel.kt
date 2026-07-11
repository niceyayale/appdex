package com.appdex.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.model.FileItem
import com.appdex.model.FileOperation
import com.appdex.model.FileOperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    // Repositories will be injected here as features are implemented
) : BaseViewModel<FileManagerIntent, FileManagerState, FileManagerEffect>(
    initialState = FileManagerState()
) {

    init {
        handleIntent(FileManagerIntent.NavigateTo(currentState.currentPath))
    }

    override fun handleIntent(intent: FileManagerIntent) {
        when (intent) {
            is FileManagerIntent.NavigateTo -> navigateTo(intent.path)
            is FileManagerIntent.NavigateUp -> navigateUp()
            is FileManagerIntent.Refresh -> refresh()
            is FileManagerIntent.ToggleHiddenFiles -> toggleHidden()
            is FileManagerIntent.ToggleSelection -> toggleSelection(intent.path)
            is FileManagerIntent.ClearSelection -> clearSelection()
            is FileManagerIntent.DeleteFiles -> deleteFiles(intent.paths)
            is FileManagerIntent.RenameFile -> renameFile(intent.path, intent.newName)
            is FileManagerIntent.SearchFiles -> searchFiles(intent.query, intent.regex)
            is FileManagerIntent.CopyFiles -> copyFiles(intent.sources, intent.target)
            is FileManagerIntent.MoveFiles -> moveFiles(intent.sources, intent.target)
            is FileManagerIntent.CompressFiles -> compressFiles(intent.paths, intent.target)
            is FileManagerIntent.ExtractArchive -> extractArchive(intent.path, intent.target)
        }
    }

    private fun navigateTo(path: String) {
        update { it.copy(currentPath = path, isLoading = true, error = null, selectedPaths = emptySet()) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) {
                    update { it.copy(isLoading = false, error = "Directory not found") }
                    return@launch
                }
                val files = dir.listFiles()?.map { file ->
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified(),
                        permissions = "",
                        mimeType = null,
                        extension = if (file.isFile) file.extension else ""
                    )
                }?.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                ) ?: emptyList()

                update { it.copy(files = files, isLoading = false) }
            } catch (e: Exception) {
                update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun navigateUp() {
        val parent = File(currentState.currentPath).parentFile
        if (parent != null && parent.canRead()) {
            navigateTo(parent.absolutePath)
        }
    }

    private fun refresh() {
        navigateTo(currentState.currentPath)
    }

    private fun toggleHidden() {
        update { it.copy(showHidden = !it.showHidden) }
        refresh()
    }

    private fun toggleSelection(path: String) {
        update {
            val newSelection = if (path in it.selectedPaths) {
                it.selectedPaths - path
            } else {
                it.selectedPaths + path
            }
            it.copy(selectedPaths = newSelection)
        }
    }

    private fun clearSelection() {
        update { it.copy(selectedPaths = emptySet()) }
    }

    private fun deleteFiles(paths: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            paths.forEachIndexed { index, path ->
                update {
                    it.copy(operationProgress = FileOperation(
                        type = FileOperationType.DELETE,
                        current = index + 1,
                        total = paths.size,
                        currentFile = File(path).name
                    ))
                }
                File(path).deleteRecursively()
            }
            update { it.copy(operationProgress = null, selectedPaths = emptySet()) }
            emitEffect(FileManagerEffect.FileDeleted)
            refresh()
        }
    }

    private fun renameFile(path: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            val target = File(file.parentFile, newName)
            if (file.renameTo(target)) {
                refresh()
            } else {
                emitEffect(FileManagerEffect.Error("Failed to rename file"))
            }
        }
    }

    private fun searchFiles(query: String, regex: Boolean) {
        update { it.copy(isSearching = true, searchQuery = query) }
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Implement recursive search
            update { it.copy(isSearching = false) }
        }
    }

    private fun copyFiles(sources: List<String>, target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sources.forEachIndexed { index, src ->
                update {
                    it.copy(operationProgress = FileOperation(
                        type = FileOperationType.COPY,
                        current = index + 1,
                        total = sources.size,
                        currentFile = File(src).name
                    ))
                }
                val srcFile = File(src)
                val targetFile = File(target, srcFile.name)
                srcFile.copyRecursively(targetFile, overwrite = true)
            }
            update { it.copy(operationProgress = null, selectedPaths = emptySet()) }
            emitEffect(FileManagerEffect.CopyComplete)
        }
    }

    private fun moveFiles(sources: List<String>, target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sources.forEachIndexed { index, src ->
                update {
                    it.copy(operationProgress = FileOperation(
                        type = FileOperationType.MOVE,
                        current = index + 1,
                        total = sources.size,
                        currentFile = File(src).name
                    ))
                }
                val srcFile = File(src)
                val targetFile = File(target, srcFile.name)
                srcFile.renameTo(targetFile)
            }
            update { it.copy(operationProgress = null, selectedPaths = emptySet()) }
            emitEffect(FileManagerEffect.MoveComplete)
            refresh()
        }
    }

    private fun compressFiles(paths: List<String>, target: String) {
        // TODO: Implement using lib-archive
        emitEffect(FileManagerEffect.CompressComplete)
    }

    private fun extractArchive(path: String, target: String) {
        // TODO: Implement using lib-archive
        emitEffect(FileManagerEffect.ExtractComplete)
    }
}
