package com.appdex.diff

import com.appdex.arch.MviState

data class ApkDiffState(
    val oldApkPath: String = "",
    val newApkPath: String = "",
    val isDiffing: Boolean = false,
    val diffResult: ApkDiffSummary? = null,
    val error: String? = null,
) : MviState
