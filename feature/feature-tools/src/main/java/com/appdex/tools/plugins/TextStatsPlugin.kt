package com.appdex.tools.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdex.plugin.AppXPlugin
import com.appdex.plugin.PluginCategory

class TextStatsPlugin : AppXPlugin {
    override val id = "text_stats"
    override val name = "Text Statistics"
    override val description = "Count characters, words, lines, and reading time"
    override val author = "AppX"
    override val version = "1.0.0"
    override val category = PluginCategory.ANALYZE

    @Composable
    override fun Content() {
        var text by rememberSaveable { mutableStateOf("") }

        val charCount = text.length
        val charCountNoSpaces = text.replace("\\s".toRegex(), "").length
        val wordCount = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        val lineCount = if (text.isEmpty()) 0 else text.split("\n").size
        val sentenceCount = text.split("[.!?]+".toRegex()).filter { it.isNotBlank() }.size
        val paragraphCount = if (text.isBlank()) 0 else text.split("\n\n+".toRegex()).filter { it.isNotBlank() }.size
        val readingTimeMin = (wordCount / 200.0).let { if (it < 1) "<1" else it.toInt().toString() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text("Type or paste text to analyze...") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Characters", charCount.toString(), Modifier.weight(1f))
                StatCard("No Spaces", charCountNoSpaces.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Words", wordCount.toString(), Modifier.weight(1f))
                StatCard("Lines", lineCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Sentences", sentenceCount.toString(), Modifier.weight(1f))
                StatCard("Paragraphs", paragraphCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        "Estimated reading time: $readingTimeMin min (200 wpm)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    @Composable
    private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
