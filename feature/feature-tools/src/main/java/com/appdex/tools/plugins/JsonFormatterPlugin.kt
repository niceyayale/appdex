package com.appdex.tools.plugins

import android.util.Log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.appdex.plugin.AppXPlugin
import com.appdex.plugin.PluginCategory
import org.json.JSONObject

class JsonFormatterPlugin : AppXPlugin {
    override val id = "json_formatter"
    override val name = "JSON Formatter"
    override val description = "Format and validate JSON data with proper indentation"
    override val author = "AppX"
    override val version = "1.0.0"
    override val category = PluginCategory.FORMAT

    @Composable
    override fun Content() {
        var input by rememberSaveable { mutableStateOf("") }
        var output by rememberSaveable { mutableStateOf("") }
        var errorMsg by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Input JSON", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; errorMsg = "" },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("Paste JSON here...") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    try {
                        val json = JSONObject(input)
                        output = json.toString(2)
                        errorMsg = ""
                    } catch (e: Exception) {
                        Log.w("AppX", "Suppressed exception", e)
                        errorMsg = "Invalid JSON: ${e.message}"
                        output = ""
                    }
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DataObject, contentDescription = null)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Format")
            }
            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (output.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Output", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = output,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
