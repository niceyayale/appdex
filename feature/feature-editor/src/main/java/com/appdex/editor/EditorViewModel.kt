package com.appdex.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor() : BaseViewModel<EditorIntent, EditorState, EditorEffect>(
    initialState = EditorState()
) {
    override fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.OpenFile -> openFile(intent.path)
            is EditorIntent.UpdateContent -> updateContent(intent.content)
            is EditorIntent.Save -> save()
        }
    }

    private fun openFile(path: String) {
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
                emitEffect(EditorEffect.Error(e.message ?: "Failed to save"))
            }
        }
    }
}

sealed interface EditorEffect : MviEffect {
    data object Saved : EditorEffect
    data class Error(val message: String) : EditorEffect
}
