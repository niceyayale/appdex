package com.appdex.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.syntax.SyntaxHighlighter
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexSnackbarHost
import com.appdex.ui.components.EmptyState
import com.appdex.ui.components.LoadingState
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    filePath: String? = null,
    onBack: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openFileFromUri(it, context.contentResolver) }
    }

    // Load file when filePath is provided
    LaunchedEffect(filePath) {
        viewModel.openFileIfProvided(filePath)
    }

    // Sync content into TextFieldValue when it changes externally (e.g. file open)
    LaunchedEffect(state.content) {
        if (textFieldValue.text != state.content) {
            textFieldValue = TextFieldValue(state.content)
        }
    }

    val ext = state.filePath?.substringAfterLast('.', "") ?: ""
    val highlightTransform = remember(ext) { SyntaxHighlightTransformation(ext) }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppDexBar(
                title = state.fileName ?: "编辑器",
                subtitle = if (state.isModified) "未保存修改" else null,
                back = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        openFileLauncher.launch(arrayOf("text/*", "application/json", "application/xml", "application/javascript"))
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "打开文件", tint = AmberGold)
                    }
                    IconButton(onClick = {
                        viewModel.handleIntent(EditorIntent.Save)
                        scope.launch { snackbarHostState.showSnackbar("已保存") }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存", tint = AmberGold)
                    }
                }
            )

            if (state.isLoading) {
                LoadingState()
            } else if (state.content.isEmpty() && state.filePath == null) {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    title = "打开文件开始编辑",
                    subtitle = "点击右上角文件夹图标选择文件"
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Code editor with syntax highlighting
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Line numbers
                            LineNumberColumn(
                                text = state.content,
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 8.dp, end = 8.dp)
                            )

                            // Editor
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    viewModel.handleIntent(EditorIntent.UpdateContent(newValue.text))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                visualTransformation = highlightTransform,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                                    MaterialTheme.colorScheme.primary
                                ),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.None
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Status bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${state.content.length} chars · Ln ${getLineCount(state.content)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.encoding,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        AppDexSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LineNumberColumn(
    text: String,
    modifier: Modifier = Modifier
) {
    val lineCount = remember(text) { getLineCount(text) }
    Column(modifier = modifier) {
        for (i in 1..lineCount) {
            Text(
                text = "$i",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getLineCount(text: String): Int {
    return text.count { it == '\n' } + 1
}

/**
 * VisualTransformation that applies syntax highlighting to the text.
 */
class SyntaxHighlightTransformation(
    private val extension: String
) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, extension)
        return TransformedText(highlighted, OffsetMapping)
    }
}

object OffsetMapping : androidx.compose.ui.text.input.OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = offset
    override fun transformedToOriginal(offset: Int): Int = offset
}
