package com.appdex.signing

import com.appdex.arch.MviIntent

sealed interface SigningIntent : MviIntent {
    /** 设置输入 APK 路径。 */
    data class SetInputApk(val path: String) : SigningIntent
    /** 加载 Keystore 文件。 */
    data class LoadKeystore(val path: String, val password: String) : SigningIntent
    /** 列出 Keystore 条目。 */
    data class ListKeystoreEntries(val path: String, val password: String) : SigningIntent
    /** 选择 Keystore 条目。 */
    data class SelectEntry(val alias: String) : SigningIntent
    /** 设置 key 密码。 */
    data class SetKeyPassword(val password: String) : SigningIntent
    /** 切换签名方案。 */
    data class ToggleScheme(val v1: Boolean, val v2: Boolean, val v3: Boolean) : SigningIntent
    /** 执行签名。 */
    data class Sign(val outputPath: String) : SigningIntent
    /** 创建新 Keystore。 */
    data class CreateKeystore(
        val path: String,
        val keystorePassword: String,
        val alias: String,
        val keyPassword: String,
        val subject: String,
    ) : SigningIntent
    /** 清除错误。 */
    data object ClearError : SigningIntent
    /** 重置状态。 */
    data object Reset : SigningIntent
}
