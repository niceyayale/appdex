package com.appdex.dex

import com.appdex.arch.MviEffect

sealed interface DexBrowserEffect : MviEffect {
    data class ShowError(val message: String) : DexBrowserEffect
    data class ShowToast(val message: String) : DexBrowserEffect
}
