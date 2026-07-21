package com.appdex.security

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviIntent
import com.appdex.arch.MviState
import com.appdex.arch.MviEffect
import com.appdex.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SecurityScannerIntent : MviIntent {
    data class SetApkPath(val path: String) : SecurityScannerIntent
    data object Scan : SecurityScannerIntent
    data object Reset : SecurityScannerIntent
}

data class SecurityScannerState(
    val apkPath: String = "",
    val apkName: String = "",
    val isScanning: Boolean = false,
    val scanResult: SecurityScanResult? = null,
    /** 统一评分 — 来自 SessionManager，与 Workspace/Report 页面完全一致 */
    val unifiedScore: Int = 100,
    val error: String? = null,
) : MviState

sealed interface SecurityScannerEffect : MviEffect {
    data class ShowError(val message: String) : SecurityScannerEffect
    data class ScanComplete(val result: SecurityScanResult) : SecurityScannerEffect
}

@HiltViewModel
class SecurityScannerViewModel @Inject constructor(
    private val scannerRepository: SecurityScannerRepository,
    private val sessionManager: SessionManager,
    private val workspaceEventBus: com.appdex.data.workspace.WorkspaceEventBus,
) : BaseViewModel<SecurityScannerIntent, SecurityScannerState, SecurityScannerEffect>(
    initialState = SecurityScannerState()
) {
    override fun handleIntent(intent: SecurityScannerIntent) {
        when (intent) {
            is SecurityScannerIntent.SetApkPath -> update {
                it.copy(apkPath = intent.path, apkName = java.io.File(intent.path).name)
            }
            is SecurityScannerIntent.Scan -> scan()
            is SecurityScannerIntent.Reset -> update { SecurityScannerState() }
        }
    }

    private fun scan() {
        update { it.copy(isScanning = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = scannerRepository.scan(currentState.apkPath)

                // ── 统一评分同步：将扫描结果同步到 SessionManager ──
                // 所有页面读取同一个 Session.securityScore
                val sessionId = sessionManager.currentSessionId.value
                if (sessionId != null) {
                    // 将 SecurityIssue 转换为 AnalysisFinding 并更新 session
                    val findings = result.issues.map { issue ->
                        com.appdex.data.session.AnalysisFinding(
                            severity = when (issue.severity) {
                                Severity.CRITICAL -> com.appdex.data.session.FindingSeverity.CRITICAL
                                Severity.HIGH -> com.appdex.data.session.FindingSeverity.HIGH
                                Severity.MEDIUM -> com.appdex.data.session.FindingSeverity.MEDIUM
                                Severity.LOW -> com.appdex.data.session.FindingSeverity.LOW
                                Severity.INFO -> com.appdex.data.session.FindingSeverity.INFO
                            },
                            category = issue.type.name,
                            title = issue.title,
                            description = issue.description,
                            recommendation = issue.recommendation,
                            toolAction = "security"
                        )
                    }
                    sessionManager.setFindings(sessionId, findings, result.securityScore)
                    // RC4: Record workspace timeline — Security scan complete
                    sessionManager.reportWorkspaceAction(
                        sessionId = sessionId,
                        panel = "Security",
                        action = "安全扫描完成: 评分 ${result.securityScore}/100",
                        timelineType = "SECURITY",
                        timelineTitle = "安全扫描完成",
                        timelineDetail = "评分 ${result.securityScore}/100, ${result.issues.size} 个问题"
                    )
                }

                update {
                    it.copy(
                        isScanning = false,
                        scanResult = result,
                        unifiedScore = result.securityScore
                    )
                }
                // Phase 2: Emit SecurityUpdated event to Workspace OS
                workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.SecurityUpdated(
                    score = result.securityScore,
                    findingCount = result.issues.size
                ))
                emitEffect(SecurityScannerEffect.ScanComplete(result))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "扫描失败"
                update { it.copy(isScanning = false, error = msg) }
                emitEffect(SecurityScannerEffect.ShowError(msg))
            }
        }
    }
}
