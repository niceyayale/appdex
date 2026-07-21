package com.appdex.sqliteviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
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
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXButton
import com.appdex.ui.theme.*

@Composable
fun SqliteViewerScreen(
    dbPath: String? = null,
    onBack: () -> Unit,
    viewModel: SqliteViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(dbPath) {
        if (dbPath != null) {
            viewModel.handleIntent(SqliteViewerIntent.OpenDatabase(dbPath))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppXBar(
                    title = if (state.fileName.isNotEmpty()) state.fileName else "SQLite 查看",
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

            // ── Table List ──
            if (state.tables.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "数据表 (${state.tables.size})",
                            color = AmberGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                items(state.tables) { table ->
                    TableListItem(
                        table = table,
                        isSelected = table.name == state.selectedTable,
                        onClick = { viewModel.handleIntent(SqliteViewerIntent.SelectTable(table.name)) },
                    )
                }

                // ── SQL Editor ──
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SQL 查询",
                        color = AmberGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = TerminalBg,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            OutlinedTextField(
                                value = state.sqlInput,
                                onValueChange = { viewModel.handleIntent(SqliteViewerIntent.UpdateSql(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = TerminalText,
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = TerminalBg,
                                    unfocusedContainerColor = TerminalBg,
                                    cursorColor = AmberGold,
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = AsteroidBelt,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                minLines = 2,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { viewModel.handleIntent(SqliteViewerIntent.ExecuteSql) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("执行", color = AmberGold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // ── Schema ──
                if (state.columns.isNotEmpty()) {
                    item {
                        Text(
                            text = "表结构",
                            color = AuroraGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    items(state.columns) { col ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SatelliteBlue,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = col.name,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = col.type,
                                        color = NebulaBlue,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    if (col.isPrimaryKey) {
                                        Surface(
                                            color = AmberGoldContainer,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = "PK",
                                                color = OnAmberGoldContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    if (col.notNull) {
                                        Surface(
                                            color = RedSupergiantContainer,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = "NN",
                                                color = OnRedSupergiantContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Query Results ──
                state.queryResult?.let { result ->
                    item {
                        Text(
                            text = "查询结果 (${result.rows.size} 行)",
                            color = AuroraGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    item {
                        QueryResultTable(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableListItem(
    table: TableInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) AmberGoldContainer else SatelliteBlue,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = if (isSelected) OnAmberGoldContainer else IconBlue,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = table.name,
                    color = if (isSelected) OnAmberGoldContainer else TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = "${table.rowCount} 行",
                color = if (isSelected) OnAmberGoldContainer else TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun QueryResultTable(result: QueryResult) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SatelliteBlue,
        shape = RoundedCornerShape(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState),
        ) {
            Column {
                // Header row
                Row(
                    modifier = Modifier
                        .background(SurfaceVariant)
                        .padding(8.dp),
                ) {
                    result.columns.forEach { col ->
                        Text(
                            text = col,
                            color = AmberGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(120.dp).padding(end = 8.dp),
                        )
                    }
                }
                // Data rows
                result.rows.forEachIndexed { rowIdx, row ->
                    Row(
                        modifier = Modifier
                            .background(if (rowIdx % 2 == 0) SatelliteBlue else SurfaceVariant)
                            .padding(8.dp),
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell ?: "NULL",
                                color = if (cell == null) TextMuted else TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.width(120.dp).padding(end = 8.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
