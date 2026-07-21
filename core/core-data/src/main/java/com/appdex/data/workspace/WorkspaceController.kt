package com.appdex.data.workspace

import android.util.Log
import com.appdex.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// WorkspaceController — The Brain
// ═══════════════════════════════════════════════════════════════
// Listens to WorkspaceEventBus, updates WorkspaceObject.
// This is the ONLY place that modifies WorkspaceObject.
// Pages read from WorkspaceController.state (read-only).
//
// Phase 1-7: Workspace Intelligence
// - Cross-tool auto-highlighting (Phase 1)
// - Cross-tool navigation targets (Phase 2)
// - Proactive AI awareness (Phase 3)
// - Live report trigger (Phase 4)
// - Workspace memory for AI (Phase 5)
// ═══════════════════════════════════════════════════════════════

@Singleton
class WorkspaceController @Inject constructor(
    private val eventBus: WorkspaceEventBus,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── The Single Source of Truth ──
    private val _state = MutableStateFlow(WorkspaceObject())
    val state: StateFlow<WorkspaceObject> = _state.asStateFlow()

    // ── Navigation commands (for NavHost to observe) ──
    private val _navigationCommands = MutableStateFlow<NavigationCommand?>(null)
    val navigationCommands: StateFlow<NavigationCommand?> = _navigationCommands.asStateFlow()

    // ── AI Insight cards (Phase 3 — proactive AI) ──
    private val _aiInsights = MutableStateFlow<List<AiInsightCard>>(emptyList())
    val aiInsights: StateFlow<List<AiInsightCard>> = _aiInsights.asStateFlow()

    // ── Cross-tool navigation suggestions (Phase 2) ──
    private val _crossToolTargets = MutableStateFlow<List<CrossToolTarget>>(emptyList())
    val crossToolTargets: StateFlow<List<CrossToolTarget>> = _crossToolTargets.asStateFlow()

    // ── Report dirty flag (Phase 4 — Live Report) ──
    private val _reportRevision = MutableStateFlow(0)
    val reportRevision: StateFlow<Int> = _reportRevision.asStateFlow()

    init {
        // Subscribe to all workspace events
        scope.launch {
            eventBus.events.collect { event -> handleEvent(event) }
        }
        // Sync with SessionManager
        scope.launch {
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    _state.update { ws ->
                        ws.copy(
                            apkFilePath = session.apkFilePath,
                            apkInfo = session.apkInfo,
                            packageName = session.packageName,
                            versionName = session.versionName,
                            fileSize = session.fileSize,
                            sessionStatus = session.status,
                            securityScore = session.securityScore,
                            findings = session.findings,
                            aiMessages = session.aiMessages,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    // Phase 4: Bump report revision when session changes
                    _reportRevision.value++
                }
            }
        }
    }

    // ── Event Handling ──

    private fun handleEvent(event: WorkspaceEvent) {
        when (event) {
            // ── Selection Events ──
            is WorkspaceEvent.SelectClass -> {
                val sel = WorkspaceSelection.Class(event.className, event.dexFile)
                updateSelection(sel)
                addBreadcrumb("Class: ${event.className.substringAfterLast('.')}", WorkspaceTool.DEX)
                recordHistory(WorkspaceTool.DEX, "查看类: ${event.className}", sel)
                // Phase 1: Cross-tool auto-highlighting
                updateCrossToolTargets(sel)
                // Phase 3: AI auto-insight
                generateInsightForClass(event.className)
                // Phase 5: Update inspection target for Inspector
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectMethod -> {
                val sel = WorkspaceSelection.Method(event.className, event.methodName, event.descriptor)
                updateSelection(sel)
                addBreadcrumb("Method: ${event.methodName}()", WorkspaceTool.DEX)
                recordHistory(WorkspaceTool.DEX, "查看方法: ${event.className}.${event.methodName}", sel)
                updateCrossToolTargets(sel)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectXmlNode -> {
                val sel = WorkspaceSelection.XmlNode(event.tagName, event.name, event.parent)
                updateSelection(sel)
                addBreadcrumb("${event.tagName}: ${event.name}", WorkspaceTool.MANIFEST)
                recordHistory(WorkspaceTool.MANIFEST, "查看 ${event.tagName}: ${event.name}", sel)
                updateCrossToolTargets(sel)
                generateInsightForXmlNode(event.tagName, event.name)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectPermission -> {
                val sel = WorkspaceSelection.Permission(event.name)
                updateSelection(sel)
                addBreadcrumb("Permission: ${event.name.substringAfterLast('.')}", WorkspaceTool.MANIFEST)
                recordHistory(WorkspaceTool.MANIFEST, "查看权限: ${event.name}", sel)
                updateCrossToolTargets(sel)
                generateInsightForPermission(event.name)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectComponent -> {
                val sel = WorkspaceSelection.Component(event.name, event.type, event.exported)
                updateSelection(sel)
                addBreadcrumb("${event.type.displayName}: ${event.name.substringAfterLast('.')}", WorkspaceTool.MANIFEST)
                recordHistory(WorkspaceTool.MANIFEST, "查看${event.type.displayName}: ${event.name}", sel)
                updateCrossToolTargets(sel)
                generateInsightForComponent(event.name, event.type, event.exported)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectResource -> {
                val sel = WorkspaceSelection.Resource(event.resourceId, event.type, event.value)
                updateSelection(sel)
                addBreadcrumb("Resource: ${event.resourceId}", WorkspaceTool.RESOURCES)
                recordHistory(WorkspaceTool.RESOURCES, "查看资源: ${event.resourceId}", sel)
                updateCrossToolTargets(sel)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectFile -> {
                val sel = WorkspaceSelection.File(event.path, event.displayName ?: event.path)
                updateSelection(sel)
                addBreadcrumb("File: ${event.displayName ?: event.path.substringAfterLast('/')}", WorkspaceTool.FILES)
                recordHistory(WorkspaceTool.FILES, "打开文件: ${event.displayName ?: event.path}", sel)
                updateCrossToolTargets(sel)
                generateInsightForFile(event.path)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectOffset -> {
                val sel = WorkspaceSelection.Offset(event.address, event.file)
                updateSelection(sel)
                addBreadcrumb("Offset: 0x${event.address.toString(16)}", WorkspaceTool.HEX)
                recordHistory(WorkspaceTool.HEX, "跳转到偏移: 0x${event.address.toString(16)}", sel)
                updateCrossToolTargets(sel)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectFinding -> {
                val sel = WorkspaceSelection.Finding(event.findingId, event.title)
                updateSelection(sel)
                addBreadcrumb("Finding: ${event.title}", WorkspaceTool.SECURITY)
                recordHistory(WorkspaceTool.SECURITY, "查看发现: ${event.title}", sel)
                updateCrossToolTargets(sel)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectField -> {
                val sel = WorkspaceSelection.Field(event.className, event.fieldName, event.type)
                updateSelection(sel)
                addBreadcrumb("Field: ${event.fieldName}", WorkspaceTool.DEX)
                recordHistory(WorkspaceTool.DEX, "查看字段: ${event.className}.${event.fieldName}", sel)
                updateCrossToolTargets(sel)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.SelectString -> {
                val sel = WorkspaceSelection.StringValue(event.value, event.className)
                updateSelection(sel)
                addBreadcrumb("String: \"${event.value.take(20)}\"", WorkspaceTool.DEX)
                recordHistory(WorkspaceTool.DEX, "查看字符串: ${event.value.take(30)}", sel)
                updateCrossToolTargets(sel)
                updateInspectionForSelection(sel)
            }

            is WorkspaceEvent.ClearSelection -> {
                _state.update { it.copy(selection = WorkspaceSelection.None) }
                _crossToolTargets.value = emptyList()
            }

            // ── Navigation Events ──
            is WorkspaceEvent.OpenTool -> {
                val tool = event.tool
                _state.update { ws ->
                    ws.copy(
                        activeTool = tool,
                        navigationState = ws.navigationState.copy(
                            previousRoute = ws.navigationState.currentRoute,
                            currentRoute = tool.routeName,
                            canGoBack = true
                        ),
                        selection = event.selection ?: ws.selection
                    )
                }
                _navigationCommands.value = NavigationCommand.NavigateTo(tool)
                Log.d("WorkspaceController", "Navigate to ${tool.displayName}")
                addTimeline(tool.displayName, "打开${tool.displayName}", tool)
                // Phase 3: Generate AI awareness when entering a tool
                generateToolAwareness(tool)
            }

            is WorkspaceEvent.CloseTool -> {
                _state.update { it.copy(activeTool = WorkspaceTool.NONE) }
            }

            is WorkspaceEvent.NavigateBack -> {
                val target = event.toTool ?: WorkspaceTool.WORKSPACE
                _navigationCommands.value = NavigationCommand.NavigateBack(target)
            }

            // ── Lifecycle Events ──
            is WorkspaceEvent.ApkLoaded -> {
                _state.update { it.copy(packageName = event.packageName, apkFilePath = event.filePath) }
                addTimeline(event.packageName, "导入 APK: ${event.packageName}", WorkspaceTool.WORKSPACE)
                recordHistory(WorkspaceTool.WORKSPACE, "导入 APK: ${event.packageName}", isCheckpoint = true)
                _reportRevision.value++ // Phase 4: Report needs update
            }

            is WorkspaceEvent.AnalysisCompleted -> {
                _state.update { it.copy(securityScore = event.score, sessionStatus = com.appdex.data.session.SessionStatus.READY) }
                addTimeline("分析完成", "安全评分: ${event.score}/100, ${event.findingCount} 项发现", WorkspaceTool.WORKSPACE)
                recordHistory(WorkspaceTool.WORKSPACE, "分析完成: 评分 ${event.score}/100", isCheckpoint = true)
                _reportRevision.value++ // Phase 4: Report needs update
            }

            is WorkspaceEvent.SecurityUpdated -> {
                _state.update { it.copy(securityScore = event.score, flags = it.flags.copy(needsSecurityRescan = false)) }
                addTimeline("安全扫描", "评分更新: ${event.score}/100", WorkspaceTool.SECURITY)
                recordHistory(WorkspaceTool.SECURITY, "安全扫描完成: ${event.score}/100")
                _reportRevision.value++ // Phase 4: Report auto-updates
            }

            is WorkspaceEvent.RepackCompleted -> {
                _state.update { it.copy(flags = it.flags.copy(isRepacked = true, isDirty = false)) }
                addTimeline("重打包", "APK 已重新打包: ${event.outputPath.substringAfterLast('/')}", WorkspaceTool.REPACK)
                recordHistory(WorkspaceTool.REPACK, "重打包完成", isCheckpoint = true)
                _reportRevision.value++ // Phase 4: Report reflects new state
            }

            is WorkspaceEvent.SignCompleted -> {
                _state.update { it.copy(flags = it.flags.copy(isSigned = true)) }
                addTimeline("签名", "APK 已签名: ${event.outputPath.substringAfterLast('/')}", WorkspaceTool.SIGNING)
                recordHistory(WorkspaceTool.SIGNING, "签名完成", isCheckpoint = true)
                _reportRevision.value++ // Phase 4: Report reflects new state
            }

            is WorkspaceEvent.EditCompleted -> {
                _state.update { it.copy(flags = it.flags.copy(isDirty = true, needsSecurityRescan = true)) }
                addTimeline("编辑", event.description, WorkspaceTool.EDITOR)
                recordHistory(WorkspaceTool.EDITOR, "编辑: ${event.description}")
                _reportRevision.value++ // Phase 4: Report auto-updates on edit
                // Phase 3: AI insight about the edit
                pushInsight(AiInsightCard(
                    title = "文件已修改",
                    detail = "${event.description}。安全评分可能已变化，建议重新扫描。",
                    tool = WorkspaceTool.SECURITY
                ))
            }

            // ── Workspace State Events ──
            is WorkspaceEvent.WorkspaceChanged -> {
                _state.update { it.copy(updatedAt = System.currentTimeMillis()) }
                _reportRevision.value++ // Phase 4: Any workspace change triggers report update
            }

            is WorkspaceEvent.SearchRequested -> {
                _state.update { it.copy(currentSearch = it.currentSearch.copy(query = event.query, searchType = event.type)) }
                recordHistory(WorkspaceTool.NONE, "搜索: ${event.query}")
            }

            is WorkspaceEvent.PinItem -> {
                _state.update { ws ->
                    ws.copy(pinnedItems = ws.pinnedItems + PinnedItem(
                        label = event.label,
                        selection = event.selection,
                        tool = event.tool
                    ))
                }
            }

            is WorkspaceEvent.UnpinLast -> {
                _state.update { it.copy(pinnedItems = it.pinnedItems.dropLast(1)) }
            }

            // ── AI Events (Phase 3) ──
            is WorkspaceEvent.AIReply -> {
                recordHistory(WorkspaceTool.AI, "AI 回复: ${event.content.take(50)}")
            }

            is WorkspaceEvent.AIInsight -> {
                pushInsight(AiInsightCard(event.title, event.detail, event.tool))
            }

            is WorkspaceEvent.RequestAIContext -> {
                Log.d("WorkspaceController", "AI context requested")
            }

            // ── Inspector Events (Phase D) ──
            is WorkspaceEvent.Inspect -> {
                _state.update { it.copy(inspectionTarget = event.target) }
            }
        }
    }

    // ── Helper Methods ──

    private fun updateSelection(selection: WorkspaceSelection) {
        _state.update { it.copy(selection = selection) }
        sessionManager.reportWorkspaceAction(
            selection = selection.displayLabel.ifEmpty { null },
            action = "选中: ${selection.displayLabel.ifEmpty { "清除" }}"
        )
    }

    private fun addBreadcrumb(label: String, tool: WorkspaceTool) {
        _state.update { ws ->
            ws.copy(breadcrumbs = (ws.breadcrumbs + BreadcrumbItem(label, tool)).takeLast(10))
        }
    }

    private fun recordHistory(
        tool: WorkspaceTool,
        action: String,
        selection: WorkspaceSelection = WorkspaceSelection.None,
        isCheckpoint: Boolean = false
    ) {
        _state.update { ws ->
            val entry = WorkspaceHistoryEntry(tool = tool, action = action, selection = selection, isCheckpoint = isCheckpoint)
            val newRecent = listOf(RecentItem(label = action, selection = selection, tool = tool)) + ws.recentItems.take(19)
            ws.copy(history = (ws.history + entry).takeLast(50), recentItems = newRecent)
        }
    }

    private fun addTimeline(title: String, detail: String?, tool: WorkspaceTool) {
        _state.update { ws ->
            ws.copy(timeline = (ws.timeline + WorkspaceTimelineEntry(
                type = "ACTION",
                title = title,
                detail = detail,
                tool = tool
            )).takeLast(30))
        }
    }

    private fun pushInsight(insight: AiInsightCard) {
        _aiInsights.update { (it + insight).takeLast(5) }
    }

    // ═══════════════════════════════════════════════════════════════
    // Phase 1: Cross-Tool Auto-Highlighting
    // ═══════════════════════════════════════════════════════════════
    // When user selects something, compute which other tools
    // can show related information. This creates a "web" not a "tree".
    // ═══════════════════════════════════════════════════════════════

    private fun updateCrossToolTargets(selection: WorkspaceSelection) {
        val targets = mutableListOf<CrossToolTarget>()
        when (selection) {
            is WorkspaceSelection.Class -> {
                // A class can be viewed in: DEX (smali), Manifest (if Activity/Service), Security, AI, Report
                targets.add(CrossToolTarget("查看 Smali", WorkspaceTool.DEX, selection))
                targets.add(CrossToolTarget("在 Manifest 中查找", WorkspaceTool.MANIFEST, selection))
                targets.add(CrossToolTarget("安全分析", WorkspaceTool.SECURITY, selection))
                targets.add(CrossToolTarget("问 AI", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.Method -> {
                targets.add(CrossToolTarget("查看 Smali", WorkspaceTool.DEX, selection))
                targets.add(CrossToolTarget("问 AI", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.XmlNode -> {
                targets.add(CrossToolTarget("在 Manifest 中查看", WorkspaceTool.MANIFEST, selection))
                targets.add(CrossToolTarget("安全分析", WorkspaceTool.SECURITY, selection))
                targets.add(CrossToolTarget("问 AI", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.Permission -> {
                targets.add(CrossToolTarget("在 Manifest 中查看", WorkspaceTool.MANIFEST, selection))
                targets.add(CrossToolTarget("安全分析", WorkspaceTool.SECURITY, selection))
                targets.add(CrossToolTarget("问 AI: 这个权限安全吗", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.Component -> {
                targets.add(CrossToolTarget("在 DEX 中查找类", WorkspaceTool.DEX, WorkspaceSelection.Class(selection.name)))
                targets.add(CrossToolTarget("在 Manifest 中查看", WorkspaceTool.MANIFEST, selection))
                targets.add(CrossToolTarget("安全分析", WorkspaceTool.SECURITY, selection))
                targets.add(CrossToolTarget("问 AI", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.Resource -> {
                targets.add(CrossToolTarget("在资源中查看", WorkspaceTool.RESOURCES, selection))
                targets.add(CrossToolTarget("问 AI", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.File -> {
                val ext = selection.path.substringAfterLast('.').lowercase()
                when (ext) {
                    "dex" -> {
                        targets.add(CrossToolTarget("DEX 浏览器", WorkspaceTool.DEX, selection))
                        targets.add(CrossToolTarget("HEX 编辑器", WorkspaceTool.HEX, selection))
                    }
                    "so" -> {
                        targets.add(CrossToolTarget("ELF 查看器", WorkspaceTool.ELF, selection))
                        targets.add(CrossToolTarget("HEX 编辑器", WorkspaceTool.HEX, selection))
                    }
                    "xml" -> {
                        targets.add(CrossToolTarget("Manifest 编辑器", WorkspaceTool.MANIFEST, selection))
                        targets.add(CrossToolTarget("文本编辑器", WorkspaceTool.EDITOR, selection))
                    }
                    "db", "sqlite" -> {
                        targets.add(CrossToolTarget("SQLite 查看器", WorkspaceTool.SQLITE, selection))
                    }
                    "arsc" -> {
                        targets.add(CrossToolTarget("资源查看器", WorkspaceTool.RESOURCES, selection))
                    }
                }
                targets.add(CrossToolTarget("HEX 编辑器", WorkspaceTool.HEX, selection))
                targets.add(CrossToolTarget("问 AI", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.Finding -> {
                targets.add(CrossToolTarget("安全详情", WorkspaceTool.SECURITY, selection))
                targets.add(CrossToolTarget("查看报告", WorkspaceTool.REPORT, selection))
                targets.add(CrossToolTarget("问 AI: 如何修复", WorkspaceTool.AI, selection))
            }
            is WorkspaceSelection.Offset -> {
                targets.add(CrossToolTarget("HEX 编辑器", WorkspaceTool.HEX, selection))
            }
            else -> {}
        }
        _crossToolTargets.value = targets
    }

    // ═══════════════════════════════════════════════════════════════
    // Phase 3: AI Workspace Awareness — Proactive Insights
    // ═══════════════════════════════════════════════════════════════

    private fun generateInsightForClass(className: String) {
        val simpleName = className.substringAfterLast('.')
        val insight = when {
            simpleName.contains("Login", ignoreCase = true) ||
            simpleName.contains("Signin", ignoreCase = true) ||
            simpleName.contains("Auth", ignoreCase = true) -> AiInsightCard(
                "检测到登录相关类", "$simpleName 可能包含登录逻辑。建议查看 onCreate 和网络请求方法。", WorkspaceTool.DEX)
            simpleName.contains("Pay", ignoreCase = true) ||
            simpleName.contains("Order", ignoreCase = true) ||
            simpleName.contains("Alipay", ignoreCase = true) ||
            simpleName.contains("Wechat", ignoreCase = true) ||
            simpleName.contains("Wxpay", ignoreCase = true) -> AiInsightCard(
                "检测到支付/订单相关类", "$simpleName 可能包含支付逻辑。建议检查安全性和数据传输。", WorkspaceTool.DEX)
            simpleName.contains("Network", ignoreCase = true) ||
            simpleName.contains("Http", ignoreCase = true) ||
            simpleName.contains("Api", ignoreCase = true) ||
            simpleName.contains("Retrofit", ignoreCase = true) ||
            simpleName.contains("OkHttp", ignoreCase = true) -> AiInsightCard(
                "检测到网络相关类", "$simpleName 可能包含网络请求逻辑。建议检查 API 地址和数据加密。", WorkspaceTool.DEX)
            simpleName.contains("Encrypt", ignoreCase = true) ||
            simpleName.contains("Decrypt", ignoreCase = true) ||
            simpleName.contains("Cipher", ignoreCase = true) ||
            simpleName.contains("AES", ignoreCase = true) ||
            simpleName.contains("RSA", ignoreCase = true) -> AiInsightCard(
                "检测到加密相关类", "$simpleName 可能包含加密/解密逻辑。建议检查密钥存储方式。", WorkspaceTool.DEX)
            simpleName.contains("Flutter", ignoreCase = true) -> AiInsightCard(
                "检测到 Flutter 引擎", "$simpleName 表明此应用使用 Flutter 框架。Dart 代码编译在 libapp.so 中。", WorkspaceTool.DEX)
            simpleName.contains("Unity", ignoreCase = true) -> AiInsightCard(
                "检测到 Unity 引擎", "$simpleName 表明此应用使用 Unity 游戏引擎。", WorkspaceTool.DEX)
            simpleName == "MainActivity" -> AiInsightCard(
                "主入口 Activity", "$simpleName 是应用的入口。建议检查 onCreate 中的初始化逻辑。", WorkspaceTool.DEX)
            simpleName.contains("Application", ignoreCase = true) && className.count { it == '.' } <= 2 -> AiInsightCard(
                "Application 类", "$simpleName 是应用入口，在所有组件之前初始化。检查 onCreate 中的 SDK 初始化。", WorkspaceTool.DEX)
            else -> null
        }
        if (insight != null) pushInsight(insight)
    }

    private fun generateInsightForXmlNode(tagName: String, name: String) {
        val insight = when {
            tagName == "manifest" -> AiInsightCard(
                "AndroidManifest.xml", "这是应用的入口配置文件，包含所有权限声明和组件注册。", WorkspaceTool.MANIFEST)
            tagName == "application" -> AiInsightCard(
                "Application 配置", "应用全局配置。检查 allowBackup、debuggable、networkSecurityConfig 等属性。", WorkspaceTool.MANIFEST)
            tagName == "activity" && name.endsWith("MainActivity") -> AiInsightCard(
                "主入口 Activity", "$name 是应用的启动 Activity。包含 LAUNCHER intent-filter。", WorkspaceTool.MANIFEST)
            tagName == "activity" && name.contains("Flutter", ignoreCase = true) -> AiInsightCard(
                "Flutter 应用", "$name 表明此应用使用 Flutter 框架开发。", WorkspaceTool.MANIFEST)
            tagName == "activity" && name.contains("Unity", ignoreCase = true) -> AiInsightCard(
                "Unity 游戏", "$name 表明此应用使用 Unity 引擎。", WorkspaceTool.MANIFEST)
            tagName == "provider" -> AiInsightCard(
                "ContentProvider", "$name 暴露了数据接口。检查 exported 属性和权限保护。", WorkspaceTool.MANIFEST)
            tagName == "service" -> AiInsightCard(
                "Service 组件", "$name 是后台服务。检查是否 exported 和所需权限。", WorkspaceTool.MANIFEST)
            else -> null
        }
        if (insight != null) pushInsight(insight)
    }

    private fun generateInsightForPermission(name: String) {
        val danger = when {
            name.contains("INTERNET") -> null // Too common
            name.contains("READ_SMS") || name.contains("SEND_SMS") || name.contains("RECEIVE_SMS") -> AiInsightCard(
                "短信权限", "应用可以读取/发送短信，存在费用消耗和隐私泄露风险。", WorkspaceTool.SECURITY)
            name.contains("READ_CONTACTS") || name.contains("WRITE_CONTACTS") -> AiInsightCard(
                "通讯录权限", "应用可以读取/修改通讯录，存在隐私泄露风险。", WorkspaceTool.SECURITY)
            name.contains("ACCESS_FINE_LOCATION") || name.contains("ACCESS_COARSE_LOCATION") -> AiInsightCard(
                "定位权限", "应用可以获取用户位置信息。", WorkspaceTool.SECURITY)
            name.contains("RECORD_AUDIO") -> AiInsightCard(
                "录音权限", "应用可以录制音频，存在窃听风险。", WorkspaceTool.SECURITY)
            name.contains("CAMERA") -> AiInsightCard(
                "相机权限", "应用可以拍照/录像。", WorkspaceTool.SECURITY)
            name.contains("READ_PHONE_STATE") || name.contains("READ_PHONE_NUMBERS") -> AiInsightCard(
                "手机状态权限", "应用可以读取设备标识符（IMEI等），存在追踪风险。", WorkspaceTool.SECURITY)
            name.contains("SYSTEM_ALERT_WINDOW") -> AiInsightCard(
                "悬浮窗权限", "应用可以显示悬浮窗，可能用于覆盖攻击。", WorkspaceTool.SECURITY)
            name.contains("REQUEST_INSTALL_PACKAGES") -> AiInsightCard(
                "安装 APK 权限", "应用可以静默安装其他 APK，高度危险。", WorkspaceTool.SECURITY)
            name.contains("READ_EXTERNAL_STORAGE") || name.contains("WRITE_EXTERNAL_STORAGE") -> AiInsightCard(
                "存储权限", "应用可以读写外部存储，可访问其他应用数据。", WorkspaceTool.SECURITY)
            else -> null
        }
        if (danger != null) pushInsight(danger)
    }

    private fun generateInsightForComponent(name: String, type: ComponentType, exported: Boolean) {
        if (exported) {
            val risk = when (type) {
                ComponentType.ACTIVITY -> "其他应用可以直接启动此 Activity"
                ComponentType.SERVICE -> "其他应用可以绑定此 Service"
                ComponentType.RECEIVER -> "其他应用可以发送广播触发此 Receiver"
                ComponentType.PROVIDER -> "其他应用可以查询/修改此 Provider 数据"
            }
            pushInsight(AiInsightCard(
                "导出组件: ${type.displayName}",
                "$name 已导出（exported=true）。$risk。检查是否有权限保护。",
                WorkspaceTool.SECURITY
            ))
        }
    }

    private fun generateInsightForFile(path: String) {
        val ext = path.substringAfterLast('.').lowercase()
        val name = path.substringAfterLast('/')
        val insight = when (ext) {
            "so" -> {
                when {
                    name.contains("flutter", ignoreCase = true) -> AiInsightCard(
                        "Flutter 引擎", "libflutter.so 表明此应用使用 Flutter 框架。Dart 代码在 libapp.so 中。", WorkspaceTool.ELF)
                    name.contains("unity", ignoreCase = true) -> AiInsightCard(
                        "Unity 引擎", "$name 是 Unity 游戏引擎库。", WorkspaceTool.ELF)
                    name.contains("tencent", ignoreCase = true) || name.contains("wechat", ignoreCase = true) -> AiInsightCard(
                        "腾讯 SDK", "$name 可能是微信/腾讯相关 SDK。", WorkspaceTool.ELF)
                    name.contains("baidu", ignoreCase = true) -> AiInsightCard(
                        "百度 SDK", "$name 可能是百度相关 SDK。", WorkspaceTool.ELF)
                    else -> AiInsightCard(
                        "Native 库", "$name 是 native 共享库。可使用 ELF 查看器分析。", WorkspaceTool.ELF)
                }
            }
            "dex" -> {
                val dexNum = name.filter { it.isDigit() }
                if (dexNum.isNotEmpty() && dexNum.toInt() > 1) {
                    AiInsightCard("多 DEX 文件", "$name 是第 $dexNum 个 DEX 文件，说明应用方法数超过 65535 限制。", WorkspaceTool.DEX)
                } else {
                    AiInsightCard("DEX 文件", "$name 包含应用编译后的字节码。", WorkspaceTool.DEX)
                }
            }
            "arsc" -> AiInsightCard("资源表", "$name 是编译后的资源索引表。", WorkspaceTool.RESOURCES)
            "xml" -> AiInsightCard("XML 文件", "$name 可以用 Manifest 编辑器或文本编辑器查看。", WorkspaceTool.MANIFEST)
            else -> null
        }
        if (insight != null) pushInsight(insight)
    }

    private fun generateToolAwareness(tool: WorkspaceTool) {
        // Phase 3: When entering a tool, AI proactively explains what this tool does
        val awareness = when (tool) {
            WorkspaceTool.MANIFEST -> AiInsightCard(
                "Manifest 视图", "这里展示 AndroidManifest.xml，包含权限、组件和应用配置。点击任意节点可查看详情。", WorkspaceTool.MANIFEST)
            WorkspaceTool.DEX -> AiInsightCard(
                "代码视图", "这里展示 DEX 反编译结果。浏览类列表，点击查看 Smali 代码。", WorkspaceTool.DEX)
            WorkspaceTool.SECURITY -> AiInsightCard(
                "安全视图", "这里展示安全扫描结果。评分和发现项实时跟随 Workspace 变化。", WorkspaceTool.SECURITY)
            WorkspaceTool.REPORT -> AiInsightCard(
                "报告视图", "这里是实时分析报告，跟随 Workspace 变化自动更新。", WorkspaceTool.REPORT)
            WorkspaceTool.HEX -> AiInsightCard(
                "HEX 视图", "十六进制编辑器，可查看和修改文件二进制内容。", WorkspaceTool.HEX)
            WorkspaceTool.RESOURCES -> AiInsightCard(
                "资源视图", "这里展示 APK 中的资源文件（图片、字符串、布局等）。", WorkspaceTool.RESOURCES)
            WorkspaceTool.SIGNING -> AiInsightCard(
                "签名视图", "这里管理 APK 签名。签名后 Report 会自动更新。", WorkspaceTool.SIGNING)
            WorkspaceTool.REPACK -> AiInsightCard(
                "重打包视图", "这里重新打包修改后的 APK。完成后签名状态会重置。", WorkspaceTool.REPACK)
            else -> null
        }
        if (awareness != null) pushInsight(awareness)
    }

    // ═══════════════════════════════════════════════════════════════
    // Phase 5: Workspace Memory — Build rich context for AI
    // ═══════════════════════════════════════════════════════════════

    /** Build a comprehensive memory string for AI, including recent actions and current state */
    fun buildWorkspaceMemory(): String = buildString {
        val ws = _state.value
        appendLine("=== Workspace Memory ===")
        appendLine("APK: ${ws.displayName}")
        appendLine("Package: ${ws.packageName}")
        appendLine("Version: ${ws.versionName}")
        appendLine("Security Score: ${ws.securityScore}/100")
        appendLine("Active Tool: ${ws.activeTool.displayName}")

        // Current selection
        ws.selection.toContextString()?.let { appendLine(it) }

        // Current search
        if (ws.currentSearch.query.isNotEmpty()) {
            appendLine("Current Search: ${ws.currentSearch.query}")
        }

        // Recent actions (last 10) — this is the "memory"
        if (ws.recentItems.isNotEmpty()) {
            appendLine("Recent Actions:")
            ws.recentItems.take(10).forEachIndexed { i, item ->
                appendLine("  ${i + 1}. [${item.tool.displayName}] ${item.label}")
            }
        }

        // Breadcrumb path
        if (ws.breadcrumbs.isNotEmpty()) {
            appendLine("Navigation Path: ${ws.breadcrumbs.joinToString(" > ") { it.label }}")
        }

        // Timeline (last 5)
        if (ws.timeline.isNotEmpty()) {
            appendLine("Timeline:")
            ws.timeline.takeLast(5).forEach { event ->
                appendLine("  - ${event.title}${event.detail?.let { ": $it" } ?: ""}")
            }
        }

        // Flags
        val flags = ws.flags
        if (flags.isDirty) appendLine("Status: Modified (unsaved changes)")
        if (flags.isRepacked) appendLine("Status: Repacked")
        if (flags.isSigned) appendLine("Status: Signed")
        if (flags.needsSecurityRescan) appendLine("Status: Security rescan needed")

        // Pinned items
        if (ws.pinnedItems.isNotEmpty()) {
            appendLine("Pinned: ${ws.pinnedItems.joinToString(", ") { it.label }}")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Phase 1: Update Inspector target based on selection
    // ═══════════════════════════════════════════════════════════════

    private fun updateInspectionForSelection(selection: WorkspaceSelection) {
        val target = when (selection) {
            is WorkspaceSelection.Class -> InspectionTarget(
                title = selection.className.substringAfterLast('.'),
                subtitle = "Class • ${selection.dexFile ?: "classes.dex"}",
                details = mapOf("全名" to selection.className, "DEX" to (selection.dexFile ?: "classes.dex"))
            )
            is WorkspaceSelection.Method -> InspectionTarget(
                title = "${selection.methodName}()",
                subtitle = "Method • ${selection.className.substringAfterLast('.')}",
                details = mapOf("类" to selection.className, "方法" to selection.methodName, "描述符" to (selection.descriptor ?: ""))
            )
            is WorkspaceSelection.Field -> InspectionTarget(
                title = selection.fieldName,
                subtitle = "Field • ${selection.className.substringAfterLast('.')}",
                details = mapOf("类" to selection.className, "字段" to selection.fieldName, "类型" to (selection.type ?: ""))
            )
            is WorkspaceSelection.StringValue -> InspectionTarget(
                title = "\"${selection.value.take(40)}\"",
                subtitle = "String${selection.className?.let { " • ${it.substringAfterLast('.')}" } ?: ""}",
                details = mapOf("值" to selection.value, "来源类" to (selection.className ?: "未知"))
            )
            is WorkspaceSelection.XmlNode -> InspectionTarget(
                title = selection.name,
                subtitle = "<${selection.tagName}>${selection.parent?.let { " • $it" } ?: ""}",
                details = mapOf("标签" to selection.tagName, "名称" to selection.name, "父节点" to (selection.parent ?: ""))
            )
            is WorkspaceSelection.Permission -> InspectionTarget(
                title = selection.name.substringAfterLast('.'),
                subtitle = "Permission",
                details = mapOf("权限" to selection.name)
            )
            is WorkspaceSelection.Component -> InspectionTarget(
                title = selection.name.substringAfterLast('.'),
                subtitle = "${selection.type.displayName}${if (selection.exported) " • Exported" else ""}",
                details = mapOf("全名" to selection.name, "类型" to selection.type.displayName, "导出" to selection.exported.toString())
            )
            is WorkspaceSelection.Resource -> InspectionTarget(
                title = selection.resourceId,
                subtitle = "Resource${selection.type?.let { " • $it" } ?: ""}",
                details = mapOf("ID" to selection.resourceId, "类型" to (selection.type ?: ""), "值" to (selection.value ?: ""))
            )
            is WorkspaceSelection.File -> InspectionTarget(
                title = selection.displayName.substringAfterLast('/'),
                subtitle = "File",
                details = mapOf("路径" to selection.path)
            )
            is WorkspaceSelection.Offset -> InspectionTarget(
                title = "0x${selection.address.toString(16)}",
                subtitle = "Offset${selection.file?.let { " • ${it.substringAfterLast('/')}" } ?: ""}",
                details = mapOf("地址" to "0x${selection.address.toString(16)}", "文件" to (selection.file ?: ""))
            )
            is WorkspaceSelection.Finding -> InspectionTarget(
                title = selection.title,
                subtitle = "Security Finding",
                details = mapOf("ID" to selection.findingId, "标题" to selection.title)
            )
            is WorkspaceSelection.None -> InspectionTarget()
        }
        _state.update { it.copy(inspectionTarget = target) }
    }

    // ── Public API for ViewModels ──

    fun setApkLoaded(filePath: String, packageName: String) {
        eventBus.emit(WorkspaceEvent.ApkLoaded(filePath, packageName))
    }

    fun setAnalysisCompleted(score: Int, findingCount: Int) {
        eventBus.emit(WorkspaceEvent.AnalysisCompleted(score, findingCount))
    }

    /** Select an item and optionally navigate to a tool (Phase 2: Cross-Tool) */
    fun select(selection: WorkspaceSelection, openTool: WorkspaceTool? = null) {
        eventBus.select(selection, openTool)
    }

    /** Clear AI insights (after user dismisses them) */
    fun clearInsights() {
        _aiInsights.value = emptyList()
    }

    fun clearNavigationCommand() {
        _navigationCommands.value = null
    }
}

// ── Supporting Types ──

sealed class NavigationCommand {
    data class NavigateTo(val tool: WorkspaceTool) : NavigationCommand()
    data class NavigateBack(val toTool: WorkspaceTool) : NavigationCommand()
}

data class AiInsightCard(
    val title: String,
    val detail: String,
    val tool: WorkspaceTool
)

/**
 * Phase 2: Cross-Tool Navigation Target
 * When user selects something, these are the other tools that can show related info.
 * Forms a "web" — any object can reach any tool.
 */
data class CrossToolTarget(
    val label: String,
    val tool: WorkspaceTool,
    val selection: WorkspaceSelection
)

// Extension for ComponentType display
val ComponentType.displayName: String
    get() = when (this) {
        ComponentType.ACTIVITY -> "Activity"
        ComponentType.SERVICE -> "Service"
        ComponentType.RECEIVER -> "Receiver"
        ComponentType.PROVIDER -> "Provider"
    }
