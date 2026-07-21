package com.appdex.data.ai

import kotlinx.serialization.Serializable

/**
 * 已保存的 AI 提供商配置实体
 *
 * 支持用户保存多个提供商配置，并从中选择一个作为当前激活的提供商。
 */
@Serializable
data class AiProviderEntity(
    val id: String,
    val name: String,
    val providerType: AiProviderType,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 转换为 AiConfig 用于 AI 服务调用
     */
    fun toAiConfig(): AiConfig = AiConfig(
        providerType = providerType,
        apiKey = apiKey,
        baseUrl = baseUrl,
        modelName = modelName,
        temperature = temperature,
        maxTokens = maxTokens
    )

    /**
     * 是否已配置完成（至少有必要的字段）
     */
    fun isConfigured(): Boolean = toAiConfig().isConfigured()

    companion object {
        fun generateId(): String = java.util.UUID.randomUUID().toString()
    }
}
