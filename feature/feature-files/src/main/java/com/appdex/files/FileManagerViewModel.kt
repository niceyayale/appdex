package com.appdex.files

import android.os.Environment
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor() : BaseViewModel<FileManagerIntent, FileManagerState, FileManagerEffect>(
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
                        permissions = getPermissions(file),
                        mimeType = null,
                        extension = if (file.isFile) file.extension else ""
                    )
                }?.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                ) ?: emptyList()

                update { it.copy(files = files, isLoading = false) }
            } catch (e: SecurityException) {
                update { it.copy(isLoading = false, error = "Permission denied: ${e.message}") }
            } catch (e: Exception) {
                update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun getPermissions(file: File): String {
        return if (file.canRead() && file.canWrite()) "rw"
        else if (file.canRead()) "r"
        else if (file.canWrite()) "w"
        else "---"
    }

    private fun navigateUp() {
        val current = File(currentState.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.canRead() && parent.absolutePath != "/") {
            navigateTo(parent.absolutePath)
        } else if (current.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
            navigateTo(Environment.getExternalStorageDirectory().absolutePath)
        }
    }

    private fun refresh() {
        navigateTo(currentState.currentPath)
    }

    private fun toggleHidden() {
        update { it.copy(showHidden = !it.showHidden) }
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
        if (query.isBlank()) {
            refresh()
            return
        }
        update { it.copy(isSearching = true, searchQuery = query) }
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = File(currentState.currentPath)
            val results = mutableListOf<FileItem>()
            baseDir.walkTopDown().maxDepth(5).forEach { file ->
                if (file.name.contains(query, ignoreCase = true)) {
                    results.add(FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified(),
                        extension = if (file.isFile) file.extension else ""
                    ))
                }
                if (results.size >= 200) return@forEach
            }
            update { it.copy(isSearching = false, files = results) }
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
        if (paths.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val total = paths.size
                val targetFile = File(target)
                ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
                    paths.forEachIndexed { index, path ->
                        update {
                            it.copy(operationProgress = FileOperation(
                                type = FileOperationType.COMPRESS,
                                current = index + 1,
                                total = total,
                                currentFile = File(path).name
                            ))
                        }
                        val srcFile = File(path)
                        if (srcFile.isDirectory) {
                            zipDirectory(srcFile, srcFile.parentFile?.name ?: "", zos)
                        } else {
                            zipFile(srcFile, "", zos)
                        }
                    }
                }
                update { it.copy(operationProgress = null, selectedPaths = emptySet()) }
                emitEffect(FileManagerEffect.CompressComplete)
                refresh()
            } catch (e: Exception) {
                update { it.copy(operationProgress = null) }
                emitEffect(FileManagerEffect.Error("Compress failed: ${e.message}"))
            }
        }
    }

    private fun zipFile(file: File, basePath: String, zos: ZipOutputStream) {
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                zos.write(buffer, 0, len)
            }
        }
        zos.closeEntry()
    }

    private fun zipDirectory(dir: File, basePath: String, zos: ZipOutputStream) {
        val dirName = if (basePath.isEmpty()) dir.name else "$basePath/${dir.name}"
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                zipDirectory(file, dirName, zos)
            } else {
                zipFile(file, dirName, zos)
            }
        }
    }

    private fun extractArchive(path: String, target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val zipFile = File(path)
                val targetDir = File(target)
                targetDir.mkdirs()

                ZipInputStream(FileInputStream(zipFile)).use { zis ->
                    var entry: ZipEntry
                    var count = 0
                    while (zis.nextEntry.also { entry = it } != null) {
                        count++
                        update {
                            it.copy(operationProgress = FileOperation(
                                type = FileOperationType.EXTRACT,
                                current = count,
                                total = 0,
                                currentFile = entry.name
                            ))
                        }
                        val outFile = File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                    }
                }
                update { it.copy(operationProgress = null) }
                emitEffect(FileManagerEffect.ExtractComplete)
            } catch (e: Exception) {
                update { it.copy(operationProgress = null) }
                emitEffect(FileManagerEffect.Error("Extract failed: ${e.message}"))
            }
        }
    }
}
