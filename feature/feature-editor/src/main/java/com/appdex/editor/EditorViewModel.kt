package com.appdex.editor

import android.util.Log

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import com.appdex.data.SettingsRepository
import com.appdex.data.ai.AiChatMessage
import com.appdex.data.ai.AiChatRequest
import com.appdex.data.ai.AiConfigRepository
import com.appdex.data.ai.AiService
import com.appdex.ui.components.CopilotInsight
import com.appdex.ui.components.InsightType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val aiConfigRepository: AiConfigRepository,
    private val workspaceEventBus: com.appdex.data.workspace.WorkspaceEventBus,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<EditorIntent, EditorState, EditorEffect>(
    initialState = EditorState(),
    savedStateHandle = savedStateHandle
) {
    private val aiService = AiService()

    val editorFontSize = settings.editorFontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 14)
    val editorTabWidth = settings.editorTabWidth
        .stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    override fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.OpenFile -> openFile(intent.path)
            is EditorIntent.UpdateContent -> updateContent(intent.content)
            is EditorIntent.Save -> save()
            EditorIntent.ToggleCopilot -> toggleCopilot()
            EditorIntent.RequestCopilotInsights -> requestCopilotInsights()
            EditorIntent.DismissCopilot -> dismissCopilot()
        }
    }

    fun openFileIfProvided(filePath: String?) {
        val path = filePath ?: restoreState("editor_file_path", "")
        if (path.isNotEmpty()) {
            handleIntent(EditorIntent.OpenFile(path))
        }
    }

    fun openFileFromUri(uri: Uri, contentResolver: ContentResolver) {
        update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = uri.lastPathSegment ?: "unknown"
                val content = contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: run {
                    update { it.copy(isLoading = false, error = "Cannot open file") }
                    return@launch
                }

                if (content.length > 5 * 1024 * 1024) {
                    update { it.copy(isLoading = false, error = "File too large (max 5MB)") }
                    return@launch
                }

                // Try to copy to a temp file for saving
                val tempFile = File.createTempFile("edit_", "_${fileName}")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                update {
                    it.copy(
                        filePath = tempFile.absolutePath,
                        fileName = fileName.substringAfterLast("/"),
                        content = content,
                        isModified = false,
                        isLoading = false,
                        copilotInsights = emptyList()
                    )
                }
                // Auto-generate local insights
                generateLocalInsights(fileName.substringAfterLast("/"), content)
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message ?: "Failed to open file") }
            }
        }
    }

    private fun openFile(path: String) {
        saveState("editor_file_path", path)
        update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists() || !file.isFile) {
                    update { it.copy(isLoading = false, error = "File not found") }
                    return@launch
                }
                if (file.length() > 5 * 1024 * 1024) {
                    update { it.copy(isLoading = false, error = "File too large (max 5MB)") }
                    return@launch
                }
                val content = file.readText()
                update {
                    it.copy(
                        filePath = path,
                        fileName = file.name,
                        content = content,
                        isModified = false,
                        isLoading = false,
                        copilotInsights = emptyList()
                    )
                }
                // Auto-generate local insights
                generateLocalInsights(file.name, content)
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message ?: "Failed to open file") }
            }
        }
    }

    private fun updateContent(newContent: String) {
        update { it.copy(content = newContent, isModified = true) }
    }

    private fun save() {
        val path = currentState.filePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
File(path).writeText(currentState.content)
update { it.copy(isModified = false) }
emitEffect(EditorEffect.Saved)
// Phase 3: Emit EditCompleted event to Workspace OS
workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.EditCompleted(
filePath = path,
description = "编辑文件: ${path.substringAfterLast('/')}"
))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                emitEffect(EditorEffect.Error(e.message ?: "Failed to save"))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Copilot — AI-assisted editing
    // ═══════════════════════════════════════════════════════════════

    private fun toggleCopilot() {
        val newVisible = !currentState.isCopilotVisible
        update { it.copy(isCopilotVisible = newVisible) }
        // If turning on and no insights yet, generate them
        if (newVisible && currentState.copilotInsights.isEmpty() && currentState.fileName != null) {
            requestCopilotInsights()
        }
    }

    private fun dismissCopilot() {
        update { it.copy(isCopilotVisible = false) }
    }

    private fun requestCopilotInsights() {
        val fileName = currentState.fileName ?: return
        val content = currentState.content

        // First, generate instant local insights
        generateLocalInsights(fileName, content)

        // Then, try AI insights if configured
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = aiConfigRepository.config.first()
                if (!config.isConfigured()) return@launch

                update { it.copy(isCopilotLoading = true) }

                val truncatedContent = if (content.length > 3000) {
                    content.take(3000) + "\n... (内容已截断，共 ${content.length} 字符)"
                } else {
                    content
                }

                val systemPrompt = """
                    你是 AppX Copilot — 文件分析助手。
                    分析用户打开的文件，用简洁的中文提供见解。

                    文件名: $fileName

                    请按以下格式回答（每部分一行，没有的省略）：
                    PURPOSE: 这个文件的用途（1-2句）
                    RISK: 潜在风险或注意事项（如果有）
                    SUGGESTION: 修改建议（如果有）
                    RELATED: 相关文件或权限（如果有）
                """.trimIndent()

                val request = AiChatRequest(
                    messages = listOf(AiChatMessage(role = "user", content = truncatedContent)),
                    model = config.modelName,
                    temperature = 0.3f,
                    maxTokens = 800,
                    systemPrompt = systemPrompt
                )

                val response = aiService.chat(config, request)
                if (response.success && response.content.isNotBlank()) {
                    val aiInsights = parseAiInsights(response.content)
                    if (aiInsights.isNotEmpty()) {
                        // Merge: keep local PURPOSE, add AI insights
                        val merged = currentState.copilotInsights.filter { it.type == InsightType.PURPOSE } + aiInsights
                        update {
                            it.copy(
                                copilotInsights = merged.distinctBy { insight -> insight.type to insight.title },
                                isCopilotLoading = false
                            )
                        }
                    } else {
                        update { it.copy(isCopilotLoading = false) }
                    }
                } else {
                    update { it.copy(isCopilotLoading = false) }
                }
            } catch (e: Exception) {
                Log.w("AppX", "Copilot AI error", e)
                update { it.copy(isCopilotLoading = false) }
            }
        }
    }

    /**
     * Generate instant local insights based on file type and content patterns.
     * This provides immediate value before AI analysis.
     */
    private fun generateLocalInsights(fileName: String, content: String) {
        val insights = mutableListOf<CopilotInsight>()
        val ext = fileName.substringAfterLast('.', "").lowercase()

        // Purpose insight based on file type
        val purpose = when {
            fileName.equals("AndroidManifest.xml", ignoreCase = true) ->
                "Android 应用的清单文件，声明了应用的包名、版本、权限、组件（Activity/Service/Receiver/Provider）等核心信息。"
            ext == "xml" ->
                "XML 配置文件，通常用于声明配置、UI 布局或资源定义。"
            ext == "json" ->
                "JSON 数据文件，用于存储或交换结构化数据。"
            ext == "js" || ext == "mjs" ->
                "JavaScript 脚本文件，可能用于 React Native 或 Web 逻辑。"
            ext == "properties" ->
                "Java 属性配置文件，用于键值对形式的配置。"
            ext == "txt" ->
                "纯文本文件。"
            ext == "md" ->
                "Markdown 文档文件。"
            ext == "smali" ->
                "Smali 反汇编代码文件，是 DEX 文件的反编译结果。"
            ext == "kt" || ext == "java" ->
                "源代码文件，包含应用逻辑。"
            else -> "文件类型: .$ext"
        }
        insights.add(CopilotInsight(InsightType.PURPOSE, "文件用途", purpose))

        // Risk insight based on content patterns
        val risks = mutableListOf<String>()
        if (content.contains("android.permission.", ignoreCase = true)) {
            val permissions = Regex("android\\.permission\\.(\\w+)").findAll(content).map { it.groupValues[1] }.toSet()
            val dangerous = permissions.intersect(setOf(
                "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_CONTACTS", "CAMERA",
                "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "READ_EXTERNAL_STORAGE",
                "WRITE_EXTERNAL_STORAGE", "SYSTEM_ALERT_WINDOW", "REQUEST_INSTALL_PACKAGES",
                "READ_PHONE_STATE", "CALL_PHONE", "INSTALL_PACKAGES"
            ))
            if (dangerous.isNotEmpty()) {
                risks.add("包含 ${dangerous.size} 个高危权限: ${dangerous.joinToString(", ")}")
            }
        }
        if (content.contains("http://", ignoreCase = true) && !content.contains("https://", ignoreCase = true)) {
            risks.add("包含不安全的 HTTP 链接（未使用 HTTPS 加密）")
        }
        if (Regex("(api[_-]?key|secret|password|token)\\s*[:=]\\s*[\"']\\S+[\"']", RegexOption.IGNORE_CASE).containsMatchIn(content)) {
            risks.add("可能包含硬编码的密钥或密码")
        }
        if (content.contains("adb_enabled") || content.contains("debuggable")) {
            risks.add("包含调试相关配置，生产环境应关闭")
        }
        if (risks.isNotEmpty()) {
            insights.add(CopilotInsight(InsightType.RISK, "潜在风险", risks.joinToString("\n")))
        }

        // Suggestion insight
        val suggestions = mutableListOf<String>()
        if (ext == "xml" && content.contains("<application") && !content.contains("android:allowBackup=\"false\"")) {
            suggestions.add("建议设置 android:allowBackup=\"false\" 以防止应用数据被备份")
        }
        if (content.contains("http://")) {
            suggestions.add("建议将 HTTP 链接升级为 HTTPS 以提高安全性")
        }
        if (ext == "json" && content.length > 10000) {
            suggestions.add("JSON 文件较大，建议使用流式解析以减少内存占用")
        }
        if (suggestions.isNotEmpty()) {
            insights.add(CopilotInsight(InsightType.SUGGESTION, "修改建议", suggestions.joinToString("\n")))
        }

        // Related insight for manifest
        if (fileName.equals("AndroidManifest.xml", ignoreCase = true)) {
            val activities = Regex("<activity[^>]*android:name=\"([^\"]+)\"").findAll(content).count()
            val services = Regex("<service[^>]*android:name=\"([^\"]+)\"").findAll(content).count()
            val receivers = Regex("<receiver[^>]*android:name=\"([^\"]+)\"").findAll(content).count()
            val providers = Regex("<provider[^>]*android:name=\"([^\"]+)\"").findAll(content).count()
            insights.add(CopilotInsight(
                InsightType.RELATED,
                "组件统计",
                "Activity: $activities\nService: $services\nReceiver: $receivers\nProvider: $providers"
            ))
        }

        update {
            it.copy(
                copilotInsights = insights,
                isCopilotVisible = true
            )
        }
    }

    /**
     * Parse AI response into CopilotInsight list.
     * Expected format: PURPOSE: ... RISK: ... SUGGESTION: ... RELATED: ...
     */
    private fun parseAiInsights(content: String): List<CopilotInsight> {
        val insights = mutableListOf<CopilotInsight>()
        val patterns = mapOf(
            InsightType.PURPOSE to Regex("(?i)PURPOSE\\s*[:：]\\s*(.*?)(?=\\n\\s*(?:RISK|SUGGESTION|RELATED|PURPOSE)\\s*[:：]|\\Z)", RegexOption.DOT_MATCHES_ALL),
            InsightType.RISK to Regex("(?i)RISK\\s*[:：]\\s*(.*?)(?=\\n\\s*(?:SUGGESTION|RELATED|PURPOSE|RISK)\\s*[:：]|\\Z)", RegexOption.DOT_MATCHES_ALL),
            InsightType.SUGGESTION to Regex("(?i)SUGGESTION\\s*[:：]\\s*(.*?)(?=\\n\\s*(?:RELATED|PURPOSE|RISK|SUGGESTION)\\s*[:：]|\\Z)", RegexOption.DOT_MATCHES_ALL),
            InsightType.RELATED to Regex("(?i)RELATED\\s*[:：]\\s*(.*?)(?=\\n\\s*(?:PURPOSE|RISK|SUGGESTION|RELATED)\\s*[:：]|\\Z)", RegexOption.DOT_MATCHES_ALL)
        )

        val labels = mapOf(
            InsightType.PURPOSE to "AI 分析：文件用途",
            InsightType.RISK to "AI 分析：风险评估",
            InsightType.SUGGESTION to "AI 分析：修改建议",
            InsightType.RELATED to "AI 分析：关联信息"
        )

        patterns.forEach { (type, pattern) ->
            pattern.find(content)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
                insights.add(CopilotInsight(type, labels[type]!!, text))
            }
        }

        return insights
    }
}

sealed interface EditorEffect : MviEffect {
    data object Saved : EditorEffect
    data class Error(val message: String) : EditorEffect
}
