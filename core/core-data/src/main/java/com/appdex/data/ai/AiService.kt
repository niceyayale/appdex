package com.appdex.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 聊天请求
 */
@Serializable
data class AiChatRequest(
    val messages: List<AiChatMessage>,
    val model: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val systemPrompt: String = ""
)

@Serializable
data class AiChatMessage(
    val role: String,
    val content: String
)

/**
 * AI 聊天响应
 */
data class AiChatResponse(
    val content: String,
    val success: Boolean,
    val error: String? = null
)

/**
 * AI 服务 - 统一接口调用多种 AI 提供商
 *
 * 支持两种模式:
 * 1. [chat] — 同步请求，等待完整响应
 * 2. [chatStream] — SSE 流式响应，逐 token 返回
 */
class AiService {

    /**
     * 发送聊天请求（同步模式）
     */
    suspend fun chat(config: AiConfig, request: AiChatRequest): AiChatResponse = withContext(Dispatchers.IO) {
        if (!config.isConfigured()) {
            return@withContext AiChatResponse(
                content = "",
                success = false,
                error = "AI 未配置，请在设置中配置 AI 提供商"
            )
        }

        try {
            when (config.providerType) {
                AiProviderType.OPENAI,
                AiProviderType.OPENAI_COMPATIBLE,
                AiProviderType.OPENROUTER,
                AiProviderType.LM_STUDIO,
                AiProviderType.LOCALAI -> chatOpenAiCompatible(config, request)

                AiProviderType.ANTHROPIC,
                AiProviderType.ANTHROPIC_COMPATIBLE -> chatAnthropic(config, request)

                AiProviderType.GEMINI -> chatGemini(config, request)

                AiProviderType.DEEPSEEK -> chatOpenAiCompatible(config, request)

                AiProviderType.OLLAMA -> chatOllama(config, request)

                AiProviderType.ANYTHINGLLM -> chatOpenAiCompatible(config, request)

                AiProviderType.CUSTOM -> chatOpenAiCompatible(config, request)
            }
        } catch (e: Exception) {
            Log.w("AppDex", "AI chat error", e)
            AiChatResponse(content = "", success = false, error = e.message ?: "AI 请求失败")
        }
    }

    /**
     * 发送聊天请求（流式模式）— 逐 token 返回内容
     *
     * 对 OpenAI 兼容 / Anthropic / Ollama 使用 SSE 流式协议。
     * Gemini 暂不支持流式，回退到同步模式。
     */
    fun chatStream(config: AiConfig, request: AiChatRequest): Flow<String> = callbackFlow {
        if (!config.isConfigured()) {
            trySend("❌ AI 未配置，请先在设置中配置 AI 提供商。")
            channel.close()
            return@callbackFlow
        }

        try {
            when (config.providerType) {
                AiProviderType.OPENAI,
                AiProviderType.OPENAI_COMPATIBLE,
                AiProviderType.OPENROUTER,
                AiProviderType.LM_STUDIO,
                AiProviderType.LOCALAI,
                AiProviderType.DEEPSEEK,
                AiProviderType.ANYTHINGLLM,
                AiProviderType.CUSTOM -> {
                    streamOpenAiCompatible(config, request) { chunk -> trySend(chunk) }
                }

                AiProviderType.ANTHROPIC,
                AiProviderType.ANTHROPIC_COMPATIBLE -> {
                    streamAnthropic(config, request) { chunk -> trySend(chunk) }
                }

                AiProviderType.OLLAMA -> {
                    streamOllama(config, request) { chunk -> trySend(chunk) }
                }

                AiProviderType.GEMINI -> {
                    // Gemini 流式复杂，回退同步
                    val response = chatGemini(config, request)
                    if (response.success) {
                        trySend(response.content)
                    } else {
                        trySend("❌ ${response.error ?: "请求失败"}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AppDex", "AI stream error", e)
            trySend("❌ 流式请求失败: ${e.message}")
        } finally {
            channel.close()
        }

        awaitClose { }
    }.flowOn(Dispatchers.IO)

    /**
     * 检查 AI 是否可用（无需发送请求，仅检查配置）
     */
    fun isAvailable(config: AiConfig): Boolean {
        return config.isConfigured()
    }

    // ═══════════════════════════════════════════════════════════════
    // 同步请求实现
    // ═══════════════════════════════════════════════════════════════

    private fun chatOpenAiCompatible(config: AiConfig, request: AiChatRequest): AiChatResponse {
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 120000
            setRequestProperty("Content-Type", "application/json")
            if (config.apiKey.isNotEmpty()) {
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }
            if (config.providerType == AiProviderType.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://github.com/appdex")
                setRequestProperty("X-Title", "AppDex")
            }
        }

        val messages = buildList {
            if (request.systemPrompt.isNotEmpty()) {
                add(mapOf("role" to "system", "content" to request.systemPrompt))
            }
            request.messages.forEach { msg ->
                add(mapOf("role" to msg.role, "content" to msg.content))
            }
        }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifEmpty { config.defaultModels().firstOrNull() ?: "gpt-4o-mini" }))
            put("messages", Json.encodeToJsonElement(messages))
            put("temperature", JsonPrimitive(request.temperature))
            put("max_tokens", JsonPrimitive(request.maxTokens))
        }

        return executeRequest(connection, requestBody.toString()) { responseJson ->
            responseJson["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: "AI 返回了空响应"
        }
    }

    private fun chatAnthropic(config: AiConfig, request: AiChatRequest): AiChatResponse {
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/messages")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 120000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }

        val messages = request.messages.map { msg ->
            mapOf("role" to msg.role, "content" to msg.content)
        }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifEmpty { "claude-3-5-sonnet-20241022" }))
            put("messages", Json.encodeToJsonElement(messages))
            put("max_tokens", JsonPrimitive(request.maxTokens))
            if (request.systemPrompt.isNotEmpty()) {
                put("system", JsonPrimitive(request.systemPrompt))
            }
        }

        return executeRequest(connection, requestBody.toString()) { responseJson ->
            responseJson["content"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: "AI 返回了空响应"
        }
    }

    private fun chatGemini(config: AiConfig, request: AiChatRequest): AiChatResponse {
        val model = config.modelName.ifEmpty { "gemini-2.0-flash" }
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/models/$model:generateContent?key=${config.apiKey}")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 120000
            setRequestProperty("Content-Type", "application/json")
        }

        val contents = request.messages.map { msg ->
            buildJsonObject {
                put("role", JsonPrimitive(if (msg.role == "assistant") "model" else "user"))
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", JsonPrimitive(msg.content)) })
                })
            }
        }

        val requestBody = buildJsonObject {
            put("contents", Json.encodeToJsonElement(contents))
            if (request.systemPrompt.isNotEmpty()) {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", JsonPrimitive(request.systemPrompt)) })
                    })
                })
            }
            put("generationConfig", buildJsonObject {
                put("temperature", JsonPrimitive(request.temperature))
                put("maxOutputTokens", JsonPrimitive(request.maxTokens))
            })
        }

        return executeRequest(connection, requestBody.toString()) { responseJson ->
            responseJson["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
                ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: "AI 返回了空响应"
        }
    }

    private fun chatOllama(config: AiConfig, request: AiChatRequest): AiChatResponse {
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/api/chat")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 120000
            setRequestProperty("Content-Type", "application/json")
        }

        val messages = buildList {
            if (request.systemPrompt.isNotEmpty()) {
                add(mapOf("role" to "system", "content" to request.systemPrompt))
            }
            request.messages.forEach { msg ->
                add(mapOf("role" to msg.role, "content" to msg.content))
            }
        }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifEmpty { "llama3.2" }))
            put("messages", Json.encodeToJsonElement(messages))
            put("stream", JsonPrimitive(false))
        }

        return executeRequest(connection, requestBody.toString()) { responseJson ->
            responseJson["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: "AI 返回了空响应"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SSE 流式请求实现
    // ═══════════════════════════════════════════════════════════════

    private fun streamOpenAiCompatible(config: AiConfig, request: AiChatRequest, onChunk: (String) -> Unit) {
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 300000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            if (config.apiKey.isNotEmpty()) {
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }
        }

        val messages = buildList {
            if (request.systemPrompt.isNotEmpty()) {
                add(mapOf("role" to "system", "content" to request.systemPrompt))
            }
            request.messages.forEach { msg ->
                add(mapOf("role" to msg.role, "content" to msg.content))
            }
        }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifEmpty { config.defaultModels().firstOrNull() ?: "gpt-4o-mini" }))
            put("messages", Json.encodeToJsonElement(messages))
            put("temperature", JsonPrimitive(request.temperature))
            put("max_tokens", JsonPrimitive(request.maxTokens))
            put("stream", JsonPrimitive(true))
        }

        connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            onChunk("❌ 请求失败 (HTTP $responseCode): ${errorBody.take(200)}")
            connection.disconnect()
            return
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) continue
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val json = Json.parseToJsonElement(data).jsonObject
                    val delta = json["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    if (!delta.isNullOrEmpty()) {
                        onChunk(delta)
                    }
                } catch (e: Exception) {
                    // 忽略解析错误的 SSE 行
                }
            }
        } finally {
            reader.close()
            connection.disconnect()
        }
    }

    private fun streamAnthropic(config: AiConfig, request: AiChatRequest, onChunk: (String) -> Unit) {
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/messages")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 300000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }

        val messages = request.messages.map { msg ->
            mapOf("role" to msg.role, "content" to msg.content)
        }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifEmpty { "claude-3-5-sonnet-20241022" }))
            put("messages", Json.encodeToJsonElement(messages))
            put("max_tokens", JsonPrimitive(request.maxTokens))
            put("stream", JsonPrimitive(true))
            if (request.systemPrompt.isNotEmpty()) {
                put("system", JsonPrimitive(request.systemPrompt))
            }
        }

        connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            onChunk("❌ 请求失败 (HTTP $responseCode): ${errorBody.take(200)}")
            connection.disconnect()
            return
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        try {
            var line: String?
            var eventType = ""
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) continue
                when {
                    line!!.startsWith("event: ") -> eventType = line!!.removePrefix("event: ").trim()
                    line!!.startsWith("data: ") -> {
                        val data = line!!.removePrefix("data: ").trim()
                        if (eventType == "content_block_delta") {
                            try {
                                val json = Json.parseToJsonElement(data).jsonObject
                                val text = json["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                                if (!text.isNullOrEmpty()) {
                                    onChunk(text)
                                }
                            } catch (e: Exception) { }
                        }
                    }
                }
            }
        } finally {
            reader.close()
            connection.disconnect()
        }
    }

    private fun streamOllama(config: AiConfig, request: AiChatRequest, onChunk: (String) -> Unit) {
        val url = URL("${config.effectiveBaseUrl().trimEnd('/')}/api/chat")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 300000
            setRequestProperty("Content-Type", "application/json")
        }

        val messages = buildList {
            if (request.systemPrompt.isNotEmpty()) {
                add(mapOf("role" to "system", "content" to request.systemPrompt))
            }
            request.messages.forEach { msg ->
                add(mapOf("role" to msg.role, "content" to msg.content))
            }
        }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifEmpty { "llama3.2" }))
            put("messages", Json.encodeToJsonElement(messages))
            put("stream", JsonPrimitive(true))
        }

        connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            onChunk("❌ 请求失败 (HTTP $responseCode)")
            connection.disconnect()
            return
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) continue
                try {
                    val json = Json.parseToJsonElement(line!!).jsonObject
                    val content = json["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                    if (!content.isNullOrEmpty()) {
                        onChunk(content)
                    }
                    if (json["done"]?.jsonPrimitive?.booleanOrNull == true) break
                } catch (e: Exception) { }
            }
        } finally {
            reader.close()
            connection.disconnect()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HTTP 工具方法
    // ═══════════════════════════════════════════════════════════════

    private fun executeRequest(
        connection: HttpURLConnection,
        body: String,
        parser: (JsonObject) -> String
    ): AiChatResponse {
        var responseCode: Int
        var responseBody: String
        try {
            connection.outputStream.use { it.write(body.toByteArray()) }
            responseCode = connection.responseCode
            responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            }
        } catch (e: Exception) {
            return AiChatResponse(content = "", success = false, error = "网络请求失败: ${e.message}")
        } finally {
            connection.disconnect()
        }

        return try {
            if (responseCode !in 200..299) {
                val errorJson = Json.parseToJsonElement(responseBody).jsonObject
                val errorMsg = errorJson["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    ?: errorJson["error"]?.jsonPrimitive?.content
                    ?: "HTTP $responseCode: $responseBody"
                AiChatResponse(content = "", success = false, error = errorMsg)
            } else {
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                val content = parser(responseJson)
                AiChatResponse(content = content, success = true)
            }
        } catch (e: Exception) {
            AiChatResponse(content = "", success = false, error = "解析响应失败: ${e.message}")
        }
    }
}
