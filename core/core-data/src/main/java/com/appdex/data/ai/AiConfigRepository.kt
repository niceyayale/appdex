package com.appdex.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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
 * AI 配置（当前激活的配置）
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

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "AppX_ai_settings")

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * AI 配置仓库
 *
 * 支持多提供商管理：
 * - [savedProviders]: 所有已保存的提供商列表
 * - [activeProviderId]: 当前激活的提供商 ID
 * - [config]: 当前激活提供商的配置（向后兼容）
 *
 * 自动迁移：如果旧版单配置存在但无已保存提供商，会自动迁移为第一个已保存提供商。
 */
@Singleton
class AiConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.aiDataStore

    // ── 已保存的提供商列表 ──
    val savedProviders: Flow<List<AiProviderEntity>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_PROVIDERS_JSON]
        if (jsonStr.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(ListSerializer(AiProviderEntity.serializer()), jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ── 当前激活的提供商 ID ──
    val activeProviderId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROVIDER_ID]
    }

    // ── 当前激活的提供商实体 ──
    val activeProvider: Flow<AiProviderEntity?> = combine(savedProviders, activeProviderId) { providers, activeId ->
        providers.find { it.id == activeId }
    }

    // ── 当前 AI 配置（向后兼容）──
    // 优先返回激活的已保存提供商配置；如果没有，则返回旧版单配置字段
    val config: Flow<AiConfig> = combine(savedProviders, activeProviderId, dataStore.data) { providers, activeId, prefs ->
        val activeProvider = providers.find { it.id == activeId }
        if (activeProvider != null) {
            activeProvider.toAiConfig()
        } else if (providers.isNotEmpty()) {
            // Fallback to first provider if active ID is invalid
            providers.first().toAiConfig()
        } else {
            // Legacy fallback: read from old single-config keys
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
    }

    val isAiEnabled: Flow<Boolean> = config.map { it.isConfigured() }

    // ═══════════════════════════════════════════════════════════════
    // 多提供商 CRUD 操作
    // ═══════════════════════════════════════════════════════════════

    /**
     * 添加新的提供商配置
     * @return 新创建的提供商 ID
     */
    suspend fun addProvider(provider: AiProviderEntity): String {
        val providerWithId = if (provider.id.isEmpty()) provider.copy(id = AiProviderEntity.generateId()) else provider
        dataStore.edit { prefs ->
            val currentList = getCurrentProviders(prefs)
            val newList = currentList + providerWithId
            prefs[KEY_PROVIDERS_JSON] = json.encodeToString(ListSerializer(AiProviderEntity.serializer()), newList)
            // 如果是第一个提供商，自动设为激活
            if (currentList.isEmpty()) {
                prefs[KEY_ACTIVE_PROVIDER_ID] = providerWithId.id
            }
        }
        return providerWithId.id
    }

    /**
     * 更新已有的提供商配置
     */
    suspend fun updateProvider(provider: AiProviderEntity) {
        dataStore.edit { prefs ->
            val currentList = getCurrentProviders(prefs)
            val newList = currentList.map { if (it.id == provider.id) provider else it }
            prefs[KEY_PROVIDERS_JSON] = json.encodeToString(ListSerializer(AiProviderEntity.serializer()), newList)
        }
    }

    /**
     * 删除提供商配置
     * 如果删除的是当前激活的提供商，会自动选择第一个剩余提供商
     */
    suspend fun deleteProvider(providerId: String) {
        dataStore.edit { prefs ->
            val currentList = getCurrentProviders(prefs)
            val newList = currentList.filter { it.id != providerId }

            if (newList.isEmpty()) {
                // Remove the providers list entirely
                prefs.remove(KEY_PROVIDERS_JSON)
                // Clear active provider ID since no providers remain
                prefs.remove(KEY_ACTIVE_PROVIDER_ID)
            } else {
                // Update the providers list
                prefs[KEY_PROVIDERS_JSON] = json.encodeToString(ListSerializer(AiProviderEntity.serializer()), newList)
                // If the deleted provider was active, switch to the first remaining one
                if (prefs[KEY_ACTIVE_PROVIDER_ID] == providerId) {
                    prefs[KEY_ACTIVE_PROVIDER_ID] = newList.first().id
                }
            }
        }
    }

    /**
     * 设置当前激活的提供商
     */
    suspend fun setActiveProvider(providerId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROVIDER_ID] = providerId
        }
    }

    /**
     * 获取当前激活的提供商配置（挂起函数版本）
     */
    suspend fun getActiveConfig(): AiConfig = config.first()

    // ═══════════════════════════════════════════════════════════════
    // 旧版兼容接口（直接修改单配置字段，会同步到激活的提供商）
    // ═══════════════════════════════════════════════════════════════

    suspend fun setProvider(type: AiProviderType) {
        val current = config.first()
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(providerType = type))
        } else {
            // Legacy: write to old keys
            dataStore.edit { it[KEY_PROVIDER] = type.name }
        }
    }

    suspend fun setApiKey(key: String) {
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(apiKey = key))
        } else {
            dataStore.edit { it[KEY_API_KEY] = key }
        }
    }

    suspend fun setBaseUrl(url: String) {
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(baseUrl = url))
        } else {
            dataStore.edit { it[KEY_BASE_URL] = url }
        }
    }

    suspend fun setModel(model: String) {
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(modelName = model))
        } else {
            dataStore.edit { it[KEY_MODEL] = model }
        }
    }

    suspend fun setTemperature(temp: Float) {
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(temperature = temp))
        } else {
            dataStore.edit { it[KEY_TEMPERATURE] = temp.toString() }
        }
    }

    suspend fun setMaxTokens(tokens: Int) {
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(maxTokens = tokens))
        } else {
            dataStore.edit { it[KEY_MAX_TOKENS] = tokens.toString() }
        }
    }

    /**
     * 旧版接口：直接更新整个配置
     * 如果存在已保存的激活提供商，则更新它；否则写入旧版字段
     */
    suspend fun updateConfig(config: AiConfig) {
        val activeId = activeProviderId.first()
        val providers = savedProviders.first()
        val activeProvider = providers.find { it.id == activeId }

        if (activeProvider != null) {
            updateProvider(activeProvider.copy(
                providerType = config.providerType,
                apiKey = config.apiKey,
                baseUrl = config.baseUrl,
                modelName = config.modelName,
                temperature = config.temperature,
                maxTokens = config.maxTokens
            ))
        } else {
            dataStore.edit {
                it[KEY_PROVIDER] = config.providerType.name
                it[KEY_API_KEY] = config.apiKey
                it[KEY_BASE_URL] = config.baseUrl
                it[KEY_MODEL] = config.modelName
                it[KEY_TEMPERATURE] = config.temperature.toString()
                it[KEY_MAX_TOKENS] = config.maxTokens.toString()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 迁移逻辑：将旧版单配置迁移为多提供商
    // ═══════════════════════════════════════════════════════════════

    /**
     * 如果旧版配置存在但没有已保存提供商，自动迁移
     */
    suspend fun migrateFromLegacyIfNeeded() {
        val providers = savedProviders.first()
        if (providers.isNotEmpty()) return

        val prefs = dataStore.data.first()
        val hasLegacyConfig = prefs[KEY_API_KEY]?.isNotEmpty() == true ||
            prefs[KEY_BASE_URL]?.isNotEmpty() == true ||
            prefs[KEY_PROVIDER] != null

        if (hasLegacyConfig) {
            val legacyConfig = AiConfig(
                providerType = prefs[KEY_PROVIDER]?.let {
                    runCatching { AiProviderType.valueOf(it) }.getOrNull()
                } ?: AiProviderType.OPENAI,
                apiKey = prefs[KEY_API_KEY] ?: "",
                baseUrl = prefs[KEY_BASE_URL] ?: "",
                modelName = prefs[KEY_MODEL] ?: "",
                temperature = (prefs[KEY_TEMPERATURE] ?: "0.7").toFloatOrNull() ?: 0.7f,
                maxTokens = (prefs[KEY_MAX_TOKENS] ?: "4096").toIntOrNull() ?: 4096
            )

            if (legacyConfig.isConfigured()) {
                val providerName = legacyConfig.providerType.displayName
                val newProvider = AiProviderEntity(
                    id = AiProviderEntity.generateId(),
                    name = providerName,
                    providerType = legacyConfig.providerType,
                    apiKey = legacyConfig.apiKey,
                    baseUrl = legacyConfig.baseUrl,
                    modelName = legacyConfig.modelName,
                    temperature = legacyConfig.temperature,
                    maxTokens = legacyConfig.maxTokens
                )
                addProvider(newProvider)
            }
        }
    }

    // ── Helper ──
    private fun getCurrentProviders(prefs: Preferences): List<AiProviderEntity> {
        val jsonStr = prefs[KEY_PROVIDERS_JSON]
        return if (jsonStr.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(ListSerializer(AiProviderEntity.serializer()), jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_API_KEY = stringPreferencesKey("ai_api_key")
        private val KEY_BASE_URL = stringPreferencesKey("ai_base_url")
        private val KEY_MODEL = stringPreferencesKey("ai_model")
        private val KEY_TEMPERATURE = stringPreferencesKey("ai_temperature")
        private val KEY_MAX_TOKENS = stringPreferencesKey("ai_max_tokens")
        // New multi-provider keys
        private val KEY_PROVIDERS_JSON = stringPreferencesKey("ai_providers_json")
        private val KEY_ACTIVE_PROVIDER_ID = stringPreferencesKey("ai_active_provider_id")
    }
}
