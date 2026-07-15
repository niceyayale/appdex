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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.common.FormatUtil
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.SessionStatus
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexDivider
import com.appdex.ui.components.AppDexFindingCard
import com.appdex.ui.components.AppDexLoadingFlow
import com.appdex.ui.components.AppDexRow
import com.appdex.ui.components.AppDexScoreCard
import com.appdex.ui.components.AppDexSection
import com.appdex.ui.components.AppDexSessionCard
import com.appdex.ui.components.AppDexTimeline
import com.appdex.ui.components.TimelineStep
import com.appdex.ui.theme.AppDexTheme
import com.appdex.ui.theme.*

@Composable
fun TaskScreen(
    sessions: List<AnalysisSession>,
    currentSession: AnalysisSession?,
    isAnalyzing: Boolean,
    workflowProgress: Float = 0f,
    workflowStepLabel: String = "",
    onOpenApk: (Uri) -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenDexBrowser: (String) -> Unit,
    onOpenSigning: (String) -> Unit,
    onOpenRepack: (String) -> Unit,
    onOpenSecurity: (String) -> Unit,
    onOpenDetail: () -> Unit
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onOpenApk(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppDexTheme.colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 0.dp, bottom = BottomNavPadding
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { AppDexBar(title = "任务") }

        // ── 当前会话 ──
        if (isAnalyzing) {
            item {
                AppDexSection(label = "分析中") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppDexLoadingFlow(
                            message = workflowStepLabel.ifEmpty { "AI 正在分析 APK..." },
                            progress = workflowProgress
                        )
                        if (workflowStepLabel.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${(workflowProgress * 100).toInt()}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        } else if (currentSession != null && currentSession.apkInfo != null) {
            val session = currentSession
            val info = session.apkInfo!!

            // Session header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderLight)
                        .background(SurfaceDeep)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(IconBoxBlue),
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
                            tint = IconBlueBright
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.displayName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = buildString {
                                session.versionName.takeIf { it.isNotEmpty() }?.let { append(it) }
                                append(" · ")
                                append(FormatUtil.formatFileSize(session.fileSize))
                                if (info.manifest.minSdk > 0) append(" · API ${info.manifest.minSdk}+")
                            },
                            fontSize = 10.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Lifecycle timeline
            item {
                AppDexSection(label = "生命周期") {
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
                        modifier = Modifier.border(1.dp, BorderLight).background(SurfaceAlt).padding(16.dp)
                    ) {
                        AppDexTimeline(steps = steps)
                    }
                }
            }

            // Security score
            if (session.status.ordinal >= SessionStatus.READY.ordinal) {
                item {
                    AppDexScoreCard(
                        score = session.securityScore,
                        riskLevel = when {
                            session.securityScore >= 80 -> "低风险"
                            session.securityScore >= 60 -> "中风险"
                            session.securityScore >= 40 -> "高风险"
                            else -> "极高风险"
                        }
                    )
                }
            }

            // Findings
            if (session.findings.isNotEmpty()) {
                item {
                    AppDexSection(label = "分析发现 (${session.findings.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            session.findings.forEach { finding ->
                                AppDexFindingCard(
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

            // Quick actions
            item {
                AppDexSection(label = "快捷操作") {
                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                        AppDexRow(
                            icon = Icons.Default.Security,
                            title = "权限信息",
                            detail = "${info.manifest.permissions.size} 个权限 · ${info.manifest.activities.size} 个 Activity",
                            onClick = { onOpenSecurity(session.apkFilePath ?: "") }
                        )
                        AppDexDivider()
                        AppDexRow(
                            icon = Icons.Default.Code,
                            title = "代码结构",
                            detail = "${info.entries.count { it.name.endsWith(".dex") }} 个 DEX 文件",
                            onClick = { onOpenDexBrowser(session.apkFilePath ?: "") }
                        )
                        AppDexDivider()
                        AppDexRow(
                            icon = Icons.Default.VpnKey,
                            title = "签名验证",
                            detail = info.signatures.joinToString(", ") { "v${it.version}" }.ifEmpty { "无签名" },
                            onClick = { onOpenSigning(session.apkFilePath ?: "") }
                        )
                        AppDexDivider()
                        AppDexRow(
                            icon = Icons.Default.Build,
                            title = "重打包",
                            detail = "Smali → DEX → APK → 签名",
                            onClick = { onOpenRepack(session.apkFilePath ?: "") }
                        )
                        AppDexDivider()
                        AppDexRow(
                            icon = Icons.Default.Folder,
                            title = "APK 文件列表",
                            detail = "${info.entries.size} 个文件",
                            onClick = onOpenDetail
                        )
                    }
                }
            }
        } else {
            // No current session - show empty state with open button
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(IconBoxBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = IconBlue
                        )
                    }
                    Text(
                        text = "开始新任务",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "选择 APK 文件开始分析",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(44.dp)
                            .background(AmberGold)
                            .clickable {
                                filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择 APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGoldDark
                        )
                    }
                }
            }
        }

        // ── 所有会话 ──
        if (sessions.isNotEmpty()) {
            item {
                AppDexSection(label = "所有任务 (${sessions.size})") {
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
                                        SessionStatus.IDLE -> TextTertiary
                                        SessionStatus.LOADING -> AmberGold
                                        SessionStatus.ANALYZING -> AmberGold
                                        SessionStatus.SUMMARIZING -> AmberGold
                                        SessionStatus.READY -> AuroraGreen
                                        SessionStatus.MODIFIED -> NebulaBlue
                                        SessionStatus.REPACKED -> NebulaBlue
                                        SessionStatus.SIGNED -> AmberGold
                                        SessionStatus.INSTALLED -> AuroraGreen
                                    }
                                    AppDexSessionCard(
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
                                        tint = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
