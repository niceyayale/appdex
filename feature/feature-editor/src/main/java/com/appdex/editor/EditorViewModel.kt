package com.appdex.editor

import android.util.Log

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import com.appdex.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<EditorIntent, EditorState, EditorEffect>(
    initialState = EditorState(),
    savedStateHandle = savedStateHandle
) {
    val editorFontSize = settings.editorFontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 14)
    val editorTabWidth = settings.editorTabWidth
        .stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    override fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.OpenFile -> openFile(intent.path)
            is EditorIntent.UpdateContent -> updateContent(intent.content)
            is EditorIntent.Save -> save()
        }
    }

    fun openFileIfProvided(filePath: String?) {
        val path = filePath ?: restoreState("editor_file_path", "")
        if (path.isNotEmpty()) {
            handleIntent(EditorIntent.OpenFile(path))
        }
    }

    fun openFileFromUri(uri: Uri, contentResolver: ContentResolver) {
        update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = uri.lastPathSegment ?: "unknown"
                val content = contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: run {
                    update { it.copy(isLoading = false, error = "Cannot open file") }
                    return@launch
                }

                if (content.length > 5 * 1024 * 1024) {
                    update { it.copy(isLoading = false, error = "File too large (max 5MB)") }
                    return@launch
                }

                // Try to copy to a temp file for saving
                val tempFile = File.createTempFile("edit_", "_${fileName}")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                update {
                    it.copy(
                        filePath = tempFile.absolutePath,
                        fileName = fileName.substringAfterLast("/"),
                        content = content,
                        isModified = false,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message ?: "Failed to open file") }
            }
        }
    }

    private fun openFile(path: String) {
        saveState("editor_file_path", path)
        update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists() || !file.isFile) {
                    update { it.copy(isLoading = false, error = "File not found") }
                    return@launch
                }
                if (file.length() > 5 * 1024 * 1024) {
                    update { it.copy(isLoading = false, error = "File too large (max 5MB)") }
                    return@launch
                }
                val content = file.readText()
                update {
                    it.copy(
                        filePath = path,
                        fileName = file.name,
                        content = content,
                        isModified = false,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message ?: "Failed to open file") }
            }
        }
    }

    private fun updateContent(newContent: String) {
        update { it.copy(content = newContent, isModified = true) }
    }

    private fun save() {
        val path = currentState.filePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(path).writeText(currentState.content)
                update { it.copy(isModified = false) }
                emitEffect(EditorEffect.Saved)
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                emitEffect(EditorEffect.Error(e.message ?: "Failed to save"))
            }
        }
    }
}

sealed interface EditorEffect : MviEffect {
    data object Saved : EditorEffect
    data class Error(val message: String) : EditorEffect
}
