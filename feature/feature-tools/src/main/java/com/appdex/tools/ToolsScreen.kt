package com.appdex.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appdex.plugin.PluginEntry
import com.appdex.plugin.PluginManager
import com.appdex.tools.plugins.JsonFormatterPlugin
import com.appdex.tools.plugins.PluginListScreen
import com.appdex.tools.plugins.TextStatsPlugin
import com.appdex.tools.plugins.TimestampConverterPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen() {
    var selectedTool by remember { mutableStateOf<ToolType?>(null) }
    var selectedPlugin by remember { mutableStateOf<PluginEntry?>(null) }
    var showPluginList by remember { mutableStateOf(false) }

    // Register built-in plugins on first launch
    remember {
        if (PluginManager.count == 0) {
            PluginManager.register(JsonFormatterPlugin())
            PluginManager.register(TextStatsPlugin())
            PluginManager.register(TimestampConverterPlugin())
        }
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                showPluginList -> {
                    PluginListScreen(
                        onBack = { showPluginList = false },
                        onPluginClick = { entry ->
                            selectedPlugin = entry
                            showPluginList = false
                        }
                    )
                }
                selectedPlugin != null -> {
                    PluginDetailScreen(
                        entry = selectedPlugin!!,
                        onBack = { selectedPlugin = null }
                    )
                }
                selectedTool != null -> {
                    when (selectedTool) {
                        ToolType.HASH_CALCULATOR -> HashCalculatorScreen(onBack = { selectedTool = null })
                        ToolType.DEVICE_INFO -> DeviceInfoScreen(onBack = { selectedTool = null })
                        ToolType.ENCODING_CONVERTER -> EncodingConverterScreen(onBack = { selectedTool = null })
                        null -> {}
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        items(ToolType.entries) { tool ->
                            ToolCard(
                                tool = tool,
                                onClick = { selectedTool = tool }
                            )
                        }
                        // Plugins entry
                        item {
                            PluginEntryCard(
                                count = PluginManager.count,
                                onClick = { showPluginList = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class ToolType(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    HASH_CALCULATOR(
        "Hash Calculator",
        "Calculate MD5, SHA-1, SHA-256 of files or text",
        Icons.Default.Fingerprint
    ),
    DEVICE_INFO(
        "Device Info",
        "View hardware and system information",
        Icons.Default.Devices
    ),
    ENCODING_CONVERTER(
        "Encoding Converter",
        "Convert between Base64, URL, Hex, and more",
        Icons.Default.TextFields
    )
}

@Composable
private fun ToolCard(
    tool: ToolType,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PluginEntryCard(
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Plugins",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count plugins available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginDetailScreen(
    entry: PluginEntry,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry.plugin.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            entry.plugin.Content()
        }
    }
}
