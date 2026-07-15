package com.appdex.hex

import com.appdex.arch.MviEffect

sealed interface HexEditorEffect : MviEffect {
    data class ShowError(val message: String) : HexEditorEffect
    data class ShowToast(val message: String) : HexEditorEffect
    data class SearchComplete(val resultCount: Int) : HexEditorEffect
}
