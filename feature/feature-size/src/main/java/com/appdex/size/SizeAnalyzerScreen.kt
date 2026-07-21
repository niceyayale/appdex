package com.appdex.size

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXButton
import com.appdex.ui.components.AppXSection
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SizeAnalyzerScreen(
    apkPath: String? = null,
    onBack: () -> Unit = {},
    viewModel: SizeAnalyzerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var inputPath by rememberSaveable { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = uri.path ?: ""
            if (path.isNotEmpty()) {
                inputPath = path
                viewModel.handleIntent(SizeAnalyzerIntent.SetApkPath(path))
            }
        }
    }

    LaunchedEffect(apkPath) {
        apkPath?.let {
            if (it.isNotEmpty()) {
                inputPath = it
                viewModel.handleIntent(SizeAnalyzerIntent.SetApkPath(it))
                viewModel.handleIntent(SizeAnalyzerIntent.Analyze)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SizeAnalyzerEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is SizeAnalyzerEffect.AnalyzeComplete -> scope.launch { snackbarHostState.showSnackbar("分析完成") }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppXBar(title = "体积分析", back = true, onBack = onBack, showBell = false)

            if (state.isAnalyzing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(48.dp))
                        Text("正在分析...", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            } else if (state.result != null) {
                SizeResultContent(state = state)
            } else {
                SizeInputContent(
                    inputPath = inputPath,
                    onPathChange = {
                        inputPath = it
                        viewModel.handleIntent(SizeAnalyzerIntent.SetApkPath(it))
                    },
                    onPickFile = { filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                    onAnalyze = { viewModel.handleIntent(SizeAnalyzerIntent.Analyze) },
                    error = state.error,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SizeInputContent(
    inputPath: String,
    onPathChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onAnalyze: () -> Unit,
    error: String?,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().border(1.dp, IconBlueBright).background(IconBlueBright.copy(alpha = 0.1f)).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = IconBlueBright, modifier = Modifier.size(16.dp))
                Text("AppX 差异分析 · 可视化空间占用", fontSize = 10.sp, color = IconBlueBright, fontFamily = FontFamily.Monospace)
            }
        }

        AppXSection(label = "APK 路径") {
            OutlinedTextField(
                value = inputPath,
                onValueChange = onPathChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("APK 文件路径", fontSize = 10.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                colors = textFieldColors(),
            )
        }

        AppXButton(
            text = "选择 APK 文件",
            icon = Icons.Default.Folder,
            onClick = onPickFile,
        )

        AppXButton(
            text = "开始分析",
            icon = Icons.Default.Analytics,
            enabled = inputPath.isNotEmpty(),
            onClick = onAnalyze,
        )

        error?.let { err ->
            Box(modifier = Modifier.fillMaxWidth().border(1.dp, RedSupergiantDark).background(RedSupergiantDark).padding(12.dp)) {
                Text(err, fontSize = 11.sp, color = RedSupergiant)
            }
        }
    }
}

@Composable
private fun SizeResultContent(state: SizeAnalyzerState) {
    val result = state.result ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().border(1.dp, AmberGold).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = FormatUtil.formatFileSize(result.totalSize),
                        fontSize = 24.sp,
                        color = AmberGold,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text("APK 总大小", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = "压缩后 ${FormatUtil.formatFileSize(result.compressedSize)} | ${result.fileCount} 个文件",
                        fontSize = 9.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        item {
            AppXSection(label = "空间分布") {
                Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(4.dp))
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            result.categories.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(cat.percentage)
                                        .background(Color(cat.category.color))
                                )
                            }
                        }
                    }

                    result.categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color(cat.category.color)))
                            Text(
                                text = cat.category.displayName,
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = FormatUtil.formatFileSize(cat.totalSize),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${String.format("%.1f", cat.percentage)}%",
                                fontSize = 10.sp,
                                color = TextTertiary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        item {
            AppXSection(label = "最大文件 (Top ${result.largestFiles.size})") {}
        }

        items(result.largestFiles.take(10)) { file ->
            Row(
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(file.category.color).copy(alpha = 0.2f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(4.dp, 32.dp).background(Color(file.category.color)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.path,
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FormatUtil.formatFileSize(file.size)} -> ${FormatUtil.formatFileSize(file.compressedSize)} (压缩)",
                        fontSize = 9.sp,
                        color = TextTertiary
                    )
                }
            }
        }

        if (result.duplicateFiles.isNotEmpty()) {
            item {
                AppXSection(label = "疑似重复文件 (${result.duplicateFiles.size})") {}
            }

            items(result.duplicateFiles) { file ->
                Row(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AmberGold.copy(alpha = 0.2f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.path,
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = FormatUtil.formatFileSize(file.size), fontSize = 9.sp, color = TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AmberGold,
    unfocusedBorderColor = BorderMedium,
    cursorColor = AmberGold,
    focusedLabelColor = AmberGold,
    unfocusedLabelColor = TextTertiary,
)
