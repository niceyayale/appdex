package com.appdex.analyzer

import android.net.Uri
import com.appdex.arch.MviIntent

sealed interface ApkAnalyzerIntent : MviIntent {
    data class OpenApk(val uri: Uri) : ApkAnalyzerIntent
    data class OpenApkPath(val path: String) : ApkAnalyzerIntent
    data object Clear : ApkAnalyzerIntent
}
