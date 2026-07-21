package com.appdex.repack

import com.appdex.arch.MviEffect

sealed interface RepackagingEffect : MviEffect {
    data class ShowError(val message: String) : RepackagingEffect
    data class ShowToast(val message: String) : RepackagingEffect
    data class RepackComplete(val result: RepackResult) : RepackagingEffect
}
