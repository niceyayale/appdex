package com.appdex.dex

import com.appdex.arch.MviIntent

sealed interface DexBrowserIntent : MviIntent {
    /** 加载 APK 中的 DEX 文件列表。 */
    data class LoadDexFiles(val apkPath: String) : DexBrowserIntent
    /** 选择某个 DEX 文件，加载其类列表。 */
    data class SelectDex(val dexName: String) : DexBrowserIntent
    /** 搜索类。 */
    data class SearchClasses(val query: String) : DexBrowserIntent
    /** 选择某个类，加载其 Smali 代码。 */
    data class SelectClass(val classType: String) : DexBrowserIntent
    /** 返回到 DEX 文件列表。 */
    data object BackToDexList : DexBrowserIntent
    /** 返回到类列表。 */
    data object BackToClassList : DexBrowserIntent
    /** 清除错误状态。 */
    data object ClearError : DexBrowserIntent
}
