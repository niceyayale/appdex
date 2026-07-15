package com.appdex.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 提供商类型
 */
enum class AiProviderType(val displayName: String, val description: String) {
    OPENAI("OpenAI", "GPT-4o, GPT-4o-mini, o1, o3 等"),
    ANTHROPIC("Anthropic", "Claude 3.5 Sonnet, Claude 3 Opus 等"),
    GEMINI("Gemini", "Google Gemini 2.0 Flash, Pro 等"),
    DEEPSEEK("DeepSeek", "DeepSeek-V3, DeepSeek-R1 等"),
    OPENROUTER("OpenRouter", "聚合多家模型"),
    OPENAI_COMPATIBLE("OpenAI Compatible", "兼容 OpenAI 格式的自定义 API"),
    ANTHROPIC_COMPATIBLE("Anthropic Compatible", "兼容 Anthropic 格式的自定义 API"),
    OLLAMA("Ollama", "本地模型服务"),
    LM_STUDIO("LM Studio", "本地模型 GUI 工具"),
    ANYTHINGLLM("AnythingLLM", "本地 RAG + LLM"),
    LOCALAI("LocalAI", "本地 AI 推理"),
    CUSTOM("Custom API", "完全自定义 API")
}

/**
 * AI 配置
 */
data class AiConfig(
    val providerType: AiProviderType = AiProviderType.OPENAI,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096
) {
    fun isConfigured(): Boolean {
        return when (providerType) {
            AiProviderType.OLLAMA,
            AiProviderType.LM_STUDIO,
            AiProviderType.LOCALAI,
            AiProviderType.ANYTHINGLLM -> baseUrl.isNotEmpty()
            else -> apiKey.isNotEmpty()
        }
    }

    fun effectiveBaseUrl(): String {
        return when {
            baseUrl.isNotEmpty() -> baseUrl
            providerType == AiProviderType.OPENAI -> "https://api.openai.com/v1"
            providerType == AiProviderType.ANTHROPIC -> "https://api.anthropic.com/v1"
            providerType == AiProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
            providerType == AiProviderType.DEEPSEEK -> "https://api.deepseek.com/v1"
            providerType == AiProviderType.OPENROUTER -> "https://openrouter.ai/api/v1"
            providerType == AiProviderType.OLLAMA -> "http://localhost:11434"
            providerType == AiProviderType.LM_STUDIO -> "http://localhost:1234/v1"
            providerType == AiProviderType.LOCALAI -> "http://localhost:8080/v1"
            providerType == AiProviderType.ANYTHINGLLM -> "http://localhost:3001/api/v1"
            else -> baseUrl
        }
    }

    fun defaultModels(): List<String> {
        return when (providerType) {
            AiProviderType.OPENAI -> listOf("gpt-4o", "gpt-4o-mini", "o1", "o3-mini")
            AiProviderType.ANTHROPIC -> listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
            AiProviderType.GEMINI -> listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")
            AiProviderType.DEEPSEEK -> listOf("deepseek-chat", "deepseek-reasoner")
            AiProviderType.OPENROUTER -> listOf("openai/gpt-4o", "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash")
            AiProviderType.OLLAMA -> listOf("llama3.2", "qwen2.5", "deepseek-r1")
            AiProviderType.LM_STUDIO -> listOf("local-model")
            AiProviderType.LOCALAI -> listOf("gpt-3.5-turbo")
            AiProviderType.ANYTHINGLLM -> listOf("local-model")
            else -> emptyList()
        }
    }
}

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "appdex_ai_settings")

/**
 * AI 配置仓库
 */
@Singleton
class AiConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.aiDataStore

    val config: Flow<AiConfig> = dataStore.data.map { prefs ->
        AiConfig(
            providerType = prefs[KEY_PROVIDER]?.let {
                runCatching { AiProviderType.valueOf(it) }.getOrNull()
            } ?: AiProviderType.OPENAI,
            apiKey = prefs[KEY_API_KEY] ?: "",
            baseUrl = prefs[KEY_BASE_URL] ?: "",
            modelName = prefs[KEY_MODEL] ?: "",
            temperature = (prefs[KEY_TEMPERATURE] ?: "0.7").toFloatOrNull() ?: 0.7f,
            maxTokens = (prefs[KEY_MAX_TOKENS] ?: "4096").toIntOrNull() ?: 4096
        )
    }

    val isAiEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_API_KEY]?.isNotEmpty() == true || prefs[KEY_PROVIDER]?.let {
            it in listOf("OLLAMA", "LM_STUDIO", "LOCALAI", "ANYTHINGLLM")
        } == true
    }

    suspend fun setProvider(type: AiProviderType) {
        dataStore.edit { it[KEY_PROVIDER] = type.name }
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { it[KEY_BASE_URL] = url }
    }

    suspend fun setModel(model: String) {
        dataStore.edit { it[KEY_MODEL] = model }
    }

    suspend fun setTemperature(temp: Float) {
        dataStore.edit { it[KEY_TEMPERATURE] = temp.toString() }
    }

    suspend fun setMaxTokens(tokens: Int) {
        dataStore.edit { it[KEY_MAX_TOKENS] = tokens.toString() }
    }

    suspend fun updateConfig(config: AiConfig) {
        dataStore.edit {
            it[KEY_PROVIDER] = config.providerType.name
            it[KEY_API_KEY] = config.apiKey
            it[KEY_BASE_URL] = config.baseUrl
            it[KEY_MODEL] = config.modelName
            it[KEY_TEMPERATURE] = config.temperature.toString()
            it[KEY_MAX_TOKENS] = config.maxTokens.toString()
        }
    }

    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_API_KEY = stringPreferencesKey("ai_api_key")
        private val KEY_BASE_URL = stringPreferencesKey("ai_base_url")
        private val KEY_MODEL = stringPreferencesKey("ai_model")
        private val KEY_TEMPERATURE = stringPreferencesKey("ai_temperature")
        private val KEY_MAX_TOKENS = stringPreferencesKey("ai_max_tokens")
    }
}
