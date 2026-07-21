package com.appdex.analyzer

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.apk.ApkInfo
import com.appdex.apk.ApkManifest
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXRow
import com.appdex.ui.components.AppXSection
import com.appdex.ui.components.EmptyState
import com.appdex.ui.theme.*

@Composable
fun ApkDetailScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onOpenDexBrowser: () -> Unit = {},
    onOpenSigning: () -> Unit = {},
    onOpenRepack: () -> Unit = {},
    viewModel: ApkAnalyzerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf("概览") }
    val tabs = listOf("概览", "清单", "DEX", "资源", "签名", "文件")

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            item { AppXBar(title = "APK 详情", back = true, onBack = onBack) }

            state.apkInfo?.let { info ->
                // APK info header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight)
                            .background(SurfaceDeep)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(IconBoxBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            state.appIcon?.let { bitmap ->
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(48.dp)
                                )
                            } ?: Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
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
                                    if (info.manifest.minSdk > 0) {
                                        append(" · API ${info.manifest.minSdk}+")
                                    }
                                },
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        IconButton(onClick = { selectedTab = "签名" }) {
                            Icon(Icons.Default.VpnKey, contentDescription = "签名", tint = TextTertiary)
                        }
                    }
                }

                // Tab bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDeep)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        tabs.forEach { tab ->
                            val selected = tab == selectedTab
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = tab }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tab,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (selected) AmberGoldHighlight else TextTertiary
                                )
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(2.dp)
                                        .background(if (selected) AmberGold else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                // Tab content
                when (selectedTab) {
                    "概览" -> {
                        // Security score card
                        val score = calculateSecurityScore(info)
                        val riskLevel = getRiskLevel(score)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .border(1.dp, BorderAccent)
                                    .background(ScoreCardBg)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "安全评分",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.6.sp,
                                    color = ScoreLabelBlue
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "$score",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = riskLevel,
                                        fontSize = 11.sp,
                                        color = when {
                                            score >= 80 -> AuroraGreen
                                            score >= 60 -> AmberGold
                                            else -> RedSupergiant
                                        },
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(ScoreBarBg)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(score.toFloat() / 100f)
                                            .height(6.dp)
                                            .background(
                                                when {
                                                    score >= 80 -> AuroraGreen
                                                    score >= 60 -> NebulaBlue
                                                    else -> RedSupergiant
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        // Key entries
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AppXSection(label = "快捷入口") {
                                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                                        AppXRow(
                                            icon = Icons.Default.Security,
                                            title = "权限信息",
                                            detail = "${info.manifest.permissions.size} 个权限 · ${info.manifest.activities.size} 个 Activity",
                                            onClick = { selectedTab = "清单" }
                                        )
                                        AppXDivider()
                                        AppXRow(
                                            icon = Icons.Default.Code,
                                            title = "DEX / 类浏览",
                                            detail = "${info.entries.count { it.name.endsWith(".dex") }} 个 DEX 文件",
                                            onClick = { onOpenDexBrowser() }
                                        )
                                        AppXDivider()
                                        AppXRow(
                                            icon = Icons.Default.VpnKey,
                                            title = "签名验证",
                                            detail = info.signatures.joinToString(", ") { "v${it.version}" }.ifEmpty { "无签名" },
                                            onClick = { onOpenSigning() }
                                        )
                                        AppXDivider()
                                        AppXRow(
                                            icon = Icons.Default.Build,
                                            title = "回编译 / 反编",
                                            detail = "Smali -> DEX -> APK -> 签名",
                                            onClick = { onOpenRepack() }
                                        )
                                        AppXDivider()
                                        AppXRow(
                                            icon = Icons.Default.Folder,
                                            title = "APK 文件列表",
                                            detail = "${info.entries.size} 个文件",
                                            onClick = { selectedTab = "文件" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "签名" -> {
                        if (info.signatures.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.VpnKey,
                                    title = "无签名信息",
                                    subtitle = "此 APK 未签名或签名格式不支持",
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            items(info.signatures) { sig ->
                                Column(modifier = Modifier.padding(16.dp)) {
                                    AppXSection(label = "v${sig.version} 签名") {
                                        Column(modifier = Modifier.border(1.dp, BorderLight)) {
                                            DetailRow("算法", sig.algorithm)
                                            AppXDivider()
                                            if (sig.certificateSubject.isNotEmpty()) {
                                                DetailRow("证书主体", sig.certificateSubject)
                                                AppXDivider()
                                            }
                                            if (sig.certificateIssuer.isNotEmpty()) {
                                                DetailRow("颁发者", sig.certificateIssuer)
                                                AppXDivider()
                                            }
                                            if (sig.serialNumber.isNotEmpty()) {
                                                DetailRow("序列号", sig.serialNumber)
                                                AppXDivider()
                                            }
                                            DetailRow("SHA-256", sig.sha256)
                                            AppXDivider()
                                            DetailRow("SHA-1", sig.sha1)
                                            AppXDivider()
                                            DetailRow("MD5", sig.md5)
                                            if (sig.validFrom > 0) {
                                                AppXDivider()
                                                DetailRow("有效期起", FormatUtil.formatTimestamp(sig.validFrom))
                                            }
                                            if (sig.validTo > 0) {
                                                AppXDivider()
                                                DetailRow("有效期至", FormatUtil.formatTimestamp(sig.validTo))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "清单" -> {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AppXSection(label = "Manifest 信息") {
                                    Column(modifier = Modifier.border(1.dp, BorderLight)) {
                                        DetailRow("包名", info.manifest.packageName)
                                        AppXDivider()
                                        DetailRow("版本", "${info.manifest.versionName} (${info.manifest.versionCode})")
                                        AppXDivider()
                                        if (info.manifest.minSdk > 0) {
                                            DetailRow("Min SDK", "${info.manifest.minSdk}")
                                            AppXDivider()
                                        }
                                        if (info.manifest.targetSdk > 0) {
                                            DetailRow("Target SDK", "${info.manifest.targetSdk}")
                                            AppXDivider()
                                        }
                                        DetailRow("Activities", "${info.manifest.activities.size}")
                                        AppXDivider()
                                        DetailRow("Services", "${info.manifest.services.size}")
                                        AppXDivider()
                                        DetailRow("Receivers", "${info.manifest.receivers.size}")
                                        AppXDivider()
                                        DetailRow("Providers", "${info.manifest.providers.size}")
                                        AppXDivider()
                                        DetailRow("Permissions", "${info.manifest.permissions.size}")
                                    }
                                }
                            }
                        }
                        if (info.manifest.permissions.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    AppXSection(label = "权限列表") {
                                        Column(modifier = Modifier.border(1.dp, BorderLight)) {
                                            info.manifest.permissions.forEachIndexed { index, perm ->
                                                AppXRow(
                                                    icon = Icons.Default.Security,
                                                    title = perm,
                                                    iconTint = if (isDangerousPermission(perm)) RedSupergiant else AmberGold,
                                                    showChevron = false
                                                )
                                                if (index < info.manifest.permissions.size - 1) {
                                                    AppXDivider()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "DEX" -> {
                        val dexFiles = info.entries.filter { it.name.endsWith(".dex") }
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AppXSection(label = "DEX 文件 (${dexFiles.size})") {}
                            }
                        }
                        if (dexFiles.isEmpty()) {
                            item {
                                Text(
                                    text = "未找到 DEX 文件",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(dexFiles) { dex ->
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    AppXRow(
                                        icon = Icons.Default.Code,
                                        title = dex.name,
                                        detail = FormatUtil.formatFileSize(dex.size),
                                        iconTint = IconBlue,
                                        onClick = { onOpenDexBrowser() }
                                    )
                                    AppXDivider()
                                }
                            }
                        }
                    }
                    "资源" -> {
                        val resFiles = info.entries.filter { it.name.startsWith("res/") || it.name == "resources.arsc" || it.name.startsWith("assets/") }
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AppXSection(label = "资源文件 (${resFiles.size})") {}
                            }
                        }
                        if (resFiles.isEmpty()) {
                            item {
                                Text(
                                    text = "未找到资源文件",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(resFiles.take(50)) { entry ->
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    AppXRow(
                                        icon = Icons.Default.Folder,
                                        title = entry.name,
                                        detail = FormatUtil.formatFileSize(entry.size),
                                        iconTint = AuroraGreen,
                                        showChevron = false
                                    )
                                    AppXDivider()
                                }
                            }
                            if (resFiles.size > 50) {
                                item {
                                    Text(
                                        text = "... 还有 ${resFiles.size - 50} 个文件",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    "文件" -> {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AppXSection(label = "APK 文件列表 (${info.entries.size})") {}
                            }
                        }
                        items(info.entries.take(100)) { entry ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                AppXRow(
                                    icon = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Code,
                                    title = entry.name,
                                    detail = FormatUtil.formatFileSize(entry.size),
                                    iconTint = if (entry.isDirectory) AmberGold else IconBlue,
                                    showChevron = false
                                )
                                AppXDivider()
                            }
                        }
                        if (info.entries.size > 100) {
                            item {
                                Text(
                                    text = "... 还有 ${info.entries.size - 100} 个文件",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            } ?: run {
                // No APK loaded
                item {
                    EmptyState(
                        icon = Icons.Default.Apps,
                        title = "未选择 APK",
                        subtitle = "请在分析页面选择 APK 文件",
                        modifier = Modifier.padding(top = 64.dp)
                    )
                }
            }
        }
    }
}

// ─── Security scoring ───

private val DANGEROUS_PERMISSIONS = setOf(
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
    "android.permission.PROCESS_OUTGOING_CALLS",
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
    "android.permission.WRITE_SETTINGS", "android.permission.DEVICE_POWER",
    "android.permission.REBOOT", "android.permission.INSTALL_PACKAGES",
    "android.permission.DELETE_PACKAGES"
)

private fun isDangerousPermission(perm: String): Boolean {
    return perm in DANGEROUS_PERMISSIONS
}

private fun calculateSecurityScore(info: ApkInfo): Int {
    var score = 100
    val manifest = info.manifest

    // 1. Dangerous permissions (-4 each, max -32)
    val dangerousCount = manifest.permissions.count { isDangerousPermission(it) }
    score -= minOf(dangerousCount * 4, 32)

    // 2. Signature scheme
    if (info.signatures.isEmpty()) {
        score -= 25
    } else {
        val hasV2 = info.signatures.any { it.version >= 2 }
        val hasV3 = info.signatures.any { it.version >= 3 }
        if (!hasV2 && !hasV3) score -= 10
        if (hasV3) score += 5
    }

    // 3. Multiple signatures (suspicious, -5 each over 1, max -15)
    if (info.signatures.size > 1) {
        score -= minOf((info.signatures.size - 1) * 5, 15)
    }

    // 4. minSdk (very low = risky)
    if (manifest.minSdk in 1..16) score -= 12
    else if (manifest.minSdk in 17..20) score -= 8
    else if (manifest.minSdk in 21..23) score -= 4

    // 5. targetSdk (low = risky)
    if (manifest.targetSdk in 1..22) score -= 12
    else if (manifest.targetSdk in 23..25) score -= 8
    else if (manifest.targetSdk in 26..28) score -= 4

    // 6. Large number of permissions (potential over-permission)
    if (manifest.permissions.size > 30) score -= 8
    else if (manifest.permissions.size > 20) score -= 4

    return score.coerceIn(0, 100)
}

private fun getRiskLevel(score: Int): String {
    return when {
        score >= 80 -> "低风险"
        score >= 60 -> "中风险"
        score >= 40 -> "高风险"
        else -> "极高风险"
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            maxLines = 1
        )
    }
}
