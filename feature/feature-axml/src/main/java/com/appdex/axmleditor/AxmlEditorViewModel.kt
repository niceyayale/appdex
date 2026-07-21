package com.appdex.axmleditor

import android.util.Log

import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import com.appdex.arch.MviIntent
import com.appdex.arch.MviState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ── State ──
data class AxmlEditorState(
    val xmlText: String = "",
    val binaryHexPreview: String = "",
    val fileName: String = "",
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val parseError: String? = null,
    val isSaved: Boolean = false,
) : MviState

// ── Intent ──
sealed interface AxmlEditorIntent : MviIntent {
    data class LoadFromApk(val apkPath: String, val entryName: String) : AxmlEditorIntent
    data class LoadFromBytes(val bytes: ByteArray, val fileName: String) : AxmlEditorIntent
    data class UpdateXmlText(val text: String) : AxmlEditorIntent
    data object EncodeToBinary : AxmlEditorIntent
    data object ToggleEdit : AxmlEditorIntent
}

// ── Effect ──
sealed interface AxmlEditorEffect : MviEffect {
    data class ShowMessage(val message: String) : AxmlEditorEffect
}

@HiltViewModel
class AxmlEditorViewModel @Inject constructor(
    private val repository: AxmlEditorRepository,
) : BaseViewModel<AxmlEditorIntent, AxmlEditorState, AxmlEditorEffect>(AxmlEditorState()) {

    override fun handleIntent(intent: AxmlEditorIntent) {
        when (intent) {
            is AxmlEditorIntent.LoadFromApk -> loadFromApk(intent.apkPath, intent.entryName)
            is AxmlEditorIntent.LoadFromBytes -> loadFromBytes(intent.bytes, intent.fileName)
            is AxmlEditorIntent.UpdateXmlText -> update {
                it.copy(xmlText = intent.text, isSaved = false)
            }
            AxmlEditorIntent.EncodeToBinary -> encodeToBinary()
            AxmlEditorIntent.ToggleEdit -> update { it.copy(isEditing = !it.isEditing) }
        }
    }

    private fun loadFromApk(apkPath: String, entryName: String) {
        update { it.copy(isLoading = true, fileName = entryName, parseError = null) }
        launchEffect {
            try {
                val xml = repository.decodeFromApk(apkPath, entryName)
                update { it.copy(xmlText = xml, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, parseError = e.message ?: "Failed to decode AXML") }
                emitEffect(AxmlEditorEffect.ShowMessage("解码失败: ${e.message}"))
            }
        }
    }

    private fun loadFromBytes(bytes: ByteArray, fileName: String) {
        update { it.copy(isLoading = true, fileName = fileName, parseError = null) }
        launchEffect {
            try {
                val xml = repository.decode(bytes)
                update { it.copy(xmlText = xml, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, parseError = e.message ?: "Failed to decode AXML") }
                emitEffect(AxmlEditorEffect.ShowMessage("解码失败: ${e.message}"))
            }
        }
    }

    private fun encodeToBinary() {
        update { it.copy(isLoading = true) }
        launchEffect {
            try {
                val binary = repository.encode(currentState.xmlText)
                val hexPreview = binary.take(512).joinToString("") { "%02x".format(it) }
                update {
                    it.copy(
                        binaryHexPreview = hexPreview,
                        isLoading = false,
                        isSaved = true,
                    )
                }
                emitEffect(AxmlEditorEffect.ShowMessage("编码成功 (${binary.size} bytes)"))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, parseError = e.message) }
                emitEffect(AxmlEditorEffect.ShowMessage("编码失败: ${e.message}"))
            }
        }
    }
}
