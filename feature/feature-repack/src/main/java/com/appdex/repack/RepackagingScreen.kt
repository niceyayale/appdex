package com.appdex.repack

import kotlinx.coroutines.launch
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXButton
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXSection
import com.appdex.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RepackagingScreen(
    apkPath: String,
    onBack: () -> Unit = {},
    viewModel: RepackagingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(apkPath) {
        if (apkPath.isNotEmpty() && state.inputApkPath != apkPath) {
            viewModel.handleIntent(RepackagingIntent.SetInputApk(apkPath))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RepackagingEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is RepackagingEffect.ShowToast -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is RepackagingEffect.RepackComplete -> {
                    val msg = if (effect.result.success) "回编译成功" else "回编译失败"
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        }
    }

    // Keystore 输入状态
    var keystorePath by rememberSaveable { mutableStateOf("") }
    var keystorePassword by rememberSaveable { mutableStateOf("") }
    var keyAlias by rememberSaveable { mutableStateOf("") }
    var keyPassword by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppXBar(
                title = "APK 回编译",
                back = true,
                onBack = onBack,
                showBell = false,
            )

            when (state.step) {
                RepackStep.SELECT_APK -> { /* 请进入下一步 */ }
                RepackStep.SELECT_DEX -> {
                    DexSelectionContent(
                        state = state,
                        onToggleDex = { viewModel.handleIntent(RepackagingIntent.SelectDexFile(it)) },
                        onNext = {
                            viewModel.handleIntent(
                                RepackagingIntent.SetKeystoreInfo(keystorePath, keystorePassword, keyAlias, keyPassword)
                            )
                        },
                    )
                }
                RepackStep.ENTER_KEYSTORE -> {
                    KeystoreInputContent(
                        state = state,
                        keystorePath = keystorePath,
                        keystorePassword = keystorePassword,
                        keyAlias = keyAlias,
                        keyPassword = keyPassword,
                        onPathChange = { keystorePath = it },
                        onPasswordChange = { keystorePassword = it },
                        onAliasChange = { keyAlias = it },
                        onKeyPasswordChange = { keyPassword = it },
                        onRepackAndSign = {
                            viewModel.handleIntent(
                                RepackagingIntent.SetKeystoreInfo(keystorePath, keystorePassword, keyAlias, keyPassword)
                            )
                            val outputPath = state.inputApkPath.replace(".apk", "_repacked.apk")
                            viewModel.handleIntent(RepackagingIntent.RepackAndSign(outputPath))
                        },
                        onRepackOnly = {
                            val outputPath = state.inputApkPath.replace(".apk", "_repacked.apk")
                            viewModel.handleIntent(RepackagingIntent.RepackOnly(outputPath))
                        },
                    )
                }
                RepackStep.REPACKING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(48.dp))
                            Text(
                                text = "正在回编译...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                RepackStep.COMPLETE -> {
                    RepackResultContent(
                        state = state,
                        onBack = onBack,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// DEX 选择
@Composable
private fun DexSelectionContent(
    state: RepackagingState,
    onToggleDex: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppXSection(label = "输入 APK") {
            Column(modifier = Modifier.border(1.dp, BorderLight).background(SurfaceAlt).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
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
                        Text(text = FormatUtil.formatFileSize(fileSize), fontSize = 10.sp, color = TextSecondary)
                    }
                    Icon(Icons.Default.Check, contentDescription = null, tint = AuroraGreen, modifier = Modifier.size(16.dp))
                }
            }
        }

        AppXSection(label = "DEX 文件 (${state.dexFiles.size})") {
            Column(modifier = Modifier.border(1.dp, BorderLight)) {
                state.dexFiles.forEachIndexed { index, dexName ->
                    val isSelected = dexName in state.selectedDexFiles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDex(dexName) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(SurfaceAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp), tint = AuroraGreen)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dexName,
                                fontSize = 12.sp,
                                color = if (isSelected) AmberGoldHighlight else TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(text = "DEX 文件", fontSize = 9.sp, color = TextTertiary)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < state.dexFiles.size - 1) {
                        AppXDivider()
                    }
                }
            }
        }

        AppXButton(
            text = "下一步 签名配置",
            icon = Icons.Default.VpnKey,
            onClick = onNext,
            enabled = state.selectedDexFiles.isNotEmpty()
        )
    }
}

// Keystore 输入
@Composable
private fun KeystoreInputContent(
    state: RepackagingState,
    keystorePath: String,
    keystorePassword: String,
    keyAlias: String,
    keyPassword: String,
    onPathChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onKeyPasswordChange: (String) -> Unit,
    onRepackAndSign: () -> Unit,
    onRepackOnly: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppXSection(label = "已选 DEX (${state.selectedDexFiles.size})") {
            Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp)) {
                state.selectedDexFiles.forEach { dexName ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AuroraGreen, modifier = Modifier.size(14.dp))
                        Text(text = dexName, fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        AppXSection(label = "Keystore 配置 (用于签名)") {
            Column(modifier = Modifier.border(1.dp, BorderLight).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keystorePath,
                    onValueChange = onPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Keystore 路径", fontSize = 10.sp) },
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
                    value = keyAlias,
                    onValueChange = onAliasChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Key Alias", fontSize = 10.sp) },
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
            }
        }

        AppXButton(
            text = "回编译 + 签名",
            icon = Icons.Default.Security,
            onClick = onRepackAndSign,
        )

        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onRepackOnly),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = IconBlueBright, modifier = Modifier.size(14.dp))
            Text(text = " 仅回编译 (不签名)", fontSize = 11.sp, color = IconBlueBright)
        }

        state.error?.let { err ->
            Box(
                modifier = Modifier.fillMaxWidth().border(1.dp, RedSupergiantDark).background(RedSupergiantDark).padding(12.dp),
            ) {
                Text(text = err, fontSize = 11.sp, color = RedSupergiant)
            }
        }
    }
}

// 回编译结果
@Composable
private fun RepackResultContent(
    state: RepackagingState,
    onBack: () -> Unit,
) {
    val result = state.repackResult
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().border(1.dp, if (result?.success == true) AuroraGreen else AmberGold).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (result?.success == true) Icons.Default.Check else Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (result?.success == true) AuroraGreen else RedSupergiant
                )
                    Text(
                    text = if (result?.success == true) "回编译成功" else "回编译失败",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                result?.message?.let {
                    Text(text = it, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        result?.signingResult?.let { signingResult ->
            AppXSection(label = "签名验证") {
                Column(modifier = Modifier.border(1.dp, BorderLight).padding(12.dp)) {
                    signingResult.verificationResult?.let { ver ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("V1", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(
                                if (ver.v1Verified) "✓" else "✗",
                                fontSize = 11.sp,
                                color = if (ver.v1Verified) AuroraGreen else RedSupergiant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("V2", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(
                                if (ver.v2Verified) "✓" else "✗",
                                fontSize = 11.sp,
                                color = if (ver.v2Verified) AuroraGreen else RedSupergiant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("V3", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(
                                if (ver.v3Verified) "✓" else "✗",
                                fontSize = 11.sp,
                                color = if (ver.v3Verified) AuroraGreen else RedSupergiant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        AppXSection(label = "输出文件") {
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

        AppXButton(
            text = "完成",
            icon = Icons.Default.Check,
            onClick = onBack
        )
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
