package com.appdex.data.workspace

import com.appdex.apk.ApkInfo
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.AnalysisFinding
import com.appdex.data.session.AiMessage
import com.appdex.data.session.SessionStatus
import java.util.UUID

// ═══════════════════════════════════════════════════════════════
// WorkspaceObject — The Single Source of Truth
// ═══════════════════════════════════════════════════════════════
// This is the ONLY state object in the entire App.
// Every page, every tool, every AI, every Report reads from this.
// No page maintains its own state. No page passes params to another.
// ═══════════════════════════════════════════════════════════════

/**
 * The unified Workspace Object — the entire App's single state.
 *
 * Design principle:
 * - Pages NEVER maintain their own CurrentXXX
 * - Tools NEVER call each other directly
 * - All communication flows through WorkspaceEventBus
 * - All state lives in WorkspaceObject
 */
data class WorkspaceObject(
    // ── Identity ──
    val id: String = UUID.randomUUID().toString(),

    // ── APK ──
    val apkFilePath: String? = null,
    val apkInfo: ApkInfo? = null,
    val packageName: String = "",
    val versionName: String = "",
    val fileSize: Long = 0L,

    // ── Session ──
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val securityScore: Int = 100,
    val findings: List<AnalysisFinding> = emptyList(),
    val aiMessages: List<AiMessage> = emptyList(),

    // ── Current Selection (Phase C — single global selection) ──
    val selection: WorkspaceSelection = WorkspaceSelection.None,

    // ── Breadcrumbs (Phase E) ──
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),

    // ── Navigation State (Phase H) ──
    val activeTool: WorkspaceTool = WorkspaceTool.NONE,
    val navigationState: NavigationState = NavigationState(),

    // ── History / Undo Tree (Phase I) ──
    val history: List<WorkspaceHistoryEntry> = emptyList(),
    val pinnedItems: List<PinnedItem> = emptyList(),
    val recentItems: List<RecentItem> = emptyList(),

    // ── Workspace Flags ──
    val flags: WorkspaceFlags = WorkspaceFlags(),

    // ── Inspection Target (Phase D — Inspector) ──
    val inspectionTarget: InspectionTarget = InspectionTarget(),

    // ── Tool States (each tool reads from here, not its own state) ──
    val toolStates: Map<WorkspaceTool, ToolState> = emptyMap(),

    // ── Timeline ──
    val timeline: List<WorkspaceTimelineEntry> = emptyList(),

    // ── Search (global search state, shared across tools) ──
    val currentSearch: SearchState = SearchState(),

    // ── Metadata ──
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = packageName.ifEmpty { apkFilePath?.substringAfterLast('/') ?: "未知应用" }

    val hasApk: Boolean
        get() = apkFilePath != null && sessionStatus != SessionStatus.IDLE

    val isReady: Boolean
        get() = sessionStatus == SessionStatus.READY

    /** Get the tool state for a specific tool, or default */
    fun toolState(tool: WorkspaceTool): ToolState = toolStates[tool] ?: ToolState()

    /** Build a human-readable context summary for AI */
    fun toContextString(): String = buildString {
        appendLine("=== Workspace Context ===")
        appendLine("APK: $displayName")
        appendLine("Tool: ${activeTool.displayName}")
        selection.toContextString()?.let { appendLine(it) }
        if (currentSearch.query.isNotEmpty()) {
            appendLine("Search: ${currentSearch.query}")
        }
        if (breadcrumbs.isNotEmpty()) {
            appendLine("Navigation: ${breadcrumbs.joinToString(" > ") { it.label }}")
        }
        if (timeline.isNotEmpty()) {
            appendLine("Recent: ${timeline.takeLast(5).joinToString(", ") { it.title }}")
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Workspace Selection (Phase C)
// ═══════════════════════════════════════════════════════════════

/**
 * The single global selection in the entire App.
 * When user selects something in Manifest, DEX auto-navigates.
 * When user selects a class in DEX, AI auto-updates, Editor auto-opens.
 */
sealed class WorkspaceSelection {
    /** No selection */
    data object None : WorkspaceSelection()

    /** A file/entry within the APK */
    data class File(val path: String, val displayName: String = path) : WorkspaceSelection()

    /** An XML node (from Manifest) */
    data class XmlNode(val tagName: String, val name: String, val parent: String? = null) : WorkspaceSelection()

    /** A class from DEX */
    data class Class(val className: String, val dexFile: String? = null) : WorkspaceSelection()

    /** A method within a class */
    data class Method(val className: String, val methodName: String, val descriptor: String? = null) : WorkspaceSelection()

    /** A field within a class */
    data class Field(val className: String, val fieldName: String, val type: String? = null) : WorkspaceSelection()

    /** A string constant from DEX */
    data class StringValue(val value: String, val className: String? = null) : WorkspaceSelection()

    /** A permission from Manifest */
    data class Permission(val name: String) : WorkspaceSelection()

    /** A component (Activity/Service/Receiver/Provider) */
    data class Component(val name: String, val type: ComponentType, val exported: Boolean = false) : WorkspaceSelection()

    /** A resource (from ARSC) */
    data class Resource(val resourceId: String, val type: String? = null, val value: String? = null) : WorkspaceSelection()

    /** An offset in HEX editor */
    data class Offset(val address: Long, val file: String? = null) : WorkspaceSelection()

    /** A finding from security scan */
    data class Finding(val findingId: String, val title: String) : WorkspaceSelection()

    fun toContextString(): String? = when (this) {
        is None -> null
        is File -> "Current File: $displayName ($path)"
        is XmlNode -> "Current XML Node: <$tagName> name=\"$name\""
        is Class -> "Current Class: $className"
        is Method -> "Current Method: $className.$methodName"
        is Field -> "Current Field: $className.$fieldName"
        is StringValue -> "Current String: \"${value.take(50)}\""
        is Permission -> "Current Permission: $name"
        is Component -> "Current Component: $type $name"
        is Resource -> "Current Resource: $resourceId"
        is Offset -> "Current Offset: 0x${address.toString(16)}"
        is Finding -> "Current Finding: $title"
    }

    val displayLabel: String get() = when (this) {
        is None -> ""
        is File -> displayName
        is XmlNode -> name
        is Class -> className.substringAfterLast('.')
        is Method -> "$methodName()"
        is Field -> fieldName
        is StringValue -> "\"${value.take(30)}\""
        is Permission -> name.substringAfterLast('.')
        is Component -> name.substringAfterLast('.')
        is Resource -> resourceId
        is Offset -> "0x${address.toString(16)}"
        is Finding -> title
    }
}

enum class ComponentType { ACTIVITY, SERVICE, RECEIVER, PROVIDER }

// ═══════════════════════════════════════════════════════════════
// Workspace Tools (Phase H — Navigation)
// ═══════════════════════════════════════════════════════════════

enum class WorkspaceTool(val routeName: String, val displayName: String) {
    NONE("", ""),
    AI("Ai", "AI"),
    WORKSPACE("Workspace", "工作区"),
    SETTINGS("Settings", "设置"),
    MANIFEST("AxmlEditor", "Manifest"),
    DEX("DexBrowser", "DEX"),
    EDITOR("Editor", "编辑器"),
    HEX("HexEditor", "HEX"),
    ELF("ElfViewer", "ELF"),
    SQLITE("SqliteViewer", "SQLite"),
    RESOURCES("ArscViewer", "资源"),
    SECURITY("ApkSecurity", "安全"),
    SIGNING("ApkSigning", "签名"),
    REPACK("ApkRepack", "重打包"),
    REPORT("Report", "报告"),
    FILES("Files", "文件"),
    TERMINAL("Terminal", "终端"),
    TOOLS("Tools", "工具"),
    REMOTE("Remote", "远程"),
    DIFF("ApkDiff", "对比"),
    SIZE("ApkSizeAnalyzer", "体积");
}

// ═══════════════════════════════════════════════════════════════
// Breadcrumb (Phase E)
// ═══════════════════════════════════════════════════════════════

data class BreadcrumbItem(
    val label: String,
    val tool: WorkspaceTool,
    val selectionHint: String? = null
)

// ═══════════════════════════════════════════════════════════════
// Navigation State (Phase H)
// ═══════════════════════════════════════════════════════════════

data class NavigationState(
    val currentRoute: String = "",
    val previousRoute: String? = null,
    val canGoBack: Boolean = false
)

// ═══════════════════════════════════════════════════════════════
// History / Undo Tree (Phase I)
// ═══════════════════════════════════════════════════════════════

data class WorkspaceHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tool: WorkspaceTool,
    val action: String,
    val selection: WorkspaceSelection = WorkspaceSelection.None,
    val detail: String? = null,
    val isCheckpoint: Boolean = false  // Checkpoints can be restored to
)

data class PinnedItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val selection: WorkspaceSelection,
    val tool: WorkspaceTool,
    val timestamp: Long = System.currentTimeMillis()
)

data class RecentItem(
    val label: String,
    val selection: WorkspaceSelection,
    val tool: WorkspaceTool,
    val timestamp: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════
// Workspace Flags
// ═══════════════════════════════════════════════════════════════

data class WorkspaceFlags(
    val isDirty: Boolean = false,           // APK has been modified
    val isRepacked: Boolean = false,        // APK has been repacked
    val isSigned: Boolean = false,          // APK has been signed
    val needsSecurityRescan: Boolean = false, // Security needs re-scan
    val aiConfigured: Boolean = false       // AI provider configured
)

// ═══════════════════════════════════════════════════════════════
// Inspection Target (Phase D — Inspector sidebar)
// ═══════════════════════════════════════════════════════════════

data class InspectionTarget(
    val title: String = "",
    val subtitle: String = "",
    val details: Map<String, String> = emptyMap()
)

// ═══════════════════════════════════════════════════════════════
// Tool State — each tool's state stored in WorkspaceObject
// ═══════════════════════════════════════════════════════════════

data class ToolState(
    val scrollPosition: Int = 0,
    val expandedItems: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val customData: Map<String, String> = emptyMap()
)

// ═══════════════════════════════════════════════════════════════
// Timeline Entry
// ═══════════════════════════════════════════════════════════════

data class WorkspaceTimelineEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val title: String,
    val detail: String? = null,
    val tool: WorkspaceTool = WorkspaceTool.NONE,
    val selection: WorkspaceSelection = WorkspaceSelection.None
)

// ═══════════════════════════════════════════════════════════════
// Search State — global, shared across tools
// ═══════════════════════════════════════════════════════════════

data class SearchState(
    val query: String = "",
    val searchType: SearchType = SearchType.ALL,
    val results: List<SearchResult> = emptyList()
)

enum class SearchType {
    ALL, CLASS, METHOD, PERMISSION, RESOURCE, FILE, STRING, XML_NODE
}

data class SearchResult(
    val label: String,
    val tool: WorkspaceTool,
    val selection: WorkspaceSelection,
    val detail: String? = null
)
