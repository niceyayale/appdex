package com.appdex.diff

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Remove
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
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXButton
import com.appdex.ui.components.AppXSection
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ApkDiffScreen(
    onBack: () -> Unit = {},
    viewModel: ApkDiffViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var oldApkPath by rememberSaveable { mutableStateOf("") }
    var newApkPath by rememberSaveable { mutableStateOf("") }

    val oldFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = uri.path ?: ""
            if (path.isNotEmpty()) {
                oldApkPath = path
                viewModel.handleIntent(ApkDiffIntent.SetOldApk(path))
            }
        }
    }

    val newFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = uri.path ?: ""
            if (path.isNotEmpty()) {
                newApkPath = path
                viewModel.handleIntent(ApkDiffIntent.SetNewApk(path))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ApkDiffEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is ApkDiffEffect.DiffComplete -> scope.launch { snackbarHostState.showSnackbar("对比完成") }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppXBar(title = "APK 对比", back = true, onBack = onBack, showBell = false)

            if (state.isDiffing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(48.dp))
                        Text("正在对比...", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            } else if (state.diffResult != null) {
                DiffResultContent(state = state)
            } else {
                DiffInputContent(
                    oldApkPath = oldApkPath,
                    newApkPath = newApkPath,
                    onOldPathChange = {
                        oldApkPath = it
                        viewModel.handleIntent(ApkDiffIntent.SetOldApk(it))
                    },
                    onNewPathChange = {
                        newApkPath = it
                        viewModel.handleIntent(ApkDiffIntent.SetNewApk(it))
                    },
                    onPickOldFile = { oldFilePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                    onPickNewFile = { newFilePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                    onDiff = { viewModel.handleIntent(ApkDiffIntent.RunDiff) },
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
private fun DiffInputContent(
    oldApkPath: String,
    newApkPath: String,
    onOldPathChange: (String) -> Unit,
    onNewPathChange: (String) -> Unit,
    onPickOldFile: () -> Unit,
    onPickNewFile: () -> Unit,
    onDiff: () -> Unit,
    error: String?,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IconBlueBright)
                .background(IconBlueBright.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Compare, contentDescription = null, tint = IconBlueBright, modifier = Modifier.size(16.dp))
                Text(
                    text = "AppX 差异分析 · 双 APK 结构对比",
                    fontSize = 10.sp,
                    color = IconBlueBright,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        AppXSection(label = "旧版 APK") {
            OutlinedTextField(
                value = oldApkPath,
                onValueChange = onOldPathChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("APK 文件路径", fontSize = 10.sp) },
                placeholder = { Text("/sdcard/app-old.apk", fontSize = 10.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                colors = textFieldColors(),
            )
        }

        AppXButton(
            text = "选择旧版 APK",
            icon = Icons.Default.Folder,
            onClick = onPickOldFile,
        )

        AppXSection(label = "新版 APK") {
            OutlinedTextField(
                value = newApkPath,
                onValueChange = onNewPathChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("APK 文件路径", fontSize = 10.sp) },
                placeholder = { Text("/sdcard/app-new.apk", fontSize = 10.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                colors = textFieldColors(),
            )
        }

        AppXButton(
            text = "选择新版 APK",
            icon = Icons.Default.Folder,
            onClick = onPickNewFile,
        )

        AppXButton(
            text = "开始对比",
            icon = Icons.Default.Compare,
            onClick = onDiff,
            enabled = oldApkPath.isNotEmpty() && newApkPath.isNotEmpty()
        )

        error?.let { err ->
            Box(
                modifier = Modifier.fillMaxWidth().border(1.dp, RedSupergiantDark).background(RedSupergiantDark).padding(12.dp),
            ) {
                Text(text = err, fontSize = 11.sp, color = RedSupergiant)
            }
        }
    }
}

@Composable
private fun DiffResultContent(state: ApkDiffState) {
    val result = state.diffResult ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppXSection(label = "对比摘要") {
                Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("旧版 APK", "${result.oldApkName} (${FormatUtil.formatFileSize(result.oldApkSize)})")
                    SummaryRow("新版 APK", "${result.newApkName} (${FormatUtil.formatFileSize(result.newApkSize)})")
                    val delta = result.newApkSize - result.oldApkSize
                    SummaryRow(
                        "体积变化",
                        "${if (delta >= 0) "+" else ""}${FormatUtil.formatFileSize(delta)}"
                    )
                    SummaryRow("签名", "${result.oldSignatureInfo} -> ${result.newSignatureInfo}")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiffStatCard("Added", result.addedCount, AuroraGreen, Modifier.weight(1f))
                DiffStatCard("Removed", result.removedCount, RedSupergiant, Modifier.weight(1f))
                DiffStatCard("Modified", result.modifiedCount, AmberGold, Modifier.weight(1f))
                DiffStatCard("Same", result.sameCount, TextTertiary, Modifier.weight(1f))
            }
        }

        if (result.manifestDiff.addedPermissions.isNotEmpty() ||
            result.manifestDiff.removedPermissions.isNotEmpty() ||
            result.manifestDiff.addedActivities.isNotEmpty() ||
            result.manifestDiff.removedActivities.isNotEmpty()) {
            item {
                ManifestDiffSection(result.manifestDiff)
            }
        }

        item {
            AppXSection(label = "文件差异 (${result.fileDiffs.size})") {
                Text(
                    text = "仅展示有变化的文件",
                    fontSize = 9.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        val changedFiles = result.fileDiffs.filter { it.type != DiffType.SAME }
        items(changedFiles) { diff ->
            FileDiffRow(diff)
        }
    }
}

@Composable
private fun DiffStatCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun ManifestDiffSection(diff: ManifestDiff) {
    AppXSection(label = "清单差异") {
        Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (diff.addedPermissions.isNotEmpty()) {
                ManifestDiffGroup("新增权限 (+${diff.addedPermissions.size})", diff.addedPermissions, AuroraGreen)
            }
            if (diff.removedPermissions.isNotEmpty()) {
                ManifestDiffGroup("移除权限 (-${diff.removedPermissions.size})", diff.removedPermissions, RedSupergiant)
            }
            if (diff.addedActivities.isNotEmpty()) {
                ManifestDiffGroup("新增 Activity (+${diff.addedActivities.size})", diff.addedActivities, AuroraGreen)
            }
            if (diff.removedActivities.isNotEmpty()) {
                ManifestDiffGroup("移除 Activity (-${diff.removedActivities.size})", diff.removedActivities, RedSupergiant)
            }
            if (diff.addedServices.isNotEmpty()) {
                ManifestDiffGroup("新增 Service (+${diff.addedServices.size})", diff.addedServices, AuroraGreen)
            }
            if (diff.removedServices.isNotEmpty()) {
                ManifestDiffGroup("移除 Service (-${diff.removedServices.size})", diff.removedServices, RedSupergiant)
            }
        }
    }
}

@Composable
private fun ManifestDiffGroup(label: String, items: List<String>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 10.sp, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        items.take(5).forEach { item ->
            Text(
                text = "  $item",
                fontSize = 9.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (items.size > 5) {
            Text(text = "  ...${items.size - 5} more", fontSize = 9.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun FileDiffRow(diff: FileDiff) {
    val (color, icon) = when (diff.type) {
        DiffType.ADDED -> AuroraGreen to Icons.Default.Add
        DiffType.REMOVED -> RedSupergiant to Icons.Default.Remove
        DiffType.MODIFIED -> AmberGold to Icons.Default.FileUpload
        DiffType.SAME -> TextTertiary to Icons.Default.Compare
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = diff.path,
                fontSize = 10.sp,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (diff.type == DiffType.MODIFIED) {
                Text(
                    text = "${FormatUtil.formatFileSize(diff.oldSize)} -> ${FormatUtil.formatFileSize(diff.newSize)} (${if (diff.sizeDelta >= 0) "+" else ""}${FormatUtil.formatFileSize(diff.sizeDelta)})",
                    fontSize = 9.sp,
                    color = TextTertiary
                )
            } else if (diff.type == DiffType.ADDED) {
                Text(text = FormatUtil.formatFileSize(diff.newSize), fontSize = 9.sp, color = TextTertiary)
            } else if (diff.type == DiffType.REMOVED) {
                Text(text = FormatUtil.formatFileSize(diff.oldSize), fontSize = 9.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = TextTertiary)
        Text(text = value, fontSize = 10.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
