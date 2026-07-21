package com.appdex.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.common.FormatUtil
import com.appdex.data.ai.AiConfig
import com.appdex.data.session.AiMessage
import com.appdex.data.session.AiRole
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.SessionStatus
import com.appdex.data.toolbridge.ActionCardData
import com.appdex.data.workspace.Goal
import com.appdex.data.workspace.GoalTemplates
import com.appdex.ui.components.AiActionCardData
import com.appdex.ui.components.AppXAiCard
import com.appdex.ui.components.bounceClick
import com.appdex.ui.components.AppXSection
import com.appdex.ui.components.AppXSessionCard
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// AppX AI — Primary Entry Point
// ═══════════════════════════════════════════════════════════════
// "The first screen is never a tool menu.
//  The first screen is a conversation."
// ═══════════════════════════════════════════════════════════════

@Composable
fun AiScreen(
    messages: List<AiMessage>,
    isAiResponding: Boolean,
    streamingContent: String,
    aiConfig: AiConfig?,
    sessions: List<AnalysisSession>,
    currentSession: AnalysisSession?,
    suggestedQuestions: List<Pair<String, String>> = emptyList(),
    onSendMessage: (String) -> Unit,
    onStopAi: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onAnalyzeApk: (Uri) -> Unit,
    onOpenSession: (String) -> Unit,
    onActionClick: (ActionCardData) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    onOpenCommandPalette: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val c = AppXTheme.colors

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        android.util.Log.d("AppX", "AiScreen filePicker (OpenDocument) callback: uri=$uri")
        if (uri != null) onAnalyzeApk(uri)
    }

    LaunchedEffect(messages.size, isAiResponding, streamingContent) {
        val totalItems = messages.size + (if (isAiResponding) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        // ── Top Bar ──
        AiTopBar(
            onOpenCommandPalette = onOpenCommandPalette,
            onNavigateToWorkspace = onNavigateToWorkspace,
            hasSession = currentSession != null,
            currentApkName = currentSession?.displayName
        )

        // ── Chat Area ──
        // Phase 3: AI Workspace Awareness — proactive insights (collected here, composable scope)
        val workspaceController = com.appdex.ui.components.LocalWorkspaceController.current
        val aiInsights by (workspaceController?.aiInsights
            ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.appdex.data.workspace.AiInsightCard>()))
            .collectAsStateWithLifecycle(initialValue = emptyList())

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                // ── Empty State: Welcome + Import + Goals + Recent ──
                item {
                    AiWelcomeSection(
                        aiConfig = aiConfig,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }

                // ── APK Status Banner (import button when no APK, status when APK loaded) ──
                item {
                    if (currentSession != null) {
                        // APK is loaded — show context banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(c.surfaceVariant)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = c.auroraGreen
                                )
                                Text(
                                    text = "当前 APK: ${currentSession.displayName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = c.textPrimary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "查看工作区",
                                fontSize = 12.sp,
                                color = c.amberGold,
                                modifier = Modifier.bounceClick { onNavigateToWorkspace() }
                            )
                        }
                    } else {
                        // No APK — show import button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(c.amberGold)
                                .bounceClick {
                                    filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = c.amberGoldDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "导入 APK 开始分析",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = c.amberGoldDark
                            )
                        }
                    }
                }

                // Goal Grid
                item {
                    AppXSection(label = "你想做什么？") {
                        AiGoalGrid(
                            goals = GoalTemplates.all.take(6),
                            onSelectGoal = { goal ->
                                if (currentSession != null) {
                                    // APK is loaded — send goal as message directly
                                    onSendMessage(goal.title)
                                } else {
                                    when (goal.category) {
                                        com.appdex.data.workspace.GoalCategory.LEARN -> {
                                            onSendMessage(goal.title)
                                        }
                                        else -> {
                                            filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // Recent Workspaces
                if (sessions.isNotEmpty()) {
                    item {
                        AppXSection(label = "最近工作区") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sessions.take(3).forEach { session ->
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
                                        onClick = { onOpenSession(session.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Phase 3: AI Workspace Awareness — proactive insights panel
                if (aiInsights.isNotEmpty()) {
                    item {
                        AppXSection(label = "AI 洞察") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                aiInsights.take(3).forEach { insight ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, c.amberGold.copy(alpha = 0.3f))
                                            .background(c.amberGold.copy(alpha = 0.08f))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = c.amberGold
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(insight.title, fontSize = 12.sp, color = c.textPrimary, fontWeight = FontWeight.Medium)
                                            Text(insight.detail, fontSize = 10.sp, color = c.textSecondary,
                                                modifier = Modifier.padding(top = 2.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .bounceClick { workspaceController?.clearInsights() }
                                                .background(c.surfaceAlt, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("×", fontSize = 12.sp, color = c.textTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Suggested Questions
                if (suggestedQuestions.isNotEmpty()) {
                    item {
                        AppXSection(label = "AI 推荐") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                suggestedQuestions.take(4).forEach { (q, desc) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, c.borderLight)
                                            .background(c.surfaceAlt)
                                            .bounceClick { onSendMessage(q) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Psychology,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = c.iconBlueBright
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(q, fontSize = 12.sp, color = c.textPrimary)
                                            Text(desc, fontSize = 10.sp, color = c.textSecondary,
                                                modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Message Bubbles ──
            items(messages) { message ->
                val actionCards = if (message.role == AiRole.ASSISTANT && message.actionCards.isNotEmpty()) {
                    message.actionCards.map { card ->
                        AiActionCardData(
                            title = card.title,
                            description = card.description,
                            iconType = card.iconType,
                            route = card.route
                        )
                    }
                } else emptyList()

                AppXAiCard(
                    content = message.content,
                    isUser = message.role == AiRole.USER,
                    reason = message.reason,
                    risk = message.risk,
                    recommendation = message.recommendation,
                    technicalDetails = message.technicalDetails,
                    actionCards = actionCards,
                    onActionClick = { aiCardData ->
                        onActionClick(ActionCardData(
                            title = aiCardData.title,
                            description = aiCardData.description,
                            iconType = aiCardData.iconType,
                            route = aiCardData.route
                        ))
                    },
                    onCopy = { content ->
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AppX AI", content))
                    },
                    onRegenerate = if (message.role == AiRole.ASSISTANT && !isAiResponding) {{ onRegenerate() }} else null,
                    onShare = { content ->
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, content)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "分享"))
                    }
                )
            }

            // ── Streaming Response ──
            if (isAiResponding) {
                item {
                    if (streamingContent.isNotEmpty()) {
                        // Show streaming content in real-time
                        AppXAiCard(
                            content = streamingContent,
                            isUser = false,
                            isStreaming = true
                        )
                    } else {
                        // Thinking state — animated pulse, no circular spinner
                        ThinkingIndicator()
                    }
                }
            }
        }

        // ── Input Bar ──
        AiInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    onSendMessage(inputText.trim())
                    inputText = ""
                }
            },
            isResponding = isAiResponding,
            onStop = onStopAi,
            aiConfig = aiConfig,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// AI Top Bar — Logo + Command Palette trigger + Workspace nav
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AiTopBar(
    onOpenCommandPalette: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    hasSession: Boolean,
    currentApkName: String? = null
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AppX Logo
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(c.aiGradientStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = c.iconBlueBright
            )
        }
        Column {
            Text(
                text = "AppX",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary
            )
            // 当前 APK 上下文 — 保证用户在 AI 页面始终知道自己在哪个 APK
            if (!currentApkName.isNullOrEmpty()) {
                Text(
                    text = currentApkName,
                    fontSize = 10.sp,
                    color = c.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        // Command Palette trigger — takes all available space
        Row(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .border(1.dp, c.borderMedium)
                .background(c.surfaceInput)
                .bounceClick(onClick = onOpenCommandPalette)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.textTertiary
            )
            Text(
                text = "搜索...",
                fontSize = 11.sp,
                color = c.textMuted,
                maxLines = 1
            )
        }
        // Workspace button — shows current session status
        if (hasSession) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, c.borderMedium)
                    .background(c.surfaceAlt)
                    .bounceClick(onClick = onNavigateToWorkspace),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = "Workspace",
                    modifier = Modifier.size(18.dp),
                    tint = c.amberGold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// AI Welcome Section — First impression
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AiWelcomeSection(
    aiConfig: AiConfig?,
    onNavigateToSettings: () -> Unit
) {
    val c = AppXTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated AI icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(c.aiGradientStart, c.aiGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = c.iconBlueBright
            )
        }
        Text(
            text = "AppX AI",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "你的 AI 逆向工程工作区",
            fontSize = 12.sp,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        // AI config status
        if (aiConfig == null || !aiConfig.isConfigured()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.amberGold)
                    .background(c.amberGoldContainer)
                    .bounceClick(onClick = onNavigateToSettings)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = c.amberGold
                )
                Text(
                    text = "配置 AI 提供商以开始",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.amberGold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// AI Goal Grid — 2-column grid of goal cards
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AiGoalGrid(
    goals: List<Goal>,
    onSelectGoal: (Goal) -> Unit
) {
    val c = AppXTheme.colors
    val rows = goals.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowGoals ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowGoals.forEach { goal ->
                    AiGoalCard(
                        goal = goal,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGoal(goal) }
                    )
                }
                if (rowGoals.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AiGoalCard(
    goal: Goal,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = AppXTheme.colors
    val icon = goalIconForType(goal.iconType)
    val iconColor = goalIconColorForType(goal.iconType)

    Column(
        modifier = modifier
            .border(1.dp, c.borderLight)
            .background(c.surfaceAlt)
            .bounceClick(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconColor
            )
        }
        Text(
            text = goal.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textPrimary,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1
        )
        Text(
            text = goal.description,
            fontSize = 10.sp,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
            maxLines = 2
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Thinking Indicator — Pulse animation, no circular spinner
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ThinkingIndicator() {
    val c = AppXTheme.colors
    val transition = rememberInfiniteTransition(label = "thinking")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(c.amberGold.copy(alpha = pulse * 0.3f))
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = c.amberGold.copy(alpha = pulse)
            )
        }
        Text(
            text = "AI 正在思考",
            fontSize = 11.sp,
            color = c.textSecondary.copy(alpha = pulse)
        )
        // Animated dots
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { index ->
                val dotAlpha by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = index * 200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_$index"
                )
                Text(
                    text = "·",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.amberGold.copy(alpha = dotAlpha)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// AI Input Bar — With stop button and multiline support
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AiInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isResponding: Boolean,
    onStop: () -> Unit,
    aiConfig: AiConfig?,
    onNavigateToSettings: () -> Unit
) {
    val c = AppXTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceDeep)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Gradient divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            c.borderMedium.copy(alpha = 0.5f),
                            c.amberGold.copy(alpha = 0.2f),
                            c.borderMedium.copy(alpha = 0.5f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (aiConfig?.isConfigured() == true)
                            "问 AI 任何关于 APK 的问题..."
                        else
                            "输入问题（需先配置 AI）...",
                        fontSize = 12.sp,
                        color = c.textMuted
                    )
                },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary,
                    focusedBorderColor = c.amberGold,
                    unfocusedBorderColor = c.borderMedium,
                    cursorColor = c.amberGold
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    color = c.textPrimary,
                    lineHeight = 18.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            // Send / Stop button — OUTSIDE the text field to avoid touch interception
            if (isResponding) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(c.redSupergiant)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(onClick = onStop),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "停止",
                        modifier = Modifier.size(20.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            } else if (inputText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(c.amberGold)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(20.dp),
                        tint = c.amberGoldDark
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Goal icon helpers
// ═══════════════════════════════════════════════════════════════

private fun goalIconForType(type: String) = when (type.lowercase()) {
    "analyze" -> Icons.Default.Apps
    "understand" -> Icons.Default.Psychology
    "security" -> Icons.Default.Security
    "permissions" -> Icons.Default.VpnKey
    "sdk" -> Icons.Default.Code
    "login" -> Icons.Default.Psychology
    "network" -> Icons.Default.Cloud
    "modify" -> Icons.Default.Edit
    "icon" -> Icons.Default.Apps
    "compare" -> Icons.Default.Compare
    "structure" -> Icons.Default.Folder
    "learn" -> Icons.Default.School
    else -> Icons.Default.Apps
}

private fun goalIconColorForType(type: String) = when (type.lowercase()) {
    "analyze" -> IconBlueBright
    "understand" -> AuroraGreen
    "security" -> RedSupergiant
    "permissions" -> AmberGold
    "sdk" -> IconBlue
    "login" -> androidx.compose.ui.graphics.Color(0xFFCE93D8)
    "network" -> IconBlue
    "modify" -> NebulaBlue
    "icon" -> AmberGold
    "compare" -> androidx.compose.ui.graphics.Color(0xFFCE93D8)
    "structure" -> AuroraGreen
    "learn" -> IconBlueBright
    else -> IconBlue
}
