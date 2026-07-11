package com.appdex.analyzer

import com.appdex.arch.MviIntent

sealed interface ApkAnalyzerIntent : MviIntent {
    data class OpenApk(val path: String, val uri: android.net.Uri) : ApkAnalyzerIntent
    data object Clear : ApkAnalyzerIntent
}
