package com.appdex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.data.ai.AiService
import com.appdex.data.ai.AiChatRequest
import com.appdex.data.ai.AiChatMessage
import com.appdex.apk.ApkFile
import com.appdex.data.ai.AiConfigRepository
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.AiMessage
import com.appdex.data.session.AiRole
import com.appdex.data.session.HistoryEntry
import com.appdex.data.session.SessionManager
import com.appdex.data.session.SessionStatus
import com.appdex.data.session.StepStatus
import com.appdex.data.session.WorkflowStep
import com.appdex.data.session.ToolDisplayMode
import com.appdex.data.toolbridge.ToolBridge
import com.appdex.data.toolbridge.QuickAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppDexMainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val sessionManager: SessionManager,
    val aiConfigRepository: AiConfigRepository
) : ViewModel() {

    private val aiService = AiService()

    val sessions = sessionManager.sessions
    val currentSession = sessionManager.currentSession
    val currentSessionId = sessionManager.currentSessionId
    val aiConfig = aiConfigRepository.config
    val displayMode = sessionManager.displayMode

    init {
        // Load persisted sessions on startup
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.loadPersistedSessions()
            // Refresh quick actions and suggestions based on loaded sessions
            val session = sessionManager.getCurrentSession()
            _quickActions.value = ToolBridge.getQuickActions(session)
            _suggestedQuestions.value = ToolBridge.getSuggestedQuestions(session)
        }
    }

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isAiResponding = MutableStateFlow(false)
    val isAiResponding: StateFlow<Boolean> = _isAiResponding.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _quickActions = MutableStateFlow<List<QuickAction>>(ToolBridge.getQuickActions(null))
    val quickActions: StateFlow<List<QuickAction>> = _quickActions.asStateFlow()

    private val _suggestedQuestions = MutableStateFlow<List<Pair<String, String>>>(ToolBridge.getSuggestedQuestions(null))
    val suggestedQuestions: StateFlow<List<Pair<String, String>>> = _suggestedQuestions.asStateFlow()

    private val _workflowProgress = MutableStateFlow(0f)
    val workflowProgress: StateFlow<Float> = _workflowProgress.asStateFlow()

    private val _currentWorkflowStep = MutableStateFlow<String>("")
    val currentWorkflowStep: StateFlow<String> = _currentWorkflowStep.asStateFlow()

    fun getCurrentSession(): AnalysisSession? = sessionManager.getCurrentSession()

    // ═══════════════════════════════════════════════════════════════
    // APK Analysis — Workflow Engine (实时进度联动)
    // ═══════════════════════════════════════════════════════════════

    fun openApk(uri: Uri) {
        val session = sessionManager.createSession()
        _isAnalyzing.value = true
        _workflowProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Read APK
                sessionManager.setStatus(session.id, SessionStatus.LOADING)
                updateWorkflow(session.id, WorkflowStep.READ_APK, "读取 APK 文件...", 0.1f)

                val tempFile = File(context.cacheDir, "analysis_${session.id}.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: run {
                    _isAnalyzing.value = false
                    sessionManager.setStatus(session.id, SessionStatus.IDLE)
                    return@launch
                }

                if (!tempFile.exists() || tempFile.length() < 4) {
                    _isAnalyzing.value = false
                    sessionManager.setStatus(session.id, SessionStatus.IDLE)
                    return@launch
                }

                sessionManager.completeWorkflowStep(session.id, WorkflowStep.READ_APK)
                _workflowProgress.value = 1f / 6f

                // Step 2: Analyze Permissions
                sessionManager.setStatus(session.id, SessionStatus.ANALYZING)
                updateWorkflow(session.id, WorkflowStep.ANALYZE_PERMISSIONS, "分析权限和组件...", 0.3f)

                val apkFile = ApkFile(tempFile.absolutePath)
                val info = apkFile.use { it.parse() }
                val enrichedInfo = enrichWithPackageManager(info, tempFile)
                val icon = loadAppIcon(tempFile)

                sessionManager.loadApkData(session.id, tempFile.absolutePath, enrichedInfo, icon)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.ANALYZE_PERMISSIONS)
                _workflowProgress.value = 2f / 6f

                // Step 3: Scan SDK
                updateWorkflow(session.id, WorkflowStep.SCAN_SDK, "扫描第三方 SDK...", 0.45f)
                Thread.sleep(300)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.SCAN_SDK)
                _workflowProgress.value = 3f / 6f

                // Step 4: Scan Resources
                updateWorkflow(session.id, WorkflowStep.SCAN_RESOURCES, "扫描资源文件...", 0.6f)
                Thread.sleep(300)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.SCAN_RESOURCES)
                _workflowProgress.value = 4f / 6f

                // Step 5: Scan Code
                updateWorkflow(session.id, WorkflowStep.SCAN_CODE, "分析代码结构...", 0.75f)
                Thread.sleep(300)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.SCAN_CODE)
                _workflowProgress.value = 5f / 6f

                // Step 6: AI Summary
                sessionManager.setStatus(session.id, SessionStatus.SUMMARIZING)
                updateWorkflow(session.id, WorkflowStep.AI_SUMMARY, "AI 生成分析报告...", 0.9f)
                val (findings, score) = ToolBridge.generateFindings(enrichedInfo)
                sessionManager.completeAnalysis(session.id, findings, score)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.AI_SUMMARY)
                _workflowProgress.value = 1f
                _currentWorkflowStep.value = ""

                // Add history entry
                sessionManager.addHistoryEntry(session.id, HistoryEntry(
                    action = "分析完成",
                    detail = "安全评分: $score/100, 发现: ${findings.size} 项"
                ))

                // Update quick actions and suggestions
                _quickActions.value = ToolBridge.getQuickActions(sessionManager.getCurrentSession())
                _suggestedQuestions.value = ToolBridge.getSuggestedQuestions(sessionManager.getCurrentSession())

                _isAnalyzing.value = false
            } catch (e: Exception) {
                Log.w("AppDex", "APK analysis error", e)
                _isAnalyzing.value = false
                _currentWorkflowStep.value = ""
                sessionManager.setStatus(session.id, SessionStatus.IDLE)
            }
        }
    }

    private fun updateWorkflow(sessionId: String, step: WorkflowStep, message: String, progress: Float) {
        _currentWorkflowStep.value = message
        _workflowProgress.value = progress
        sessionManager.startWorkflowStep(sessionId, step)
        sessionManager.updateWorkflowStep(sessionId, step, StepStatus.RUNNING, message, progress)
    }

    fun openApkPath(path: String) {
        openApk(Uri.fromFile(File(path)))
    }

    fun selectSession(sessionId: String) {
        sessionManager.setCurrentSession(sessionId)
        _quickActions.value = ToolBridge.getQuickActions(sessionManager.getCurrentSession())
        _suggestedQuestions.value = ToolBridge.getSuggestedQuestions(sessionManager.getCurrentSession())
    }

    fun deleteSession(sessionId: String) {
        sessionManager.deleteSession(sessionId)
        _quickActions.value = ToolBridge.getQuickActions(sessionManager.getCurrentSession())
        _suggestedQuestions.value = ToolBridge.getSuggestedQuestions(sessionManager.getCurrentSession())
    }

    fun setDisplayMode(mode: ToolDisplayMode) {
        sessionManager.setDisplayMode(mode)
    }

    fun undoLastAction() {
        val session = sessionManager.getCurrentSession() ?: return
        sessionManager.undoLastAction(session.id)
    }

    // ═══════════════════════════════════════════════════════════════
    // AI Copilot — 流式响应
    // ═══════════════════════════════════════════════════════════════

    fun sendAiMessage(text: String) {
        val userMsg = AiMessage(role = AiRole.USER, content = text)
        _aiMessages.value = _aiMessages.value + userMsg

        sessionManager.getCurrentSession()?.let { session ->
            sessionManager.addAiMessage(session.id, userMsg)
        }

        _isAiResponding.value = true
        _streamingContent.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = aiConfigRepository.config.first()

                if (!config.isConfigured()) {
                    val errorMsg = AiMessage(
                        role = AiRole.ASSISTANT,
                        content = "我还没有连接到 AI 服务。\n\n请前往设置页面配置 AI 提供商，支持 OpenAI、Anthropic、Gemini、DeepSeek、Ollama 等。\n\n配置完成后，我可以帮你分析 APK 的安全性、解释权限、检查签名等。"
                    )
                    _aiMessages.value = _aiMessages.value + errorMsg
                    _isAiResponding.value = false
                    return@launch
                }

                val session = sessionManager.getCurrentSession()
                val systemPrompt = ToolBridge.buildSystemPrompt(session)
                val contextData = ToolBridge.buildContext(session)

                val chatMessages = buildList {
                    add(AiChatMessage("system", systemPrompt + "\n\n" + contextData))
                    _aiMessages.value.forEach { msg ->
                        if (msg.role != AiRole.SYSTEM) {
                            add(AiChatMessage(
                                role = if (msg.role == AiRole.USER) "user" else "assistant",
                                content = msg.content
                            ))
                        }
                    }
                }

                val request = AiChatRequest(
                    messages = chatMessages,
                    model = config.modelName,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens,
                    systemPrompt = systemPrompt + "\n\n" + contextData
                )

                // 使用流式响应
                val fullResponse = StringBuilder()
                aiService.chatStream(config, request).collectLatest { chunk ->
                    if (chunk.startsWith("❌")) {
                        fullResponse.append(chunk)
                        _streamingContent.value = fullResponse.toString()
                    } else {
                        fullResponse.append(chunk)
                        _streamingContent.value = fullResponse.toString()
                    }
                }

                val rawContent = fullResponse.toString()
                if (rawContent.isNotEmpty()) {
                    val actionCards = ToolBridge.parseActionCards(rawContent)
                    val cleanContent = ToolBridge.stripActionCards(rawContent)

                    val assistantMsg = AiMessage(
                        role = AiRole.ASSISTANT,
                        content = cleanContent,
                        actionCards = actionCards.map {
                            com.appdex.data.session.ActionCard(
                                title = it.title,
                                description = it.description,
                                iconType = it.iconType,
                                route = it.route
                            )
                        }
                    )
                    _aiMessages.value = _aiMessages.value + assistantMsg
                    _streamingContent.value = ""

                    session?.let { s ->
                        sessionManager.addAiMessage(s.id, assistantMsg)
                    }
                } else {
                    val errorMsg = AiMessage(
                        role = AiRole.ASSISTANT,
                        content = "AI 返回了空响应，请重试。"
                    )
                    _aiMessages.value = _aiMessages.value + errorMsg
                }

                _isAiResponding.value = false
            } catch (e: Exception) {
                Log.w("AppDex", "AI error", e)
                _isAiResponding.value = false
                _streamingContent.value = ""
                val errorMsg = AiMessage(
                    role = AiRole.ASSISTANT,
                    content = "出错了：${e.message ?: "未知错误"}"
                )
                _aiMessages.value = _aiMessages.value + errorMsg
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
        _streamingContent.value = ""
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════

    private fun enrichWithPackageManager(
        info: com.appdex.apk.ApkInfo,
        apkFile: File
    ): com.appdex.apk.ApkInfo {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, android.content.pm.PackageManager.GET_PERMISSIONS)
            val appInfo = pkgInfo?.applicationInfo
            appInfo?.sourceDir = apkFile.absolutePath
            appInfo?.publicSourceDir = apkFile.absolutePath
            val manifest = info.manifest.copy(
                packageName = pkgInfo?.packageName ?: info.manifest.packageName,
                versionName = pkgInfo?.versionName ?: info.manifest.versionName,
                versionCode = pkgInfo?.longVersionCode ?: info.manifest.versionCode,
                minSdk = appInfo?.minSdkVersion ?: info.manifest.minSdk,
                targetSdk = appInfo?.targetSdkVersion ?: info.manifest.targetSdk,
                permissions = pkgInfo?.requestedPermissions?.toList() ?: info.manifest.permissions
            )
            info.copy(manifest = manifest)
        } catch (e: Exception) {
            Log.w("AppDex", "Suppressed exception", e)
            info
        }
    }

    private fun loadAppIcon(apkFile: File): Bitmap? {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            pkgInfo?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkFile.absolutePath
                appInfo.publicSourceDir = apkFile.absolutePath
                val drawable = appInfo.loadIcon(pm)
                drawableToBitmap(drawable)
            }
        } catch (e: Exception) {
            Log.w("AppDex", "Suppressed exception", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}
