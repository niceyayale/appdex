package com.appdex.elfviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.theme.*

@Composable
fun ElfViewerScreen(
    filePath: String? = null,
    onBack: () -> Unit,
    viewModel: ElfViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(filePath) {
        if (filePath != null) {
            viewModel.handleIntent(ElfViewerIntent.LoadFile(filePath))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppDexBar(
                    title = if (state.fileName.isNotEmpty()) state.fileName else "ELF 查看",
                    back = true,
                    onBack = onBack,
                    showBell = false,
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AmberGold)
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = RedSupergiantContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = err,
                            modifier = Modifier.padding(16.dp),
                            color = OnRedSupergiantContainer,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // ── Tab Bar ──
            state.elfData?.let { elfData ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ElfTab.values().forEach { tab ->
                            val isSelected = tab == state.selectedTab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.handleIntent(ElfViewerIntent.SelectTab(tab)) },
                                color = if (isSelected) AmberGold else SatelliteBlue,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = tab.title,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    color = if (isSelected) AmberGoldDark else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                when (state.selectedTab) {
                    ElfTab.HEADER -> item { ElfHeaderView(elfData.header) }
                    ElfTab.SECTIONS -> items(elfData.sections) { ElfSectionItem(it) }
                    ElfTab.SEGMENTS -> items(elfData.segments) { ElfSegmentItem(it) }
                    ElfTab.SYMBOLS -> {
                        item {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.handleIntent(ElfViewerIntent.Search(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜索符号名...", color = TextMuted, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceInput,
                                    unfocusedContainerColor = SurfaceInput,
                                    cursorColor = AmberGold,
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = BorderDefault,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 13.sp),
                            )
                        }
                        items(state.filteredSymbols) { ElfSymbolItem(it) }
                        if (state.filteredSymbols.isEmpty()) {
                            item {
                                Text(
                                    text = "无符号",
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                    ElfTab.DYNAMIC -> items(elfData.dynamicEntries) { ElfDynamicItem(it) }
                }
            }
        }
    }
}

@Composable
private fun ElfHeaderView(header: ElfHeader) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Magic", header.magic)
            InfoRow("架构", "${if (header.is64Bit) "64-bit" else "32-bit"} ${if (header.isLittleEndian) "LE" else "BE"}")
            InfoRow("ABI", header.abi)
            InfoRow("类型", header.type)
            InfoRow("机器", header.machine)
            InfoRow("入口点", "0x${header.entry.toString(16)}")
            InfoRow("节区数", "${header.sectionCount}")
            InfoRow("段数", "${header.segmentCount}")
            InfoRow("节名索引", "${header.sectionNameIndex}")
        }
    }
}

@Composable
private fun ElfSectionItem(section: ElfSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "[${section.index}] ${section.name}",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color = when {
                        section.flags.contains("X") -> RedSupergiantContainer
                        section.flags.contains("W") -> AmberGoldContainer
                        else -> NebulaBlueContainer
                    },
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = section.flags.ifEmpty { "-" },
                        color = when {
                            section.flags.contains("X") -> OnRedSupergiantContainer
                            section.flags.contains("W") -> OnAmberGoldContainer
                            else -> OnNebulaBlueContainer
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = section.type, color = AuroraGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(text = "0x${section.offset.toString(16)} (${section.size}B)", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ElfSegmentItem(segment: ElfSegment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "[${segment.index}] ${segment.type}",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = segment.flags.ifEmpty { "-" },
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "vaddr: 0x${segment.virtualAddress.toString(16)}", color = NebulaBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(text = "filesz: ${segment.fileSize} memsz: ${segment.memorySize}", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ElfSymbolItem(symbol: ElfSymbol) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = symbol.name.ifEmpty { "<unnamed>" },
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${symbol.bind} ${symbol.type}",
                    color = when (symbol.type) {
                        "FUNC" -> AuroraGreen
                        "OBJECT" -> NebulaBlue
                        else -> TextSecondary
                    },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "0x${symbol.value.toString(16)}", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(text = "size=${symbol.size}", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ElfDynamicItem(entry: ElfDynamicEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entry.tag,
                color = AmberGold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "0x${entry.value.toString(16)}",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}
