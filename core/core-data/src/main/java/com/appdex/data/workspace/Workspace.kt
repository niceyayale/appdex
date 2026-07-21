package com.appdex.data.workspace

import android.graphics.Bitmap
import com.appdex.apk.ApkInfo
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.AiMessage
import com.appdex.data.session.AnalysisFinding
import com.appdex.data.session.HistoryEntry
import com.appdex.data.session.SessionStatus
import com.appdex.data.session.WorkflowStepState
import com.appdex.data.session.ToolDisplayMode
import java.util.UUID

// ═══════════════════════════════════════════════════════════════
// AppX Workspace — The Core Object
// ═══════════════════════════════════════════════════════════════
// Everything revolves around Workspace.
// Every APK creates a Workspace.
// Everything belongs inside the Workspace.
// ═══════════════════════════════════════════════════════════════

/**
 * Workspace lifecycle states — more granular than SessionStatus
 *
 * Lifecycle: Import → Analyzing → Ready → Modifying → Rebuilding → Signing → Installing → Archived
 * Every state is resumable, undoable, replayable, recoverable.
 */
enum class WorkspaceState {
    EMPTY,          // No APK loaded yet
    IMPORTING,      // APK file being imported
    ANALYZING,      // Auto-analysis pipeline running
    READY,          // Analysis complete, workspace ready
    MODIFYING,      // User is editing/modifying files
    REBUILDING,     // APK being repacked
    SIGNING,        // APK being signed
    INSTALLING,     // APK being installed
    ARCHIVED,       // Workspace archived
    ERROR           // Error state
}

/**
 * The type of panel currently active in the workspace.
 * This drives the workspace UI layout.
 */
enum class WorkspacePanel {
    OVERVIEW,       // Dashboard — summary, score, quick actions
    MANIFEST,       // AndroidManifest.xml viewer/editor
    DEX,            // DEX browser
    RESOURCES,      // Resource viewer (arsc, images, strings)
    SECURITY,       // Security scanner results
    SIGNING,        // Signing tools
    REPACK,         // Repack tools
    SQLITE,         // SQLite viewer
    NATIVE,         // Native library (ELF) viewer
    FILES,          // File browser
    AI,             // AI conversation
    HISTORY,        // Timeline / history
    REPORT,         // Executive report
    DIFF            // APK comparison
}

/**
 * A user goal — what the user wants to accomplish.
 * This is the core of the Goal-Driven experience.
 */
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,              // "Analyze this APK"
    val description: String,        // "Understand what this app does"
    val iconType: String,           // icon identifier
    val category: GoalCategory,
    val suggestedWorkflow: List<String> = emptyList(),  // suggested workflow steps
    val timestamp: Long = System.currentTimeMillis()
)

enum class GoalCategory {
    ANALYZE,        // "Analyze this APK"
    UNDERSTAND,     // "Understand this application"
    SECURITY,       // "Check if it is safe"
    PERMISSIONS,    // "Inspect permissions"
    SDK,            // "Inspect SDKs"
    LOGIN,          // "Locate login logic"
    NETWORK,        // "Inspect network requests"
    MODIFY,         // "Change application name", "Replace icon"
    COMPARE,        // "Compare two APKs"
    LEARN,          // "Learn reverse engineering"
    RECOVER,        // "Recover deleted files"
    STRUCTURE       // "View application structure"
}

/**
 * The context of what the user is currently doing.
 * AI uses this to understand the full context.
 */
data class WorkspaceContext(
    val activePanel: WorkspacePanel = WorkspacePanel.OVERVIEW,
    val activeFilePath: String? = null,
    val activeFileContent: String? = null,
    val activeSelection: String? = null,
    val activeGoal: Goal? = null,
    val recentActions: List<String> = emptyList()
) {
    /**
     * Build a human-readable context summary for AI.
     */
    fun toContextString(): String = buildString {
        appendLine("Current Panel: ${activePanel.name}")
        activeFilePath?.let { appendLine("Current File: $it") }
        activeGoal?.let { appendLine("Current Goal: ${it.title}") }
        if (recentActions.isNotEmpty()) {
            appendLine("Recent Actions: ${recentActions.joinToString(", ")}")
        }
    }
}

/**
 * Workspace — the central object that wraps everything.
 *
 * Design principle: Wrap existing AnalysisSession, don't replace it.
 * The Session engine continues to work as-is.
 * Workspace adds: goals, context, panels, enhanced timeline.
 */
data class Workspace(
    val id: String = UUID.randomUUID().toString(),
    val session: AnalysisSession = AnalysisSession(),
    val state: WorkspaceState = WorkspaceState.EMPTY,
    val context: WorkspaceContext = WorkspaceContext(),
    val goals: List<Goal> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val lastOpenedAt: Long = System.currentTimeMillis()
) {
    // ── Delegated properties from session (backward compatibility) ──
    val apkFilePath: String? get() = session.apkFilePath
    val apkInfo: ApkInfo? get() = session.apkInfo
    val appIcon: Bitmap? get() = session.appIcon
    val packageName: String get() = session.packageName
    val versionName: String get() = session.versionName
    val fileSize: Long get() = session.fileSize
    val status: SessionStatus get() = session.status
    val findings: List<AnalysisFinding> get() = session.findings
    val securityScore: Int get() = session.securityScore
    val aiMessages: List<AiMessage> get() = session.aiMessages
    val history: List<HistoryEntry> get() = session.history
    val createdAt: Long get() = session.createdAt

    val displayName: String
        get() = packageName.ifEmpty {
            apkFilePath?.substringAfterLast('/') ?: "未知应用"
        }

    val isReady: Boolean
        get() = state == WorkspaceState.READY || status == SessionStatus.READY

    val hasApk: Boolean
        get() = apkFilePath != null && session.status != SessionStatus.IDLE

    /**
     * Convert to a lightweight metadata for persistence.
     */
    fun toWorkspaceMetadata(): WorkspaceMetadata = WorkspaceMetadata(
        id = id,
        sessionId = session.id,
        state = state.name,
        activePanel = context.activePanel.name,
        tags = tags,
        notes = notes,
        lastOpenedAt = lastOpenedAt
    )
}

/**
 * Timeline event — enhanced history with categories.
 * Every action creates a timeline event.
 */
data class TimelineEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: TimelineEventType,
    val title: String,
    val description: String? = null,
    val iconType: String? = null,
    val sessionId: String? = null
)

enum class TimelineEventType {
    ANALYSIS,       // APK analyzed
    MODIFICATION,   // File modified
    PATCH,          // Patch applied
    BUILD,          // APK rebuilt
    SIGN,           // APK signed
    INSTALL,        // APK installed
    UNDO,           // Action undone
    AI_CHAT,        // AI conversation
    GOAL_SET,       // Goal set
    GOAL_COMPLETED, // Goal completed
    NOTE,           // Note added
    EXPORT          // Report exported
}

/**
 * Persistable workspace metadata (lightweight).
 */
data class WorkspaceMetadata(
    val id: String,
    val sessionId: String,
    val state: String = "EMPTY",
    val activePanel: String = "OVERVIEW",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val lastOpenedAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════
// Goal Templates — Pre-defined goals for the Goal-Driven home screen
// ═══════════════════════════════════════════════════════════════

/**
 * Pre-defined goal templates shown on the home screen.
 * "What would you like to do today?"
 */
object GoalTemplates {
    val all: List<Goal> = listOf(
        Goal(
            title = "分析这个 APK",
            description = "全面分析应用的结构、权限、SDK 和安全性",
            iconType = "analyze",
            category = GoalCategory.ANALYZE,
            suggestedWorkflow = listOf("parse", "scan_manifest", "scan_sdk", "security_check", "ai_summary")
        ),
        Goal(
            title = "了解这个应用",
            description = "用通俗语言解释这个应用做什么",
            iconType = "understand",
            category = GoalCategory.UNDERSTAND,
            suggestedWorkflow = listOf("parse", "ai_summary")
        ),
        Goal(
            title = "检查是否安全",
            description = "安全扫描，检查危险权限和追踪 SDK",
            iconType = "security",
            category = GoalCategory.SECURITY,
            suggestedWorkflow = listOf("parse", "security_check", "ai_summary")
        ),
        Goal(
            title = "检查权限",
            description = "查看应用请求了哪些权限",
            iconType = "permissions",
            category = GoalCategory.PERMISSIONS,
            suggestedWorkflow = listOf("parse", "scan_manifest")
        ),
        Goal(
            title = "检查 SDK",
            description = "检测应用集成了哪些第三方 SDK",
            iconType = "sdk",
            category = GoalCategory.SDK,
            suggestedWorkflow = listOf("parse", "scan_sdk")
        ),
        Goal(
            title = "定位登录逻辑",
            description = "在代码中搜索登录/认证相关逻辑",
            iconType = "login",
            category = GoalCategory.LOGIN,
            suggestedWorkflow = listOf("parse", "scan_dex", "search_keywords")
        ),
        Goal(
            title = "检查网络请求",
            description = "分析应用与哪些服务器通信",
            iconType = "network",
            category = GoalCategory.NETWORK,
            suggestedWorkflow = listOf("parse", "scan_dex", "extract_urls")
        ),
        Goal(
            title = "修改应用名称",
            description = "在资源中找到并修改应用名称",
            iconType = "modify",
            category = GoalCategory.MODIFY,
            suggestedWorkflow = listOf("parse", "scan_resources", "edit_resource")
        ),
        Goal(
            title = "替换应用图标",
            description = "替换 APK 中的应用图标",
            iconType = "icon",
            category = GoalCategory.MODIFY,
            suggestedWorkflow = listOf("parse", "scan_resources", "replace_icon")
        ),
        Goal(
            title = "比较两个 APK",
            description = "对比两个 APK 版本的差异",
            iconType = "compare",
            category = GoalCategory.COMPARE,
            suggestedWorkflow = listOf("parse", "diff")
        ),
        Goal(
            title = "查看应用结构",
            description = "浏览 APK 的文件结构",
            iconType = "structure",
            category = GoalCategory.STRUCTURE,
            suggestedWorkflow = listOf("parse", "browse_files")
        ),
        Goal(
            title = "学习逆向工程",
            description = "通过分析这个 APK 学习逆向基础知识",
            iconType = "learn",
            category = GoalCategory.LEARN,
            suggestedWorkflow = listOf("parse", "ai_explain")
        )
    )
}
