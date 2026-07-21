package com.appdex.settings

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdex.data.ai.AiProviderEntity
import com.appdex.data.ai.AiProviderType
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXButton
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXSection
import com.appdex.ui.components.bounceClick
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.DefaultBottomPadding

@Composable
fun AiProvidersScreen(
    onBack: () -> Unit,
    viewModel: AiProvidersViewModel = hiltViewModel()
) {
    val c = AppXTheme.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val providers by viewModel.savedProviders.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeProviderId by viewModel.activeProviderId.collectAsStateWithLifecycle(initialValue = null)
    val editForm by viewModel.editForm.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(c.background)) {
        when (val state = uiState) {
            is AiProvidersViewModel.UiState.List -> {
                ProviderListContent(
                    providers = providers,
                    activeProviderId = activeProviderId,
                    onBack = onBack,
                    onAdd = { viewModel.showCreateForm() },
                    onEdit = { viewModel.showEditForm(it) },
                    onSelect = { viewModel.setActiveProvider(it.id) },
                    onDelete = { viewModel.deleteProvider(it.id) }
                )
            }
            is AiProvidersViewModel.UiState.Edit -> {
                ProviderEditContent(
                    form = editForm,
                    testResult = testResult,
                    onBack = { viewModel.backToList() },
                    onNameChange = { viewModel.updateName(it) },
                    onTypeChange = { viewModel.updateProviderType(it) },
                    onApiKeyChange = { viewModel.updateApiKey(it) },
                    onBaseUrlChange = { viewModel.updateBaseUrl(it) },
                    onModelChange = { viewModel.updateModel(it) },
                    onSave = { viewModel.saveProvider() },
                    onTest = { viewModel.testConnection() },
                    onDelete = state.provider?.let { { viewModel.deleteProvider(it.id) } }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = c.surfaceDeep,
                contentColor = c.textPrimary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Provider List View
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProviderListContent(
    providers: List<AiProviderEntity>,
    activeProviderId: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (AiProviderEntity) -> Unit,
    onSelect: (AiProviderEntity) -> Unit,
    onDelete: (AiProviderEntity) -> Unit
) {
    val c = AppXTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 0.dp, bottom = DefaultBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AppXBar(
                title = "AI 提供商管理",
                back = true,
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(c.amberGold)
                    .bounceClick(onClick = onAdd),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.amberGoldDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "新建 AI 提供商",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.amberGoldDark
                )
            }
        }

        if (providers.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = c.textMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无 AI 提供商",
                        fontSize = 13.sp,
                        color = c.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击上方按钮新建提供商配置",
                        fontSize = 11.sp,
                        color = c.textTertiary
                    )
                }
            }
        } else {
            item {
                AppXSection(label = "已保存的提供商 (${providers.size})") {
                    Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                        providers.forEachIndexed { index, provider ->
                            ProviderCard(
                                provider = provider,
                                isActive = provider.id == activeProviderId,
                                onClick = { onSelect(provider) },
                                onEdit = { onEdit(provider) },
                                onDelete = { onDelete(provider) }
                            )
                            if (index < providers.size - 1) {
                                AppXDivider()
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "提示：点击提供商卡片可设为当前激活，长按编辑按钮可修改配置",
                fontSize = 10.sp,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val c = AppXTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) c.amberGold.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider type icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (isActive) c.amberGoldContainer else c.surfaceAlt, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isActive) c.amberGold else c.iconBlue
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Provider info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.name,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) c.amberGold else c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(c.amberGold, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "当前",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.amberGoldDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${provider.providerType.displayName} · ${provider.modelName.ifEmpty { "未设置模型" }}",
                fontSize = 10.sp,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (provider.isConfigured()) "已配置" else "未完成配置",
                fontSize = 9.sp,
                color = if (provider.isConfigured()) c.auroraGreen else c.redSupergiant
            )
        }

        // Edit button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(c.surfaceAlt, CircleShape)
                .bounceClick(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = "编辑",
                modifier = Modifier.size(14.dp),
                tint = c.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(c.redSupergiantContainer.copy(alpha = 0.3f), CircleShape)
                .bounceClick(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                modifier = Modifier.size(14.dp),
                tint = c.redSupergiant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Provider Edit/Create View
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProviderEditContent(
    form: AiProvidersViewModel.EditForm,
    testResult: AiProvidersViewModel.TestResult,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (AiProviderType) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val c = AppXTheme.colors
    val scrollState = rememberScrollState()
    val needsApiKey = form.providerType !in listOf(
        AiProviderType.OLLAMA, AiProviderType.LM_STUDIO,
        AiProviderType.LOCALAI, AiProviderType.ANYTHINGLLM
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, bottom = DefaultBottomPadding)
    ) {
        AppXBar(
            title = if (form.isExisting) "编辑提供商" else "新建提供商",
            back = true,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Name ──
        AppXSection(label = "提供商名称") {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：我的 OpenAI", fontSize = 11.sp, color = c.textMuted) },
                singleLine = true,
                colors = outlineColors(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = c.textPrimary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Provider Type ──
        AppXSection(label = "提供商类型") {
            Column(modifier = Modifier.border(1.dp, c.borderLight)) {
                AiProviderType.entries.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { provider ->
                            val isSelected = provider == form.providerType
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) c.amberGold.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { onTypeChange(provider) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = provider.displayName,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) c.amberGold else c.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    AppXDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── API Key (if needed) ──
        if (needsApiKey) {
            AppXSection(label = "API Key") {
                OutlinedTextField(
                    value = form.apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入 API Key", fontSize = 11.sp, color = c.textMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = outlineColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = c.textPrimary)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Base URL ──
        AppXSection(label = "Base URL") {
            OutlinedTextField(
                value = form.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        AiConfigPlaceholder(form.providerType),
                        fontSize = 11.sp,
                        color = c.textMuted
                    )
                },
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                colors = outlineColors(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = c.textPrimary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Model ──
        AppXSection(label = "模型名称") {
            OutlinedTextField(
                value = form.modelName,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        com.appdex.data.ai.AiConfig(providerType = form.providerType).defaultModels().firstOrNull() ?: "模型名称",
                        fontSize = 11.sp,
                        color = c.textMuted
                    )
                },
                singleLine = true,
                colors = outlineColors(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = c.textPrimary)
            )
            // Show default model suggestions
            val suggestions = com.appdex.data.ai.AiConfig(providerType = form.providerType).defaultModels()
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.take(3).forEach { model: String ->
                        Text(
                            text = model,
                            fontSize = 9.sp,
                            color = c.iconBlueBright,
                            modifier = Modifier
                                .border(1.dp, c.borderMedium)
                                .clickable { onModelChange(model) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Connection Test ──
        AppXSection(label = "连接测试") {
            Column(modifier = Modifier.border(1.dp, c.borderLight).padding(12.dp)) {
                when (val result = testResult) {
                    is AiProvidersViewModel.TestResult.Idle -> {
                        Row(
                            modifier = Modifier
                                .border(1.dp, c.borderMedium)
                                .bounceClick(onClick = onTest)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.iconBlueBright)
                            Text("测试连接", fontSize = 11.sp, color = c.iconBlueBright)
                        }
                    }
                    is AiProvidersViewModel.TestResult.Testing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = c.amberGold)
                            Text("测试中...", fontSize = 11.sp, color = c.textSecondary)
                        }
                    }
                    is AiProvidersViewModel.TestResult.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.auroraGreen)
                            Text(result.message, fontSize = 11.sp, color = c.auroraGreen, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "点击此处重新测试",
                            fontSize = 9.sp,
                            color = c.textTertiary,
                            modifier = Modifier.clickable(onClick = onTest)
                        )
                    }
                    is AiProvidersViewModel.TestResult.Failure -> {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.redSupergiant)
                                Text("连接失败", fontSize = 11.sp, color = c.redSupergiant, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.message,
                                fontSize = 9.sp,
                                color = c.textTertiary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .border(1.dp, c.borderMedium)
                                    .bounceClick(onClick = onTest)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("重试", fontSize = 10.sp, color = c.iconBlueBright)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Save Button ──
        AppXButton(
            text = if (form.isExisting) "保存修改" else "创建提供商",
            icon = Icons.Default.Save,
            onClick = onSave
        )

        // ── Delete Button (only for existing) ──
        if (onDelete != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(c.redSupergiantContainer.copy(alpha = 0.3f))
                    .bounceClick(onClick = onDelete),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.redSupergiant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "删除此提供商",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.redSupergiant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun outlineColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppXTheme.colors.textPrimary,
    unfocusedTextColor = AppXTheme.colors.textPrimary,
    focusedBorderColor = AppXTheme.colors.amberGold,
    unfocusedBorderColor = AppXTheme.colors.borderMedium,
    cursorColor = AppXTheme.colors.amberGold,
    focusedContainerColor = AppXTheme.colors.surfaceInput,
    unfocusedContainerColor = AppXTheme.colors.surfaceInput
)

@Composable
private fun AiConfigPlaceholder(providerType: AiProviderType): String {
    return when (providerType) {
        AiProviderType.OPENAI -> "https://api.openai.com/v1"
        AiProviderType.ANTHROPIC -> "https://api.anthropic.com/v1"
        AiProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
        AiProviderType.DEEPSEEK -> "https://api.deepseek.com/v1"
        AiProviderType.OPENROUTER -> "https://openrouter.ai/api/v1"
        AiProviderType.OLLAMA -> "http://localhost:11434"
        AiProviderType.LM_STUDIO -> "http://localhost:1234/v1"
        AiProviderType.LOCALAI -> "http://localhost:8080/v1"
        AiProviderType.ANYTHINGLLM -> "http://localhost:3001/api/v1"
        else -> "API Base URL"
    }
}
