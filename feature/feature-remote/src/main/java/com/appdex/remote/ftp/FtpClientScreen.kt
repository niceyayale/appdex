package com.appdex.remote.ftp

import android.os.Environment
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpClientScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("21") }
    var username by rememberSaveable { mutableStateOf("anonymous") }
    var password by rememberSaveable { mutableStateOf("") }

    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var statusMsg by rememberSaveable { mutableStateOf("Not connected") }
    var currentDir by rememberSaveable { mutableStateOf("/") }
    var downloadMsg by rememberSaveable { mutableStateOf("") }

    val entries = remember { mutableStateListOf<FtpClientManager.FtpEntry>() }
    val ftpManager = remember { FtpClientManager(context) }

    fun refreshDir() {
        scope.launch {
            val dir = ftpManager.getCurrentDirectory()
            if (dir != null) currentDir = dir
            val files = ftpManager.listFiles(currentDir)
            entries.clear()
            entries.addAll(files)
        }
    }

    fun connect() {
        scope.launch {
            isConnecting = true
            statusMsg = "Connecting to $host:${port}..."
            val config = FtpClientManager.FtpConnectionConfig(
                host = host,
                port = port.toIntOrNull() ?: 21,
                username = username,
                password = password
            )
            val ok = ftpManager.connect(config)
            isConnecting = false
            if (ok) {
                isConnected = true
                statusMsg = "Connected"
                refreshDir()
            } else {
                statusMsg = "Connection failed"
            }
        }
    }

    fun disconnect() {
        ftpManager.disconnect()
        isConnected = false
        statusMsg = "Disconnected"
        entries.clear()
    }

    DisposableEffect(Unit) {
        onDispose { ftpManager.disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("FTP Client")
                    }
                },
                actions = {
                    if (isConnected) {
                        IconButton(onClick = { refreshDir() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { disconnect() }) {
                            Icon(Icons.Default.CloudOff, contentDescription = "Disconnect")
                        }
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
        ) {
            if (!isConnected) {
                // Connection form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("FTP Server", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host / IP Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = port,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 5) port = it },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { connect() },
                            enabled = host.isNotEmpty() && !isConnecting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(if (isConnecting) "Connecting..." else "Connect")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusMsg.startsWith("Connected"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Connected — file browser
                // Breadcrumb
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = currentDir,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (downloadMsg.isNotEmpty()) {
                    Text(
                        text = downloadMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // File list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(entries) { entry ->
                        FtpFileRow(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    scope.launch {
                                        val newPath = if (currentDir.endsWith("/"))
                                            "$currentDir${entry.name}"
                                        else
                                            "$currentDir/${entry.name}"
                                        if (ftpManager.changeDirectory(newPath)) {
                                            currentDir = ftpManager.getCurrentDirectory() ?: newPath
                                            refreshDir()
                                        }
                                    }
                                }
                            },
                            onDownload = {
                                if (!entry.isDirectory) {
                                    scope.launch {
                                        downloadMsg = "Downloading ${entry.name}..."
                                        val remotePath = if (currentDir.endsWith("/"))
                                            "$currentDir${entry.name}"
                                        else
                                            "$currentDir/${entry.name}"
                                        val localDir = Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DOWNLOADS
                                        ).absolutePath
                                        val result = ftpManager.downloadFile(remotePath, localDir)
                                        downloadMsg = if (result != null) {
                                            "Saved to: $result"
                                        } else {
                                            "Download failed"
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FtpFileRow(
    entry: FtpClientManager.FtpEntry,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = if (entry.modifiedTime > 0) dateFormatter.format(Date(entry.modifiedTime)) else "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatSize(entry.size)} • $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!entry.isDirectory) {
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
    if (bytes < 1024 * 1024 * 1024) return "%.1f MB".format(bytes / (1024.0 * 1024))
    return "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
}
