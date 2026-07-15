package com.appdex.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.data.ai.AiConfig
import com.appdex.data.session.AiMessage
import com.appdex.data.session.AiRole
import com.appdex.data.toolbridge.ActionCardData
import com.appdex.ui.components.AiActionCardData
import com.appdex.ui.components.AppDexAiCard
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.theme.AppDexTheme
import com.appdex.ui.theme.*

@Composable
fun AiScreen(
    messages: List<AiMessage>,
    isAiResponding: Boolean,
    aiConfig: AiConfig?,
    suggestedQuestions: List<Pair<String, String>> = emptyList(),
    onSendMessage: (String) -> Unit,
    onActionClick: (ActionCardData) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isAiResponding) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDexTheme.colors.background)
    ) {
        AppDexBar(title = "AI 助手", subtitle = aiConfig?.let { it.providerType.displayName } ?: "未配置")

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).background(IconBoxBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = IconBlueBright
                            )
                        }
                        Text(
                            text = "AppDex AI",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = "你的 APK 分析助手",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val suggestions = if (suggestedQuestions.isNotEmpty()) {
                            suggestedQuestions.map { it.first }
                        } else {
                            listOf(
                                "这个 APK 安全吗？",
                                "分析权限列表",
                                "查看代码结构",
                                "检查签名信息"
                            )
                        }
                        suggestions.forEach { question ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, BorderLight)
                                    .background(SurfaceAlt)
                                    .clickable {
                                        onSendMessage(question)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = question,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (aiConfig == null || !aiConfig.isConfigured()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AmberGold)
                                    .background(AmberGoldContainer)
                                    .clickable(onClick = onNavigateToSettings)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "配置 AI 提供商",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberGold
                                )
                            }
                        }
                    }
                }
            }

            items(messages) { message ->
                val actionCards = if (message.role == AiRole.ASSISTANT && message.actionCards.isNotEmpty()) {
                    message.actionCards.map { card ->
                        AiActionCardData(
                            title = card.title,
                            description = card.description,
                            iconType = card.iconType,
                            route = card.route
                        )
                    }
                } else emptyList()

                AppDexAiCard(
                    content = message.content,
                    isUser = message.role == AiRole.USER,
                    actionCards = actionCards,
                    onActionClick = { aiCardData ->
                        onActionClick(ActionCardData(
                            title = aiCardData.title,
                            description = aiCardData.description,
                            iconType = aiCardData.iconType,
                            route = aiCardData.route
                        ))
                    }
                )
            }

            if (isAiResponding) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AmberGold
                        )
                        Text(
                            text = "AI 正在思考...",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Input bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDeep)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderDefault)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "问 AI 任何关于 APK 的问题...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = BorderMedium,
                        cursorColor = AmberGold
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AmberGold)
                                    .clickable {
                                        if (inputText.isNotBlank()) {
                                            onSendMessage(inputText.trim())
                                            inputText = ""
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "发送",
                                    modifier = Modifier.size(16.dp),
                                    tint = AmberGoldDark
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
