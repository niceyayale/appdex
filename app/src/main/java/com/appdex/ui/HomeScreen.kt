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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.common.FormatUtil
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.SessionStatus
import com.appdex.data.toolbridge.QuickAction
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexDivider
import com.appdex.ui.components.AppDexHero
import com.appdex.ui.components.AppDexSection
import com.appdex.ui.components.AppDexSessionCard
import com.appdex.ui.theme.AppDexTheme
import com.appdex.ui.theme.*

private fun quickActionIcon(iconType: String): ImageVector = when (iconType.lowercase()) {
    "scan", "analyze" -> Icons.Default.Devices
    "chat", "ai" -> Icons.Default.Psychology
    "edit" -> Icons.Default.Edit
    "key", "signing" -> Icons.Default.VpnKey
    "folder", "files" -> Icons.Default.Folder
    "security" -> Icons.Default.Security
    else -> Icons.Default.Apps
}

@Composable
fun HomeScreen(
    sessions: List<AnalysisSession>,
    suggestedQuestions: List<Pair<String, String>> = emptyList(),
    quickActions: List<QuickAction> = emptyList(),
    onAnalyzeApk: (Uri) -> Unit,
    onScanInstalled: () -> Unit,
    onOpenSession: (String) -> Unit,
    onNavigateToAi: () -> Unit,
    onAskQuestion: (String) -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    onNavigateToTask: () -> Unit
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onAnalyzeApk(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppDexTheme.colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 0.dp, bottom = BottomNavPadding
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { AppDexBar(title = "AppDex") }

        // ── Hero ──
        item {
            AppDexHero(
                title = "AppDex",
                subtitle = "AI 驱动的 APK 分析平台",
                version = "2.0",
                onAnalyze = {
                    filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                }
            )
        }

        // ── 快速操作 — 横向滚动 ──
        if (quickActions.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickActions) { action ->
                        val icon = quickActionIcon(action.iconType)
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .border(1.dp, BorderLight)
                                .background(SurfaceAlt)
                                .clickable { onQuickAction(action) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(IconBoxBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = IconBlue
                                    )
                                }
                                Text(
                                    text = action.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                                Text(
                                    text = action.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, BorderLight)
                            .background(SurfaceAlt)
                            .clickable(onClick = onScanInstalled)
                            .padding(12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(IconBoxBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Devices,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = IconBlue
                                )
                            }
                            Text(
                                text = "扫描已安装应用",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "从设备中选择",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, BorderLight)
                            .background(SurfaceAlt)
                            .clickable(onClick = onNavigateToAi)
                            .padding(12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(IconBoxBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = IconBlue
                                )
                            }
                            Text(
                                text = "问 AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "智能分析助手",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 最近任务 ──
        item {
            AppDexSection(label = "最近任务") {
                if (sessions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight)
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = AsteroidBelt
                        )
                        Text(
                            text = "暂无分析任务",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "选择 APK 开始分析",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sessions.take(5).forEach { session ->
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
                                onClick = { onOpenSession(session.id) }
                            )
                        }
                    }
                }
            }
        }

        // ── AI 推荐问题 ──
        if (suggestedQuestions.isNotEmpty()) {
            item {
                AppDexSection(label = "AI 推荐问题") {
                    Column(
                        modifier = Modifier.border(1.dp, BorderLight)
                    ) {
                        suggestedQuestions.forEachIndexed { index, (q, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAskQuestion(q) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = IconBlue
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = q,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TextTertiary
                                )
                            }
                            if (index < suggestedQuestions.size - 1) {
                                AppDexDivider()
                            }
                        }
                    }
                }
            }
        }

        // ── 查看全部任务 ──
        if (sessions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderMedium)
                        .clickable(onClick = onNavigateToTask)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "查看全部任务",
                        fontSize = 12.sp,
                        color = AmberGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AmberGold
                    )
                }
            }
        }
    }
}
