package com.appdex.dex

import com.appdex.arch.MviState

/**
 * DEX 浏览器的视图层级。
 */
enum class DexViewLevel {
    /** DEX 文件列表 */
    DEX_LIST,
    /** 类列表 */
    CLASS_LIST,
    /** Smali 代码查看 */
    SMALI_VIEWER,
}

/**
 * DEX 浏览器状态。
 */
data class DexBrowserState(
    val apkPath: String = "",
    val viewLevel: DexViewLevel = DexViewLevel.DEX_LIST,
    val dexFiles: List<DexFileInfo> = emptyList(),
    val selectedDexName: String = "",
    val allClasses: List<DexClassInfo> = emptyList(),
    val filteredClasses: List<DexClassInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedClassType: String = "",
    val smaliCode: String = "",
    val isLoading: Boolean = false,
    val isLoadingSmali: Boolean = false,
    val error: String? = null,
) : MviState
