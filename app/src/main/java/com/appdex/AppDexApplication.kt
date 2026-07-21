package com.appdex

import android.app.Application
import android.util.Log
import com.appdex.data.ai.AiConfig
import com.appdex.data.ai.AiConfigRepository
import com.appdex.data.ai.AiProviderType
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AppXApplication : Application() {

    @Inject lateinit var aiConfigRepo: AiConfigRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
        seedDebugAiConfig()
    }

    /**
     * Debug 模式下自动从 assets/ai_config.md 读取并填充 AI 配置。
     * 仅在 API Key 为空时填充，不覆盖用户已修改的配置。
     */
    private fun seedDebugAiConfig() {
        if (!BuildConfig.DEBUG) return
        appScope.launch {
            try {
                val existing = aiConfigRepo.config.first()
                if (existing.apiKey.isNotEmpty()) return@launch

                val configText = readAsset("ai_config.md") ?: return@launch
                val seed = parseAiConfig(configText)
                if (seed.apiKey.isEmpty()) return@launch

                aiConfigRepo.updateConfig(
                    AiConfig(
                        providerType = seed.provider,
                        apiKey = seed.apiKey,
                        baseUrl = seed.baseUrl,
                        modelName = seed.model,
                        temperature = 0.7f,
                        maxTokens = 4096
                    )
                )
                Log.d("AppX", "Debug AI config seeded: provider=${seed.provider}, model=${seed.model}")
            } catch (e: Exception) {
                Log.w("AppX", "Failed to seed debug AI config", e)
            }
        }
    }

    private fun readAsset(name: String): String? {
        return try {
            assets.open(name).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析 ai_config.md 格式:
     * url:https://openrouter.ai/api/v1/chat/completions
     * model:tencent/hy3:free
     * api_key:sk-or-v1-xxx
     */
    private fun parseAiConfig(text: String): AiConfigSeed {
        var url = ""
        var model = ""
        var apiKey = ""
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val colonIdx = trimmed.indexOf(':')
            if (colonIdx <= 0) return@forEach
            val key = trimmed.substring(0, colonIdx).trim().lowercase()
            val value = trimmed.substring(colonIdx + 1).trim()
            when (key) {
                "url" -> url = value
                "model" -> model = value
                "api_key" -> apiKey = value
            }
        }

        val provider = when {
            url.contains("openrouter.ai") -> AiProviderType.OPENROUTER
            url.contains("openai.com") -> AiProviderType.OPENAI
            url.contains("anthropic.com") -> AiProviderType.ANTHROPIC
            url.contains("deepseek.com") -> AiProviderType.DEEPSEEK
            url.contains("generativelanguage.googleapis.com") -> AiProviderType.GEMINI
            url.contains("localhost") || url.contains("127.0.0.1") -> {
                when {
                    url.contains("11434") -> AiProviderType.OLLAMA
                    url.contains("1234") -> AiProviderType.LM_STUDIO
                    url.contains("8080") -> AiProviderType.LOCALAI
                    url.contains("3001") -> AiProviderType.ANYTHINGLLM
                    else -> AiProviderType.OPENAI_COMPATIBLE
                }
            }
            else -> AiProviderType.OPENAI_COMPATIBLE
        }

        val baseUrl = url
            .removeSuffix("/chat/completions")
            .removeSuffix("/messages")
            .removeSuffix("/")

        return AiConfigSeed(provider, baseUrl, model, apiKey)
    }

    private data class AiConfigSeed(
        val provider: AiProviderType,
        val baseUrl: String,
        val model: String,
        val apiKey: String
    )

    private fun setupGlobalExceptionHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppXCrash", "Uncaught exception on ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
