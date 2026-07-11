package com.appdex.settings

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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appdex.data.DensityMode
import com.appdex.data.LanguageMode
import com.appdex.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val densityMode by viewModel.densityMode.collectAsState(initial = DensityMode.STANDARD)
    val languageMode by viewModel.languageMode.collectAsState(initial = LanguageMode.SYSTEM)
    val showHidden by viewModel.showHidden.collectAsState(initial = false)
    val rememberPath by viewModel.rememberLastPath.collectAsState(initial = true)
    val fontSize by viewModel.editorFontSize.collectAsState(initial = 14)
    val tabWidth by viewModel.editorTabWidth.collectAsState(initial = 4)
    val termFontSize by viewModel.terminalFontSize.collectAsState(initial = 13)
    val termScrollback by viewModel.terminalScrollback.collectAsState(initial = 1000)

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf("") }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Cache size: $cacheSize\n\nAre you sure you want to clear the cache?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    cacheSize = viewModel.getCacheSize()
                    showClearCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.titleLarge) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Appearance ──
            SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Info density", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DensityMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = densityMode == mode,
                            onClick = { viewModel.setDensityMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, DensityMode.entries.size)
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }

            // ── Language ──
            SettingsSection(title = "Language", icon = Icons.Default.Language) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LanguageMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = languageMode == mode,
                            onClick = { viewModel.setLanguageMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, LanguageMode.entries.size)
                        ) {
                            Text(when (mode) {
                                LanguageMode.ENGLISH -> "English"
                                LanguageMode.CHINESE -> "中文"
                                LanguageMode.SYSTEM -> "System"
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Language change will take effect after app restart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── File Manager ──
            SettingsSection(title = "File Manager", icon = Icons.Default.Folder) {
                SwitchRow(
                    title = "Show hidden files",
                    checked = showHidden,
                    onCheckedChange = { viewModel.setShowHidden(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "Remember last path",
                    checked = rememberPath,
                    onCheckedChange = { viewModel.setRememberPath(it) }
                )
            }

            // ── Editor ──
            SettingsSection(title = "Editor", icon = Icons.Default.Code) {
                Text("Font size: $fontSize sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { viewModel.setFontSize(it.toInt()) },
                    valueRange = 10f..24f,
                    steps = 13
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text("Tab width: $tabWidth", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = tabWidth.toFloat(),
                    onValueChange = { viewModel.setTabWidth(it.toInt()) },
                    valueRange = 2f..8f,
                    steps = 5
                )
            }

            // ── Terminal ──
            SettingsSection(title = "Terminal", icon = Icons.Default.Terminal) {
                Text("Font size: $termFontSize sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = termFontSize.toFloat(),
                    onValueChange = { viewModel.setTerminalFontSize(it.toInt()) },
                    valueRange = 10f..20f,
                    steps = 9
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text("Scrollback lines: $termScrollback", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = termScrollback.toFloat(),
                    onValueChange = { viewModel.setTerminalScrollback(it.toInt()) },
                    valueRange = 100f..5000f,
                    steps = 48
                )
            }

            // ── Storage ──
            SettingsSection(title = "Storage", icon = Icons.Default.CleaningServices) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cache", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = cacheSize.ifEmpty { viewModel.getCacheSize().also { cacheSize = it } },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = {
                        cacheSize = viewModel.getCacheSize()
                        showClearCacheDialog = true
                    }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Clear")
                    }
                }
            }

            // ── About ──
            SettingsSection(title = "About", icon = Icons.Default.Info) {
                InfoRow("Version", viewModel.getAppVersion())
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("License", "Apache 2.0")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("GitHub", "github.com/niceyayale/appdex")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("Architecture", "MVI + Compose")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("Min Android", "8.0 (API 26)")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "APPDEX • Open Source Android Toolkit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
