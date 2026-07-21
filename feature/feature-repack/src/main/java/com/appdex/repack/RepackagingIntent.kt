package com.appdex.repack

import com.appdex.arch.MviIntent

sealed interface RepackagingIntent : MviIntent {
    /** 设置输入 APK 路径。 */
    data class SetInputApk(val path: String) : RepackagingIntent
    /** 加载 DEX 文件列表。 */
    data object LoadDexFiles : RepackagingIntent
    /** 选择要替换的 DEX 文件。 */
    data class SelectDexFile(val dexName: String) : RepackagingIntent
    /** 设置 Smali 替换内容。 */
    data class SetSmaliContent(val dexName: String, val smaliContents: Map<String, String>) : RepackagingIntent
    /** 设置 Keystore 信息。 */
    data class SetKeystoreInfo(
        val keystorePath: String,
        val keystorePassword: String,
        val keyAlias: String,
        val keyPassword: String,
    ) : RepackagingIntent
    /** 执行回编译+签名。 */
    data class RepackAndSign(val outputPath: String) : RepackagingIntent
    /** 仅回编译（不签名）。 */
    data class RepackOnly(val outputPath: String) : RepackagingIntent
    /** 清除错误。 */
    data object ClearError : RepackagingIntent
    /** 重置。 */
    data object Reset : RepackagingIntent
}
