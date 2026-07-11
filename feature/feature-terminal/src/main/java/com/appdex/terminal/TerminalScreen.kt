package com.appdex.terminal

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    workingDir: String = "/data/data/com.appdex/files"
) {
    val scope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val session = remember { TerminalSession(workingDir) }
    val lines = remember { mutableStateListOf<TerminalLine>() }
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var historyIndex by remember { mutableStateOf(-1) }
    val commandHistory = remember { mutableStateListOf<String>() }

    // Collect terminal output
    LaunchedEffect(session) {
        session.output.collect { output ->
            when (output) {
                is TerminalOutput.Prompt -> {
                    lines.add(TerminalLine.Prompt(output.workingDir))
                    isRunning = false
                }
                is TerminalOutput.Data -> {
                    lines.add(TerminalLine.Output(output.text))
                }
                is TerminalOutput.Exit -> {
                    if (output.code != 0) {
                        lines.add(TerminalLine.Error("Exit code: ${output.code}"))
                    }
                }
                is TerminalOutput.Error -> {
                    lines.add(TerminalLine.Error(output.message))
                    isRunning = false
                }
            }
            // Auto scroll to bottom
            if (lines.isNotEmpty()) {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }

    // Initial prompt
    LaunchedEffect(Unit) {
        if (lines.isEmpty()) {
            lines.add(TerminalLine.System("APPDEX Terminal v1.0 — type 'exit' to close session"))
            lines.add(TerminalLine.Prompt(workingDir))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Terminal", style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = lines.joinToString("\n") { line ->
                                when (line) {
                                    is TerminalLine.Prompt -> "appdex@device:${line.workingDir}$ "
                                    is TerminalLine.Command -> line.text
                                    is TerminalLine.Output -> line.text
                                    is TerminalLine.Error -> line.text
                                    is TerminalLine.System -> line.text
                                }
                            }
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = { lines.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            )
        },
        bottomBar = {
            // Quick keys bar
            QuickKeysBar(
                onKey = { key ->
                    when (key) {
                        "Enter" -> {
                            if (input.isNotEmpty() && !isRunning) {
                                executeCommand(
                                    input, session, scope, lines, commandHistory,
                                    onStarted = { isRunning = true }
                                )
                                commandHistory.add(input)
                                historyIndex = commandHistory.size
                                input = ""
                            }
                        }
                        "Ctrl+C" -> {
                            session.kill()
                            lines.add(TerminalLine.Error("^C"))
                            lines.add(TerminalLine.Prompt(workingDir))
                            isRunning = false
                        }
                        "↑" -> {
                            if (historyIndex > 0) {
                                historyIndex--
                                input = commandHistory[historyIndex]
                            }
                        }
                        "↓" -> {
                            if (historyIndex < commandHistory.size - 1) {
                                historyIndex++
                                input = commandHistory[historyIndex]
                            } else {
                                historyIndex = commandHistory.size
                                input = ""
                            }
                        }
                        "Tab" -> input += "\t"
                        "Esc" -> { /* no-op for now */ }
                        else -> input += key
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Terminal output
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = listState
            ) {
                items(lines) { line ->
                    TerminalLineView(line)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isRunning,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (input.isNotEmpty() && !isRunning) {
                                    executeCommand(
                                        input, session, scope, lines, commandHistory,
                                        onStarted = { isRunning = true }
                                    )
                                    commandHistory.add(input)
                                    historyIndex = commandHistory.size
                                    input = ""
                                }
                            },
                            enabled = !isRunning && input.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        }
                    }
                )
            }
        }
    }
}

private fun executeCommand(
    command: String,
    session: TerminalSession,
    scope: kotlinx.coroutines.CoroutineScope,
    lines: MutableList<TerminalLine>,
    history: MutableList<String>,
    onStarted: () -> Unit
) {
    lines.add(TerminalLine.Command(command))
    if (command.trim() == "clear" || command.trim() == "cls") {
        lines.clear()
        return
    }
    if (command.trim() == "exit") {
        lines.add(TerminalLine.System("Session closed."))
        return
    }
    onStarted()
    session.execute(command, scope)
}

@Composable
private fun TerminalLineView(line: TerminalLine) {
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    val promptColor = MaterialTheme.colorScheme.primary
    val systemColor = MaterialTheme.colorScheme.onSurfaceVariant

    when (line) {
        is TerminalLine.Prompt -> {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(
                    text = "appdex@device:",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = promptColor
                )
                Text(
                    text = line.workingDir,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "$ ",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = promptColor
                )
            }
        }
        is TerminalLine.Command -> {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = defaultColor,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        is TerminalLine.Output -> {
            val parsed = AnsiColorParser.parse(line.text, defaultColor)
            Text(
                text = parsed,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        is TerminalLine.Error -> {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = errorColor,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        is TerminalLine.System -> {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = systemColor,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun QuickKeysBar(onKey: (String) -> Unit) {
    val keys = listOf("Ctrl+C", "Tab", "↑", "↓", "Esc", "Enter")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        keys.forEach { key ->
            androidx.compose.material3.TextButton(
                onClick = { onKey(key) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

sealed interface TerminalLine {
    data class Prompt(val workingDir: String) : TerminalLine
    data class Command(val text: String) : TerminalLine
    data class Output(val text: String) : TerminalLine
    data class Error(val text: String) : TerminalLine
    data class System(val text: String) : TerminalLine
}
