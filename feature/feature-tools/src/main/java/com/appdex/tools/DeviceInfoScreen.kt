package com.appdex.tools

import android.util.Log
import android.os.Build
import android.os.StatFs
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val infoGroups = getDeviceInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(infoGroups) { group ->
                InfoGroupCard(group)
            }
        }
    }
}

data class InfoGroup(
    val title: String,
    val items: List<Pair<String, String>>
)

@Composable
private fun InfoGroupCard(group: InfoGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            group.items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

private fun getDeviceInfo(): List<InfoGroup> {
    val groups = mutableListOf<InfoGroup>()

    // Device info
    groups.add(InfoGroup(
        title = "Device",
        items = listOf(
            "Manufacturer" to Build.MANUFACTURER,
            "Model" to Build.MODEL,
            "Brand" to Build.BRAND,
            "Device" to Build.DEVICE,
            "Product" to Build.PRODUCT,
            "Board" to Build.BOARD
        )
    ))

    // OS info
    groups.add(InfoGroup(
        title = "Operating System",
        items = listOf(
            "Android Version" to Build.VERSION.RELEASE,
            "SDK Level" to Build.VERSION.SDK_INT.toString(),
            "Security Patch" to (Build.VERSION.SECURITY_PATCH ?: "N/A"),
            "Build ID" to Build.ID,
            "Build Type" to Build.TYPE,
            "Build Time" to java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date(Build.TIME))
        )
    ))

    // CPU info
    groups.add(InfoGroup(
        title = "CPU",
        items = listOf(
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            "Cores" to Runtime.getRuntime().availableProcessors().toString()
        )
    ))

    // Memory info
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory()
    val totalMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    groups.add(InfoGroup(
        title = "Memory (JVM Heap)",
        items = listOf(
            "Max Heap" to formatBytes(maxMemory),
            "Total Heap" to formatBytes(totalMemory),
            "Free Heap" to formatBytes(freeMemory),
            "Used Heap" to formatBytes(totalMemory - freeMemory)
        )
    ))

    // Storage info
    try {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        groups.add(InfoGroup(
            title = "Storage",
            items = listOf(
                "Total" to formatBytes(totalBlocks * blockSize),
                "Available" to formatBytes(availableBlocks * blockSize),
                "Used" to formatBytes((totalBlocks - availableBlocks) * blockSize)
            )
        ))
    } catch (e: Exception) { Log.w("AppX", "Suppressed exception", e) }

    // Display info
    groups.add(InfoGroup(
        title = "Build",
        items = listOf(
            "Fingerprint" to Build.FINGERPRINT,
            "Bootloader" to Build.BOOTLOADER,
            "Hardware" to Build.HARDWARE
        )
    ))

    return groups
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
    if (bytes < 1024 * 1024 * 1024) return "%.1f MB".format(bytes / (1024.0 * 1024))
    return "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
}
