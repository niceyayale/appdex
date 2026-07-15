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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexDivider
import com.appdex.ui.components.AppDexSection
import com.appdex.ui.components.AppDexToggle
import com.appdex.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
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
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle(initialValue = ToolDisplayMode.NORMAL)

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

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 0.dp, bottom = DefaultBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { AppDexBar(title = "设置") }

            // ── Config section (expandable) ──
            item {
                AppDexSection(label = "配置") {
                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
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
                                    Text("显示隐藏文件", fontSize = 12.sp, color = TextPrimary)
                                    AppDexToggle(checked = showHidden, onCheckedChange = { viewModel.setShowHidden(it) })
                                }
                                AppDexDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("记住最后路径", fontSize = 12.sp, color = TextPrimary)
                                    AppDexToggle(checked = rememberPath, onCheckedChange = { viewModel.setRememberPath(it) })
                                }
                            }
                        }
                        AppDexDivider()

                        ExpandableConfigRow(
                            icon = Icons.Default.DarkMode,
                            title = "外观",
                            detail = "深色模式、信息密度与无障碍",
                            isExpanded = expandedSection == "appearance",
                            onToggle = { expandedSection = if (expandedSection == "appearance") null else "appearance" }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("主题模式", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 6.dp))
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
                                Text("信息密度", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 6.dp))
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
                        AppDexDivider()

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
                                        Text("缓存", fontSize = 12.sp, color = TextPrimary)
                                        Text(
                                            text = cacheSize,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .border(1.dp, BorderMedium)
                                            .clickable {
                                                cacheSize = viewModel.getCacheSize()
                                                showClearCacheDialog = true
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("清除", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── AI Configuration ──
            item {
                AppDexSection(label = "AI 配置") {
                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                        // Provider selector
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = IconBlue)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("AI 提供商", fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                text = aiConfig.providerType.displayName,
                                fontSize = 11.sp,
                                color = AmberGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        AppDexDivider()
                        // Provider list
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            AiProviderType.entries.chunked(2).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { provider ->
                                        val isSelected = provider == aiConfig.providerType
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) AmberGold else Color.Transparent)
                                                .clickable { viewModel.setAiProvider(provider) }
                                                .padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = provider.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) AmberGoldDark else TextSecondary
                                            )
                                        }
                                    }
                                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        AppDexDivider()
                        // API Key (for providers that need it)
                        if (aiConfig.providerType !in listOf(
                                AiProviderType.OLLAMA, AiProviderType.LM_STUDIO,
                                AiProviderType.LOCALAI, AiProviderType.ANYTHINGLLM
                            )) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("API Key", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                                OutlinedTextField(
                                    value = aiConfig.apiKey,
                                    onValueChange = { viewModel.setAiApiKey(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("输入 API Key", fontSize = 10.sp, color = TextMuted) },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = BorderMedium,
                                        cursorColor = AmberGold
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                            }
                            AppDexDivider()
                        }
                        // Base URL
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Base URL", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                            OutlinedTextField(
                                value = aiConfig.baseUrl,
                                onValueChange = { viewModel.setAiBaseUrl(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(aiConfig.providerType.let { p -> AiConfig(providerType = p).effectiveBaseUrl() }, fontSize = 10.sp, color = TextMuted) },
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = BorderMedium,
                                    cursorColor = AmberGold
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                        }
                        AppDexDivider()
                        // Model name
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("模型", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                            OutlinedTextField(
                                value = aiConfig.modelName,
                                onValueChange = { viewModel.setAiModel(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(aiConfig.defaultModels().firstOrNull() ?: "模型名称", fontSize = 10.sp, color = TextMuted) },
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = BorderMedium,
                                    cursorColor = AmberGold
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                        }
                        // Status
                        AppDexDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("状态", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (isAiEnabled) "已配置" else "未配置",
                                fontSize = 11.sp,
                                color = if (isAiEnabled) AuroraGreen else RedSupergiant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Display Mode ──
            item {
                AppDexSection(label = "工具显示模式") {
                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                        ToolDisplayMode.entries.forEachIndexed { index, mode ->
                            val isSelected = mode == displayMode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) AmberGold.copy(alpha = 0.1f) else Color.Transparent)
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
                                        color = if (isSelected) AmberGold else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = when (mode) {
                                            ToolDisplayMode.NORMAL -> "使用友好名称，隐藏专业术语"
                                            ToolDisplayMode.ADVANCED -> "同时显示友好名称和原始名称"
                                            ToolDisplayMode.EXPERT -> "显示原始技术名称"
                                        },
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = AmberGold)
                                }
                            }
                            if (index < ToolDisplayMode.entries.size - 1) {
                                AppDexDivider()
                            }
                        }
                    }
                }
            }

            // ── Language ──
            item {
                AppDexSection(label = "语言") {
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
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val packageManager = context.packageManager
                                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = IconBlueBright)
                        Text(
                            text = " 立即重启应用",
                            fontSize = 11.sp,
                            color = IconBlueBright
                        )
                    }
                }
            }

            // ── Editor settings ──
            item {
                AppDexSection(label = "编辑器") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight)
                            .background(SurfaceDeep)
                            .padding(16.dp)
                    ) {
                        Text("字体大小: $fontSize sp", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { viewModel.setFontSize(it.toInt()) },
                            valueRange = 10f..24f,
                            steps = 13
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tab 宽度: $tabWidth", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = tabWidth.toFloat(),
                            onValueChange = { viewModel.setTabWidth(it.toInt()) },
                            valueRange = 2f..8f,
                            steps = 5
                        )
                    }
                }
            }

            // ── Terminal settings ──
            item {
                AppDexSection(label = "终端") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight)
                            .background(SurfaceDeep)
                            .padding(16.dp)
                    ) {
                        Text("字体大小: $termFontSize sp", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = termFontSize.toFloat(),
                            onValueChange = { viewModel.setTerminalFontSize(it.toInt()) },
                            valueRange = 10f..20f,
                            steps = 9
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("回滚行数: $termScrollback", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = termScrollback.toFloat(),
                            onValueChange = { viewModel.setTerminalScrollback(it.toInt()) },
                            valueRange = 100f..5000f,
                            steps = 48
                        )
                    }
                }
            }

            // ── About ──
            item {
                AppDexSection(label = "关于") {
                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                        AboutRow("版本", viewModel.getAppVersion())
                        AppDexDivider()
                        AboutRow("协议", "Apache 2.0")
                        AppDexDivider()
                        AboutRow("GitHub", "github.com/niceyayale/appdex")
                        AppDexDivider()
                        AboutRow("引擎", "Jetpack Compose")
                        AppDexDivider()
                        AboutRow("Min Android", "8.0 (API 26)")
                    }
                }
            }

            item {
                Text(
                    text = "APPDEX · 开源 Android 工具箱",
                    fontSize = 10.sp,
                    color = TextTertiary,
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
                    .background(SurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = IconBlue)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, color = TextPrimary)
                Text(detail, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = TextTertiary
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderMedium)
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) AmberGold else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label(item),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AmberGoldDark else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextPrimary)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
    }
}
