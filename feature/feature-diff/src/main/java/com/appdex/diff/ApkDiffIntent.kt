package com.appdex.diff

import com.appdex.arch.MviIntent

sealed interface ApkDiffIntent : MviIntent {
    data class SetOldApk(val path: String) : ApkDiffIntent
    data class SetNewApk(val path: String) : ApkDiffIntent
    data object RunDiff : ApkDiffIntent
    data object ClearError : ApkDiffIntent
    data object Reset : ApkDiffIntent
}
