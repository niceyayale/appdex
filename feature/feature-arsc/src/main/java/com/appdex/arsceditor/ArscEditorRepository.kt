package com.appdex.arsceditor

import com.appdex.apk.ApkFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ARSC 资源表查看器:解析 resources.arsc 中的 Package → Type → Entry → Value。
 *
 * 增强版:在 core/arsc 的基础解析之上,深入解析 TypeSpec、TypeEntry、KeyStringPool、
 * TypeStringPool,将每个资源条目解析为 (resourceId, name, type, value) 四元组。
 */
@Suppress("MagicNumber")
@Singleton
class ArscEditorRepository @Inject constructor() {

    fun parse(binary: ByteArray): ArscParseResult {
        val parser = ArscParser(binary)
        return parser.parse()
    }

    fun parseFromApk(apkPath: String): ArscParseResult {
        return ApkFile(apkPath).use { apk ->
            val bytes = apk.getEntryInputStream("resources.arsc")?.readBytes()
                ?: throw IllegalArgumentException("resources.arsc not found")
            parse(bytes)
        }
    }
}

// ── Data Models ──

data class ArscParseResult(
    val packages: List<ArscPackageData>,
)

data class ArscPackageData(
    val id: Int,
    val name: String,
    val typeStrings: List<String>,
    val keyStrings: List<String>,
    val entries: List<ArscEntryData>,
)

data class ArscEntryData(
    val resourceId: Int,
    val type: String,
    val name: String,
    val value: String,
    val config: String,
)
