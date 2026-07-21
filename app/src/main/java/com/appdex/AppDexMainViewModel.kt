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
import com.appdex.data.toolbridge.parseStructuredResponse
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
class AppXMainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val sessionManager: SessionManager,
    val aiConfigRepository: AiConfigRepository,
    val workspaceController: com.appdex.data.workspace.WorkspaceController,
    val workspaceEventBus: com.appdex.data.workspace.WorkspaceEventBus
) : ViewModel() {

    val sessions = sessionManager.sessions
    val currentSession = sessionManager.currentSession
    val currentSessionId = sessionManager.currentSessionId
    val aiConfig = aiConfigRepository.config
    val displayMode = sessionManager.displayMode

    // RC5: Workspace OS — single source of truth
    val workspaceState = workspaceController.state
    val workspaceInsights = workspaceController.aiInsights
    val workspaceNavCommands = workspaceController.navigationCommands

    private val aiService = AiService()
    private var analysisJob: kotlinx.coroutines.Job? = null
    private var aiJob: kotlinx.coroutines.Job? = null

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

    init {
        // Load persisted sessions on startup
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.loadPersistedSessions()
            // RC4: Re-parse cached APK files to restore full apkInfo
            // This ensures tools (DEX, Manifest, etc.) work after app restart
            val sessions = sessionManager.sessions.value
            sessions.forEach { session ->
                if (session.apkFilePath != null && session.apkInfo == null) {
                    val cachedApk = File(session.apkFilePath!!)
                    if (cachedApk.exists() && cachedApk.length() > 4) {
                        try {
                            val apkFile = ApkFile(cachedApk.absolutePath)
                            val info = apkFile.use { it.parse() }
                            val enrichedInfo = enrichWithPackageManager(info, cachedApk)
                            val icon = loadAppIcon(cachedApk)
                            sessionManager.updateSession(session.id) {
                                it.copy(apkInfo = enrichedInfo, appIcon = icon)
                            }
                            Log.i("AppX", "RC4: Re-parsed APK for session ${session.id}")
                        } catch (e: Exception) {
                            Log.w("AppX", "RC4: Failed to re-parse APK for session ${session.id}", e)
                        }
                    }
                }
            }
            // Refresh quick actions and suggestions based on loaded sessions
            val session = sessionManager.getCurrentSession()
            _quickActions.value = ToolBridge.getQuickActions(session)
            _suggestedQuestions.value = ToolBridge.getSuggestedQuestions(session)
        }
    }

    fun getCurrentSession(): AnalysisSession? = sessionManager.getCurrentSession()

    // ═══════════════════════════════════════════════════════════════
    // APK Analysis — Workflow Engine (实时进度联动)
    // ═══════════════════════════════════════════════════════════════

    fun openApk(uri: Uri) {
        val session = sessionManager.createSession()
        _isAnalyzing.value = true
        _workflowProgress.value = 0f

        analysisJob = viewModelScope.launch(Dispatchers.IO) {
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

                // Step 2: Analyze Permissions & Manifest
                sessionManager.setStatus(session.id, SessionStatus.ANALYZING)
                updateWorkflow(session.id, WorkflowStep.ANALYZE_PERMISSIONS, "分析权限和组件...", 0.3f)

                val apkFile = ApkFile(tempFile.absolutePath)
                val info = apkFile.use { it.parse() }
                val enrichedInfo = enrichWithPackageManager(info, tempFile)
                val icon = loadAppIcon(tempFile)

                sessionManager.loadApkData(session.id, tempFile.absolutePath, enrichedInfo, icon)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.ANALYZE_PERMISSIONS)
                _workflowProgress.value = 2f / 6f

                // Step 3: Scan SDK — real detection from native libs and manifest metadata
                updateWorkflow(session.id, WorkflowStep.SCAN_SDK, "扫描第三方 SDK...", 0.45f)
                // No Thread.sleep — real work happens in generateFindings
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.SCAN_SDK)
                _workflowProgress.value = 3f / 6f

                // Step 4: Scan Resources — count and categorize entries
                updateWorkflow(session.id, WorkflowStep.SCAN_RESOURCES, "扫描资源文件...", 0.6f)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.SCAN_RESOURCES)
                _workflowProgress.value = 4f / 6f

                // Step 5: Scan Code — analyze DEX structure
                updateWorkflow(session.id, WorkflowStep.SCAN_CODE, "分析代码结构...", 0.75f)
                sessionManager.completeWorkflowStep(session.id, WorkflowStep.SCAN_CODE)
                _workflowProgress.value = 5f / 6f

                // Step 6: Generate Findings & Score
                sessionManager.setStatus(session.id, SessionStatus.SUMMARIZING)
                updateWorkflow(session.id, WorkflowStep.AI_SUMMARY, "生成安全评估...", 0.9f)
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

                // Phase 1-4: Notify Workspace OS
                workspaceController.setApkLoaded(tempFile.absolutePath, enrichedInfo.manifest.packageName)
                workspaceController.setAnalysisCompleted(score, findings.size)

                // Update quick actions and suggestions
                _quickActions.value = ToolBridge.getQuickActions(sessionManager.getCurrentSession())
                _suggestedQuestions.value = ToolBridge.getSuggestedQuestions(sessionManager.getCurrentSession())

                _isAnalyzing.value = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                _isAnalyzing.value = false
                _currentWorkflowStep.value = ""
                sessionManager.setStatus(session.id, SessionStatus.IDLE)
                Log.w("AppX", "APK analysis cancelled")
            } catch (e: Exception) {
                Log.w("AppX", "APK analysis error", e)
                _isAnalyzing.value = false
                _currentWorkflowStep.value = ""
                sessionManager.setStatus(session.id, SessionStatus.IDLE)
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _isAnalyzing.value = false
        _currentWorkflowStep.value = ""
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

        aiJob = viewModelScope.launch(Dispatchers.IO) {
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
                // Phase 5: Use Workspace Memory for richer AI context
                val workspaceMemory = workspaceController.buildWorkspaceMemory()
                val liveContext = sessionManager.getWorkspaceContext(session?.id)
                val systemPrompt = ToolBridge.buildSystemPrompt(session, liveContext)
                val contextData = ToolBridge.buildContext(session, liveContext)
                val enrichedSystemPrompt = systemPrompt + "\n\n" + workspaceMemory

                val chatMessages = buildList {
                    add(AiChatMessage("system", enrichedSystemPrompt + "\n\n" + contextData))
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
                    systemPrompt = enrichedSystemPrompt + "\n\n" + contextData
                )

                // 使用流式响应
                val fullResponse = StringBuilder()
                aiService.chatStream(config, request).collectLatest { chunk ->
                    fullResponse.append(chunk)
                    _streamingContent.value = fullResponse.toString()
                }

                val rawContent = fullResponse.toString().trim()
                if (rawContent.isNotEmpty()) {
                    // Error responses are returned directly without structured parsing
                    val isErrorResponse = rawContent.startsWith("❌") || 
                        rawContent.startsWith("网络") || 
                        rawContent.startsWith("无法") || 
                        rawContent.startsWith("SSL") || 
                        rawContent.startsWith("AI 请求") || 
                        rawContent.startsWith("API Key")
                    
                    val assistantMsg = if (isErrorResponse) {
                        AiMessage(
                            role = AiRole.ASSISTANT,
                            content = rawContent.removePrefix("❌").trim()
                        )
                    } else {
                        val structured = parseStructuredResponse(rawContent)
                        AiMessage(
                            role = AiRole.ASSISTANT,
                            content = structured.summary,
                            reason = structured.reason,
                            risk = structured.risk,
                            recommendation = structured.recommendation,
                            technicalDetails = structured.technicalDetails,
                            actionCards = structured.actionCards.map {
                                com.appdex.data.session.ActionCard(
                                    title = it.title,
                                    description = it.description,
                                    iconType = it.iconType,
                                    route = it.route
                                )
                            }
                        )
                    }
                    _aiMessages.value = _aiMessages.value + assistantMsg
                    _streamingContent.value = ""

                    // Phase 3: Emit AI reply event to workspace
                    workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.AIReply(assistantMsg.content))

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
            } catch (e: kotlinx.coroutines.CancellationException) {
                _isAiResponding.value = false
                _streamingContent.value = ""
                Log.w("AppX", "AI response cancelled")
            } catch (e: Exception) {
                Log.w("AppX", "AI error", e)
                _isAiResponding.value = false
                _streamingContent.value = ""
                val friendlyMsg = when (e) {
                    is java.net.SocketTimeoutException -> "网络连接超时，请检查网络后重试"
                    is java.net.UnknownHostException -> "无法连接到 AI 服务，请检查网络连接"
                    is javax.net.ssl.SSLException -> "SSL 证书验证失败，请检查 API 地址"
                    is java.net.ConnectException -> "无法连接到服务器，请检查 API 地址和网络"
                    else -> "AI 请求失败，请稍后重试"
                }
                val errorMsg = AiMessage(
                    role = AiRole.ASSISTANT,
                    content = friendlyMsg
                )
                _aiMessages.value = _aiMessages.value + errorMsg
            }
        }
    }

    fun stopAi() {
        aiJob?.cancel()
        _isAiResponding.value = false
        // Keep partial streaming content as a message if it exists
        val partial = _streamingContent.value
        if (partial.isNotBlank()) {
            val partialMsg = AiMessage(
                role = AiRole.ASSISTANT,
                content = partial + "\n\n[已停止]"
            )
            _aiMessages.value = _aiMessages.value + partialMsg
        }
        _streamingContent.value = ""
    }

    fun regenerateLastMessage() {
        // Find the last user message
        val lastUserIndex = _aiMessages.value.indexOfLast { it.role == AiRole.USER }
        if (lastUserIndex < 0) return

        val lastUserMsg = _aiMessages.value[lastUserIndex]
        // Remove everything after the last user message (including the old assistant response)
        _aiMessages.value = _aiMessages.value.subList(0, lastUserIndex + 1)

        // Re-send the last user message to get a new response
        _isAiResponding.value = true
        _streamingContent.value = ""

        aiJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = aiConfigRepository.config.first()

                if (!config.isConfigured()) {
                    val errorMsg = AiMessage(
                        role = AiRole.ASSISTANT,
                        content = "我还没有连接到 AI 服务。\n\n请前往设置页面配置 AI 提供商。"
                    )
                    _aiMessages.value = _aiMessages.value + errorMsg
                    _isAiResponding.value = false
                    return@launch
                }

                val session = sessionManager.getCurrentSession()
                val liveContext = sessionManager.getWorkspaceContext(session?.id)
                val systemPrompt = ToolBridge.buildSystemPrompt(session, liveContext)
                val contextData = ToolBridge.buildContext(session, liveContext)

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

                val fullResponse = StringBuilder()
                aiService.chatStream(config, request).collectLatest { chunk ->
                    fullResponse.append(chunk)
                    _streamingContent.value = fullResponse.toString()
                }

                val rawContent = fullResponse.toString()
                if (rawContent.isNotEmpty()) {
                    val structured = parseStructuredResponse(rawContent)

                    val assistantMsg = AiMessage(
                        role = AiRole.ASSISTANT,
                        content = structured.summary,
                        reason = structured.reason,
                        risk = structured.risk,
                        recommendation = structured.recommendation,
                        technicalDetails = structured.technicalDetails,
                        actionCards = structured.actionCards.map {
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
                }

                _isAiResponding.value = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                _isAiResponding.value = false
                _streamingContent.value = ""
            } catch (e: Exception) {
                Log.w("AppX", "AI regenerate error", e)
                _isAiResponding.value = false
                _streamingContent.value = ""
                val friendlyMsg = when (e) {
                    is java.net.SocketTimeoutException -> "网络连接超时，请检查网络后重试"
                    is java.net.UnknownHostException -> "无法连接到 AI 服务，请检查网络连接"
                    is javax.net.ssl.SSLException -> "SSL 证书验证失败，请检查 API 地址"
                    is java.net.ConnectException -> "无法连接到服务器，请检查 API 地址和网络"
                    else -> "AI 请求失败，请稍后重试"
                }
                val errorMsg = AiMessage(
                    role = AiRole.ASSISTANT,
                    content = friendlyMsg
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
                versionCode = pkgInfo?.let { androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it) } ?: info.manifest.versionCode,
                minSdk = appInfo?.minSdkVersion ?: info.manifest.minSdk,
                targetSdk = appInfo?.targetSdkVersion ?: info.manifest.targetSdk,
                permissions = pkgInfo?.requestedPermissions?.toList() ?: info.manifest.permissions
            )
            info.copy(manifest = manifest)
        } catch (e: Exception) {
            Log.w("AppX", "Suppressed exception", e)
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
            Log.w("AppX", "Suppressed exception", e)
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
