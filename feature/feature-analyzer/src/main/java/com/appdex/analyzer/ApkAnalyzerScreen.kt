package com.appdex.analyzer

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.apk.ApkEntry
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.EmptyState
import com.appdex.ui.components.ErrorState
import com.appdex.ui.components.LoadingState
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkAnalyzerScreen(
    onOpenDetail: () -> Unit = {},
    viewModel: ApkAnalyzerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ApkAnalyzerEffect.Error -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleIntent(ApkAnalyzerIntent.OpenApk(uri))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppDexBar(title = "APK 分析")

            if (state.isLoading) {
                LoadingState()
            } else if (state.apkInfo == null && state.error == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Apps,
                        title = "选择 APK 文件进行分析",
                        subtitle = "点击下方按钮选择 APK"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(44.dp)
                            .background(AmberGold)
                            .clickable {
                                filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择 APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGoldDark
                        )
                    }
                }
            } else if (state.error != null) {
                ErrorState(
                    message = state.error ?: "Unknown error",
                    onRetry = { filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) }
                )
            } else {
                val info = state.apkInfo!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Detail navigation button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderAccent)
                                .background(ScoreCardBg)
                                .clickable { onOpenDetail() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(IconBoxBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = IconBlueBright
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.manifest.packageName.ifEmpty { "未知包名" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = buildString {
                                        info.manifest.versionName.takeIf { it.isNotEmpty() }?.let { append(it) }
                                        append(" · ")
                                        append(FormatUtil.formatFileSize(info.fileSize))
                                    },
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "查看详情",
                                modifier = Modifier.size(20.dp),
                                tint = AmberGold
                            )
                        }
                    }

                    // Basic info card
                    item {
                        InfoCard(title = "基本信息") {
                            InfoRow("文件大小", FormatUtil.formatFileSize(info.fileSize))
                            InfoRow("文件总数", "${info.entries.size}")
                            info.manifest.packageName.takeIf { it.isNotEmpty() }?.let { InfoRow("包名", it) }
                            info.manifest.versionName.takeIf { it.isNotEmpty() }?.let {
                                InfoRow("版本", "${it} (${info.manifest.versionCode})")
                            }
                            if (info.manifest.minSdk > 0) InfoRow("Min SDK", "${info.manifest.minSdk}")
                            if (info.manifest.targetSdk > 0) InfoRow("Target SDK", "${info.manifest.targetSdk}")
                        }
                    }

                    // Signatures card
                    if (info.signatures.isNotEmpty()) {
                        item {
                            InfoCard(title = "签名 (${info.signatures.size})") {
                                info.signatures.forEachIndexed { index, sig ->
                                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "v${sig.version} 签名",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    InfoRow("算法", sig.algorithm)
                                    if (sig.certificateSubject.isNotEmpty()) {
                                        InfoRow("主体", sig.certificateSubject)
                                    }
                                    if (sig.certificateIssuer.isNotEmpty()) {
                                        InfoRow("颁发者", sig.certificateIssuer)
                                    }
                                    if (sig.serialNumber.isNotEmpty()) {
                                        InfoRow("序列号", sig.serialNumber)
                                    }
                                    if (sig.validFrom > 0) {
                                        InfoRow("有效期起", FormatUtil.formatTimestamp(sig.validFrom))
                                    }
                                    if (sig.validTo > 0) {
                                        InfoRow("有效期至", FormatUtil.formatTimestamp(sig.validTo))
                                    }
                                    InfoRow("SHA-256", sig.sha256)
                                    InfoRow("SHA-1", sig.sha1)
                                    InfoRow("MD5", sig.md5)
                                }
                            }
                        }
                    }

                    // Permissions — lazy items
                    if (info.manifest.permissions.isNotEmpty()) {
                        item {
                            Text(
                                text = "权限 (${info.manifest.permissions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(info.manifest.permissions) { perm ->
                            Text(
                                text = "• $perm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Activities — lazy items
                    if (info.manifest.activities.isNotEmpty()) {
                        item {
                            Text(
                                text = "Activities (${info.manifest.activities.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(info.manifest.activities) { act ->
                            Text(
                                text = "• $act",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Services — lazy items
                    if (info.manifest.services.isNotEmpty()) {
                        item {
                            Text(
                                text = "Services (${info.manifest.services.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(info.manifest.services) { svc ->
                            Text(
                                text = "• $svc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Receivers — lazy items
                    if (info.manifest.receivers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Receivers (${info.manifest.receivers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(info.manifest.receivers) { rcv ->
                            Text(
                                text = "• $rcv",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Providers — lazy items
                    if (info.manifest.providers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Providers (${info.manifest.providers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(info.manifest.providers) { prov ->
                            Text(
                                text = "• $prov",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Entries
                    item {
                        Text(
                            text = "文件列表 (${info.entries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(info.entries.sortedBy { it.name }) { entry ->
                        EntryRow(entry)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun EntryRow(entry: ApkEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Monospace
        )
        if (!entry.isDirectory) {
            Text(
                text = FormatUtil.formatFileSize(entry.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
