package com.appdex.signing

import com.appdex.arch.MviEffect

sealed interface SigningEffect : MviEffect {
    data class ShowError(val message: String) : SigningEffect
    data class ShowToast(val message: String) : SigningEffect
    data class SigningComplete(val result: SigningResult) : SigningEffect
}
