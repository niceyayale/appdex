package com.appdex.repack

import com.appdex.arch.MviState

enum class RepackStep {
    /** 选择 APK */
    SELECT_APK,
    /** 选择 DEX */
    SELECT_DEX,
    /** 输入 Keystore 信息 */
    ENTER_KEYSTORE,
    /** 回编译中 */
    REPACKING,
    /** 完成 */
    COMPLETE,
}

data class RepackagingState(
    val step: RepackStep = RepackStep.SELECT_APK,
    val inputApkPath: String = "",
    val inputApkName: String = "",
    val dexFiles: List<String> = emptyList(),
    val selectedDexFiles: Set<String> = emptySet(),
    val smaliReplacements: Map<String, Map<String, String>> = emptyMap(),
    val keystorePath: String = "",
    val keystorePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = "",
    val isProcessing: Boolean = false,
    val repackResult: RepackResult? = null,
    val error: String? = null,
) : MviState
