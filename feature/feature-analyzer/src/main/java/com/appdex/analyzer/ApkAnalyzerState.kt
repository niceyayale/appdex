package com.appdex.analyzer

import com.appdex.apk.ApkInfo
import com.appdex.arch.MviState

data class ApkAnalyzerState(
    val apkInfo: ApkInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) : MviState
