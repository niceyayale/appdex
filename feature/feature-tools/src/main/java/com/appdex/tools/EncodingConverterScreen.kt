package com.appdex.tools

import android.util.Log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import java.net.URLDecoder
import java.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncodingConverterScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(EncodingMode.BASE64_ENCODE) }
    val clipboard: ClipboardManager = LocalClipboardManager.current

    fun convert() {
        output = try {
            when (selectedMode) {
                EncodingMode.BASE64_ENCODE -> Base64.getEncoder().encodeToString(input.toByteArray())
                EncodingMode.BASE64_DECODE -> String(Base64.getDecoder().decode(input))
                EncodingMode.URL_ENCODE -> URLEncoder.encode(input, "UTF-8")
                EncodingMode.URL_DECODE -> URLDecoder.decode(input, "UTF-8")
                EncodingMode.HEX_ENCODE -> input.toByteArray().joinToString("") { "%02x".format(it) }
                EncodingMode.HEX_DECODE -> {
                    val bytes = input.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    String(bytes)
                }
                EncodingMode.BINARY_ENCODE -> input.toByteArray().joinToString(" ") {
                    it.toInt().and(0xFF).toString(2).padStart(8, '0')
                }
                EncodingMode.BINARY_DECODE -> {
                    val bytes = input.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
                        .map { it.toInt(2).toByte() }.toByteArray()
                    String(bytes)
                }
            }
        } catch (e: Exception) {
            Log.w("AppX", "Suppressed exception", e)
            "Error: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encoding Converter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EncodingMode.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        pair.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input
            Text(
                text = "Input",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { convert() },
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotEmpty()
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Convert")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Output
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Output",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (output.isNotEmpty()) {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(output)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = output.ifEmpty { "Result will appear here..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (output.startsWith("Error:"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

enum class EncodingMode(val label: String) {
    BASE64_ENCODE("Base64 Enc"),
    BASE64_DECODE("Base64 Dec"),
    URL_ENCODE("URL Enc"),
    URL_DECODE("URL Dec"),
    HEX_ENCODE("Hex Enc"),
    HEX_DECODE("Hex Dec"),
    BINARY_ENCODE("Binary Enc"),
    BINARY_DECODE("Binary Dec")
}
