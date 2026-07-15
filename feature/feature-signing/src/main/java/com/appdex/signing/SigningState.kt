package com.appdex.signing

import com.appdex.arch.MviState

/**
 * 签名流程步骤。
 */
enum class SigningStep {
    /** 选择 APK */
    SELECT_APK,
    /** 选择 Keystore */
    SELECT_KEYSTORE,
    /** 输入密码和选择条目 */
    ENTER_CREDENTIALS,
    /** 签名选项 */
    SIGN_OPTIONS,
    /** 签名中 */
    SIGNING,
    /** 签名完成 */
    COMPLETE,
}

data class SigningState(
    val step: SigningStep = SigningStep.SELECT_APK,
    val inputApkPath: String = "",
    val inputApkName: String = "",
    val keystorePath: String = "",
    val keystorePassword: String = "",
    val keyPassword: String = "",
    val keystoreEntries: List<KeystoreEntryInfo> = emptyList(),
    val selectedAlias: String = "",
    val schemeConfig: SigningSchemeConfig = SigningSchemeConfig(),
    val isSigning: Boolean = false,
    val isCreatingKeystore: Boolean = false,
    val signingResult: SigningResult? = null,
    val error: String? = null,
) : MviState
