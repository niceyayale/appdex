package com.appdex.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 可持久化的会话元数据 — 仅包含基本字段，不包含 Bitmap/ApkInfo
 */
@Serializable
data class SessionMetadata(
    val id: String,
    val apkFilePath: String? = null,
    val packageName: String = "",
    val versionName: String = "",
    val fileSize: Long = 0L,
    val status: String = "IDLE",
    val securityScore: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "appdex_sessions")

/**
 * 会话持久化仓库 — 将会话元数据保存到 DataStore
 *
 * 保存内容：包名、版本、文件大小、状态、评分、APK 路径
 * 不保存：Bitmap（太大）、ApkInfo（结构复杂，可从 APK 重新解析）
 */
@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.sessionDataStore

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * 加载所有已保存的会话元数据
     */
    suspend fun loadSessions(): List<SessionMetadata> {
        val prefs = dataStore.data.first()
        val jsonStr = prefs[KEY_SESSIONS] ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(SessionMetadata.serializer()), jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存所有会话元数据
     */
    suspend fun saveSessions(sessions: List<SessionMetadata>) {
        val jsonStr = json.encodeToString(ListSerializer(SessionMetadata.serializer()), sessions)
        dataStore.edit { it[KEY_SESSIONS] = jsonStr }
    }

    /**
     * 保存单个会话（更新或新增）
     */
    suspend fun saveSession(metadata: SessionMetadata) {
        val current = loadSessions().toMutableList()
        val index = current.indexOfFirst { it.id == metadata.id }
        if (index >= 0) {
            current[index] = metadata
        } else {
            current.add(metadata)
        }
        saveSessions(current)
    }

    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: String) {
        val current = loadSessions().toMutableList()
        current.removeAll { it.id == sessionId }
        saveSessions(current)
    }

    /**
     * 清空所有会话
     */
    suspend fun clearAll() {
        dataStore.edit { it.remove(KEY_SESSIONS) }
    }

    companion object {
        private val KEY_SESSIONS = stringPreferencesKey("sessions_json")
    }
}

/**
 * 扩展函数：将 AnalysisSession 转换为可持久化的 SessionMetadata
 */
fun AnalysisSession.toMetadata(): SessionMetadata = SessionMetadata(
    id = id,
    apkFilePath = apkFilePath,
    packageName = packageName,
    versionName = versionName,
    fileSize = fileSize,
    status = status.name,
    securityScore = securityScore,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * 扩展函数：从 SessionMetadata 恢复 AnalysisSession（不含 ApkInfo 和 Bitmap）
 */
fun SessionMetadata.toSession(): AnalysisSession = AnalysisSession(
    id = id,
    apkFilePath = apkFilePath,
    packageName = packageName,
    versionName = versionName,
    fileSize = fileSize,
    status = runCatching { SessionStatus.valueOf(status) }.getOrNull() ?: SessionStatus.IDLE,
    securityScore = securityScore,
    createdAt = createdAt,
    updatedAt = updatedAt
)
