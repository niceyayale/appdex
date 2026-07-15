package com.appdex.hex

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HexEditorViewModel @Inject constructor(
    private val hexRepository: HexRepository,
) : BaseViewModel<HexEditorIntent, HexEditorState, HexEditorEffect>(
    initialState = HexEditorState()
) {

    override fun handleIntent(intent: HexEditorIntent) {
        when (intent) {
            is HexEditorIntent.OpenFile -> openFile(intent.filePath)
            is HexEditorIntent.EditByte -> editByte(intent.offset, intent.value)
            is HexEditorIntent.Save -> save()
            is HexEditorIntent.Search -> search(intent.query, intent.isHex)
            is HexEditorIntent.JumpToOffset -> jumpToOffset(intent.offset)
            is HexEditorIntent.ToggleEditMode -> update { it.copy(isEditMode = !it.isEditMode) }
            is HexEditorIntent.ClearSearch -> update {
                it.copy(searchQuery = "", searchResults = emptyList(), searchResultIndex = -1, jumpOffset = -1)
            }
            is HexEditorIntent.ClearError -> update { it.copy(error = null) }
        }
    }

    private fun openFile(filePath: String) {
        update {
            it.copy(
                filePath = filePath,
                fileName = File(filePath).name,
                isLoading = true,
                error = null,
                isDirty = false,
                searchQuery = "",
                searchResults = emptyList(),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = hexRepository.readBytes(filePath)
                val rows = hexRepository.toHexRows(bytes)
                update {
                    it.copy(
                        bytes = bytes,
                        hexRows = rows,
                        fileSize = bytes.size.toLong(),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "打开文件失败"
                update { it.copy(isLoading = false, error = msg) }
                emitEffect(HexEditorEffect.ShowError(msg))
            }
        }
    }

    private fun editByte(offset: Int, value: Byte) {
        if (offset < 0 || offset >= currentState.bytes.size) return
        val newBytes = currentState.bytes.copyOf()
        newBytes[offset] = value
        val rows = hexRepository.toHexRows(newBytes)
        update {
            it.copy(
                bytes = newBytes,
                hexRows = rows,
                isDirty = true,
            )
        }
    }

    private fun save() {
        if (!currentState.isDirty) {
            emitEffect(HexEditorEffect.ShowToast("文件未修改"))
            return
        }
        update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                hexRepository.writeBytes(currentState.filePath, currentState.bytes)
                update { it.copy(isSaving = false, isDirty = false) }
                emitEffect(HexEditorEffect.ShowToast("保存成功"))
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "保存失败"
                update { it.copy(isSaving = false, error = msg) }
                emitEffect(HexEditorEffect.ShowError(msg))
            }
        }
    }

    private fun search(query: String, isHex: Boolean) {
        if (query.isBlank()) {
            update { it.copy(searchQuery = "", searchResults = emptyList(), searchResultIndex = -1) }
            return
        }
        update { it.copy(searchQuery = query) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = if (isHex) {
                    val pattern = hexRepository.parseHexString(query)
                    hexRepository.searchHex(currentState.bytes, pattern)
                } else {
                    hexRepository.searchAscii(currentState.bytes, query)
                }
                update {
                    it.copy(
                        searchResults = results,
                        searchResultIndex = if (results.isNotEmpty()) 0 else -1,
                        jumpOffset = results.firstOrNull() ?: -1,
                    )
                }
                emitEffect(HexEditorEffect.SearchComplete(results.size))
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "搜索失败"
                emitEffect(HexEditorEffect.ShowError(msg))
            }
        }
    }

    private fun jumpToOffset(offset: Long) {
        update { it.copy(jumpOffset = offset) }
    }
}
