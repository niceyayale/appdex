package com.appdex.analyzer

import android.graphics.Bitmap
import com.appdex.apk.ApkInfo
import com.appdex.arch.MviState

data class ApkAnalyzerState(
    val apkInfo: ApkInfo? = null,
    val appIcon: Bitmap? = null,
    val apkFilePath: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) : MviState
