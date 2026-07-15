package com.appdex.axmleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexButton
import com.appdex.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxmlEditorScreen(
    apkPath: String? = null,
    entryName: String? = null,
    onBack: () -> Unit,
    viewModel: AxmlEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(apkPath, entryName) {
        if (apkPath != null && entryName != null) {
            viewModel.handleIntent(AxmlEditorIntent.LoadFromApk(apkPath, entryName))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            AppDexBar(
                title = if (state.fileName.isNotEmpty()) state.fileName else "AXML 编辑器",
                back = true,
                onBack = onBack,
                showBell = false,
            )

            // ── Action buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppDexButton(
                        text = if (state.isEditing) "预览" else "编辑",
                        icon = if (state.isEditing) Icons.Default.Visibility else Icons.Default.Edit,
                        onClick = { viewModel.handleIntent(AxmlEditorIntent.ToggleEdit) },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AppDexButton(
                        text = "编码",
                        icon = Icons.Default.Build,
                        onClick = { viewModel.handleIntent(AxmlEditorIntent.EncodeToBinary) },
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AmberGold)
                }
            }

            state.parseError?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = RedSupergiantContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = OnRedSupergiantContainer,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                }
            }

            // ── XML Text Editor / Viewer ──
            SectionHeader("XML 内容")
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = SatelliteBlue,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.xmlText,
                        onValueChange = { viewModel.handleIntent(AxmlEditorIntent.UpdateXmlText(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp)
                            .padding(8.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextPrimary,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceInput,
                            unfocusedContainerColor = SurfaceInput,
                            cursorColor = AmberGold,
                        ),
                    )
                } else {
                    SelectionContainer {
                        Text(
                            text = state.xmlText.ifEmpty { "// 点击编辑按钮开始编辑" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .horizontalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextPrimary,
                        )
                    }
                }
            }

            // ── Binary Hex Preview ──
            if (state.binaryHexPreview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("二进制预览 (Hex)")
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = TerminalBg,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    SelectionContainer {
                        Text(
                            text = state.binaryHexPreview.chunked(32).joinToString("\n") {
                                it.chunked(2).joinToString(" ")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .horizontalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalText,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        color = AmberGold,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}
