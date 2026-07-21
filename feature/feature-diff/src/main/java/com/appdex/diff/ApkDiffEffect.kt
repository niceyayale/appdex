package com.appdex.diff

import com.appdex.arch.MviEffect

sealed interface ApkDiffEffect : MviEffect {
    data class ShowError(val message: String) : ApkDiffEffect
    data class DiffComplete(val summary: ApkDiffSummary) : ApkDiffEffect
}
