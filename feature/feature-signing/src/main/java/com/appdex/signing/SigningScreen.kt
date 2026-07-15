package com.appdex.signing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexButton
import com.appdex.ui.components.AppDexDivider
import com.appdex.ui.components.AppDexRow
import com.appdex.ui.components.AppDexSection
import com.appdex.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SigningScreen(
    apkPath: String,
    onBack: () -> Unit = {},
    viewModel: SigningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val apkFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = uri.path ?: ""
            viewModel.handleIntent(SigningIntent.SetInputApk(path))
        }
    }

    // 首次打开时设置 APK 路径
    LaunchedEffect(apkPath) {
        if (apkPath.isNotEmpty() && state.inputApkPath != apkPath) {
            viewModel.handleIntent(SigningIntent.SetInputApk(apkPath))
        }
    }

    // 收集 Effect
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SigningEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is SigningEffect.ShowToast -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is SigningEffect.SigningComplete -> {
                    val msg = if (effect.result.success) "签名成功" else "签名完成但验证失败"
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        }
    }

    // Keystore 路径和密码状态
    var keystorePath by remember { mutableStateOf("") }
    var keystorePassword by remember { mutableStateOf("") }

    // 创建 Keystore 状态
    var showCreateKeystore by remember { mutableStateOf(false) }
    var newKeystorePath by remember { mutableStateOf("") }
    var newKeystorePassword by remember { mutableStateOf("") }
    var newAlias by remember { mutableStateOf("appdex") }
    var newKeyPassword by remember { mutableStateOf("") }
    var newSubject by remember { mutableStateOf("CN=APPDEX, O=APPDEX, C=CN") }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppDexBar(
                title = "APK 签名",
                back = true,
                onBack = onBack,
                showBell = false,
            )

            when (state.step) {
                SigningStep.SELECT_APK -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        com.appdex.ui.components.AppDexSection(label = "选择 APK") {
                            com.appdex.ui.components.AppDexButton(
                                text = "选择 APK 文件",
                                icon = Icons.Default.Folder,
                                onClick = { apkFilePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) }
                            )
                        }
                    }
                }
                SigningStep.SELECT_KEYSTORE -> {
                    KeystoreSelectionContent(
                        state = state,
                        keystorePath = keystorePath,
                        keystorePassword = keystorePassword,
                        onPathChange = { keystorePath = it },
                        onPasswordChange = { keystorePassword = it },
                        onLoad = {
                            viewModel.handleIntent(SigningIntent.LoadKeystore(keystorePath, keystorePassword))
                        },
                        onCreateNew = { showCreateKeystore = true },
                    )
                }
                SigningStep.ENTER_CREDENTIALS -> {
                    CredentialsContent(
                        state = state,
                        keyPassword = state.keyPassword,
                        onKeyPasswordChange = { viewModel.handleIntent(SigningIntent.SetKeyPassword(it)) },
                        onSelectEntry = { viewModel.handleIntent(SigningIntent.SelectEntry(it)) },
                    )
                }
                SigningStep.SIGN_OPTIONS -> {
                    SignOptionsContent(
                        state = state,
                        onToggleScheme = { v1, v2, v3 ->
                            viewModel.handleIntent(SigningIntent.ToggleScheme(v1, v2, v3))
                        },
                        onSign = {
                            val outputPath = state.inputApkPath.replace(".apk", "_signed.apk")
                            viewModel.handleIntent(SigningIntent.Sign(outputPath))
                        },
                    )
                }
                SigningStep.SIGNING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(48.dp))
                            Text(
                                text = "正在签名...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                SigningStep.COMPLETE -> {
                    SigningResultContent(
                        state = state,
                        onBack = onBack,
                    )
                }
            }
        }
        
        // 创建 Keystore 对话框
        if (showCreateKeystore) {
        CreateKeystoreDialog(
        keystorePath = newKeystorePath,
        keystorePassword = newKeystorePassword,
        alias = newAlias,
        keyPassword = newKeyPassword,
        subject = newSubject,
        isCreating = state.isCreatingKeystore,
        onPathChange = { newKeystorePath = it },
        onPasswordChange = { newKeystorePassword = it },
        onAliasChange = { newAlias = it },
        onKeyPasswordChange = { newKeyPassword = it },
        onSubjectChange = { newSubject = it },
        onCreate = {
        viewModel.handleIntent(
        SigningIntent.CreateKeystore(
        path = newKeystorePath,
        keystorePassword = newKeystorePassword,
        alias = newAlias,
        keyPassword = newKeyPassword,
        subject = newSubject,
        )
        )
        },
        onDismiss = { showCreateKeystore = false },
        )
        }
        
        SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// Keystore 选择
@Composable
private fun KeystoreSelectionContent(
    state: SigningState,
    keystorePath: String,
    keystorePassword: String,
    onPathChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoad: () -> Unit,
    onCreateNew: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // APK 淇℃伅
        AppDexSection(label = "APK 信息") {
            Column(modifier = Modifier.border(1.dp, BorderLight).background(SurfaceAlt).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.inputApkName,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val fileSize = try { File(state.inputApkPath).length() } catch (e: Exception) { 0L }
                        Text(
                            text = FormatUtil.formatFileSize(fileSize),
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(Icons.Default.Check, contentDescription = null, tint = AuroraGreen, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Keystore 錝犺浇
        AppDexSection(label = "Keystore 加载") {
            Column(modifier = Modifier.border(1.dp, BorderLight).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keystorePath,
                    onValueChange = onPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Keystore 路径", fontSize = 10.sp) },
                    placeholder = { Text("/sdcard/keystore.jks", fontSize = 10.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
                OutlinedTextField(
                    value = keystorePassword,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Keystore 密码", fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
                AppDexButton(
                    text = "加载 Keystore",
                    icon = Icons.Default.Folder,
                    onClick = onLoad
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onCreateNew),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Create, contentDescription = null, tint = IconBlueBright, modifier = Modifier.size(14.dp))
                    Text(
                        text = " 创建新 Keystore",
                        fontSize = 11.sp,
                        color = IconBlueBright,
                    )
                }
            }
        }

        // Error
        state.error?.let { err ->
            Box(
                modifier = Modifier.fillMaxWidth().border(1.dp, RedSupergiantDark).background(RedSupergiantDark).padding(12.dp),
            ) {
                Text(text = err, fontSize = 11.sp, color = RedSupergiant)
            }
        }
    }
}

// 凭证输入
@Composable
private fun CredentialsContent(
    state: SigningState,
    keyPassword: String,
    onKeyPasswordChange: (String) -> Unit,
    onSelectEntry: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppDexSection(label = "选择别名") {
            Column(modifier = Modifier.border(1.dp, BorderLight)) {
                state.keystoreEntries.forEachIndexed { index, entry ->
                    val isSelected = entry.alias == state.selectedAlias
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectEntry(entry.alias) }
                            .background(if (isSelected) AmberGoldDark else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(SurfaceAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp), tint = AmberGold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.alias,
                                fontSize = 12.sp,
                                color = if (isSelected) AmberGoldHighlight else TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = entry.subject,
                                fontSize = 9.sp,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            Text(
                                text = "${sdf.format(entry.notBefore)} ~ ${sdf.format(entry.notAfter)}",
                                fontSize = 9.sp,
                                color = TextTertiary
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < state.keystoreEntries.size - 1) {
                        AppDexDivider()
                    }
                }
            }
        }

        if (state.selectedAlias.isNotEmpty()) {
            AppDexSection(label = "Key 密码 (留空则使用 Keystore 密码)") {
                OutlinedTextField(
                    value = keyPassword,
                    onValueChange = onKeyPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Key 密码", fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
            }

            AppDexButton(
                text = "下一步 签名选项",
                icon = Icons.Default.Check,
                onClick = { onSelectEntry(state.selectedAlias) }
            )
        }
    }
}

// 签名选项
@Composable
private fun SignOptionsContent(
    state: SigningState,
    onToggleScheme: (Boolean, Boolean, Boolean) -> Unit,
    onSign: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppDexSection(label = "签名方案") {
            Column(modifier = Modifier.border(1.dp, BorderLight)) {
                SchemeToggleRow(
                    name = "V1 (JAR 签名)",
                    description = "兼容旧版 Android",
                    checked = state.schemeConfig.v1Enabled,
                    onCheckedChange = { onToggleScheme(it, state.schemeConfig.v2Enabled, state.schemeConfig.v3Enabled) }
                )
                AppDexDivider()
                SchemeToggleRow(
                    name = "V2 (APK 签名方案)",
                    description = "Android 7.0+，更快的安装",
                    checked = state.schemeConfig.v2Enabled,
                    onCheckedChange = { onToggleScheme(state.schemeConfig.v1Enabled, it, state.schemeConfig.v3Enabled) }
                )
                AppDexDivider()
                SchemeToggleRow(
                    name = "V3 (APK 签名方案 v3)",
                    description = "Android 9.0+，支持密钥轮换",
                    checked = state.schemeConfig.v3Enabled,
                    onCheckedChange = { onToggleScheme(state.schemeConfig.v1Enabled, state.schemeConfig.v2Enabled, it) }
                )
            }
        }

        // 签名摘要
        AppDexSection(label = "签名摘要") {
            Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryRow("APK", state.inputApkName)
                SummaryRow("Keystore", File(state.keystorePath).name)
                SummaryRow("Alias", state.selectedAlias)
                SummaryRow("方案", buildString {
                    if (state.schemeConfig.v1Enabled) append("V1 ")
                    if (state.schemeConfig.v2Enabled) append("V2 ")
                    if (state.schemeConfig.v3Enabled) append("V3")
                }.trim().ifEmpty { "N/A" })
                SummaryRow("输出", state.inputApkName.replace(".apk", "_signed.apk"))
            }
        }

        AppDexButton(
            text = "执行签名",
            icon = Icons.Default.Security,
            onClick = onSign
        )
    }
}

// 签名结果
@Composable
private fun SigningResultContent(
    state: SigningState,
    onBack: () -> Unit,
) {
    val result = state.signingResult
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 结果状态
        Box(
            modifier = Modifier.fillMaxWidth().border(1.dp, if (result?.success == true) AuroraGreen else AmberGold).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (result?.success == true) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (result?.success == true) AuroraGreen else RedSupergiant
                )
                Text(
                    text = if (result?.success == true) "签名成功" else "签名完成",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // 验证结果
        result?.verificationResult?.let { ver ->
            AppDexSection(label = "验证结果") {
                Column(modifier = Modifier.border(1.dp, BorderLight)) {
                    VerificationRow("V1 签名", ver.v1Verified)
                    AppDexDivider()
                    VerificationRow("V2 签名", ver.v2Verified)
                    AppDexDivider()
                    VerificationRow("V3 签名", ver.v3Verified)
                }
            }

            if (ver.errors.isNotEmpty()) {
                AppDexSection(label = "错误信息") {
                    Column(modifier = Modifier.border(1.dp, RedSupergiantDark).padding(12.dp)) {
                        ver.errors.forEach { err ->
                            Text(text = err, fontSize = 10.sp, color = RedSupergiant, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // 输出文件信息
        AppDexSection(label = "输出文件") {
            Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp)) {
                val outputFile = File(result?.outputFilePath ?: "")
                Text(
                    text = outputFile.name,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = FormatUtil.formatFileSize(outputFile.length()),
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = result?.outputFilePath ?: "",
                    fontSize = 9.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        AppDexButton(
            text = "完成",
            icon = Icons.Default.Check,
            onClick = onBack
        )
    }
}

// 创建 Keystore 对话框
@Composable
private fun CreateKeystoreDialog(
    keystorePath: String,
    keystorePassword: String,
    alias: String,
    keyPassword: String,
    subject: String,
    isCreating: Boolean,
    onPathChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onKeyPasswordChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建新 Keystore", fontSize = 14.sp, color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = keystorePath,
                    onValueChange = onPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("保存路径", fontSize = 10.sp) },
                    placeholder = { Text("/sdcard/appdex.jks", fontSize = 10.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
                OutlinedTextField(
                    value = keystorePassword,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Keystore 密码", fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
                OutlinedTextField(
                    value = alias,
                    onValueChange = onAliasChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("别名 (Alias)", fontSize = 10.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
                OutlinedTextField(
                    value = keyPassword,
                    onValueChange = onKeyPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Key 密码", fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = onSubjectChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("证书主体 (X500)", fontSize = 10.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = textFieldColors(),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onCreate,
                enabled = !isCreating && keystorePath.isNotEmpty() && keystorePassword.isNotEmpty() && alias.isNotEmpty()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("创建", color = AmberGold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("取消", color = TextTertiary, fontSize = 12.sp)
            }
        },
        containerColor = SurfaceDeep,
    )
}

// 辅助 Composable

@Composable
private fun SchemeToggleRow(
    name: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
            Text(text = description, fontSize = 9.sp, color = TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = AmberGold,
                checkedTrackColor = AmberGoldDark,
            )
        )
    }
}

@Composable
private fun VerificationRow(name: String, verified: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        Text(
            text = if (verified) "✓ 已验证" else "✗ 未验证",
            fontSize = 11.sp,
            color = if (verified) AuroraGreen else RedSupergiant,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
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
