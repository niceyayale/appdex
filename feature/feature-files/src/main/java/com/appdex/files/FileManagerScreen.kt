package com.appdex.files

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.common.FormatUtil
import com.appdex.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "APPDEX",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = state.currentPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(FileManagerIntent.NavigateUp) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(FileManagerIntent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.handleIntent(FileManagerIntent.ToggleHiddenFiles) }) {
                        Icon(
                            if (state.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle hidden"
                        )
                    }
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.hasSelection,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SelectionBottomBar(
                    selectedCount = state.selectedPaths.size,
                    onDelete = { viewModel.handleIntent(FileManagerIntent.DeleteFiles(state.selectedPaths.toList())) },
                    onClear = { viewModel.handleIntent(FileManagerIntent.ClearSelection) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                val filtered = if (state.showHidden) state.files else state.files.filter { !it.isHidden }
                LazyColumn {
                    items(filtered) { file ->
                        FileRow(
                            file = file,
                            isSelected = file.path in state.selectedPaths,
                            onClick = {
                                if (state.hasSelection) {
                                    viewModel.handleIntent(FileManagerIntent.ToggleSelection(file.path))
                                } else if (file.isDirectory) {
                                    viewModel.handleIntent(FileManagerIntent.NavigateTo(file.path))
                                } else {
                                    openFile(context, file)
                                }
                            },
                            onLongClick = {
                                viewModel.handleIntent(FileManagerIntent.ToggleSelection(file.path))
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator or file icon
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                tint = getIconTint(file),
                modifier = Modifier.size(40.dp).padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!file.isDirectory) {
                    Text(
                        text = FormatUtil.formatFileSize(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FormatUtil.formatTimestamp(file.lastModified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (file.isDirectory) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Check, contentDescription = "Clear")
            }
        }
    }
}

private fun getFileIcon(file: FileItem): androidx.compose.ui.graphics.vector.ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    val ext = file.extension.lowercase()
    return when {
        ext == "apk" -> Icons.Default.Apps
        ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg") -> Icons.Default.Image
        ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a") -> Icons.Default.MusicNote
        ext in setOf("mp4", "avi", "mkv", "mov", "flv", "wmv") -> Icons.Default.PlayArrow
        ext in setOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.FolderOpen
        ext in setOf("kt", "java", "py", "js", "ts", "xml", "json", "yaml", "yml", "md",
            "txt", "c", "cpp", "h", "go", "rs", "rb", "php", "sh", "html", "css", "sql") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getIconTint(file: FileItem): Color {
    if (file.isDirectory) return MaterialTheme.colorScheme.primary
    val ext = file.extension.lowercase()
    return when {
        ext == "apk" -> MaterialTheme.colorScheme.secondary
        ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg") -> MaterialTheme.colorScheme.tertiary
        ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a") -> MaterialTheme.colorScheme.tertiary
        ext in setOf("mp4", "avi", "mkv", "mov", "flv", "wmv") -> MaterialTheme.colorScheme.secondary
        ext in setOf("zip", "rar", "7z", "tar", "gz") -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun openFile(context: android.content.Context, file: FileItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        val uri = Uri.parse(file.path)
        setDataAndType(uri, getMimeType(file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // No app to handle this file type
    }
}

private fun getMimeType(file: FileItem): String {
    val ext = file.extension.lowercase()
    return when (ext) {
        "txt", "md" -> "text/plain"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "kt", "java", "py", "c", "cpp", "go", "rs", "rb", "php", "sh" -> "text/plain"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "mp4" -> "video/mp4"
        "avi" -> "video/x-msvideo"
        "mkv" -> "video/x-matroska"
        "pdf" -> "application/pdf"
        "apk" -> "application/vnd.android.package-archive"
        "zip" -> "application/zip"
        else -> "*/*"
    }
}
