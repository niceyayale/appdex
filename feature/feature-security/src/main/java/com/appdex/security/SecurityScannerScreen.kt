package com.appdex.security

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXButton
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXSection
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SecurityScannerScreen(
    apkPath: String? = null,
    onBack: () -> Unit = {},
    viewModel: SecurityScannerViewModel = hiltViewModel(),
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
                viewModel.handleIntent(SecurityScannerIntent.SetApkPath(path))
            }
        }
    }

    LaunchedEffect(apkPath) {
        apkPath?.let {
            if (it.isNotEmpty()) {
                inputPath = it
                viewModel.handleIntent(SecurityScannerIntent.SetApkPath(it))
                viewModel.handleIntent(SecurityScannerIntent.Scan)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SecurityScannerEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is SecurityScannerEffect.ScanComplete -> {
                    val msg = if (effect.result.passed) "安全扫描已通过" else "发现 ${effect.result.criticalCount + effect.result.highCount} 个危险项"
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppXBar(title = "安全扫描", back = true, onBack = onBack, showBell = false)

            if (state.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(48.dp))
                        Text("正在扫描...", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            } else if (state.scanResult != null) {
                ScanResultContent(state = state)
            } else {
                ScanInputContent(
                    inputPath = inputPath,
                    onPathChange = {
                        inputPath = it
                        viewModel.handleIntent(SecurityScannerIntent.SetApkPath(it))
                    },
                    onPickFile = { filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                    onScan = { viewModel.handleIntent(SecurityScannerIntent.Scan) },
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
private fun ScanInputContent(
    inputPath: String,
    onPathChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onScan: () -> Unit,
    error: String?,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().border(1.dp, RedSupergiant).background(RedSupergiant.copy(alpha = 0.1f)).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = RedSupergiant, modifier = Modifier.size(16.dp))
                Text("AppX 安全扫描 · 深度安全审计", fontSize = 10.sp, color = RedSupergiant, fontFamily = FontFamily.Monospace)
            }
        }

        AppXSection(label = "APK 路径") {
            OutlinedTextField(
                value = inputPath,
                onValueChange = onPathChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("APK 文件路径", fontSize = 10.sp) },
                placeholder = { Text("/sdcard/app.apk", fontSize = 10.sp) },
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
            text = "开始安全扫描",
            icon = Icons.Default.BugReport,
            enabled = inputPath.isNotEmpty(),
            onClick = onScan,
        )

        AppXSection(label = "扫描项") {
            Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScanItem("硬编码 API 密钥 / Token / 密码")
                ScanItem("危险权限使用检查")
                ScanItem("导出组件安全风险")
                ScanItem("签名方案安全检测 (V1/V2/V3)")
                ScanItem("弱加密算法检测")
                ScanItem("Missing tamper protection")
                ScanItem("不安全的网络通信配置")
            }
        }

        error?.let { err ->
            Box(modifier = Modifier.fillMaxWidth().border(1.dp, RedSupergiantDark).background(RedSupergiantDark).padding(12.dp)) {
                Text(err, fontSize = 11.sp, color = RedSupergiant)
            }
        }
    }
}

@Composable
private fun ScanResultContent(state: SecurityScannerState) {
    val result = state.scanResult ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // ── 统一评分：使用 state.unifiedScore，与 Workspace/Report 完全一致 ──
            val displayScore = state.unifiedScore
            val scoreColor = when {
                displayScore >= 80 -> AuroraGreen
                displayScore >= 60 -> AmberGold
                else -> RedSupergiant
            }
            Box(
                modifier = Modifier.fillMaxWidth().border(1.dp, scoreColor).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayScore.toString(),
                        fontSize = 36.sp,
                        color = scoreColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (result.passed) "安全扫描已通过" else "存在安全隐患",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatCard("严重", result.criticalCount, RedSupergiant, Modifier.weight(1f))
                StatCard("高危", result.highCount, AmberGold, Modifier.weight(1f))
                StatCard("中危", result.mediumCount, IconBlueBright, Modifier.weight(1f))
                StatCard("低危", result.lowCount, TextSecondary, Modifier.weight(1f))
            }
        }

        item {
            AppXSection(label = "安全隐患 (${result.issues.size})") {}
        }

        items(result.issues) { issue ->
            IssueCard(issue)
        }

        if (result.issues.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuroraGreen).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AuroraGreen, modifier = Modifier.size(32.dp))
                        Text("未发现安全隐患", fontSize = 12.sp, color = AuroraGreen, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.3f)).background(color.copy(alpha = 0.1f)).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), fontSize = 16.sp, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 9.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun IssueCard(issue: SecurityIssue) {
    val color = when (issue.severity) {
        Severity.CRITICAL -> RedSupergiant
        Severity.HIGH -> AmberGold
        Severity.MEDIUM -> IconBlueBright
        Severity.LOW -> TextSecondary
        Severity.INFO -> TextTertiary
    }

    val icon = when (issue.severity) {
        Severity.CRITICAL, Severity.HIGH -> Icons.Default.Warning
        Severity.MEDIUM, Severity.LOW -> Icons.Default.BugReport
        Severity.INFO -> Icons.Default.Shield
    }

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, color.copy(alpha = 0.3f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                text = issue.title,
                fontSize = 11.sp,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = issue.severity.name,
                fontSize = 9.sp,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(text = issue.description, fontSize = 10.sp, color = TextSecondary)
        if (issue.location.isNotEmpty()) {
            Text(text = "位置: ${issue.location}", fontSize = 9.sp, color = TextTertiary, fontFamily = FontFamily.Monospace)
        }
        if (issue.recommendation.isNotEmpty()) {
            Text(text = "建议: ${issue.recommendation}", fontSize = 9.sp, color = IconBlueBright)
        }
    }
}

@Composable
private fun ScanItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = AuroraGreen, modifier = Modifier.size(12.dp))
        Text(text = text, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
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
