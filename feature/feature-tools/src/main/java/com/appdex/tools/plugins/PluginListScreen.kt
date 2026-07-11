package com.appdex.tools.plugins

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appdex.plugin.PluginCategory
import com.appdex.plugin.PluginEntry
import com.appdex.plugin.PluginManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginListScreen(
    onBack: () -> Unit,
    onPluginClick: (PluginEntry) -> Unit = {}
) {
    val pluginList = remember { mutableStateListOf<PluginEntry>() }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        pluginList.clear()
        pluginList.addAll(PluginManager.getAll())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Plugins (${pluginList.size})")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pluginList) { entry ->
                PluginCard(
                    entry = entry,
                    onClick = { /* Could open plugin directly */ },
                    onToggle = { enabled ->
                        PluginManager.setEnabled(entry.plugin.id, enabled)
                        entry.enabled = enabled
                        refreshTrigger++
                    }
                )
            }
        }
    }
}

@Composable
private fun PluginCard(
    entry: PluginEntry,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (entry.enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (entry.enabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.plugin.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (entry.enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = entry.plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(entry.plugin.category)
                    Text(
                        text = "v${entry.plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "by ${entry.plugin.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = entry.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun CategoryChip(category: PluginCategory) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
