package com.appdex.files

import android.util.Log

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import com.appdex.common.FormatUtil
import com.appdex.model.FileItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onOpenTextFile: (String) -> Unit = {},
    onOpenHexFile: (String) -> Unit = {},
    onNavigateToTab: (String) -> Unit = {},
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Collect effects for Snackbar
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FileManagerEffect.ShowToast -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is FileManagerEffect.FileDeleted -> scope.launch { snackbarHostState.showSnackbar("已删除") }
                is FileManagerEffect.CopyComplete -> scope.launch { snackbarHostState.showSnackbar("复制完成") }
                is FileManagerEffect.MoveComplete -> scope.launch { snackbarHostState.showSnackbar("移动完成") }
                is FileManagerEffect.CompressComplete -> scope.launch { snackbarHostState.showSnackbar("压缩完成") }
                is FileManagerEffect.ExtractComplete -> scope.launch { snackbarHostState.showSnackbar("解压完成") }
                is FileManagerEffect.Error -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is FileManagerEffect.OpenFile, is FileManagerEffect.OpenEditor, is FileManagerEffect.OpenApkAnalyzer, is FileManagerEffect.NavigateToPath -> { /* Handled by navigation callbacks */ }
            }
        }
    }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showFileOpsSheet by remember { mutableStateOf(false) }
    var showDirPicker by remember { mutableStateOf(false) }
    var dirPickerAction by remember { mutableStateOf<DirPickerAction?>(null) }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        viewModel.handleIntent(FileManagerIntent.SearchFiles(searchQuery, false))
                    },
                    onClose = {
                        showSearch = false
                        searchQuery = ""
                        viewModel.handleIntent(FileManagerIntent.Refresh)
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "AppX",
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
                        val isBookmarked = state.bookmarks.any { it.path == state.currentPath }
                        IconButton(onClick = {
                            if (isBookmarked) {
                                viewModel.handleIntent(FileManagerIntent.RemoveBookmark(state.currentPath))
                            } else {
                                val name = File(state.currentPath).name.ifEmpty { state.currentPath }
                                viewModel.handleIntent(FileManagerIntent.AddBookmark(name, state.currentPath))
                            }
                        }) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark"
                            )
                        }
                        IconButton(onClick = { viewModel.handleIntent(FileManagerIntent.Refresh) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { viewModel.handleIntent(FileManagerIntent.ToggleHiddenFiles) }) {
                            Icon(
                                if (state.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle hidden"
                            )
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.hasSelection,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                SelectionBottomBar(
                    selectedCount = state.selectedPaths.size,
                    onDelete = { viewModel.handleIntent(FileManagerIntent.DeleteFiles(state.selectedPaths.toList())) },
                    onMore = { showFileOpsSheet = true },
                    onClear = { viewModel.handleIntent(FileManagerIntent.ClearSelection) }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                    strokeWidth = 2.dp
                )
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = { viewModel.handleIntent(FileManagerIntent.Refresh) }) {
                        Text("Retry")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Bookmarks bar
                    if (state.bookmarks.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.bookmarks) { bookmark ->
                                BookmarkChip(
                                    name = bookmark.name,
                                    onClick = { viewModel.handleIntent(FileManagerIntent.NavigateTo(bookmark.path)) },
                                    onRemove = { viewModel.handleIntent(FileManagerIntent.RemoveBookmark(bookmark.path)) }
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }

                    // File list
                    val filtered = if (state.showHidden) state.files else state.files.filter { !it.isHidden }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No results found" else "Empty directory",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
                                    val ext = file.extension.lowercase()
                                    when {
                                        ext in setOf("txt", "md", "json", "xml", "kt", "java", "py", "js", "ts",
                                            "html", "css", "sh", "yaml", "yml", "c", "cpp", "h", "go", "rs", "rb", "php", "sql") -> {
                                            onOpenTextFile(file.path)
                                        }
                                        ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg") -> {
                                            val imageFiles = state.files.filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg") }
                                            com.appdex.common.MediaNavigationBus.openImage(file.path, imageFiles.map { it.path })
                                        }
                                        ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a") -> {
                                            val audioFiles = state.files.filter { it.extension.lowercase() in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a") }
                                            val idx = audioFiles.indexOfFirst { it.path == file.path }
                                            com.appdex.common.MediaNavigationBus.openAudio(audioFiles.map { it.path }, idx.coerceAtLeast(0))
                                        }
                                        ext in setOf("mp4", "avi", "mkv", "mov", "webm", "3gp") -> {
                                            com.appdex.common.MediaNavigationBus.openVideo(file.path)
                                        }
                                        ext == "apk" -> {
                                            com.appdex.common.MediaNavigationBus.openApk(file.path)
                                        }
                                        else -> openFile(context, file)
                                    }
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

        // Operation progress
        state.operationProgress?.let { op ->
            if (op.total > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { op.current.toFloat() / op.total },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${op.type.name} ${op.current}/${op.total}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = op.currentFile,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog && renameTarget != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameTarget = null
            },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameTarget?.let { target ->
                        viewModel.handleIntent(FileManagerIntent.RenameFile(target.path, renameText))
                    }
                    showRenameDialog = false
                    renameTarget = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    renameTarget = null
                }) { Text("Cancel") }
            }
        )
    }

    // File operations bottom sheet
    if (showFileOpsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFileOpsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "${state.selectedPaths.size} items selected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                FileOpItem(
                    icon = Icons.Outlined.ContentCopy,
                    title = "Copy to...",
                    onClick = {
                        showFileOpsSheet = false
                        dirPickerAction = DirPickerAction.COPY
                        showDirPicker = true
                    }
                )
                FileOpItem(
                    icon = Icons.Outlined.ContentCut,
                    title = "Move to...",
                    onClick = {
                        showFileOpsSheet = false
                        dirPickerAction = DirPickerAction.MOVE
                        showDirPicker = true
                    }
                )
                FileOpItem(
                    icon = Icons.Outlined.FolderZip,
                    title = "Compress (ZIP)",
                    onClick = {
                        val target = "${state.currentPath}/archive_${System.currentTimeMillis()}.zip"
                        viewModel.handleIntent(FileManagerIntent.CompressFiles(state.selectedPaths.toList(), target))
                        showFileOpsSheet = false
                    }
                )
                FileOpItem(
                    icon = Icons.Default.DriveFileRenameOutline,
                    title = "Rename",
                    onClick = {
                        val first = state.selectedPaths.firstOrNull()
                        if (first != null && state.selectedPaths.size == 1) {
                            renameTarget = state.files.find { it.path == first }
                            renameText = renameTarget?.name ?: ""
                            showRenameDialog = true
                        }
                        showFileOpsSheet = false
                    }
                )
                FileOpItem(
                    icon = Icons.Default.Delete,
                    title = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.handleIntent(FileManagerIntent.DeleteFiles(state.selectedPaths.toList()))
                        showFileOpsSheet = false
                    }
                )
            }
        }
    }

    // Directory picker dialog
    if (showDirPicker) {
        DirectoryPickerDialog(
            title = when (dirPickerAction) {
                DirPickerAction.COPY -> "Copy to..."
                DirPickerAction.MOVE -> "Move to..."
                null -> "Select directory"
            },
            initialPath = state.currentPath,
            onConfirm = { target ->
                val sources = state.selectedPaths.toList()
                when (dirPickerAction) {
                    DirPickerAction.COPY -> viewModel.handleIntent(FileManagerIntent.CopyFiles(sources, target))
                    DirPickerAction.MOVE -> viewModel.handleIntent(FileManagerIntent.MoveFiles(sources, target))
                    null -> {}
                }
                showDirPicker = false
                dirPickerAction = null
            },
            onDismiss = {
                showDirPicker = false
                dirPickerAction = null
            }
        )
    }
}

enum class DirPickerAction { COPY, MOVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit
) {
    DockedSearchBar(
        inputField = {
            androidx.compose.material3.SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch() },
                expanded = false,
                onExpandedChange = {},
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                placeholder = { Text("Search files...") }
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {}
}

@Composable
private fun FileOpItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
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
    onMore: () -> Unit,
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
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
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
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
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
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(file.path)
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("AppX", "Suppressed exception", e)
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

@Composable
private fun BookmarkChip(
    name: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove bookmark",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    )
}
