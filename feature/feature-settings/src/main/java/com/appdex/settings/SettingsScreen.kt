package com.appdex.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdex.data.DensityMode
import com.appdex.data.LanguageMode
import com.appdex.data.ThemeMode
import com.appdex.data.ai.AiConfig
import com.appdex.data.ai.AiProviderType
import com.appdex.data.session.ToolDisplayMode
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXSection
import com.appdex.ui.components.AppXToggle
import com.appdex.ui.components.bounceClick
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.DefaultBottomPadding

@Composable
fun SettingsScreen(
    onNavigateToAiProviders: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val c = AppXTheme.colors
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val densityMode by viewModel.densityMode.collectAsStateWithLifecycle(initialValue = DensityMode.STANDARD)
    val languageMode by viewModel.languageMode.collectAsStateWithLifecycle(initialValue = LanguageMode.SYSTEM)
    val showHidden by viewModel.showHidden.collectAsStateWithLifecycle(initialValue = false)
    val rememberPath by viewModel.rememberLastPath.collectAsStateWithLifecycle(initialValue = true)
    val fontSize by viewModel.editorFontSize.collectAsStateWithLifecycle(initialValue = 14)
    val tabWidth by viewModel.editorTabWidth.collectAsStateWithLifecycle(initialValue = 4)
    val termFontSize by viewModel.terminalFontSize.collectAsStateWithLifecycle(initialValue = 13)
    val termScrollback by viewModel.terminalScrollback.collectAsStateWithLifecycle(initialValue = 1000)
    val aiConfig by viewModel.aiConfig.collectAsStateWithLifecycle(initialValue = AiConfig())
    val isAiEnabled by viewModel.isAiEnabled.collectAsStateWithLifecycle(initialValue = false)
    val savedProviders by viewModel.savedProviders.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeProvider by viewModel.activeProvider.collectAsStateWithLifecycle(initialValue = null)
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle(initialValue = ToolDisplayMode.NORMAL)
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()

    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var cacheSize by rememberSaveable { mutableStateOf("") }
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        cacheSize = viewModel.getCacheSize()
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存") },
            text = { Text("缓存大小: $cacheSize\n\n确定要清除缓存吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    cacheSize = viewModel.getCacheSize()
                    showClearCacheDialog = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 0.dp, bottom = DefaultBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { AppXBar(title = "设置") }

            item {
                AppXSection(label = "配置") {
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        ExpandableConfigRow(
                            icon = Icons.Default.Folder,
                            title = "文件管理",
                            detail = "隐藏文件、默认目录与操作确认",
                            isExpanded = expandedSection == "files",
                            onToggle = { expandedSection = if (expandedSection == "files") null else "files" }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("显示隐藏文件", fontSize = 12.sp, color = c.textPrimary)
                                    AppXToggle(checked = showHidden, onCheckedChange = { viewModel.setShowHidden(it) })
                                }
                                AppXDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("记住最后路径", fontSize = 12.sp, color = c.textPrimary)
                                    AppXToggle(checked = rememberPath, onCheckedChange = { viewModel.setRememberPath(it) })
                                }
                            }
                        }
                        AppXDivider()

                        ExpandableConfigRow(
                            icon = Icons.Default.DarkMode,
                            title = "外观",
                            detail = "深色模式、信息密度与无障碍",
                            isExpanded = expandedSection == "appearance",
                            onToggle = { expandedSection = if (expandedSection == "appearance") null else "appearance" }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("主题模式", fontSize = 11.sp, color = c.textSecondary, modifier = Modifier.padding(vertical = 6.dp))
                                SegmentedSelector(
                                    items = ThemeMode.entries,
                                    selected = themeMode,
                                    onSelect = { viewModel.setThemeMode(it) },
                                    label = { mode ->
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "跟随系统"
                                            ThemeMode.LIGHT -> "浅色"
                                            ThemeMode.DARK -> "深色"
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("信息密度", fontSize = 11.sp, color = c.textSecondary, modifier = Modifier.padding(vertical = 6.dp))
                                SegmentedSelector(
                                    items = DensityMode.entries,
                                    selected = densityMode,
                                    onSelect = { viewModel.setDensityMode(it) },
                                    label = { mode ->
                                        when (mode) {
                                            DensityMode.COMPACT -> "紧凑"
                                            DensityMode.STANDARD -> "标准"
                                            DensityMode.COMFORTABLE -> "舒适"
                                        }
                                    }
                                )
                            }
                        }
                        AppXDivider()

                        ExpandableConfigRow(
                            icon = Icons.Default.Settings,
                            title = "高级选项",
                            detail = "缓存、日志与实验功能",
                            isExpanded = expandedSection == "advanced",
                            onToggle = { expandedSection = if (expandedSection == "advanced") null else "advanced" }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("缓存", fontSize = 12.sp, color = c.textPrimary)
                                        Text(text = cacheSize, fontSize = 10.sp, color = c.textSecondary)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .border(1.dp, c.borderMedium)
                                            .clickable {
                                                cacheSize = viewModel.getCacheSize()
                                                showClearCacheDialog = true
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.textSecondary)
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("清除", fontSize = 10.sp, color = c.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                AppXSection(label = "AI 配置") {
                    // Navigation card to AI Providers management page
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(onClick = onNavigateToAiProviders)
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(c.surfaceAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.iconBlue)
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI 提供商", fontSize = 12.sp, color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                                val providerCount = savedProviders.size
                                val activeName = activeProvider?.name
                                val detailText = when {
                                    activeName != null -> "当前: $activeName"
                                    providerCount > 0 -> "$providerCount 个提供商，未选择"
                                    else -> "点击新建提供商配置"
                                }
                                Text(
                                    text = detailText,
                                    fontSize = 10.sp,
                                    color = c.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            // Status indicator
                            Text(
                                text = if (isAiEnabled) "已配置" else "未配置",
                                fontSize = 10.sp,
                                color = if (isAiEnabled) c.auroraGreen else c.redSupergiant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.textTertiary)
                        }
                    }
                }
            }

            item {
                AppXSection(label = "工具显示模式") {
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        ToolDisplayMode.entries.forEachIndexed { index, mode ->
                            val isSelected = mode == displayMode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) c.amberGold.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { viewModel.setDisplayMode(mode) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (mode) {
                                            ToolDisplayMode.NORMAL -> "普通模式"
                                            ToolDisplayMode.ADVANCED -> "高级模式"
                                            ToolDisplayMode.EXPERT -> "专家模式"
                                        },
                                        fontSize = 12.sp,
                                        color = if (isSelected) c.amberGold else c.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = when (mode) {
                                            ToolDisplayMode.NORMAL -> "使用友好名称，隐藏专业术语"
                                            ToolDisplayMode.ADVANCED -> "同时显示友好名称和原始名称"
                                            ToolDisplayMode.EXPERT -> "显示原始技术名称"
                                        },
                                        fontSize = 10.sp,
                                        color = c.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.amberGold)
                                }
                            }
                            if (index < ToolDisplayMode.entries.size - 1) {
                                AppXDivider()
                            }
                        }
                    }
                }
            }

            item {
                AppXSection(label = "语言") {
                    SegmentedSelector(
                        items = LanguageMode.entries,
                        selected = languageMode,
                        onSelect = { viewModel.setLanguageMode(it) },
                        label = { mode ->
                            when (mode) {
                                LanguageMode.ENGLISH -> "English"
                                LanguageMode.CHINESE -> "中文"
                                LanguageMode.SYSTEM -> "系统"
                            }
                        }
                    )
                    Text(
                        text = "语言切换将在应用重启后生效",
                        fontSize = 10.sp,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val packageManager = context.packageManager
                                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.iconBlueBright)
                        Text(text = " 立即重启应用", fontSize = 11.sp, color = c.iconBlueBright)
                    }
                }
            }

            item {
                AppXSection(label = "编辑器") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, c.borderLight)
                            .background(c.surfaceDeep)
                            .padding(16.dp)
                    ) {
                        Text("字体大小: $fontSize sp", fontSize = 12.sp, color = c.textPrimary)
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { viewModel.setFontSize(it.toInt()) },
                            valueRange = 10f..24f,
                            steps = 13
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tab 宽度: $tabWidth", fontSize = 12.sp, color = c.textPrimary)
                        Slider(
                            value = tabWidth.toFloat(),
                            onValueChange = { viewModel.setTabWidth(it.toInt()) },
                            valueRange = 2f..8f,
                            steps = 5
                        )
                    }
                }
            }

            item {
                AppXSection(label = "终端") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, c.borderLight)
                            .background(c.surfaceDeep)
                            .padding(16.dp)
                    ) {
                        Text("字体大小: $termFontSize sp", fontSize = 12.sp, color = c.textPrimary)
                        Slider(
                            value = termFontSize.toFloat(),
                            onValueChange = { viewModel.setTerminalFontSize(it.toInt()) },
                            valueRange = 10f..20f,
                            steps = 9
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("回滚行数: $termScrollback", fontSize = 12.sp, color = c.textPrimary)
                        Slider(
                            value = termScrollback.toFloat(),
                            onValueChange = { viewModel.setTerminalScrollback(it.toInt()) },
                            valueRange = 100f..5000f,
                            steps = 48
                        )
                    }
                }
            }

            item {
                AppXSection(label = "关于") {
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        AboutRow("版本", viewModel.getAppVersion())
                        AppXDivider()
                        AboutRow("协议", "Apache 2.0")
                        AppXDivider()
                        AboutRow("GitHub", "github.com/niceyayale/AppX")
                        AppXDivider()
                        AboutRow("引擎", "Jetpack Compose")
                        AppXDivider()
                        AboutRow("Min Android", "8.0 (API 26)")
                    }
                }
            }

            item {
                Text(
                    text = "AppX · 开源 Android 工具箱",
                    fontSize = 10.sp,
                    color = c.textTertiary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ExpandableConfigRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val c = AppXTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(c.surfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.iconBlue)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, color = c.textPrimary)
                Text(detail, fontSize = 10.sp, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = c.textTertiary
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            content()
        }
    }
}

@Composable
private fun <T> SegmentedSelector(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderMedium)
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) c.amberGold else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label(item),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) c.amberGoldDark else c.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = c.textPrimary)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = c.textSecondary)
    }
}
