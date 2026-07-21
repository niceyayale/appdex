package com.appdex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdex.data.workspace.*
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.AmberGold
import com.appdex.ui.theme.SurfaceDeep
import com.appdex.ui.theme.SurfaceAlt
import com.appdex.ui.theme.BorderLight
import com.appdex.ui.theme.TextPrimary
import com.appdex.ui.theme.TextSecondary
import com.appdex.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
// Workspace Inspector — Global Sidebar (Phase D)
// ═══════════════════════════════════════════════════════════════
// Always visible (when in a tool). Shows what the user is operating on.
// ═══════════════════════════════════════════════════════════════

@Composable
fun WorkspaceInspector(
    controller: WorkspaceController,
    onNavigateToTool: (WorkspaceTool) -> Unit,
    onClearInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val insights by controller.aiInsights.collectAsStateWithLifecycle()
    val crossToolTargets by controller.crossToolTargets.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(SurfaceDeep)
            .padding(12.dp)
    ) {
        // ── Current APK ──
        InspectorSection(title = "当前 APK") {
            if (state.hasApk) {
                InspectorRow(label = "包名", value = state.packageName)
                InspectorRow(label = "版本", value = state.versionName)
                InspectorRow(label = "大小", value = formatFileSize(state.fileSize))
                InspectorRow(label = "评分", value = "${state.securityScore}/100")
            } else {
                Text("未导入 APK", fontSize = 10.sp, color = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Current Tool ──
        InspectorSection(title = "当前工具") {
            InspectorRow(label = "工具", value = state.activeTool.displayName.ifEmpty { "—" })
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Current Selection ──
        InspectorSection(title = "当前选中") {
            val sel = state.selection
            if (sel is WorkspaceSelection.None) {
                Text("无选中", fontSize = 10.sp, color = TextTertiary)
            } else {
                InspectorRow(label = "类型", value = sel::class.simpleName ?: "")
                InspectorRow(label = "内容", value = sel.displayLabel)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Phase 2: Cross-Tool Navigation (web, not tree) ──
        if (crossToolTargets.isNotEmpty()) {
            InspectorSection(title = "关联工具") {
                crossToolTargets.forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceAlt, RoundedCornerShape(4.dp))
                            .clickable {
                                controller.select(target.selection, target.tool)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = AmberGold
                        )
                        Text(
                            target.label,
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            target.tool.displayName,
                            fontSize = 8.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Inspection Target details (Phase 1) ──
        if (state.inspectionTarget.details.isNotEmpty()) {
            InspectorSection(title = "详情") {
                state.inspectionTarget.details.forEach { (key, value) ->
                    if (value.isNotEmpty()) {
                        InspectorRow(label = key, value = value)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Breadcrumbs ──
        if (state.breadcrumbs.isNotEmpty()) {
            InspectorSection(title = "导航路径") {
                state.breadcrumbs.takeLast(5).forEach { crumb ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTool(crumb.tool) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("›", fontSize = 10.sp, color = AmberGold)
                        Text(
                            text = crumb.label,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── AI Insights (Phase F) ──
        if (insights.isNotEmpty()) {
            InspectorSection(title = "AI 洞察") {
                insights.forEach { insight ->
                    AiInsightCardView(insight = insight, onClick = {
                        onNavigateToTool(insight.tool)
                        onClearInsight()
                    })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Timeline ──
        if (state.timeline.isNotEmpty()) {
            InspectorSection(title = "时间线") {
                state.timeline.takeLast(8).reversed().forEach { event ->
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(time, fontSize = 9.sp, color = TextTertiary, fontFamily = FontFamily.Monospace)
                        Text(
                            event.title,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Pinned Items ──
        if (state.pinnedItems.isNotEmpty()) {
            InspectorSection(title = "已固定") {
                state.pinnedItems.forEach { pinned ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTool(pinned.tool) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(10.dp), tint = AmberGold)
                        Text(
                            pinned.label,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = AmberGold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Column(content = content)
    }
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontSize = 9.sp, color = TextTertiary, modifier = Modifier.width(36.dp))
        Text(
            value,
            fontSize = 10.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AiInsightCardView(insight: AiInsightCard, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceAlt, RoundedCornerShape(6.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(12.dp), tint = AmberGold)
            Text(insight.title, fontSize = 10.sp, color = AmberGold, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(insight.detail, fontSize = 9.sp, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

// ═══════════════════════════════════════════════════════════════
// Workspace Breadcrumb Bar (Phase E)
// ═══════════════════════════════════════════════════════════════

@Composable
fun WorkspaceBreadcrumbBar(
    controller: WorkspaceController,
    onNavigateToTool: (WorkspaceTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsStateWithLifecycle()

    if (state.breadcrumbs.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDeep)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Root
        Text(
            "Workspace",
            fontSize = 10.sp,
            color = AmberGold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onNavigateToTool(WorkspaceTool.WORKSPACE) }
        )

        state.breadcrumbs.takeLast(6).forEach { crumb ->
            Text(" › ", fontSize = 10.sp, color = TextTertiary)
            Text(
                crumb.label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onNavigateToTool(crumb.tool) }
            )
        }
    }
}
