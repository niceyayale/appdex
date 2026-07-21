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
 * 可持久化的会话元数据 — 包含基本字段和摘要信息，不包含 Bitmap/ApkInfo
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
    val updatedAt: Long = System.currentTimeMillis(),
    // ── RC3: 增强持久化字段 ──
    val dexCount: Int = 0,
    val resourceCount: Int = 0,
    val nativeLibCount: Int = 0,
    val hasSignature: Boolean = false,
    val signatureVersion: Int = 0,
    val permissionCount: Int = 0,
    val dangerousPermissionCount: Int = 0,
    val activityCount: Int = 0,
    val serviceCount: Int = 0,
    val receiverCount: Int = 0,
    val providerCount: Int = 0,
    val findingCount: Int = 0,
    val lastTool: String = "",
    val lastRoute: String = ""
)

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "AppX_sessions")

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
 * 提取 ApkInfo 中的摘要信息，确保重启后可恢复完整工作区
 */
fun AnalysisSession.toMetadata(): SessionMetadata {
    val info = apkInfo
    val manifest = info?.manifest
    val sigs = info?.signatures ?: emptyList()
    val entries = info?.entries ?: emptyList()

    // RC4: When apkInfo is null (restored session), use summary as fallback
    // This prevents "0 DEX, 0 permissions" after app restart
    val dexCount = if (info != null) entries.count { it.name.endsWith(".dex") } else summary.dexCount
    val resourceCount = if (info != null) entries.count { it.name.endsWith(".xml") || it.name.endsWith(".png") || it.name.endsWith(".arsc") } else summary.resourceCount
    val nativeLibCount = if (info != null) entries.count { it.name.endsWith(".so") } else summary.nativeLibCount

    // 签名信息
    val hasSignature = if (info != null) sigs.isNotEmpty() else summary.hasSignature
    val signatureVersion = if (info != null) (sigs.maxOfOrNull { it.version } ?: 0) else summary.signatureVersion

    // 权限统计
    val permissions = manifest?.permissions ?: emptyList()
    val permissionCount = if (info != null) permissions.size else summary.permissionCount
    val dangerousPermissionCount = if (info != null) permissions.count { it in DANGEROUS_PERMISSIONS_PERSIST } else summary.dangerousPermissionCount

    // 组件统计
    val activityCount = if (info != null) (manifest?.activities?.size ?: 0) else summary.activityCount
    val serviceCount = if (info != null) (manifest?.services?.size ?: 0) else summary.serviceCount
    val receiverCount = if (info != null) (manifest?.receivers?.size ?: 0) else summary.receiverCount
    val providerCount = if (info != null) (manifest?.providers?.size ?: 0) else summary.providerCount

    return SessionMetadata(
        id = id,
        apkFilePath = apkFilePath,
        packageName = packageName,
        versionName = versionName,
        fileSize = fileSize,
        status = status.name,
        securityScore = securityScore,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dexCount = dexCount,
        resourceCount = resourceCount,
        nativeLibCount = nativeLibCount,
        hasSignature = hasSignature,
        signatureVersion = signatureVersion,
        permissionCount = permissionCount,
        dangerousPermissionCount = dangerousPermissionCount,
        activityCount = activityCount,
        serviceCount = serviceCount,
        receiverCount = receiverCount,
        providerCount = providerCount,
        findingCount = if (findings.isNotEmpty()) findings.size else summary.findingCount,
        lastTool = lastTool,
        lastRoute = lastRoute
    )
}

// 危险权限列表（用于持久化时统计）
private val DANGEROUS_PERMISSIONS_PERSIST = setOf(
    "android.permission.READ_SMS", "android.permission.SEND_SMS",
    "android.permission.RECEIVE_SMS", "android.permission.RECEIVE_MMS",
    "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
    "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
    "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
    "android.permission.WRITE_SETTINGS", "android.permission.REBOOT",
    "android.permission.INSTALL_PACKAGES", "android.permission.DELETE_PACKAGES"
)

/**
 * 扩展函数：从 SessionMetadata 恢复 AnalysisSession（不含 ApkInfo 和 Bitmap）
 * 使用持久化的摘要数据填充 summary，确保重启后工作区显示完整信息
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
    updatedAt = updatedAt,
    summary = SessionSummary(
        dexCount = dexCount,
        resourceCount = resourceCount,
        nativeLibCount = nativeLibCount,
        hasSignature = hasSignature,
        signatureVersion = signatureVersion,
        permissionCount = permissionCount,
        dangerousPermissionCount = dangerousPermissionCount,
        activityCount = activityCount,
        serviceCount = serviceCount,
        receiverCount = receiverCount,
        providerCount = providerCount,
        findingCount = findingCount
    ),
    lastTool = lastTool,
    lastRoute = lastRoute
)
