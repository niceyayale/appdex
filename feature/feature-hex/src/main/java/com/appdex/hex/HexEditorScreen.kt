package com.appdex.hex

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXDivider
import com.appdex.ui.components.AppXSnackbarHost
import com.appdex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HexEditorScreen(
    filePath: String,
    initialOffset: Long = 0L,
    onBack: () -> Unit = {},
    viewModel: HexEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 首次打开时加载文件
    LaunchedEffect(filePath) {
        if (state.filePath != filePath) {
            viewModel.handleIntent(HexEditorIntent.OpenFile(filePath))
        }
    }

    // ── Navigation Context: 恢复 offset ──
    LaunchedEffect(initialOffset, state.filePath) {
        if (initialOffset > 0L && state.filePath == filePath && state.bytes.isNotEmpty()) {
            val rowIndex = (initialOffset / HexRepository.BYTES_PER_ROW).toInt()
            listState.scrollToItem(rowIndex)
        }
    }

    // 收集 Effect
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HexEditorEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is HexEditorEffect.ShowToast -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is HexEditorEffect.SearchComplete -> {
                    val msg = if (effect.resultCount > 0) "Found ${effect.resultCount} results" else "No results found"
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        }
    }

    // 跳转至搜索结果
    LaunchedEffect(state.jumpOffset) {
        if (state.jumpOffset >= 0) {
            val rowIndex = (state.jumpOffset / HexRepository.BYTES_PER_ROW).toInt()
            listState.scrollToItem(rowIndex)
        }
    }

    // 搜索 UI 状态
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isHexSearch by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AppXBar(
                title = state.fileName.ifEmpty { "十六进制查看" },
                back = true,
                onBack = onBack,
                showBell = false,
            )

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDeep)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = FormatUtil.formatFileSize(state.fileSize),
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                )
                if (state.isDirty) {
                    Text(
                        text = "未保存",
                        fontSize = 10.sp,
                        color = AmberGold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = AmberGold,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Edit toggle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (state.isEditMode) AmberGoldDark else SurfaceAlt)
                        .clickable { viewModel.handleIntent(HexEditorIntent.ToggleEditMode) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                        tint = if (state.isEditMode) AmberGold else TextTertiary
                    )
                }

                // Search
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(SurfaceAlt)
                        .clickable { showSearch = !showSearch },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(16.dp),
                        tint = if (showSearch) AmberGold else TextTertiary
                    )
                }

                // Save
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (state.isDirty) AmberGold else SurfaceAlt)
                        .clickable { viewModel.handleIntent(HexEditorIntent.Save) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(16.dp),
                        tint = if (state.isDirty) AmberGoldDark else TextTertiary
                    )
                }
            }

            // Search bar
            if (showSearch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceInput)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = if (isHexSearch) "Hex search (e.g. 48656C6C6F)" else "ASCII search",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isHexSearch) KeyboardType.Ascii else KeyboardType.Text
                        ),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = BorderMedium,
                            cursorColor = AmberGold,
                        )
                    )
                    // Toggle hex/ascii search
                    Box(
                        modifier = Modifier
                            .background(if (isHexSearch) AmberGoldDark else SurfaceAlt)
                            .clickable { isHexSearch = !isHexSearch }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                                text = if (isHexSearch) "Hex search (e.g. 48656C6C6F)" else "ASCII search",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isHexSearch) AmberGold else TextTertiary
                        )
                    }
                    // Search button
                    Box(
                        modifier = Modifier
                            .background(AmberGold)
                            .clickable {
                                viewModel.handleIntent(HexEditorIntent.Search(searchQuery, isHexSearch))
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "搜索",
                            fontSize = 10.sp,
                            color = AmberGoldDark
                        )
                    }
                }
                if (state.searchResults.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "结果: ${state.searchResultIndex + 1}/${state.searchResults.size}",
                            fontSize = 10.sp,
                            color = IconBlueBright,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Error
            state.error?.let { err ->
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = err, fontSize = 12.sp, color = RedSupergiant)
                }
            }

            // Loading
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(32.dp))
                }
                return@Column
            }

            // Hex content
            if (state.hexRows.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(state.hexRows) { index, row ->
                        HexRowView(
                            row = row,
                            isEditMode = state.isEditMode,
                            searchOffsets = state.searchResults,
                            currentSearchIndex = state.searchResultIndex,
                            onByteClick = { byteIndex ->
                                val absoluteOffset = (index * HexRepository.BYTES_PER_ROW + byteIndex).toInt()
                                if (absoluteOffset < state.bytes.size) {
                                    // Toggle byte value for quick edit (simple editing)
                                    val currentValue = state.bytes[absoluteOffset]
                                    viewModel.handleIntent(HexEditorIntent.EditByte(absoluteOffset, (currentValue.toInt() xor 0xFF).toByte()))
                                }
                            }
                        )
                    }
                }
            }
        }

        AppXSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HexRowView(
    row: HexRow,
    isEditMode: Boolean,
    searchOffsets: List<Long>,
    currentSearchIndex: Int,
    onByteClick: (Int) -> Unit,
) {
    val currentSearchOffset = searchOffsets.getOrNull(currentSearchIndex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        // Offset
        Text(
            text = "%08X".format(row.offset),
            fontSize = 10.sp,
            color = AsteroidBelt,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(72.dp).padding(end = 8.dp),
        )

        // Hex bytes
        Row(modifier = Modifier.weight(1f)) {
            row.bytes.forEachIndexed { i, byte ->
                val absoluteOffset = row.offset.toInt() + i
                val isSearchMatch = currentSearchOffset != null &&
                    absoluteOffset >= currentSearchOffset &&
                    absoluteOffset < currentSearchOffset + (searchOffsets.getOrNull(currentSearchIndex + 1) ?: row.offset + row.bytes.size)

                Text(
                    text = "%02X".format(byte),
                    fontSize = 10.sp,
                    color = when {
                        isSearchMatch -> AmberGoldDark
                        byte == 0x00.toByte() -> AsteroidBelt
                        byte < 0x20.toByte() || byte.toInt() and 0xFF > 126 -> AuroraGreen
                        else -> TextPrimary
                    },
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .width(24.dp)
                        .then(
                            if (isEditMode) {
                                Modifier.clickable { onByteClick(i) }
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (isSearchMatch) {
                                Modifier.background(AmberGold.copy(alpha = 0.2f))
                            } else {
                                Modifier
                            }
                        ),
                )
            }
        }

        // ASCII
        Text(
            text = row.asciiString,
            fontSize = 10.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(160.dp).padding(start = 8.dp),
        )
    }
    AppXDivider(color = HexDividerColor)
}

@Composable
private fun Spacer(modifier: Modifier) {
    Box(modifier = modifier)
}
