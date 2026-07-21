package com.appdex.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.plugin.PluginEntry
import com.appdex.plugin.PluginManager
import com.appdex.tools.plugins.JsonFormatterPlugin
import com.appdex.tools.plugins.PluginListScreen
import com.appdex.tools.plugins.TextStatsPlugin
import com.appdex.tools.plugins.TimestampConverterPlugin
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXRow
import com.appdex.ui.components.AppXSection
import com.appdex.data.session.ToolDisplayMode
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

// ── Tool definition for search/filter ──
private data class ToolEntry(
    val friendlyName: String,
    val originalName: String,
    val detail: String,
    val icon: ImageVector,
    val section: String,
    val keywords: String,
    val onClickKey: String
)

@Composable
fun ToolsScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onOpenDiff: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenSizeAnalyzer: () -> Unit = {},
    onOpenDexBrowser: () -> Unit = {},
    onOpenSigning: () -> Unit = {},
    onOpenAxmlEditor: () -> Unit = {},
    onOpenArscViewer: () -> Unit = {},
    onOpenSqliteViewer: () -> Unit = {},
    onOpenElfViewer: () -> Unit = {},
    onOpenHexEditor: () -> Unit = {},
    displayMode: ToolDisplayMode = ToolDisplayMode.NORMAL
) {
    fun toolName(friendly: String, original: String): String = when (displayMode) {
        ToolDisplayMode.NORMAL -> friendly
        ToolDisplayMode.ADVANCED -> "$friendly ($original)"
        ToolDisplayMode.EXPERT -> original
    }

    var selectedTool by remember { mutableStateOf<ToolType?>(null) }
    var selectedPlugin by remember { mutableStateOf<PluginEntry?>(null) }
    var showPluginList by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    remember {
        if (PluginManager.count == 0) {
            PluginManager.register(JsonFormatterPlugin())
            PluginManager.register(TextStatsPlugin())
            PluginManager.register(TimestampConverterPlugin())
        }
        true
    }

    // All tools as data for filtering
    val allTools = remember(displayMode) {
        listOf(
            ToolEntry("快速扫描", "APK 扫描", "选择 APK 后执行快速结构扫描", Icons.Default.Devices, "分析与开发", "扫描 scan apk 分析", "analyze"),
            ToolEntry("代码结构", "DEX 浏览器", "类、方法与字符串索引", Icons.Default.Code, "分析与开发", "dex 代码 class method", "dex"),
            ToolEntry("权限审计", "权限检查", "组件导出与危险权限检查", Icons.Default.Security, "分析与开发", "权限 permission security 安全", "security"),
            ToolEntry("签名验证", "签名信息", "查看证书、摘要与签名方案", Icons.Default.VpnKey, "分析与开发", "签名 signing certificate 证书", "signing"),
            ToolEntry("APK 对比", "差异分析", "双 APK 差异分析", Icons.Default.Compare, "分析与开发", "对比 diff compare 差异", "diff"),
            ToolEntry("安全扫描", "漏洞检测", "硬编码密钥/漏洞检测", Icons.Default.BugReport, "分析与开发", "安全 security 漏洞 bug 扫描", "security_scan"),
            ToolEntry("体积分析", "大小分析", "可视化空间占用", Icons.Default.Analytics, "分析与开发", "体积 size 大小 空间", "size"),
            ToolEntry("配置文件", "AXML 编辑器", "二进制 XML 解码/编码", Icons.Default.Code, "分析与开发", "axml xml 配置 manifest 编辑", "axml"),
            ToolEntry("资源文件", "ARSC 资源表", "解析 resources.arsc", Icons.Default.DataObject, "分析与开发", "arsc 资源 resource", "arsc"),
            ToolEntry("数据库", "SQLite 查看", "浏览 .db 数据库表/行", Icons.Default.Storage, "分析与开发", "sqlite db 数据库 database", "sqlite"),
            ToolEntry("原生库", "ELF 查看", "解析 .so 共享库", Icons.Default.Memory, "分析与开发", "elf so 原生 native library", "elf"),
            ToolEntry("终端", "Shell", "本地 shell 会话", Icons.Default.Terminal, "系统工具", "终端 terminal shell 命令", "terminal"),
            ToolEntry("文本编辑器", "编辑器", "支持 .adx 语法定义", Icons.Default.Edit, "系统工具", "编辑 editor 文本 text", "editor"),
            ToolEntry("远程管理", "FTP/WebDAV", "局域网文件访问", Icons.Default.Cloud, "系统工具", "远程 remote ftp webdav 文件", "remote"),
            ToolEntry("插件中心", "Plugins", "${PluginManager.count} 个插件可用", Icons.Default.Extension, "系统工具", "插件 plugin 扩展 extension", "plugins"),
            ToolEntry("哈希计算器", "Hash", "MD5、SHA-1、SHA-256", Icons.Default.Fingerprint, "实用工具", "哈希 hash md5 sha", "hash"),
            ToolEntry("设备信息", "DeviceInfo", "硬件与系统信息", Icons.Default.Devices, "实用工具", "设备 device info 硬件", "device_info"),
            ToolEntry("编码转换", "Encoding", "Base64、URL、Hex", Icons.Default.TextFields, "实用工具", "编码 encoding base64 url hex 转换", "encoding"),
            ToolEntry("二进制查看", "HEX Editor", "十六进制查看与编辑", Icons.Default.Code, "开发工具", "hex 十六进制 二进制 binary 查看 编辑", "hex")
        )
    }

    // Filter tools by search query
    val filteredTools = remember(searchQuery, allTools) {
        if (searchQuery.isBlank()) {
            allTools
        } else {
            val query = searchQuery.lowercase().trim()
            allTools.filter { tool ->
                tool.friendlyName.lowercase().contains(query) ||
                tool.originalName.lowercase().contains(query) ||
                tool.detail.lowercase().contains(query) ||
                tool.keywords.lowercase().contains(query) ||
                tool.section.lowercase().contains(query)
            }
        }
    }

    // Group filtered tools by section
    val groupedTools = remember(filteredTools) {
        filteredTools.groupBy { it.section }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppXTheme.colors.background)) {
        when {
            showPluginList -> {
                PluginListScreen(
                    onBack = { showPluginList = false },
                    onPluginClick = { entry ->
                        selectedPlugin = entry
                        showPluginList = false
                    }
                )
            }
            selectedPlugin != null -> {
                PluginDetailScreen(
                    entry = selectedPlugin!!,
                    onBack = { selectedPlugin = null }
                )
            }
            selectedTool != null -> {
                when (selectedTool) {
                    ToolType.HASH_CALCULATOR -> HashCalculatorScreen(onBack = { selectedTool = null })
                    ToolType.DEVICE_INFO -> DeviceInfoScreen(onBack = { selectedTool = null })
                    ToolType.ENCODING_CONVERTER -> EncodingConverterScreen(onBack = { selectedTool = null })
                    null -> {}
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 0.dp, bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { AppXBar(title = "工具集", back = true, onBack = onBack) }

                    // ── Search Bar ──
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "搜索工具...",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = TextTertiary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { searchQuery = "" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "清除",
                                            modifier = Modifier.size(16.dp),
                                            tint = TextTertiary
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = BorderMedium,
                                cursorColor = AmberGold
                            ),
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    // ── Search results or grouped sections ──
                    if (searchQuery.isNotBlank() && filteredTools.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = TextTertiary
                                )
                                Text(
                                    text = "未找到匹配的工具",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                                Text(
                                    text = "试试其他关键词",
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        groupedTools.forEach { (sectionName, tools) ->
                            item(key = sectionName) {
                                AppXSection(label = sectionName) {
                                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                                        tools.forEachIndexed { index, tool ->
                                            val toolTitle = toolName(tool.friendlyName, tool.originalName)
                                            val onClickAction = when (tool.onClickKey) {
                                                "analyze" -> { { onNavigate("分析") } }
                                                "dex" -> onOpenDexBrowser
                                                "security" -> onOpenSecurity
                                                "signing" -> onOpenSigning
                                                "diff" -> onOpenDiff
                                                "security_scan" -> onOpenSecurity
                                                "size" -> onOpenSizeAnalyzer
                                                "axml" -> onOpenAxmlEditor
                                                "arsc" -> onOpenArscViewer
                                                "sqlite" -> onOpenSqliteViewer
                                                "elf" -> onOpenElfViewer
                                                "terminal" -> { { onNavigate("终端") } }
                                                "editor" -> { { onNavigate("编辑器") } }
                                                "remote" -> { { onNavigate("远程管理") } }
                                                "plugins" -> { { showPluginList = true } }
                                                "hash" -> { { selectedTool = ToolType.HASH_CALCULATOR } }
                                                "device_info" -> { { selectedTool = ToolType.DEVICE_INFO } }
                                                "encoding" -> { { selectedTool = ToolType.ENCODING_CONVERTER } }
                                                "hex" -> onOpenHexEditor
                                                else -> { {} }
                                            }
                                            if (index > 0) AppXDivider()
                                            AppXRow(
                                                icon = tool.icon,
                                                title = toolTitle,
                                                detail = tool.detail,
                                                onClick = onClickAction
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
    }
}

enum class ToolType(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    HASH_CALCULATOR(
        "哈希计算器",
        "MD5、SHA-1、SHA-256",
        Icons.Default.Fingerprint
    ),
    DEVICE_INFO(
        "设备信息",
        "硬件与系统信息",
        Icons.Default.Devices
    ),
    ENCODING_CONVERTER(
        "编码转换",
        "Base64、URL、Hex",
        Icons.Default.TextFields
    )
}

@Composable
private fun PluginDetailScreen(
    entry: PluginEntry,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(AppXTheme.colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppXBar(title = entry.plugin.name, back = true, onBack = onBack)
            Box(modifier = Modifier.fillMaxSize()) {
                entry.plugin.Content()
            }
        }
    }
}
