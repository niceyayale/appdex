package com.appdex.ui

import kotlinx.serialization.Serializable

sealed interface Route {
    // ── Root tabs (bottom nav) — AppX 3.5 AI-First ──
    // "Less UI. More Intelligence."
    // 3 tabs only: AI / Workspace / Settings
    @Serializable
    data object Ai : Route            // AI — Primary entry point, ChatGPT-style

    @Serializable
    data object Workspace : Route     // Workspace — APK analysis dashboard & tools

    @Serializable
    data object Settings : Route      // Settings — Configuration

    // ── Sub-routes (accessible from Workspace) ──
    @Serializable
    data class Editor(val filePath: String? = null) : Route

    @Serializable
    data object Terminal : Route

    @Serializable
    data object Remote : Route

    @Serializable
    data object ApkDetail : Route

    @Serializable
    data class DexBrowser(val apkPath: String? = null, val searchQuery: String? = null) : Route

    @Serializable
    data class HexEditor(val filePath: String, val offset: Long = 0L) : Route

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
    data class ArscViewer(val apkPath: String? = null, val resourceId: String? = null) : Route

    @Serializable
    data class SqliteViewer(val dbPath: String? = null) : Route

    @Serializable
    data class ElfViewer(val filePath: String? = null) : Route

    @Serializable
    data class Files(val path: String? = null) : Route

    @Serializable
    data object Tools : Route

    @Serializable
    data object AiProviders : Route  // AI 提供商管理页面

    @Serializable
    data object Report : Route  // Executive report

    // ── Legacy compatibility ──
    @Serializable
    data object Home : Route      // Legacy alias → redirects to Ai

    @Serializable
    data object Task : Route      // Legacy alias → redirects to Workspace

    @Serializable
    data object Analyzer : Route  // Legacy alias → redirects to Workspace
}
