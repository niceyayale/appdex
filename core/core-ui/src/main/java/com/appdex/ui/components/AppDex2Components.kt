package com.appdex.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// AppX 2.0 Design System Components
// ═══════════════════════════════════════════════════════════════

// ─── AppXHero — 首页 Hero 区域 ───
@Composable
fun AppXHero(
    title: String = "AppX",
    subtitle: String = "AI 驱动的 APK 分析平台",
    version: String = "2.0",
    onAnalyze: () -> Unit
) {
    val c = AppXTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderMedium)
            .background(
                Brush.linearGradient(
                    colors = listOf(c.heroGradientStart, c.heroGradientEnd)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(c.aiGradientStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = c.iconBlueBright
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "v$version",
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                color = c.auroraGreenBright,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 巨大分析按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(c.amberGold)
                .bounceClick(onClick = onAnalyze),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分析 APK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.amberGoldDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = c.amberGoldDark
                )
            }
        }
    }
}

// ─── AppXTaskCard — 任务卡片 ───
@Composable
fun AppXTaskCard(
    appName: String,
    packageName: String,
    version: String,
    fileSize: String,
    securityScore: Int,
    statusText: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight)
            .background(SurfaceAlt)
            .bounceClick(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(IconBoxBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = IconBlueBright
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$packageName · $version",
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "$fileSize · 评分 $securityScore/100",
                fontSize = 10.sp,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        // Status badge
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

// ─── AppXFindingCard — 分析发现卡片 ───
@Composable
fun AppXFindingCard(
    severity: String,
    category: String,
    title: String,
    description: String,
    recommendation: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = AppXTheme.colors
    val (severityColor, severityBg) = when (severity) {
        "CRITICAL", "严重" -> c.redSupergiant to c.redSupergiantContainer
        "HIGH", "高危" -> Color(0xFFFF9800) to Color(0xFF3D2410)
        "MEDIUM", "中等" -> c.amberGold to c.amberGoldContainer
        "LOW", "低" -> c.nebulaBlue to c.nebulaBlueContainer
        else -> c.auroraGreen to c.auroraGreenContainer
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderLight)
            .background(c.surfaceAlt)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(severityColor)
            )
            Text(
                text = severity,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = severityColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "·",
                fontSize = 10.sp,
                color = c.textTertiary
            )
            Text(
                text = category,
                fontSize = 10.sp,
                color = c.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            color = c.textSecondary,
            lineHeight = 16.sp
        )
        if (recommendation != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = c.iconBlue
                )
                Text(
                    text = recommendation,
                    fontSize = 10.sp,
                    color = c.iconBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (onAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .border(1.dp, c.borderMedium)
                    .bounceClick(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "查看详情",
                    fontSize = 10.sp,
                    color = c.amberGold,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = c.amberGold
                )
            }
        }
    }
}

// ─── AppXActionCard — AI 推荐动作卡片 ───
@Composable
fun AppXActionCard(
    title: String,
    description: String,
    iconType: String,
    onClick: () -> Unit
) {
    val c = AppXTheme.colors
    val icon = actionIconForType(iconType)
    val iconColor = actionIconColorForType(iconType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderMedium)
            .background(c.surfaceDeep)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconColor
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = c.textTertiary
        )
    }
}

// ─── AppXAiCard — AI 对话气泡 ───
data class AiActionCardData(
    val title: String,
    val description: String,
    val iconType: String,
    val route: String
)

@Composable
fun AppXAiCard(
    content: String,
    isUser: Boolean,
    actionCards: List<AiActionCardData> = emptyList(),
    reason: String? = null,
    risk: String? = null,
    recommendation: String? = null,
    technicalDetails: String? = null,
    isStreaming: Boolean = false,
    onActionClick: ((AiActionCardData) -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onShare: ((String) -> Unit)? = null
) {
    val c = AppXTheme.colors
    val bgGradient = if (isUser) {
        Brush.linearGradient(listOf(c.amberGoldContainer, c.amberGoldContainer))
    } else {
        Brush.linearGradient(listOf(c.aiGradientStart, c.aiGradientEnd))
    }

    // Determine if this is a structured response (has extra sections beyond summary)
    val hasStructuredSections = !isUser && !isStreaming && (
        !reason.isNullOrBlank() || !risk.isNullOrBlank() ||
        !recommendation.isNullOrBlank() || !technicalDetails.isNullOrBlank()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(bgGradient)
                .border(
                    1.dp,
                    if (isUser) c.borderMedium else c.borderLight
                )
                .padding(12.dp)
        ) {
            if (!isUser) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = c.iconBlueBright
                        )
                        Text(
                            text = "AppX AI",
                            fontSize = 9.sp,
                            color = c.iconBlueBright,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Summary (main content) — with Markdown rendering
                    if (content.contains("```") || content.contains("##") || content.contains("**") || content.contains("- ") || content.contains("> ")) {
                        MarkdownText(text = content)
                    } else {
                        Text(
                            text = content,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = c.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                    // Streaming cursor indicator
                    if (isStreaming) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 14.dp)
                                .background(c.amberGold)
                        )
                    }
                    // Structured sections
                    if (hasStructuredSections) {
                        Spacer(modifier = Modifier.height(8.dp))
                        reason?.takeIf { it.isNotBlank() }?.let {
                            StructuredSection(
                                icon = Icons.Default.Info,
                                iconColor = c.iconBlue,
                                label = "原因",
                                text = it
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        risk?.takeIf { it.isNotBlank() }?.let {
                            StructuredSection(
                                icon = Icons.Default.Warning,
                                iconColor = c.redSupergiant,
                                label = "风险",
                                text = it
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        recommendation?.takeIf { it.isNotBlank() }?.let {
                            StructuredSection(
                                icon = Icons.Default.Lightbulb,
                                iconColor = c.amberGold,
                                label = "建议",
                                text = it
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        technicalDetails?.takeIf { it.isNotBlank() }?.let {
                            StructuredSection(
                                icon = Icons.Default.Code,
                                iconColor = c.auroraGreen,
                                label = "技术详情",
                                text = it
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = content,
                    fontSize = 12.sp,
                    color = c.amberGoldHighlight,
                    lineHeight = 18.sp
                )
            }
        }
        // Action cards
        if (!isUser && actionCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            actionCards.forEach { card ->
                AppXActionCard(
                    title = card.title,
                    description = card.description,
                    iconType = card.iconType,
                    onClick = { onActionClick?.invoke(card) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        // P1-2: Message action bar (copy / regenerate / share)
        if (!isUser && !isStreaming && content.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AiActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "复制",
                    onClick = { onCopy?.invoke(content) }
                )
                if (onRegenerate != null) {
                    AiActionButton(
                        icon = Icons.Default.Refresh,
                        label = "重新生成",
                        onClick = onRegenerate
                    )
                }
                if (onShare != null) {
                    AiActionButton(
                        icon = Icons.Default.Share,
                        label = "分享",
                        onClick = { onShare?.invoke(content) }
                    )
                }
            }
        }
    }
}

/**
 * A labeled section within an AI response card.
 */
@Composable
private fun StructuredSection(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    text: String
) {
    val c = AppXTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceAlt.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = iconColor
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = iconColor,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            color = c.textSecondary,
            lineHeight = 15.sp
        )
    }
}

// ─── AppXScoreCard — 安全评分卡片 ───
@Composable
fun AppXScoreCard(
    score: Int,
    riskLevel: String,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    val scoreColor = when {
        score >= 80 -> c.auroraGreen
        score >= 60 -> c.amberGold
        else -> c.redSupergiant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, c.borderAccent)
            .background(c.scoreCardBg)
            .padding(16.dp)
    ) {
        Text(
            text = "安全评分",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.6.sp,
            color = c.scoreLabelBlue
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$score",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary
            )
            Text(
                text = riskLevel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(c.scoreBarBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score.toFloat() / 100f)
                    .height(6.dp)
                    .background(scoreColor)
            )
        }
    }
}

// ─── AppXTimeline — 分析进度时间线 ───
@Composable
fun AppXTimeline(
    steps: List<TimelineStep>,
    modifier: Modifier = Modifier
) {
    val c = AppXTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline dot/line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (step.isCompleted) c.auroraGreen
                                else if (step.isCurrent) c.amberGold
                                else c.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = c.background
                            )
                        } else if (step.isCurrent) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = c.background
                            )
                        }
                    }
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(
                                    if (step.isCompleted) c.auroraGreen.copy(alpha = 0.4f)
                                    else c.borderLight
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).padding(top = 3.dp)) {
                    Text(
                        text = step.title,
                        fontSize = 12.sp,
                        fontWeight = if (step.isCurrent || step.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (step.isCompleted || step.isCurrent) c.textPrimary else c.textTertiary
                    )
                    if (step.description.isNotEmpty()) {
                        Text(
                            text = step.description,
                            fontSize = 10.sp,
                            color = c.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

data class TimelineStep(
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false
)

// ─── AppXLoadingFlow — 分析加载动画（原则14：加载节奏） ───
// 交错入场：图标 → 文字 → 进度条，每个延迟 150ms
// 持续脉冲：平滑的 FastOutSlowInEasing，1200ms 呼吸节奏
@Composable
fun AppXLoadingFlow(
    message: String = "正在分析...",
    progress: Float? = null
) {
    val c = AppXTheme.colors

    // ── Staggered entrance animation ──
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val iconAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "iconEnter"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "textEnter"
    )
    val progressAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "progressEnter"
    )

    // ── Continuous breathing pulse ──
    val infiniteTransition = rememberInfiniteTransition(label = "ai_loading")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI icon — scale-in entrance + continuous breathing pulse
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = iconScale * pulseScale
                    scaleY = iconScale * pulseScale
                    alpha = iconAlpha * pulseAlpha
                }
                .background(c.aiGradientStart.copy(alpha = pulseAlpha * 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = c.iconBlueBright
            )
        }

        Text(
            text = message,
            fontSize = 13.sp,
            color = c.textSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.graphicsLayer { alpha = textAlpha }
        )

        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = progressAlpha },
                color = c.amberGold,
                trackColor = c.surfaceVariant
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { alpha = progressAlpha },
                strokeWidth = 2.dp,
                color = c.amberGold
            )
        }
    }
}

// ─── AppXSessionCard — 会话卡片 ───
@Composable
fun AppXSessionCard(
    title: String,
    subtitle: String,
    score: Int,
    statusText: String,
    statusColor: Color,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderLight)
            .background(c.surfaceAlt)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(c.iconBoxBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    score >= 80 -> c.auroraGreen
                    score >= 60 -> c.amberGold
                    else -> c.redSupergiant
                }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Helper functions
// ═══════════════════════════════════════════════════════════════

private val AiGradientStart = Color(0xFF142842)
private val AiGradientEnd = Color(0xFF0D1B30)

fun actionIconForType(type: String): ImageVector = when (type.lowercase()) {
    "security", "权限" -> Icons.Default.Security
    "code", "dex" -> Icons.Default.Code
    "key", "signing" -> Icons.Default.VpnKey
    "folder", "files" -> Icons.Default.Folder
    "terminal" -> Icons.Default.Terminal
    "edit", "editor" -> Icons.Default.Edit
    "compare", "diff" -> Icons.Default.Compare
    "database", "sqlite" -> Icons.Default.Storage
    "memory", "elf" -> Icons.Default.Memory
    "scan" -> Icons.Default.Devices
    "hex" -> Icons.Default.Code
    "data", "arsc" -> Icons.Default.DataObject
    "chat", "ai" -> Icons.Default.Psychology
    "search" -> Icons.Default.Search
    "bug" -> Icons.Default.BugReport
    "cloud" -> Icons.Default.Cloud
    "fingerprint" -> Icons.Default.Fingerprint
    else -> Icons.Default.Apps
}

fun actionIconColorForType(type: String): Color = when (type.lowercase()) {
    "security", "权限" -> RedSupergiant
    "code", "dex" -> IconBlue
    "key", "signing" -> AmberGold
    "folder", "files" -> AuroraGreen
    "terminal" -> TerminalPrompt
    "edit", "editor" -> NebulaBlue
    "compare", "diff" -> Color(0xFFCE93D8)
    "database", "sqlite" -> Color(0xFF90CAF9)
    "memory", "elf" -> Color(0xFFEF9A9A)
    "scan" -> IconBlueBright
    "hex" -> Color(0xFFA5D6A7)
    "data", "arsc" -> Color(0xFFFFAB91)
    "chat", "ai" -> IconBlueBright
    "search" -> TextSecondary
    "bug" -> RedSupergiant
    "cloud" -> IconBlue
    "fingerprint" -> AmberGold
    else -> IconBlue
}

// ─── AiActionButton — Small action button for AI message toolbar ───
@Composable
fun AiActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surfaceAlt.copy(alpha = 0.5f))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            tint = c.textTertiary
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = c.textTertiary
        )
    }
}

