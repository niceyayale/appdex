package com.appdex.arsceditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.appdex.ui.components.AppDexSearchBar
import com.appdex.ui.theme.*

@Composable
fun ArscEditorScreen(
    apkPath: String? = null,
    onBack: () -> Unit,
    viewModel: ArscEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(apkPath) {
        if (apkPath != null) {
            viewModel.handleIntent(ArscEditorIntent.LoadFromApk(apkPath))
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
                    title = "ARSC 资源表",
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

            // ── Package selector ──
            if (state.packages.isNotEmpty()) {
                item {
                    Text(
                        text = "包 (${state.packages.size})",
                        color = AmberGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.packages.forEachIndexed { index, pkg ->
                            val isSelected = index == state.selectedPackageIndex
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.handleIntent(ArscEditorIntent.SelectPackage(index))
                                    },
                                color = if (isSelected) AmberGoldContainer else SatelliteBlue,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "0x${pkg.id.toString(16).padStart(2, '0')}",
                                        color = if (isSelected) OnAmberGoldContainer else TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = pkg.name,
                                        color = if (isSelected) OnAmberGoldContainer else TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Stats ──
                val selectedPkg = state.packages.getOrNull(state.selectedPackageIndex)
                if (selectedPkg != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InfoChip("类型", "${selectedPkg.typeStrings.size}", AuroraGreen)
                            InfoChip("键名", "${selectedPkg.keyStrings.size}", NebulaBlue)
                            InfoChip("条目", "${selectedPkg.entries.size}", AmberGold)
                        }
                    }
                }

                // ── Search ──
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.handleIntent(ArscEditorIntent.Search(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("搜索资源 ID、名称、值...", color = TextMuted, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceInput,
                            unfocusedContainerColor = SurfaceInput,
                            cursorColor = AmberGold,
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = BorderDefault,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = TextPrimary,
                            fontSize = 13.sp,
                        ),
                    )
                }

                // ── Entries ──
                items(state.filteredEntries) { entry ->
                    ArscEntryCard(entry)
                }

                if (state.filteredEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.searchQuery.isNotBlank()) "未找到匹配资源" else "无资源条目",
                                color = TextSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.InfoChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        modifier = Modifier.weight(1f),
        color = SatelliteBlue,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
            )
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ArscEntryCard(entry: ArscEntryData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entry.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "@${entry.type}/${entry.config}",
                    color = AuroraGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "0x${entry.resourceId.toString(16).padStart(8, '0')}",
                    color = NebulaBlue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = entry.value,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
        }
    }
}
