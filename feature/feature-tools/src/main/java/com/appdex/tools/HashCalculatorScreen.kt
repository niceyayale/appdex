package com.appdex.tools

import android.util.Log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashCalculatorScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }
    var md5Result by remember { mutableStateOf("") }
    var sha1Result by remember { mutableStateOf("") }
    var sha256Result by remember { mutableStateOf("") }
    var isCalculating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.path ?: ""
            filePath = path
            fileName = path.substringAfterLast('/')
        }
    }

    fun calculate() {
        scope.launch(Dispatchers.IO) {
            isCalculating = true
            try {
                if (selectedTab == 0) {
                    // Text mode — small data, direct digest is fine
                    val data = inputText.toByteArray()
                    md5Result = bytesToHex(MessageDigest.getInstance("MD5").digest(data))
                    sha1Result = bytesToHex(MessageDigest.getInstance("SHA-1").digest(data))
                    sha256Result = bytesToHex(MessageDigest.getInstance("SHA-256").digest(data))
                } else {
                    // File mode — stream the file through MessageDigest to avoid OOM on large files
                    val path = filePath
                    if (path.isNullOrEmpty()) {
                        md5Result = "No file selected"
                        sha1Result = ""
                        sha256Result = ""
                    } else {
                        val file = File(path)
                        val maxSize = 500 * 1024 * 1024 // 500MB limit
                        if (file.length() > maxSize) {
                            md5Result = "File too large (max 500MB)"
                            sha1Result = ""
                            sha256Result = ""
                        } else {
                            val md5 = MessageDigest.getInstance("MD5")
                            val sha1 = MessageDigest.getInstance("SHA-1")
                            val sha256 = MessageDigest.getInstance("SHA-256")
                            val buffer = ByteArray(8192)
                            file.inputStream().use { input ->
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } > 0) {
                                    md5.update(buffer, 0, bytesRead)
                                    sha1.update(buffer, 0, bytesRead)
                                    sha256.update(buffer, 0, bytesRead)
                                }
                            }
                            md5Result = bytesToHex(md5.digest())
                            sha1Result = bytesToHex(sha1.digest())
                            sha256Result = bytesToHex(sha256.digest())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                md5Result = "Error: ${e.message}"
                sha1Result = ""
                sha256Result = ""
            }
            isCalculating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hash Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Text") },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("File") },
                    icon = { Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Enter text to hash") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (fileName.isNotEmpty()) fileName else "No file selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (fileName.isNotEmpty())
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Select File")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { calculate() },
                enabled = if (selectedTab == 0) inputText.isNotEmpty() else filePath != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCalculating) "Calculating..." else "Calculate Hashes")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (md5Result.isNotEmpty()) {
                HashResultCard("MD5", md5Result, clipboard)
                Spacer(modifier = Modifier.height(8.dp))
                HashResultCard("SHA-1", sha1Result, clipboard)
                Spacer(modifier = Modifier.height(8.dp))
                HashResultCard("SHA-256", sha256Result, clipboard)
            }
        }
    }
}

@Composable
private fun HashResultCard(
    algorithm: String,
    hash: String,
    clipboard: ClipboardManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = algorithm,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(hash)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hash,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}
