package com.appdex.tools.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.appdex.plugin.AppXPlugin
import com.appdex.plugin.PluginCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TimestampConverterPlugin : AppXPlugin {
    override val id = "timestamp_converter"
    override val name = "Timestamp Converter"
    override val description = "Convert between Unix timestamps and human-readable dates"
    override val author = "AppX"
    override val version = "1.0.0"
    override val category = PluginCategory.CONVERT

    @Composable
    override fun Content() {
        var timestampInput by rememberSaveable { mutableStateOf(System.currentTimeMillis().toString()) }
        var dateInput by rememberSaveable { mutableStateOf("") }
        val clipboard: ClipboardManager = LocalClipboardManager.current

        val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val utcFormatZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val tsLong = timestampInput.toLongOrNull() ?: 0L
        val tsDate = Date(tsLong)
        val dateStr = utcFormat.format(tsDate)
        val dateStrUtc = utcFormatZ.format(tsDate)

        val parsedTs = try {
            if (dateInput.isNotEmpty()) {
                val parsed = utcFormat.parse(dateInput)
                parsed?.time ?: 0L
            } else 0L
        } catch (e: Exception) { android.util.Log.w("TimestampConverter", "parse failed", e); 0L }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Timestamp to Date
            Text("Timestamp → Date", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = timestampInput,
                onValueChange = { if (it.all { c -> c.isDigit() }) timestampInput = it },
                label = { Text("Unix timestamp (ms)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ResultCard("Local Time", dateStr, clipboard)
            Spacer(modifier = Modifier.height(8.dp))
            ResultCard("UTC Time", dateStrUtc, clipboard)

            Spacer(modifier = Modifier.height(24.dp))

            // Date to Timestamp
            Text("Date → Timestamp", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dateInput,
                onValueChange = { dateInput = it },
                label = { Text("Date (yyyy-MM-dd HH:mm:ss)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (dateInput.isNotEmpty() && parsedTs > 0) {
                ResultCard("Timestamp (ms)", parsedTs.toString(), clipboard)
                Spacer(modifier = Modifier.height(8.dp))
                ResultCard("Timestamp (s)", (parsedTs / 1000).toString(), clipboard)
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { timestampInput = System.currentTimeMillis().toString() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Use Current Time")
            }
        }
    }

    @Composable
    private fun ResultCard(label: String, value: String, clipboard: ClipboardManager) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
