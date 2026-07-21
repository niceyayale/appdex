package com.appdex.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.common.FormatUtil
import com.appdex.apk.ApkInfo
import com.appdex.apk.ApkManifest
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.SessionStatus
import com.appdex.data.session.ToolDisplayMode
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.bounceClick
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXFindingCard
import com.appdex.ui.components.AppXLoadingFlow
import com.appdex.ui.components.AppXRow
import com.appdex.ui.components.AppXScoreCard
import com.appdex.ui.components.AppXSection
import com.appdex.ui.components.AppXSessionCard
import com.appdex.ui.components.AppXTimeline
import com.appdex.ui.components.CopilotButton
import com.appdex.ui.components.TimelineStep
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// AppX Workspace — APK Analysis Dashboard
// ═══════════════════════════════════════════════════════════════
// "The workspace is not a file manager.
//  It's a mission control for your APK."
// ═══════════════════════════════════════════════════════════════

@Composable
fun WorkspaceScreen(
    sessions: List<AnalysisSession>,
    currentSession: AnalysisSession?,
    isAnalyzing: Boolean,
    workflowProgress: Float = 0f,
    workflowStepLabel: String = "",
    displayMode: ToolDisplayMode = ToolDisplayMode.NORMAL,
    onOpenApk: (Uri) -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenDexBrowser: (String) -> Unit,
    onOpenSigning: (String) -> Unit,
    onOpenRepack: (String) -> Unit,
    onOpenSecurity: (String) -> Unit,
    onOpenDetail: () -> Unit,
    onOpenReport: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenTools: () -> Unit = {},
    onOpenAxmlEditor: (String) -> Unit = {},
    onOpenArscViewer: (String) -> Unit = {},
    onNavigateToAi: () -> Unit = {},
    onAskAi: (String) -> Unit = {}
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onOpenApk(uri)
    }

    val c = AppXTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.background)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 0.dp, bottom = BottomNavPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "工作区",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick AI access
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, c.borderMedium)
                            .background(c.surfaceAlt)
                            .bounceClick(onClick = onNavigateToAi),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "AI",
                            modifier = Modifier.size(18.dp),
                            tint = c.iconBlueBright
                        )
                    }
                }
            }
        }

        // ── Analyzing State ──
        if (isAnalyzing) {
            item {
                AppXSection(label = "分析中") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppXLoadingFlow(
                            message = workflowStepLabel.ifEmpty { "AI 正在分析 APK..." },
                            progress = workflowProgress
                        )
                        if (workflowStepLabel.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${(workflowProgress * 100).toInt()}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = c.amberGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // ── Current Session Dashboard ──
        else if (currentSession != null) {
            val session = currentSession
            // 保存的会话可能没有缓存的 apkInfo（Bitmap/复杂结构未持久化），
            // 用空壳对象优雅降级，保证工作区仪表盘始终可渲染。
            val info = session.apkInfo ?: ApkInfo(
                manifest = ApkManifest(
                    packageName = session.packageName,
                    versionName = session.versionName,
                    versionCode = 0,
                    minSdk = 0,
                    targetSdk = 0,
                    permissions = emptyList(),
                    activities = emptyList(),
                    services = emptyList(),
                    receivers = emptyList(),
                    providers = emptyList(),
                    metaData = emptyMap()
                ),
                signatures = emptyList(),
                entries = emptyList(),
                fileSize = session.fileSize
            )

            // ── P0-2: APK Overview Card ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, c.borderLight)
                        .background(c.surfaceDeep)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(c.iconBoxBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            session.appIcon?.let { bitmap ->
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(48.dp)
                                )
                            } ?: Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = c.iconBlueBright
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.displayName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.textPrimary
                            )
                            Text(
                                text = buildString {
                                    session.versionName.takeIf { it.isNotEmpty() }?.let { append("v$it") }
                                    append(" · ")
                                    append(FormatUtil.formatFileSize(session.fileSize))
                                    if (info.manifest.minSdk > 0) append(" · API ${info.manifest.minSdk}+")
                                },
                                fontSize = 10.sp,
                                color = c.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        // Security score badge
                        if (session.status.ordinal >= SessionStatus.READY.ordinal) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        when {
                                            session.securityScore >= 80 -> c.auroraGreen.copy(alpha = 0.15f)
                                            session.securityScore >= 60 -> c.amberGold.copy(alpha = 0.15f)
                                            else -> c.redSupergiant.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${session.securityScore}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        session.securityScore >= 80 -> c.auroraGreen
                                        session.securityScore >= 60 -> c.amberGold
                                        else -> c.redSupergiant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── P0-1: Quick Actions (visible in all modes) ──
            item {
                AppXSection(label = "快捷操作") {
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        AppXRow(
                            icon = Icons.Default.Description,
                            title = if (displayMode == ToolDisplayMode.EXPERT) "AXML Editor" else "查看应用配置",
                            detail = "AndroidManifest.xml",
                            onClick = { onOpenAxmlEditor(session.apkFilePath ?: "") }
                        )
                        AppXDivider()
                        AppXRow(
                            icon = Icons.Default.Code,
                            title = if (displayMode == ToolDisplayMode.EXPERT) "DEX Browser" else "查看代码结构",
                            detail = if (session.apkInfo != null) "${info.entries.count { it.name.endsWith(".dex") }} 个 DEX 文件" else "${session.summary.dexCount} 个 DEX 文件",
                            onClick = { onOpenDexBrowser(session.apkFilePath ?: "") }
                        )
                        AppXDivider()
                        AppXRow(
                            icon = Icons.Default.Folder,
                            title = if (displayMode == ToolDisplayMode.EXPERT) "ARSC Viewer" else "查看资源",
                            detail = if (session.apkInfo != null) "${info.entries.count { it.name.startsWith("res/") }} 个资源文件" else "${session.summary.resourceCount} 个资源文件",
                            onClick = { onOpenArscViewer(session.apkFilePath ?: "") }
                        )
                        AppXDivider()
                        AppXRow(
                            icon = Icons.Default.VpnKey,
                            title = if (displayMode == ToolDisplayMode.EXPERT) "Signing" else "重新签名",
                            detail = if (session.apkInfo != null) info.signatures.joinToString(", ") { "v${it.version}" }.ifEmpty { "无签名" } else if (session.summary.hasSignature) "v${session.summary.signatureVersion}" else "无签名",
                            onClick = { onOpenSigning(session.apkFilePath ?: "") }
                        )
                        AppXDivider()
                        AppXRow(
                            icon = Icons.Default.Build,
                            title = if (displayMode == ToolDisplayMode.EXPERT) "Repack" else "重新打包",
                            detail = "Smali → DEX → APK → 签名",
                            onClick = { onOpenRepack(session.apkFilePath ?: "") }
                        )
                    }
                }
            }

            // ── Security & Report (visible in all modes) ──
            item {
                AppXSection(label = "分析") {
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        AppXRow(
                            icon = Icons.Default.Security,
                            title = "安全分析",
                            detail = if (session.apkInfo != null) "${info.manifest.permissions.size} 个权限 · ${info.manifest.activities.size} 个 Activity" else "${session.summary.permissionCount} 个权限 · ${session.summary.activityCount} 个 Activity",
                            onClick = { onOpenSecurity(session.apkFilePath ?: "") }
                        )
                        AppXDivider()
                        AppXRow(
                            icon = Icons.Default.Assessment,
                            title = "查看报告",
                            detail = "执行报告 · 可分享",
                            onClick = onOpenReport
                        )
                        AppXDivider()
                        AppXRow(
                            icon = Icons.Default.Folder,
                            title = "APK 文件列表",
                            detail = if (session.apkInfo != null) "${info.entries.size} 个文件" else "${session.summary.dexCount + session.summary.resourceCount + session.summary.nativeLibCount} 个文件",
                            onClick = onOpenDetail
                        )
                    }
                }
            }

            // Lifecycle timeline
            item {
                AppXSection(label = "生命周期") {
                    val steps = listOf(
                        TimelineStep("选择 APK", "已选择 ${session.displayName}", isCompleted = true),
                        TimelineStep("自动分析", "提取清单、签名、权限、文件结构",
                            isCompleted = session.status != SessionStatus.IDLE,
                            isCurrent = session.status == SessionStatus.ANALYZING
                        ),
                        TimelineStep("生成报告", "${session.findings.size} 项发现，评分 ${session.securityScore}",
                            isCompleted = session.status.ordinal >= SessionStatus.READY.ordinal,
                            isCurrent = session.status == SessionStatus.READY
                        ),
                        TimelineStep("修改 APK", "编辑、重打包",
                            isCompleted = session.status.ordinal >= SessionStatus.MODIFIED.ordinal,
                            isCurrent = session.status == SessionStatus.MODIFIED
                        ),
                        TimelineStep("签名", "V2/V3 签名方案",
                            isCompleted = session.status.ordinal >= SessionStatus.SIGNED.ordinal,
                            isCurrent = session.status == SessionStatus.SIGNED
                        ),
                        TimelineStep("安装", "安装到设备",
                            isCompleted = session.status == SessionStatus.INSTALLED,
                            isCurrent = session.status == SessionStatus.INSTALLED
                        )
                    )
                    Column(
                        modifier = Modifier.border(1.dp, c.borderLight).background(c.surfaceAlt).padding(16.dp)
                    ) {
                        AppXTimeline(steps = steps)
                    }
                }
            }

            // Security score (if ready)
            if (session.status.ordinal >= SessionStatus.READY.ordinal) {
                item {
                    AppXScoreCard(
                        score = session.securityScore,
                        riskLevel = com.appdex.data.session.RiskScoreCalculator.getRiskLevel(session.securityScore)
                    )
                }
            }

            // Findings
            if (session.findings.isNotEmpty()) {
                item {
                    AppXSection(label = "分析发现 (${session.findings.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            session.findings.forEach { finding ->
                                AppXFindingCard(
                                    severity = when (finding.severity) {
                                        com.appdex.data.session.FindingSeverity.CRITICAL -> "严重"
                                        com.appdex.data.session.FindingSeverity.HIGH -> "高危"
                                        com.appdex.data.session.FindingSeverity.MEDIUM -> "中等"
                                        com.appdex.data.session.FindingSeverity.LOW -> "低"
                                        com.appdex.data.session.FindingSeverity.INFO -> "信息"
                                    },
                                    category = finding.category,
                                    title = finding.title,
                                    description = finding.description,
                                    recommendation = finding.recommendation,
                                    onAction = finding.toolAction?.let { _ -> { onOpenDetail() } }
                                )
                            }
                        }
                    }
                }
            }

            // ── Advanced Tools (hidden in Normal mode) ──
            if (displayMode != ToolDisplayMode.NORMAL) {
                item {
                    AppXSection(label = if (displayMode == ToolDisplayMode.EXPERT) "高级工具" else "更多工具") {
                        Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                            if (displayMode == ToolDisplayMode.EXPERT) {
                                AppXRow(
                                    icon = Icons.Default.Folder,
                                    title = "文件管理器",
                                    detail = "浏览设备文件",
                                    onClick = onOpenFiles
                                )
                                AppXDivider()
                                AppXRow(
                                    icon = Icons.Default.Build,
                                    title = "工具集",
                                    detail = "终端 · 编辑器 · 对比 · SQLite · ELF · HEX",
                                    onClick = onOpenTools
                                )
                            }
                        }
                    }
                }
            }

            // Ask AI prompt — sends context-aware query and navigates to AI
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, c.iconBlueBright.copy(alpha = 0.3f))
                        .background(c.aiGradientStart.copy(alpha = 0.3f))
                        .bounceClick {
                            onAskAi("请基于当前 APK 的分析结果，给出安全评估和改进建议")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = c.iconBlueBright
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "问 AI 关于这个 APK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.iconBlueBright
                        )
                        Text(
                            text = "AI 已了解完整上下文，随时为你解答",
                            fontSize = 10.sp,
                            color = c.textSecondary
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = c.iconBlueBright
                    )
                }
            }
        }

        // ── Empty State ──
        else {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(c.iconBoxBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = c.iconBlue
                        )
                    }
                    Text(
                        text = "开始新任务",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "选择 APK 文件开始分析",
                        fontSize = 11.sp,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(44.dp)
                            .background(c.amberGold)
                            .bounceClick {
                                filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择 APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.amberGoldDark
                        )
                    }
                }
            }
        }

        // ── All Sessions ──
        if (sessions.isNotEmpty()) {
            item {
                AppXSection(label = "所有工作区 (${sessions.size})") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sessions.forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    val statusText = when (session.status) {
                                        SessionStatus.IDLE -> "待分析"
                                        SessionStatus.LOADING -> "读取中"
                                        SessionStatus.ANALYZING -> "分析中"
                                        SessionStatus.SUMMARIZING -> "总结中"
                                        SessionStatus.READY -> "已完成"
                                        SessionStatus.MODIFIED -> "已修改"
                                        SessionStatus.REPACKED -> "已打包"
                                        SessionStatus.SIGNED -> "已签名"
                                        SessionStatus.INSTALLED -> "已安装"
                                    }
                                    val statusColor = when (session.status) {
                                        SessionStatus.IDLE -> c.textTertiary
                                        SessionStatus.LOADING, SessionStatus.ANALYZING, SessionStatus.SUMMARIZING -> c.amberGold
                                        SessionStatus.READY, SessionStatus.INSTALLED -> c.auroraGreen
                                        SessionStatus.MODIFIED, SessionStatus.REPACKED -> c.nebulaBlue
                                        SessionStatus.SIGNED -> c.amberGold
                                    }
                                    AppXSessionCard(
                                        title = session.displayName,
                                        subtitle = if (session.versionName.isNotEmpty())
                                            "${session.versionName} · ${FormatUtil.formatFileSize(session.fileSize)}"
                                        else FormatUtil.formatFileSize(session.fileSize),
                                        score = session.securityScore,
                                        statusText = statusText,
                                        statusColor = statusColor,
                                        onClick = { onSelectSession(session.id) }
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(16.dp),
                                        tint = c.textTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // ── Floating Copilot ──
        CopilotButton(
            onClick = onNavigateToAi,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
        )
    }
}
