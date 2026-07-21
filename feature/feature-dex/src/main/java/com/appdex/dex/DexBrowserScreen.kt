package com.appdex.dex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXSearchBar
import com.appdex.ui.components.AppXSection
import com.appdex.ui.components.AppXSnackbarHost
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DexBrowserScreen(
    apkPath: String,
    searchQuery: String? = null,
    onBack: () -> Unit = {},
    viewModel: DexBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val path = uri.path ?: ""
            if (path.isNotEmpty()) {
                viewModel.handleIntent(DexBrowserIntent.LoadDexFiles(path))
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(apkPath) {
        if (apkPath.isNotEmpty() && state.apkPath != apkPath) {
            viewModel.handleIntent(DexBrowserIntent.LoadDexFiles(apkPath))
        }
    }

    // ── Navigation Context: auto-search when searchQuery is provided ──
    // 来自 Command Palette 或 Manifest 组件点击的跨工具联动
    // 如果在 DEX_LIST 层级，自动选择第一个 DEX 文件，然后搜索
    androidx.compose.runtime.LaunchedEffect(searchQuery, state.dexFiles, state.viewLevel) {
        if (!searchQuery.isNullOrEmpty() && state.dexFiles.isNotEmpty()) {
            if (state.viewLevel == DexViewLevel.DEX_LIST && state.dexFiles.size > 1) {
                // 多个 DEX 时自动选择第一个
                viewModel.handleIntent(DexBrowserIntent.SelectDex(state.dexFiles[0].name))
            } else if (state.viewLevel == DexViewLevel.CLASS_LIST && state.searchQuery != searchQuery) {
                // 已在类列表层级，直接搜索
                viewModel.handleIntent(DexBrowserIntent.SearchClasses(searchQuery))
            }
        }
    }

    // ── RC5: Report selection to workspace context + emit WorkspaceEvent ──
    val workspaceReporter = com.appdex.ui.components.LocalWorkspaceReporter.current
    val workspaceEventBus = com.appdex.ui.components.LocalWorkspaceEventBus.current
    androidx.compose.runtime.LaunchedEffect(state.selectedClassType, state.viewLevel) {
        if (state.viewLevel == DexViewLevel.SMALI_VIEWER && state.selectedClassType.isNotEmpty()) {
            workspaceReporter?.report(
                panel = "DEX Browser",
                selection = state.selectedClassType,
                action = "查看类: ${state.selectedClassType}",
                timelineType = "VIEW",
                timelineTitle = "查看 Smali 代码",
                timelineDetail = state.selectedClassType
            )
            // RC5: Emit SelectClass event so all tools auto-react
            workspaceEventBus?.emit(com.appdex.data.workspace.WorkspaceEvent.SelectClass(state.selectedClassType))
        }
    }

    // 收集 Effect
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DexBrowserEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is DexBrowserEffect.ShowToast -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                AppXBar(
                    title = when (state.viewLevel) {
                        DexViewLevel.DEX_LIST -> "DEX 浏览"
                        DexViewLevel.CLASS_LIST -> state.selectedDexName
                        DexViewLevel.SMALI_VIEWER -> "Smali"
                    },
                    back = true,
                    onBack = {
                        when (state.viewLevel) {
                            DexViewLevel.DEX_LIST -> onBack()
                            DexViewLevel.CLASS_LIST -> viewModel.handleIntent(DexBrowserIntent.BackToDexList)
                            DexViewLevel.SMALI_VIEWER -> viewModel.handleIntent(DexBrowserIntent.BackToClassList)
                        }
                    },
                    showBell = false,
                )
            }

            when (state.viewLevel) {
                DexViewLevel.DEX_LIST -> DexListContent(state, viewModel)
                DexViewLevel.CLASS_LIST -> ClassListContent(state, viewModel)
                DexViewLevel.SMALI_VIEWER -> SmaliContent(state)
            }
        }

        AppXSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// DEX 文件列表
private fun androidx.compose.foundation.lazy.LazyListScope.DexListContent(
    state: DexBrowserState,
    viewModel: DexBrowserViewModel,
) {
    if (state.isLoading) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    state.error?.let { err ->
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = err, fontSize = 12.sp, color = RedSupergiant)
            }
        }
        return
    }

    item {
        Column(modifier = Modifier.padding(16.dp)) {
                AppXSection(label = "DEX 文件 (${state.dexFiles.size})") {
                Column(modifier = Modifier.border(1.dp, BorderLight)) {
                    state.dexFiles.forEachIndexed { index, dex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.handleIntent(DexBrowserIntent.SelectDex(dex.name))
                                }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SurfaceAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = IconBlue
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dex.name,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FormatUtil.formatFileSize(dex.size),
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextTertiary
                            )
                        }
                        if (index < state.dexFiles.size - 1) {
                            AppXDivider()
                        }
                    }
                }
            }
        }
    }
}

// 类列表
private fun androidx.compose.foundation.lazy.LazyListScope.ClassListContent(
    state: DexBrowserState,
    viewModel: DexBrowserViewModel,
) {
    // 搜索栏
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppXSearchBar(
                placeholder = "搜索类名、方法名",
                query = state.searchQuery,
                onQueryChange = { query ->
                    viewModel.handleIntent(DexBrowserIntent.SearchClasses(query))
                }
            )
        }
    }

    if (state.isLoading) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    state.error?.let { err ->
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = err, fontSize = 12.sp, color = RedSupergiant)
            }
        }
        return
    }

    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "共 ${state.filteredClasses.size} 个类",
                fontSize = 10.sp,
                color = TextTertiary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    // 类列表按包名分组
    val grouped = state.filteredClasses.groupBy { it.packageName }
    grouped.forEach { (packageName, classes) ->
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = packageName.ifEmpty { "(default package)" },
                    fontSize = 10.sp,
                    color = IconBlueBright,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
        }
        items(classes) { cls ->
            ClassRow(cls) {
                viewModel.handleIntent(DexBrowserIntent.SelectClass(cls.type))
            }
        }
    }

    if (state.filteredClasses.isEmpty() && !state.isLoading) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.searchQuery.isNotEmpty()) "No matching classes found" else "No classes",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ClassRow(
    cls: DexClassInfo,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Access modifier indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                val isInterface = (cls.accessFlags and 0x200) != 0
                val isAbstract = (cls.accessFlags and 0x400) != 0
                val isEnum = (cls.accessFlags and 0x4000) != 0
                val icon = when {
                    isInterface -> Icons.Default.SportsEsports
                    isEnum -> Icons.Default.Code
                    else -> Icons.Default.Code
                }
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when {
                        isInterface -> AuroraGreen
                        isAbstract -> AmberGold
                        isEnum -> NebulaBlue
                        else -> IconBlue
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cls.simpleName,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(formatAccessFlags(cls.accessFlags))
                        if (cls.superclass != null) {
                            append(" | extends ")
                            append(extractSimpleName(cls.superclass))
                        }
                        append(" | ${cls.methodCount}m/${cls.fieldCount}f")
                    },
                    fontSize = 9.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = TextTertiary
            )
        }
        AppXDivider()
    }
}

// Smali 代码查看
private fun androidx.compose.foundation.lazy.LazyListScope.SmaliContent(
    state: DexBrowserState,
) {
    if (state.isLoadingSmali) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(32.dp))
                    Text(
                        text = "正在反汇编...",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        return
    }

    state.error?.let { err ->
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = err, fontSize = 12.sp, color = RedSupergiant)
            }
        }
        return
    }

    // 类信息头
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = state.selectedClassType,
                fontSize = 11.sp,
                color = AmberGoldHighlight,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "${state.selectedDexName} | ${state.smaliCode.count { it == '\n' }} lines",
                fontSize = 9.sp,
                color = TextTertiary,
            )
        }
    }

    // Smali 代码逐行渲染
    val lines = state.smaliCode.split("\n")
    items(lines.size) { index ->
        val line = lines[index]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 10.sp,
                color = AsteroidBelt,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(36.dp).padding(end = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            Text(
                text = line,
                fontSize = 10.sp,
                color = when {
                    line.startsWith(".class") || line.startsWith(".super") || line.startsWith(".source") -> AmberGold
                    line.startsWith(".method") || line.startsWith(".end method") -> AuroraGreen
                    line.startsWith(".field") -> NebulaBlue
                    line.startsWith("#") -> AsteroidBelt
                    line.startsWith(".") -> IconBlueBright
                    else -> TerminalText
                },
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 工具函数

private fun formatAccessFlags(flags: Int): String {
    val parts = mutableListOf<String>()
    if (flags and 0x1 != 0) parts.add("public")
    if (flags and 0x2 != 0) parts.add("private")
    if (flags and 0x4 != 0) parts.add("protected")
    if (flags and 0x8 != 0) parts.add("static")
    if (flags and 0x10 != 0) parts.add("final")
    if (flags and 0x200 != 0) parts.add("interface")
    if (flags and 0x400 != 0) parts.add("abstract")
    if (flags and 0x1000 != 0) parts.add("synthetic")
    if (flags and 0x4000 != 0) parts.add("enum")
    if (flags and 0x20000 != 0) parts.add("annotation")
    return if (parts.isEmpty()) "package" else parts.joinToString(" ")
}

private fun extractSimpleName(typeDescriptor: String): String {
    val inner = typeDescriptor.removePrefix("L").removeSuffix(";")
    val lastSlash = inner.lastIndexOf('/')
    return if (lastSlash >= 0) inner.substring(lastSlash + 1) else inner
}
