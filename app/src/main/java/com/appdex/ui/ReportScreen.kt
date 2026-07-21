package com.appdex.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.apk.ApkInfo
import com.appdex.common.FormatUtil
import com.appdex.data.session.AnalysisFinding
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.AiMessage
import com.appdex.data.session.AiRole
import com.appdex.data.session.FindingSeverity
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.bounceClick
import com.appdex.ui.components.CopilotButton
import com.appdex.ui.components.EmptyState
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

@Composable
fun ReportScreen(
    session: AnalysisSession?,
    onBack: () -> Unit = {},
    onAskAi: (String) -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToDex: () -> Unit = {},
    onNavigateToManifest: () -> Unit = {},
    onNavigateToSigning: () -> Unit = {},
    onNavigateToReport: () -> Unit = {}
) {
    val context = LocalContext.current

    // Phase 4: Live Report — observe Workspace state for real-time updates
    val workspaceController = com.appdex.ui.components.LocalWorkspaceController.current
    val workspaceState by (workspaceController?.state
        ?: kotlinx.coroutines.flow.MutableStateFlow(com.appdex.data.workspace.WorkspaceObject()))
        .collectAsStateWithLifecycle(initialValue = com.appdex.data.workspace.WorkspaceObject())
    val reportRevision by (workspaceController?.reportRevision
        ?: kotlinx.coroutines.flow.MutableStateFlow(0))
        .collectAsStateWithLifecycle(initialValue = 0)

    // Use workspace state for live score/findings; fall back to session
    val liveScore = workspaceState.securityScore.let { if (it > 0) it else session?.securityScore ?: 100 }
    val liveFindings = workspaceState.findings.ifEmpty { session?.findings ?: emptyList() }
    val liveFlags = workspaceState.flags

    Box(modifier = Modifier.fillMaxSize().background(AppXTheme.colors.background)) {
        if (session == null) {
            EmptyState(
                icon = Icons.Default.Assessment,
                title = "暂无分析报告",
                subtitle = "请先分析 APK 以生成报告"
            )
            return@Box
        }

        // 保存的会话可能没有缓存的 apkInfo（未持久化），用空壳降级
        val info = session.apkInfo ?: ApkInfo(
            manifest = com.appdex.apk.ApkManifest(
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
        val manifest = info.manifest
        val c = AppXTheme.colors

        // ── One-sentence executive summary (Phase 4: Live) ──
        val executiveSummary = when {
            liveScore >= 80 -> "这款应用整体安全，可以放心使用。"
            liveScore >= 60 -> "这款应用存在一些安全顾虑，建议仔细检查。"
            liveScore >= 40 -> "这款应用存在较高安全风险，谨慎使用。"
            else -> "这款应用存在严重安全风险，不建议安装。"
        }

        // ── Score color (Phase 4: Live) ──
        val scoreColor = when {
            liveScore >= 80 -> c.auroraGreen
            liveScore >= 60 -> c.amberGold
            liveScore >= 40 -> Color(0xFFFF9800)
            else -> c.redSupergiant
        }

        // Phase 4: Live status badge
        val liveStatusText = when {
            liveFlags.isSigned -> "已签名"
            liveFlags.isRepacked -> "已重打包"
            liveFlags.isDirty -> "已修改"
            liveFlags.needsSecurityRescan -> "需重新扫描"
            else -> "已分析"
        }

        val cardShape = RoundedCornerShape(16.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // SECTION 1: Header with Back + Share
            // ═══════════════════════════════════════════════════
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c.surfaceAlt)
                                .bounceClick(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                modifier = Modifier.size(18.dp),
                                tint = c.textPrimary
                            )
                        }
                        Text(
                            text = "分析报告",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary
                        )
                        // Phase 4: Live status badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (liveFlags.isDirty || liveFlags.needsSecurityRescan) c.amberGold.copy(alpha = 0.2f)
                                    else c.auroraGreen.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = liveStatusText,
                                fontSize = 9.sp,
                                color = if (liveFlags.isDirty || liveFlags.needsSecurityRescan) c.amberGold else c.auroraGreen
                            )
                        }
                    }
                    IconButton(onClick = {
                        val reportText = generateReportText(session)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            putExtra(Intent.EXTRA_SUBJECT, "AppX 分析报告 - ${manifest.packageName}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "分享报告"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享", tint = c.amberGold)
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 2: App Identity Card — Apple Style
            // ═══════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(cardShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(c.heroGradientStart, c.heroGradientEnd)
                            )
                        )
                        .border(1.dp, c.borderLight, cardShape)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Icon — large, centered
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.iconBoxBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        session.appIcon?.let { bitmap ->
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                            )
                        } ?: Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = c.iconBlueBright
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Package name
                    Text(
                        text = manifest.packageName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Meta info: version · size · API
                    Text(
                        text = buildString {
                            manifest.versionName.takeIf { it.isNotEmpty() }?.let { append("v$it") }
                            append(" · ")
                            append(FormatUtil.formatFileSize(info.fileSize))
                            if (manifest.minSdk > 0) append(" · API ${manifest.minSdk}")
                        },
                        fontSize = 12.sp,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 3: One-Sentence Executive Summary — THE HERO
            // ═══════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = executiveSummary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 4: Score Ring — Visual Impact
            // ═══════════════════════════════════════════════════
item {
ScoreRingCard(
score = liveScore,
scoreColor = scoreColor,
riskLevel = getRiskLevel(liveScore),
modifier = Modifier.padding(horizontal = 16.dp)
)
}

            // ═══════════════════════════════════════════════════
            // SECTION 5: Quick Stats Grid — At a Glance
            // ═══════════════════════════════════════════════════
            item {
                val dangerousPerms = manifest.permissions.filter { isDangerousPermission(it) }
                val dexCount = if (session.apkInfo != null) info.entries.count { it.name.endsWith(".dex") } else session.summary.dexCount
                val nativeCount = if (session.apkInfo != null) info.entries.count { it.name.endsWith(".so") } else session.summary.nativeLibCount
                val findingCount = liveFindings.size

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickStatCard(
                        icon = Icons.Default.Security,
                        value = dangerousPerms.size,
                        label = "敏感权限",
                        color = c.redSupergiant,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        icon = Icons.Default.Warning,
                        value = findingCount,
                        label = "安全发现",
                        color = c.amberGold,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        icon = Icons.Default.Code,
                        value = dexCount,
                        label = "DEX",
                        color = c.iconBlue,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        icon = Icons.Default.Memory,
                        value = nativeCount,
                        label = "Native",
                        color = c.auroraGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 6: Security Findings — Clean Cards
            // ═══════════════════════════════════════════════════
if (liveFindings.isNotEmpty()) {
item {
SectionHeader(title = "安全发现", subtitle = "FINDINGS")
}
val grouped = liveFindings.groupBy { it.severity }
                val severityOrder = listOf(
                    FindingSeverity.CRITICAL,
                    FindingSeverity.HIGH,
                    FindingSeverity.MEDIUM,
                    FindingSeverity.LOW,
                    FindingSeverity.INFO
                )
                severityOrder.forEach { severity ->
                    grouped[severity]?.let { findings ->
                        items(findings) { finding ->
                            AppleFindingCard(finding, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp), onClick = onNavigateToSecurity)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 7: AI Summary — Distinct Style
            // ═══════════════════════════════════════════════════
            val aiSummary = session.aiMessages.lastOrNull { it.role == AiRole.ASSISTANT }
            if (aiSummary != null) {
                item {
                    SectionHeader(title = "AI 总结", subtitle = "AI SUMMARY")
                }
                item {
                    AiSummaryCard(
                        message = aiSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 8: Permissions Breakdown
            // ═══════════════════════════════════════════════════
            if (manifest.permissions.isNotEmpty()) {
                item {
                    SectionHeader(title = "权限分析", subtitle = "PERMISSIONS")
                }
                val dangerousPerms = manifest.permissions.filter { isDangerousPermission(it) }
                val normalPerms = manifest.permissions.filterNot { isDangerousPermission(it) }

                item {
                    PermissionBreakdownCard(
                        dangerous = dangerousPerms,
                        normal = normalPerms,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onPermissionClick = { perm ->
                            onAskAi("请详细解释权限 $perm 的作用、风险和必要性")
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 9: Component Stats
            // ═══════════════════════════════════════════════════
            item {
                SectionHeader(title = "组件统计", subtitle = "COMPONENTS")
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComponentStatCard("Activity", manifest.activities.size, c.iconBlue, Modifier.weight(1f), onClick = onNavigateToManifest)
                    ComponentStatCard("Service", manifest.services.size, c.auroraGreen, Modifier.weight(1f), onClick = onNavigateToManifest)
                    ComponentStatCard("Receiver", manifest.receivers.size, c.amberGold, Modifier.weight(1f), onClick = onNavigateToManifest)
                    ComponentStatCard("Provider", manifest.providers.size, c.redSupergiant, Modifier.weight(1f), onClick = onNavigateToManifest)
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 10: Signature Info
            // ═══════════════════════════════════════════════════
            if (info.signatures.isNotEmpty()) {
                item {
                    SectionHeader(title = "签名信息", subtitle = "SIGNATURE")
                }
                info.signatures.forEach { sig ->
                    item {
                        SignatureCard(
                            version = sig.version,
                            algorithm = sig.algorithm,
                            subject = sig.certificateSubject,
                            issuer = sig.certificateIssuer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 11: Next Steps — Actionable, Numbered
            // ═══════════════════════════════════════════════════
            item {
                SectionHeader(title = "下一步建议", subtitle = "NEXT STEPS")
            }
            item {
                val nextSteps = buildNextSteps(
                    session, manifest, info,
                    onNavigateToSecurity = onNavigateToSecurity,
                    onNavigateToDex = onNavigateToDex,
                    onNavigateToManifest = onNavigateToManifest,
                    onNavigateToSigning = onNavigateToSigning,
                    onAskAi = onAskAi
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    nextSteps.forEachIndexed { index, step ->
                        NextStepCard(
                            index = index + 1,
                            icon = step.icon,
                            title = step.title,
                            description = step.description,
                            modifier = Modifier.padding(vertical = 3.dp),
                            onClick = step.onClick
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // SECTION 12: Footer
            // ═══════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppXDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AppX",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.amberGold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Generated at ${
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                        }",
                        fontSize = 9.sp,
                        color = c.textMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ── Floating Copilot — pass rich context to AI ──
        CopilotButton(
            onClick = {
                val contextQuery = buildAiContextQuery(session)
                onAskAi(contextQuery)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 32.dp),
            label = "问 AI"
        )
    }
}
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary
        )
        Text(
            text = subtitle,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = c.textTertiary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 3.dp)
        )
    }
}

@Composable
private fun ScoreRingCard(
    score: Int,
    scoreColor: Color,
    riskLevel: String,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "scoreAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.scoreCardBg)
            .border(1.dp, c.borderAccent, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .drawBehind {
                    val stroke = 10.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Background ring
                    drawArc(
                        color = c.scoreBarBg,
                        topLeft = topLeft,
                        size = arcSize,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Progress ring
                    drawArc(
                        color = scoreColor,
                        topLeft = topLeft,
                        size = arcSize,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    text = "/ 100",
                    fontSize = 10.sp,
                    color = c.textTertiary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Risk level badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(scoreColor.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = riskLevel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceAlt)
            .border(1.dp, c.borderLight, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = c.textSecondary
        )
    }
}

@Composable
private fun AppleFindingCard(
    finding: AnalysisFinding,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val c = AppXTheme.colors
    val (severityColor, severityBg) = when (finding.severity) {
        FindingSeverity.CRITICAL -> c.redSupergiant to c.redSupergiantContainer.copy(alpha = 0.3f)
        FindingSeverity.HIGH -> Color(0xFFFF9800) to Color(0xFF3D2410).copy(alpha = 0.5f)
        FindingSeverity.MEDIUM -> c.amberGold to c.amberGoldContainer.copy(alpha = 0.3f)
        FindingSeverity.LOW -> c.iconBlue to c.nebulaBlueContainer.copy(alpha = 0.3f)
        FindingSeverity.INFO -> c.auroraGreen to c.auroraGreenContainer.copy(alpha = 0.3f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceAlt)
            .border(1.dp, severityColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Severity badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(severityColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = finding.severity.label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = severityColor,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = finding.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = finding.description,
            fontSize = 11.sp,
            color = c.textSecondary,
            lineHeight = 16.sp
        )

        finding.recommendation?.let { rec ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).padding(top = 1.dp),
                    tint = c.amberGold
                )
                Text(
                    text = rec,
                    fontSize = 10.sp,
                    color = c.amberGold,
                    lineHeight = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AiSummaryCard(
    message: AiMessage,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(c.aiGradientStart, c.aiGradientEnd)
                )
            )
            .border(1.dp, c.borderLight, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // AI badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = c.iconBlueBright
            )
            Text(
                text = "AppX AI",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.iconBlueBright,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = message.content,
            fontSize = 12.sp,
            color = c.textPrimary,
            lineHeight = 18.sp
        )

        // Risk
        message.risk?.let { risk ->
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.redSupergiantContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.redSupergiant)
                Text(
                    text = risk,
                    fontSize = 11.sp,
                    color = c.redSupergiant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recommendation
        message.recommendation?.let { rec ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.amberGoldContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.amberGold)
                Text(
                    text = rec,
                    fontSize = 11.sp,
                    color = c.amberGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PermissionBreakdownCard(
    dangerous: List<String>,
    normal: List<String>,
    modifier: Modifier = Modifier,
    onPermissionClick: ((String) -> Unit)? = null
) {
    val c = AppXTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceAlt)
            .border(1.dp, c.borderLight, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Dangerous permissions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.redSupergiant)
            Text(
                text = "敏感权限",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.redSupergiant
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.redSupergiant.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${dangerous.size}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.redSupergiant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        dangerous.forEach { perm ->
            Row(
                modifier = Modifier
                    .then(if (onPermissionClick != null) Modifier.bounceClick { onPermissionClick(perm) } else Modifier)
                    .padding(vertical = 2.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ ${perm.removePrefix("android.permission.")}",
                    fontSize = 10.sp,
                    color = c.redSupergiant.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (dangerous.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Normal permissions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.auroraGreen)
            Text(
                text = "普通权限",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.auroraGreen
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.auroraGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${normal.size}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.auroraGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ComponentStatCard(
    name: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val c = AppXTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceAlt)
            .border(1.dp, c.borderLight, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = name,
            fontSize = 9.sp,
            color = c.textSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SignatureCard(
    version: Int,
    algorithm: String,
    subject: String,
    issuer: String,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceAlt)
            .border(1.dp, c.borderLight, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.amberGold.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "v$version",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.amberGold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = algorithm,
                fontSize = 11.sp,
                color = c.textPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Subject: $subject",
            fontSize = 9.sp,
            color = c.textSecondary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Issuer: $issuer",
            fontSize = 9.sp,
            color = c.textSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NextStepCard(
    index: Int,
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val c = AppXTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceAlt)
            .border(1.dp, c.borderLight, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Number badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(c.amberGold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = c.amberGold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (onClick != null) c.amberGold else c.textTertiary
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Data Classes & Helpers
// ═══════════════════════════════════════════════════════════════

private data class NextStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: (() -> Unit)? = null
)

private fun buildNextSteps(
    session: AnalysisSession,
    manifest: com.appdex.apk.ApkManifest,
    info: ApkInfo,
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToDex: () -> Unit = {},
    onNavigateToManifest: () -> Unit = {},
    onNavigateToSigning: () -> Unit = {},
    onAskAi: (String) -> Unit = {}
): List<NextStep> {
    val steps = mutableListOf<NextStep>()

    if (session.findings.any { it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.HIGH }) {
        steps.add(NextStep(
            icon = Icons.Default.Security,
            title = "查看高危安全发现",
            description = "评估关键风险影响，决定是否继续使用",
            onClick = onNavigateToSecurity
        ))
    }

    val dangerousPerms = manifest.permissions.filter { isDangerousPermission(it) }
    if (dangerousPerms.isNotEmpty()) {
        steps.add(NextStep(
            icon = Icons.Default.Warning,
            title = "审查 ${dangerousPerms.size} 个敏感权限",
            description = "确认这些权限是否为应用正常运行所必需",
            onClick = onNavigateToManifest
        ))
    }

    if (info.signatures.isEmpty()) {
        steps.add(NextStep(
            icon = Icons.Default.Check,
            title = "为 APK 生成签名",
            description = "未签名的 APK 无法正常安装",
            onClick = onNavigateToSigning
        ))
    } else if (!info.signatures.any { it.version >= 2 }) {
        steps.add(NextStep(
            icon = Icons.Default.Check,
            title = "升级到 V2+ 签名方案",
            description = "V1 签名在 Android 7+ 上安全性不足",
            onClick = onNavigateToSigning
        ))
    }

    steps.add(NextStep(
        icon = Icons.Default.Psychology,
        title = "向 AI 提问获取深入分析",
        description = "AI 可以解读代码逻辑、追踪数据流向、识别潜在后门",
        onClick = { onAskAi(buildAiContextQuery(session)) }
    ))

    return steps
}

/**
 * 构建 AI 上下文查询 — 将报告关键信息传递给 AI
 */
private fun buildAiContextQuery(session: AnalysisSession?): String {
    if (session == null) return "请基于当前分析结果进行深入分析"
    val info = session.apkInfo
    val manifest = info?.manifest
    val dangerousPerms = manifest?.permissions?.filter { isDangerousPermission(it) } ?: emptyList()
    val criticalFindings = session.findings.filter {
        it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.HIGH
    }
    val dexCount = info?.entries?.count { it.name.endsWith(".dex") } ?: session.summary.dexCount
    val nativeCount = info?.entries?.count { it.name.endsWith(".so") } ?: session.summary.nativeLibCount
    return buildString {
        append("基于以下 APK 分析报告进行深入分析：\n")
        append("包名：${manifest?.packageName ?: session.packageName}\n")
        append("版本：${manifest?.versionName ?: session.versionName}\n")
        append("安全评分：${session.securityScore}/100\n")
        append("敏感权限：${dangerousPerms.size} 个\n")
        append("安全发现：${session.findings.size} 项")
        if (criticalFindings.isNotEmpty()) {
            append("（其中 ${criticalFindings.size} 项高危）")
        }
        append("\n")
        append("DEX 文件：$dexCount 个\n")
        append("Native 库：$nativeCount 个\n")
        append("请分析这些数据背后的安全含义，并给出具体的改进建议。")
    }
}

private fun getRiskLevel(score: Int): String = com.appdex.data.session.RiskScoreCalculator.getRiskLevel(score)

private fun isDangerousPermission(perm: String): Boolean {
    val dangerous = setOf(
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
    return perm in dangerous
}

private fun generateReportText(session: AnalysisSession): String {
    val info = session.apkInfo
    val manifest = info?.manifest
    val sb = StringBuilder()

    sb.appendLine("═══════════════════════════════════════════")
    sb.appendLine("  AppX 分析报告")
    sb.appendLine("═══════════════════════════════════════════")
    sb.appendLine()

    // Executive summary
    val summary = when {
        session.securityScore >= 80 -> "这款应用整体安全，可以放心使用。"
        session.securityScore >= 60 -> "这款应用存在一些安全顾虑，建议仔细检查。"
        session.securityScore >= 40 -> "这款应用存在较高安全风险，谨慎使用。"
        else -> "这款应用存在严重安全风险，不建议安装。"
    }
    sb.appendLine("总结: $summary")
    sb.appendLine()

    sb.appendLine("包名: ${manifest?.packageName ?: session.packageName}")
    sb.appendLine("版本: ${manifest?.versionName ?: session.versionName}")
    sb.appendLine("大小: ${FormatUtil.formatFileSize(info?.fileSize ?: session.fileSize)}")
    sb.appendLine("安全评分: ${session.securityScore}/100 (${getRiskLevel(session.securityScore)})")
    sb.appendLine()

    sb.appendLine("── 安全发现 ──")
    if (session.findings.isEmpty()) {
        sb.appendLine("未发现安全问题")
    } else {
        session.findings.forEach { f ->
            sb.appendLine("[${f.severity.label}] ${f.title}")
            sb.appendLine("  ${f.description}")
            f.recommendation?.let { sb.appendLine("  → $it") }
        }
    }
    sb.appendLine()

    sb.appendLine("── 权限分析 ──")
    val allPerms = manifest?.permissions ?: emptyList()
    val dangerous = allPerms.filter { isDangerousPermission(it) }
    sb.appendLine("危险权限: ${dangerous.size} 个")
    dangerous.forEach { sb.appendLine("  ⚠ $it") }
    sb.appendLine("普通权限: ${allPerms.size - dangerous.size} 个")
    sb.appendLine()

    sb.appendLine("── 组件统计 ──")
    sb.appendLine("Activity: ${manifest?.activities?.size ?: session.summary.activityCount}")
    sb.appendLine("Service: ${manifest?.services?.size ?: session.summary.serviceCount}")
    sb.appendLine("Receiver: ${manifest?.receivers?.size ?: session.summary.receiverCount}")
    sb.appendLine("Provider: ${manifest?.providers?.size ?: session.summary.providerCount}")
    sb.appendLine()

    sb.appendLine("── 签名信息 ──")
    if (info?.signatures?.isNotEmpty() == true) {
        info.signatures.forEach { sig ->
            sb.appendLine("v${sig.version} · ${sig.algorithm}")
            sb.appendLine("  Subject: ${sig.certificateSubject}")
        }
    } else if (session.summary.hasSignature) {
        sb.appendLine("v${session.summary.signatureVersion}（摘要数据）")
    } else {
        sb.appendLine("无签名")
    }
    sb.appendLine()

    sb.appendLine("── AI 总结 ──")
    session.aiMessages.lastOrNull { it.role == AiRole.ASSISTANT }?.let { msg ->
        sb.appendLine(msg.content)
        msg.risk?.let { sb.appendLine("风险: $it") }
        msg.recommendation?.let { sb.appendLine("建议: $it") }
    }
    sb.appendLine()

    sb.appendLine("── 文件统计 ──")
    val entries = info?.entries ?: emptyList()
    sb.appendLine("DEX: ${if (info != null) entries.count { it.name.endsWith(".dex") } else session.summary.dexCount}")
    sb.appendLine("Native: ${if (info != null) entries.count { it.name.endsWith(".so") } else session.summary.nativeLibCount}")
    sb.appendLine("Resources: ${if (info != null) entries.count { it.name.startsWith("res/") } else session.summary.resourceCount}")
    sb.appendLine()
    sb.appendLine("═══════════════════════════════════════════")
    sb.appendLine("Generated by AppX at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")

    return sb.toString()
}
