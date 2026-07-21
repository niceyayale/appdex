package com.appdex.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.FindingSeverity
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.bounceClick
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// AppX Search Everything — Global Command Palette
// ═══════════════════════════════════════════════════════════════
// Search: Commands, Permissions, Findings, Files, AI Actions
// ═══════════════════════════════════════════════════════════════

data class CommandItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val category: String,
    val action: () -> Unit
)

@Composable
fun CommandPalette(
    visible: Boolean,
    onDismiss: () -> Unit,
    session: AnalysisSession? = null,
    onNavigateAi: () -> Unit = {},
    onNavigateWorkspace: () -> Unit = {},
    onNavigateFiles: () -> Unit = {},
    onNavigateTools: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateTerminal: () -> Unit = {},
    onNavigateEditor: () -> Unit = {},
    onAnalyzeApk: () -> Unit = {},
    onScanSecurity: () -> Unit = {},
    onOpenSigning: () -> Unit = {},
    onOpenDex: (String?) -> Unit = {},
    onOpenDiff: () -> Unit = {},
    onOpenSqlite: () -> Unit = {},
    onOpenHexEditor: () -> Unit = {},
    onOpenRepack: () -> Unit = {},
    onOpenSizeAnalyzer: () -> Unit = {},
    onOpenAxmlEditor: (String?) -> Unit = {},
    onOpenArscViewer: (String?) -> Unit = {},
    onOpenElfViewer: () -> Unit = {},
    onNavigateRemote: () -> Unit = {},
    onAskAi: (String) -> Unit = {}
) {
    val c = AppXTheme.colors
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(visible) {
        if (visible) {
            query = ""
            selectedIndex = 0
            focusRequester.requestFocus()
        }
    }

    // Build all searchable items
    val allItems = remember(
        session, onNavigateAi, onNavigateWorkspace, onNavigateFiles, onNavigateTools,
        onNavigateSettings, onNavigateTerminal, onNavigateEditor,
        onAnalyzeApk, onScanSecurity, onOpenSigning, onOpenDex, onOpenDiff, onOpenSqlite,
        onOpenHexEditor, onOpenRepack, onOpenSizeAnalyzer, onOpenAxmlEditor,
        onOpenArscViewer, onOpenElfViewer, onNavigateRemote, onAskAi
    ) {
        buildSearchableItems(
            session = session,
            onNavigateAi = onNavigateAi,
            onNavigateWorkspace = onNavigateWorkspace,
            onNavigateFiles = onNavigateFiles,
            onNavigateTools = onNavigateTools,
            onNavigateSettings = onNavigateSettings,
            onNavigateTerminal = onNavigateTerminal,
            onNavigateEditor = onNavigateEditor,
            onAnalyzeApk = onAnalyzeApk,
            onScanSecurity = onScanSecurity,
            onOpenSigning = onOpenSigning,
            onOpenDex = onOpenDex,
            onOpenDiff = onOpenDiff,
            onOpenSqlite = onOpenSqlite,
            onOpenHexEditor = onOpenHexEditor,
            onOpenRepack = onOpenRepack,
            onOpenSizeAnalyzer = onOpenSizeAnalyzer,
            onOpenAxmlEditor = onOpenAxmlEditor,
            onOpenArscViewer = onOpenArscViewer,
            onOpenElfViewer = onOpenElfViewer,
            onNavigateRemote = onNavigateRemote,
            onAskAi = onAskAi,
            onDismiss = onDismiss
        )
    }

    // Filter items by query
    val filteredItems = remember(query, allItems) {
        if (query.isBlank()) {
            allItems
        } else {
            allItems.filter { cmd ->
                cmd.title.contains(query, ignoreCase = true) ||
                cmd.description.contains(query, ignoreCase = true) ||
                cmd.category.contains(query, ignoreCase = true)
            }
        }
    }

    // Reset selection when query changes
    LaunchedEffect(query) {
        selectedIndex = 0
    }

    // Clamp selectedIndex
    val clampedIndex = selectedIndex.coerceIn(0, filteredItems.lastIndex.coerceAtLeast(0))

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, c.borderMedium, RoundedCornerShape(16.dp))
                    .background(c.surfaceDeep)
                    .clickable(enabled = false) { /* prevent click-through */ }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    selectedIndex = (clampedIndex + 1).coerceAtMost(filteredItems.lastIndex.coerceAtLeast(0))
                                    true
                                }
                                Key.DirectionUp -> {
                                    selectedIndex = (clampedIndex - 1).coerceAtLeast(0)
                                    true
                                }
                                Key.Enter -> {
                                    filteredItems.getOrNull(clampedIndex)?.action?.invoke()
                                    true
                                }
                                Key.Escape -> {
                                    onDismiss()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                // ── Search Input ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = c.textTertiary
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = c.textPrimary,
                            fontFamily = FontFamily.Default
                        ),
                        cursorBrush = SolidColor(c.amberGold),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜索类名、Activity、权限、资源、文件...",
                                    fontSize = 15.sp,
                                    color = c.textMuted
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .bounceClick { query = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp),
                                tint = c.textTertiary
                            )
                        }
                    }
                }

                AppXDivider()

                // ── Search Results ──
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "未找到 \"$query\"",
                                fontSize = 14.sp,
                                color = c.textTertiary
                            )
                            // Show AI ask option
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c.aiGradientStart)
                                    .bounceClick {
                                        onAskAi(query)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = c.iconBlueBright
                                )
                                Text(
                                    text = "问 AI: \"$query\"",
                                    fontSize = 13.sp,
                                    color = c.iconBlueBright,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // Group by category
                    val grouped = filteredItems.groupBy { it.category }
                    var runningIndex = 0

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                (filteredItems.size * 52 + grouped.size * 28 + 16).dp.coerceAtMost(440.dp)
                            ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        grouped.forEach { (category, items) ->
                            val categoryStartIndex = runningIndex
                            item(key = "header_$category") {
                                Text(
                                    text = category,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.6.sp,
                                    color = c.sectionLabelColor,
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        top = 8.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }
                            itemsIndexed(items, key = { _, cmd -> cmd.id }) { localIndex, cmd ->
                                val globalIndex = categoryStartIndex + localIndex
                                runningIndex = globalIndex + 1
                                CommandRow(
                                    command = cmd,
                                    isSelected = globalIndex == clampedIndex,
                                    onClick = {
                                        cmd.action()
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandRow(
    command: CommandItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) c.amberGold.copy(alpha = 0.08f) else Color.Transparent)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(command.iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                command.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = command.iconColor
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = command.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) c.amberGoldHighlight else c.textPrimary
            )
            Text(
                text = command.description,
                fontSize = 10.sp,
                color = c.textSecondary,
                maxLines = 1
            )
        }
        // Show a subtle indicator for selected item
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(3.dp, 16.dp)
                    .background(c.amberGold)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Searchable Items Builder — Search Everything
// ═══════════════════════════════════════════════════════════════

private fun buildSearchableItems(
    session: AnalysisSession?,
    onNavigateAi: () -> Unit,
    onNavigateWorkspace: () -> Unit,
    onNavigateFiles: () -> Unit,
    onNavigateTools: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateTerminal: () -> Unit,
    onNavigateEditor: () -> Unit,
    onAnalyzeApk: () -> Unit,
    onScanSecurity: () -> Unit,
    onOpenSigning: () -> Unit,
    onOpenDex: (String?) -> Unit,
    onOpenDiff: () -> Unit,
    onOpenSqlite: () -> Unit,
    onOpenHexEditor: () -> Unit,
    onOpenRepack: () -> Unit,
    onOpenSizeAnalyzer: () -> Unit,
    onOpenAxmlEditor: (String?) -> Unit,
    onOpenArscViewer: (String?) -> Unit,
    onOpenElfViewer: () -> Unit,
    onNavigateRemote: () -> Unit,
    onAskAi: (String) -> Unit,
    onDismiss: () -> Unit
): List<CommandItem> {
    val items = mutableListOf<CommandItem>()

    // ── Goals ──
    items.add(CommandItem("goal_analyze", "分析 APK", "全面分析应用结构、权限、SDK 和安全性", Icons.Default.Devices, IconBlueBright, "目标") { onAnalyzeApk(); onDismiss() })
    items.add(CommandItem("goal_security", "安全扫描", "检查危险权限、追踪 SDK、硬编码密钥", Icons.Default.Security, RedSupergiant, "目标") { onScanSecurity(); onDismiss() })
    items.add(CommandItem("goal_ask_ai", "问 AI", "用自然语言提问，AI 帮你分析", Icons.Default.Psychology, IconBlueBright, "目标") { onNavigateAi(); onDismiss() })

    // ── Navigation ──
    items.add(CommandItem("nav_ai", "AI 对话", "AI 助手对话", Icons.Default.Psychology, IconBlueBright, "导航") { onNavigateAi(); onDismiss() })
    items.add(CommandItem("nav_workspace", "工作区", "查看所有工作区", Icons.Default.Apps, AuroraGreen, "导航") { onNavigateWorkspace(); onDismiss() })
    items.add(CommandItem("nav_files", "文件", "文件管理器", Icons.Default.Folder, AuroraGreen, "导航") { onNavigateFiles(); onDismiss() })
    items.add(CommandItem("nav_tools", "工具", "工具集", Icons.Default.Apps, IconBlue, "导航") { onNavigateTools(); onDismiss() })
    items.add(CommandItem("nav_settings", "设置", "应用设置", Icons.Default.Settings, TextSecondary, "导航") { onNavigateSettings(); onDismiss() })

    // ── Tools ──
    items.add(CommandItem("tool_terminal", "终端", "打开终端", Icons.Default.Terminal, TerminalPrompt, "工具") { onNavigateTerminal(); onDismiss() })
    items.add(CommandItem("tool_editor", "编辑器", "打开文本编辑器", Icons.Default.Edit, NebulaBlue, "工具") { onNavigateEditor(); onDismiss() })
    items.add(CommandItem("tool_dex", "DEX 浏览器", "浏览 DEX 文件", Icons.Default.Code, IconBlue, "工具") { onOpenDex(null); onDismiss() })
    items.add(CommandItem("tool_signing", "签名工具", "APK 签名", Icons.Default.VpnKey, AmberGold, "工具") { onOpenSigning(); onDismiss() })
    items.add(CommandItem("tool_sqlite", "SQLite 查看器", "浏览 SQLite 数据库", Icons.Default.Storage, IconBlue, "工具") { onOpenSqlite(); onDismiss() })
    items.add(CommandItem("tool_diff", "APK 对比", "比较两个 APK", Icons.Default.Compare, Color(0xFFCE93D8), "工具") { onOpenDiff(); onDismiss() })
    items.add(CommandItem("tool_hex", "HEX 编辑器", "十六进制查看和编辑文件", Icons.Default.Memory, IconBlue, "工具") { onOpenHexEditor(); onDismiss() })
    items.add(CommandItem("tool_repack", "APK 重打包", "重新打包和签名 APK", Icons.Default.Apps, AmberGold, "工具") { onOpenRepack(); onDismiss() })
    items.add(CommandItem("tool_size", "大小分析", "分析 APK 体积构成", Icons.Default.Storage, AuroraGreen, "工具") { onOpenSizeAnalyzer(); onDismiss() })
    items.add(CommandItem("tool_axml", "Manifest 编辑器", "编辑 AndroidManifest.xml", Icons.Default.Edit, AmberGoldHighlight, "工具") { onOpenAxmlEditor(null); onDismiss() })
    items.add(CommandItem("tool_arsc", "资源表查看器", "浏览 resources.arsc", Icons.Default.Description, NebulaBlue, "工具") { onOpenArscViewer(null); onDismiss() })
    items.add(CommandItem("tool_elf", "ELF 查看器", "分析 ELF/SO 二进制文件", Icons.Default.Code, AuroraGreen, "工具") { onOpenElfViewer(); onDismiss() })
    items.add(CommandItem("tool_remote", "远程管理", "FTP 和 Web 服务器管理", Icons.Default.Cloud, IconBlueBright, "工具") { onNavigateRemote(); onDismiss() })

    // ── AI Actions ──
    items.add(CommandItem("ai_explain_perms", "AI 解释权限", "让 AI 分析当前应用的所有权限", Icons.Default.Lightbulb, AmberGold, "AI") {
        onAskAi("请分析这个应用的所有权限，哪些是敏感的？为什么需要？"); onDismiss()
    })
    items.add(CommandItem("ai_find_risks", "AI 查找风险", "让 AI 深入分析潜在安全风险", Icons.Default.Warning, RedSupergiant, "AI") {
        onAskAi("请深入分析这个应用的安全风险，包括隐私泄露、恶意行为等"); onDismiss()
    })
    items.add(CommandItem("ai_summarize", "AI 总结应用", "让 AI 生成一段话的应用总结", Icons.Default.Psychology, IconBlueBright, "AI") {
        onAskAi("请用一句话总结这个应用是什么，做什么的"); onDismiss()
    })

    // ── Session-aware search: Permissions ──
    session?.apkInfo?.manifest?.permissions?.forEachIndexed { index, perm ->
        val isDangerous = isDangerousPermission(perm)
        items.add(CommandItem(
            id = "perm_$index",
            title = perm.removePrefix("android.permission."),
            description = if (isDangerous) "敏感权限" else "普通权限",
            icon = Icons.Default.Security,
            iconColor = if (isDangerous) RedSupergiant else AuroraGreen,
            category = "权限",
            action = { onScanSecurity(); onDismiss() }
        ))
    }

    // ── Session-aware search: Findings ──
    session?.findings?.forEachIndexed { index, finding ->
        val (icon, color) = when (finding.severity) {
            FindingSeverity.CRITICAL -> Icons.Default.Warning to RedSupergiant
            FindingSeverity.HIGH -> Icons.Default.Warning to AmberGold
            FindingSeverity.MEDIUM -> Icons.Default.Warning to AmberGold
            FindingSeverity.LOW -> Icons.Default.Security to IconBlue
            FindingSeverity.INFO -> Icons.Default.Security to AuroraGreen
        }
        items.add(CommandItem(
            id = "finding_$index",
            title = finding.title,
            description = "${finding.severity.label} · ${finding.category}",
            icon = icon,
            iconColor = color,
            category = "发现",
            action = { onScanSecurity(); onDismiss() }
        ))
    }

    // ── Session-aware search: File Entries ──
    session?.apkInfo?.entries?.filter { entry ->
        entry.name.endsWith(".xml") || entry.name.endsWith(".dex") ||
        entry.name.endsWith(".so") || entry.name.endsWith(".json") ||
        entry.name.endsWith(".txt") || entry.name.endsWith(".properties")
    }?.take(100)?.forEachIndexed { index, entry ->
        val (icon, color) = when {
            entry.name.endsWith(".xml") -> Icons.Default.Description to AmberGold
            entry.name.endsWith(".dex") -> Icons.Default.Code to IconBlue
            entry.name.endsWith(".so") -> Icons.Default.Memory to AuroraGreen
            entry.name.endsWith(".json") -> Icons.Default.Storage to NebulaBlue
            else -> Icons.Default.Description to TextSecondary
        }
        // CRITICAL: The navigation action must be inside a lambda, NOT evaluated
        // during buildSearchableItems(). Evaluating it eagerly would call
        // navController.navigate() during recomposition, causing a navigation loop
        // (HexEditor → DexBrowser → ElfViewer → AxmlEditor).
        val entryName = entry.name
        items.add(CommandItem(
            id = "file_$index",
            title = entryName.substringAfterLast('/'),
            description = entryName,
            icon = icon,
            iconColor = color,
            category = "文件",
            action = {
                when {
                    entryName.endsWith(".dex") -> { onOpenDex(null); onDismiss() }
                    entryName.endsWith(".xml") -> { onOpenAxmlEditor(entryName); onDismiss() }
                    entryName.endsWith(".so") -> { onOpenElfViewer(); onDismiss() }
                    else -> { onOpenHexEditor(); onDismiss() }
                }
            }
        ))
    }

    // ── Session-aware search: Manifest Components ──
    session?.apkInfo?.manifest?.let { manifest ->
        manifest.activities.take(20).forEachIndexed { index, activity ->
            val activityName = activity
            items.add(CommandItem(
                id = "activity_$index",
                title = activity.substringAfterLast('.'),
                description = "Activity · $activity",
                icon = Icons.Default.Apps,
                iconColor = IconBlue,
                category = "组件",
                action = { onOpenDex(activityName); onDismiss() }
            ))
        }
        manifest.services.take(10).forEachIndexed { index, service ->
            val serviceName = service
            items.add(CommandItem(
                id = "service_$index",
                title = service.substringAfterLast('.'),
                description = "Service · $service",
                icon = Icons.Default.Apps,
                iconColor = AuroraGreen,
                category = "组件",
                action = { onOpenDex(serviceName); onDismiss() }
            ))
        }
        manifest.receivers.take(10).forEachIndexed { index, receiver ->
            val receiverName = receiver
            items.add(CommandItem(
                id = "receiver_$index",
                title = receiver.substringAfterLast('.'),
                description = "Receiver · $receiver",
                icon = Icons.Default.Apps,
                iconColor = AmberGold,
                category = "组件",
                action = { onOpenDex(receiverName); onDismiss() }
            ))
        }
    }

    return items
}

// ── Helper ──

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
