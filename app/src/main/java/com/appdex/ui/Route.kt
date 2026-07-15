package com.appdex.ui

import kotlinx.serialization.Serializable

sealed interface Route {
    // ── Root tabs (bottom nav) — AppDex 2.0 ──
    @Serializable
    data object Home : Route          // 首页 - Hero + 分析入口

    @Serializable
    data object Task : Route          // 任务 - APK 生命周期管理

    @Serializable
    data object Files : Route         // 文件管理

    @Serializable
    data object Tools : Route         // 工具集（普通/高级/专家模式）

    @Serializable
    data object Ai : Route            // AI 对话

    @Serializable
    data object Settings : Route      // 设置

    // ── Sub-routes ──
    @Serializable
    data class Editor(val filePath: String? = null) : Route

    @Serializable
    data object Terminal : Route

    @Serializable
    data object Remote : Route

    @Serializable
    data object ApkDetail : Route

    @Serializable
    data class DexBrowser(val apkPath: String? = null) : Route

    @Serializable
    data class HexEditor(val filePath: String) : Route

    @Serializable
    data class ApkSigning(val apkPath: String? = null) : Route

    @Serializable
    data class ApkRepack(val apkPath: String? = null) : Route

    @Serializable
    data object ApkDiff : Route

    @Serializable
    data class ApkSecurity(val apkPath: String? = null) : Route

    @Serializable
    data class ApkSizeAnalyzer(val apkPath: String? = null) : Route

    @Serializable
    data class AxmlEditor(val apkPath: String? = null, val entryName: String? = null) : Route

    @Serializable
    data class ArscViewer(val apkPath: String? = null) : Route

    @Serializable
    data class SqliteViewer(val dbPath: String? = null) : Route

    @Serializable
    data class ElfViewer(val filePath: String? = null) : Route

    // ── Legacy compatibility ──
    @Serializable
    data object Analyzer : Route  // 保留旧路由别名，指向 Task
}
