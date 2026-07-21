package com.appdex.data.session

import android.graphics.Bitmap
import android.util.Log
import com.appdex.apk.ApkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// Session Lifecycle — APK Copilot Workflow
// ═══════════════════════════════════════════════════════════════

/**
 * APK 分析会话的生命周期状态
 * 
 * 流程: Idle → Loading → Analyzing → Summarizing → Ready → Modified → Repacked → Signed → Installed
 */
enum class SessionStatus {
    IDLE,           // 空闲 — 已创建但未开始
    LOADING,        // 读取 APK 文件中
    ANALYZING,      // 分析中 — 权限/SDK/资源/代码
    SUMMARIZING,    // AI 总结中
    READY,          // 报告就绪
    MODIFIED,       // APK 已被修改
    REPACKED,       // 已重新打包
    SIGNED,         // 已签名
    INSTALLED       // 已安装
}

/**
 * 分析 Workflow 步骤
 */
enum class WorkflowStep(val displayLabel: String, val friendlyName: String) {
    READ_APK("读取 APK", "读取应用文件"),
    ANALYZE_PERMISSIONS("分析权限", "检查应用权限"),
    SCAN_SDK("扫描 SDK", "检测第三方 SDK"),
    SCAN_RESOURCES("扫描资源", "检查应用资源"),
    SCAN_CODE("扫描代码", "分析代码结构"),
    AI_SUMMARY("AI 总结", "生成分析报告");

    fun next(): WorkflowStep? = when (this) {
        READ_APK -> ANALYZE_PERMISSIONS
        ANALYZE_PERMISSIONS -> SCAN_SDK
        SCAN_SDK -> SCAN_RESOURCES
        SCAN_RESOURCES -> SCAN_CODE
        SCAN_CODE -> AI_SUMMARY
        AI_SUMMARY -> null
    }

    companion object {
        fun fromIndex(index: Int): WorkflowStep? = entries.getOrNull(index)
    }
}

/**
 * 单个 Workflow 步骤的运行状态
 */
data class WorkflowStepState(
    val step: WorkflowStep,
    val status: StepStatus = StepStatus.PENDING,
    val progress: Float = 0f,
    val message: String = ""
)

enum class StepStatus { PENDING, RUNNING, DONE, ERROR }

/**
 * 工具显示模式
 */
enum class ToolDisplayMode {
    NORMAL,     // 普通模式 — 友好名称
    ADVANCED,   // 高级模式 — 双名称
    EXPERT      // 专家模式 — 原始名称
}

// ═══════════════════════════════════════════════════════════════
// Analysis Findings
// ═══════════════════════════════════════════════════════════════

data class AnalysisFinding(
    val id: String = UUID.randomUUID().toString(),
    val severity: FindingSeverity,
    val category: String,
    val title: String,
    val description: String,
    val recommendation: String? = null,
    val toolAction: String? = null
)

enum class FindingSeverity(val label: String, val color: String) {
    CRITICAL("严重", "red"),
    HIGH("高危", "orange"),
    MEDIUM("中等", "amber"),
    LOW("低", "blue"),
    INFO("信息", "green")
}

// ═══════════════════════════════════════════════════════════════
// AI Conversation
// ═══════════════════════════════════════════════════════════════

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AiRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionCards: List<ActionCard> = emptyList(),
    // Structured AI response fields (populated when AI follows structured format)
    val reason: String? = null,
    val risk: String? = null,
    val recommendation: String? = null,
    val technicalDetails: String? = null
)

enum class AiRole { USER, ASSISTANT, SYSTEM }

/**
 * AI 推荐 Action Card — 结构化操作面板
 */
data class ActionCard(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val iconType: String,
    val route: String,
    val routeParam: String? = null,
    val actionType: ActionType = ActionType.NAVIGATE
)

enum class ActionType {
    NAVIGATE,       // 跳转到工具页面
    EXECUTE,        // 执行某个操作
    INFO            // 仅展示信息
}

// ═══════════════════════════════════════════════════════════════
// Session History — 支持 Undo
// ═══════════════════════════════════════════════════════════════

/**
 * 历史记录条目 — 记录每次修改
 */
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,           // 动作描述："修改了 AndroidManifest.xml"
    val toolName: String? = null, // 使用的工具
    val detail: String? = null,   // 详细信息
    val snapshotPath: String? = null  // 文件快照路径（用于 Undo）
)

/**
 * 会话摘要 — 从 ApkInfo 提取的持久化摘要，用于 APP 重启后恢复工作区
 */
data class SessionSummary(
    val dexCount: Int = 0,
    val resourceCount: Int = 0,
    val nativeLibCount: Int = 0,
    val hasSignature: Boolean = false,
    val signatureVersion: Int = 0,
    val permissionCount: Int = 0,
    val dangerousPermissionCount: Int = 0,
    val activityCount: Int = 0,
    val serviceCount: Int = 0,
    val receiverCount: Int = 0,
    val providerCount: Int = 0,
    val findingCount: Int = 0
)

// ═══════════════════════════════════════════════════════════════
// Analysis Session
// ═══════════════════════════════════════════════════════════════

/**
 * APK 分析会话 — 整个 App 的核心数据模型
 */
data class AnalysisSession(
    val id: String = UUID.randomUUID().toString(),
    val apkFilePath: String? = null,
    val apkInfo: ApkInfo? = null,
    val appIcon: Bitmap? = null,
    val packageName: String = "",
    val versionName: String = "",
    val fileSize: Long = 0L,
    val status: SessionStatus = SessionStatus.IDLE,
    val workflowSteps: List<WorkflowStepState> = WorkflowStep.entries.map { WorkflowStepState(it) },
    val findings: List<AnalysisFinding> = emptyList(),
    val securityScore: Int = 100,
    val aiMessages: List<AiMessage> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // ── RC3: 持久化摘要 & 导航上下文 ──
    val summary: SessionSummary = SessionSummary(),
    val lastTool: String = "",
    val lastRoute: String = ""
) {
    val displayName: String
        get() = packageName.ifEmpty { apkFilePath?.substringAfterLast('/') ?: "未知应用" }

    val currentWorkflowStep: WorkflowStepState?
        get() = workflowSteps.firstOrNull { it.status == StepStatus.RUNNING }
            ?: workflowSteps.firstOrNull { it.status == StepStatus.PENDING }

    val workflowProgress: Float
        get() {
            val doneCount = workflowSteps.count { it.status == StepStatus.DONE }
            return doneCount.toFloat() / workflowSteps.size
        }
}

// ═══════════════════════════════════════════════════════════════
// Session Manager — 全局单例
// ═══════════════════════════════════════════════════════════════

/**
 * Session 管理器 — 管理所有 APK 分析会话
 * 
 * 职责:
 * - 创建/删除/切换 Session
 * - 跟踪 Workflow 步骤进度
 * - 管理 Session History（支持 Undo）
 * - 管理 Display Mode
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionRepository: SessionRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessions = MutableStateFlow<List<AnalysisSession>>(emptyList())
    val sessions: StateFlow<List<AnalysisSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentSession = MutableStateFlow<AnalysisSession?>(null)
    val currentSession: StateFlow<AnalysisSession?> = _currentSession.asStateFlow()

    private val _displayMode = MutableStateFlow(ToolDisplayMode.NORMAL)
    val displayMode: StateFlow<ToolDisplayMode> = _displayMode.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // RC4: Workspace Context — Live tracking of user activity
    // AI uses this to know what the user just did.
    // ═══════════════════════════════════════════════════════════════

    data class LiveWorkspaceContext(
        val activePanel: String = "",
        val activeFilePath: String? = null,
        val activeSelection: String? = null,
        val recentActions: List<String> = emptyList(),
        val timeline: List<WorkspaceTimelineEvent> = emptyList()
    )

    data class WorkspaceTimelineEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val type: String,
        val title: String,
        val detail: String? = null
    )

    private val _workspaceContexts = MutableStateFlow<Map<String, LiveWorkspaceContext>>(emptyMap())
    val workspaceContexts: StateFlow<Map<String, LiveWorkspaceContext>> = _workspaceContexts.asStateFlow()

    /**
     * Report user activity — called by tool screens when user navigates, selects, or acts.
     * This feeds into AI context so the Copilot knows what the user just did.
     */
    fun reportWorkspaceAction(
        sessionId: String? = _currentSessionId.value,
        panel: String? = null,
        filePath: String? = null,
        selection: String? = null,
        action: String? = null,
        timelineType: String? = null,
        timelineTitle: String? = null,
        timelineDetail: String? = null
    ) {
        if (sessionId == null) return
        _workspaceContexts.update { contexts ->
            val current = contexts[sessionId] ?: LiveWorkspaceContext()
            val updated = current.copy(
                activePanel = panel ?: current.activePanel,
                activeFilePath = filePath ?: current.activeFilePath,
                activeSelection = selection ?: current.activeSelection,
                recentActions = (if (action != null) listOf(action) + current.recentActions else current.recentActions).take(10),
                timeline = if (timelineType != null && timelineTitle != null) {
                    (current.timeline + WorkspaceTimelineEvent(
                        type = timelineType,
                        title = timelineTitle,
                        detail = timelineDetail
                    )).takeLast(20)
                } else current.timeline
            )
            contexts + (sessionId to updated)
        }
    }

    /**
     * Get the live workspace context for a session (for AI context building).
     */
    fun getWorkspaceContext(sessionId: String? = _currentSessionId.value): LiveWorkspaceContext {
        return _workspaceContexts.value[sessionId] ?: LiveWorkspaceContext()
    }

    /**
     * Update last tool and route when user navigates.
     */
    fun updateNavigationContext(tool: String, route: String) {
        _currentSessionId.value?.let { id ->
            updateSessionInternal(id) { it.copy(lastTool = tool, lastRoute = route) }
            reportWorkspaceAction(sessionId = id, panel = tool, action = "导航到 $tool")
        }
    }

    // ── Persistence ──

    /**
     * 从 DataStore 加载已持久化的会话
     */
    suspend fun loadPersistedSessions() {
        try {
            val metadataList = sessionRepository.loadSessions()
            if (metadataList.isNotEmpty()) {
                val restored = metadataList.map { it.toSession() }
                _sessions.value = restored
                val lastSession = restored.lastOrNull()
                if (lastSession != null) {
                    _currentSessionId.value = lastSession.id
                    _currentSession.value = lastSession
                }
                Log.i("SessionManager", "Loaded ${restored.size} persisted sessions")
            }
        } catch (e: Exception) {
            Log.w("SessionManager", "Failed to load persisted sessions", e)
        }
    }

    /**
     * 持久化当前所有会话
     */
    private fun persistSessions() {
        scope.launch {
            try {
                val metadataList = _sessions.value.map { it.toMetadata() }
                sessionRepository.saveSessions(metadataList)
            } catch (e: Exception) {
                Log.w("SessionManager", "Failed to persist sessions", e)
            }
        }
    }

    /**
     * 持久化单个会话
     */
    private fun persistSession(session: AnalysisSession) {
        scope.launch {
            try {
                sessionRepository.saveSession(session.toMetadata())
            } catch (e: Exception) {
                Log.w("SessionManager", "Failed to persist session", e)
            }
        }
    }

    /**
     * 从 DataStore 删除会话
     */
    private fun persistDeleteSession(sessionId: String) {
        scope.launch {
            try {
                sessionRepository.deleteSession(sessionId)
            } catch (e: Exception) {
                Log.w("SessionManager", "Failed to delete persisted session", e)
            }
        }
    }

    // ── Display Mode ──

    fun setDisplayMode(mode: ToolDisplayMode) {
        _displayMode.value = mode
    }

    // ── Session CRUD ──

    fun createSession(): AnalysisSession {
        val session = AnalysisSession()
        _sessions.update { it + session }
        _currentSessionId.value = session.id
        _currentSession.value = session
        persistSession(session)
        return session
    }

    fun setCurrentSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _currentSession.value = _sessions.value.find { it.id == sessionId }
    }

    fun getCurrentSession(): AnalysisSession? {
        val id = _currentSessionId.value ?: return null
        return _sessions.value.find { it.id == id }
    }

    private fun updateSessionInternal(sessionId: String, updater: (AnalysisSession) -> AnalysisSession) {
        _sessions.update { sessions ->
            sessions.map { if (it.id == sessionId) updater(it) else it }
        }
        val updated = _sessions.value.find { it.id == sessionId }
        if (_currentSessionId.value == sessionId) {
            _currentSession.value = updated
        }
        // Persist if this is a meaningful update (not just progress)
        if (updated != null) {
            persistSession(updated)
        }
    }

    fun updateSession(sessionId: String, updater: (AnalysisSession) -> AnalysisSession) {
        updateSessionInternal(sessionId, updater)
    }

    fun updateCurrentSession(updater: (AnalysisSession) -> AnalysisSession) {
        _currentSessionId.value?.let { id -> updateSessionInternal(id, updater) }
    }

    fun deleteSession(sessionId: String) {
        _sessions.update { it.filter { s -> s.id != sessionId } }
        if (_currentSessionId.value == sessionId) {
            val next = _sessions.value.firstOrNull()
            _currentSessionId.value = next?.id
            _currentSession.value = next
        }
        persistDeleteSession(sessionId)
    }

    // ── Workflow Management ──

    fun updateWorkflowStep(sessionId: String, step: WorkflowStep, status: StepStatus, message: String = "", progress: Float = 0f) {
        updateSessionInternal(sessionId) { session ->
            session.copy(
                workflowSteps = session.workflowSteps.map { ws ->
                    if (ws.step == step) ws.copy(status = status, message = message, progress = progress)
                    else ws
                },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun completeWorkflowStep(sessionId: String, step: WorkflowStep) {
        updateSessionInternal(sessionId) { session ->
            session.copy(
                workflowSteps = session.workflowSteps.map { ws ->
                    if (ws.step == step) ws.copy(status = StepStatus.DONE, progress = 1f)
                    else ws
                },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun startWorkflowStep(sessionId: String, step: WorkflowStep) {
        updateSessionInternal(sessionId) { session ->
            session.copy(
                workflowSteps = session.workflowSteps.map { ws ->
                    if (ws.step == step) ws.copy(status = StepStatus.RUNNING, progress = 0f)
                    else ws
                },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // ── AI Messages ──

    fun addAiMessage(sessionId: String, message: AiMessage) {
        updateSessionInternal(sessionId) {
            it.copy(aiMessages = it.aiMessages + message, updatedAt = System.currentTimeMillis())
        }
        // RC4: Record workspace timeline — AI chat
        if (message.role == AiRole.USER) {
            reportWorkspaceAction(
                sessionId = sessionId,
                panel = "AI",
                action = "向 AI 提问: ${message.content.take(50)}",
                timelineType = "AI_CHAT",
                timelineTitle = "AI 对话",
                timelineDetail = message.content.take(80)
            )
        }
    }

    // ── Findings ──

    fun setFindings(sessionId: String, findings: List<AnalysisFinding>, score: Int) {
        updateSessionInternal(sessionId) {
            it.copy(findings = findings, securityScore = score, status = SessionStatus.READY, updatedAt = System.currentTimeMillis())
        }
    }

    fun completeAnalysis(sessionId: String, findings: List<AnalysisFinding>, score: Int) {
        updateSessionInternal(sessionId) {
            it.copy(
                findings = findings,
                securityScore = score,
                status = SessionStatus.READY,
                workflowSteps = it.workflowSteps.map { ws -> ws.copy(status = StepStatus.DONE, progress = 1f) },
                summary = it.summary.copy(findingCount = findings.size),
                updatedAt = System.currentTimeMillis()
            )
        }
        // RC4: Record workspace timeline — Analysis complete
        reportWorkspaceAction(
            sessionId = sessionId,
            action = "分析完成: 评分 $score/100",
            timelineType = "ANALYSIS",
            timelineTitle = "分析完成",
            timelineDetail = "安全评分: $score/100, 发现: ${findings.size} 项"
        )
    }

    // ── Status ──

    fun setStatus(sessionId: String, status: SessionStatus) {
        updateSessionInternal(sessionId) { it.copy(status = status, updatedAt = System.currentTimeMillis()) }
    }

    fun loadApkData(
        sessionId: String,
        apkFilePath: String,
        apkInfo: ApkInfo,
        appIcon: Bitmap?
    ) {
        // 构建 SessionSummary — 持久化后重启可恢复完整工作区
        val summary = SessionSummary(
            dexCount = apkInfo.entries.count { it.name.endsWith(".dex") },
            resourceCount = apkInfo.entries.count { it.name.endsWith(".xml") || it.name.endsWith(".png") || it.name.endsWith(".arsc") },
            nativeLibCount = apkInfo.entries.count { it.name.endsWith(".so") },
            hasSignature = apkInfo.signatures.isNotEmpty(),
            signatureVersion = apkInfo.signatures.maxOfOrNull { it.version } ?: 0,
            permissionCount = apkInfo.manifest.permissions.size,
            dangerousPermissionCount = apkInfo.manifest.permissions.count { it in DANGEROUS_PERMISSIONS_SESSION },
            activityCount = apkInfo.manifest.activities.size,
            serviceCount = apkInfo.manifest.services.size,
            receiverCount = apkInfo.manifest.receivers.size,
            providerCount = apkInfo.manifest.providers.size,
            findingCount = 0
        )
        updateSessionInternal(sessionId) {
            it.copy(
                apkFilePath = apkFilePath,
                apkInfo = apkInfo,
                appIcon = appIcon,
                packageName = apkInfo.manifest.packageName,
                versionName = apkInfo.manifest.versionName,
                fileSize = apkInfo.fileSize,
                status = SessionStatus.ANALYZING,
                summary = summary,
                updatedAt = System.currentTimeMillis()
            )
        }
        // RC4: Record workspace timeline — APK opened
        reportWorkspaceAction(
            sessionId = sessionId,
            panel = "Workspace",
            action = "导入 APK: ${apkInfo.manifest.packageName}",
            timelineType = "IMPORT",
            timelineTitle = "导入 APK",
            timelineDetail = apkInfo.manifest.packageName
        )
    }

    // 危险权限列表（用于会话摘要构建）
    private val DANGEROUS_PERMISSIONS_SESSION = setOf(
        "android.permission.READ_SMS", "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS", "android.permission.RECEIVE_MMS",
        "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
        "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.WRITE_SETTINGS", "android.permission.REBOOT",
        "android.permission.INSTALL_PACKAGES", "android.permission.DELETE_PACKAGES"
    )

    // ── History & Undo ──

    fun addHistoryEntry(sessionId: String, entry: HistoryEntry) {
        updateSessionInternal(sessionId) {
            it.copy(history = it.history + entry, updatedAt = System.currentTimeMillis())
        }
    }

    fun undoLastAction(sessionId: String): HistoryEntry? {
        val session = _sessions.value.find { it.id == sessionId } ?: return null
        val lastEntry = session.history.lastOrNull() ?: return null
        updateSessionInternal(sessionId) {
            it.copy(history = it.history.dropLast(1), updatedAt = System.currentTimeMillis())
        }
        return lastEntry
    }

    fun clearHistory(sessionId: String) {
        updateSessionInternal(sessionId) {
            it.copy(history = emptyList(), updatedAt = System.currentTimeMillis())
        }
    }
}
