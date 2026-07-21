package com.appdex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// AppX Copilot Panel — AI-assisted editing
// ═══════════════════════════════════════════════════════════════
// Every editor becomes AI assisted.
// Opening AndroidManifest.xml explains: Purpose, Risk, Suggestions
// ═══════════════════════════════════════════════════════════════

data class CopilotInsight(
    val type: InsightType,
    val title: String,
    val description: String
)

enum class InsightType {
    PURPOSE,        // What this file does
    RISK,           // Potential risks
    SUGGESTION,     // Modification suggestions
    RELATED,        // Related files/permissions
    SUMMARY         // AI summary
}

@Composable
fun AppXCopilotPanel(
    fileName: String,
    insights: List<CopilotInsight> = emptyList(),
    isLoading: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val c = AppXTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.borderMedium)
            .background(c.aiGradientStart)
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.iconBlueBright
            )
            Text(
                text = "AppX Copilot",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.iconBlueBright,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = c.amberGold
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(14.dp),
                    tint = c.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (insights.isEmpty() && !isLoading) {
            Text(
                text = "打开文件后，AppX Copilot 会自动分析并提供见解。",
                fontSize = 11.sp,
                color = c.textSecondary
            )
        } else if (isLoading) {
            Text(
                text = "正在分析 $fileName ...",
                fontSize = 11.sp,
                color = c.textSecondary
            )
        } else {
            insights.forEach { insight ->
                CopilotInsightRow(insight)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CopilotInsightRow(insight: CopilotInsight) {
    var expanded by remember { mutableStateOf(true) }
    val c = AppXTheme.colors

    val (icon, color) = when (insight.type) {
        InsightType.PURPOSE -> Icons.Default.Info to c.iconBlue
        InsightType.RISK -> Icons.Default.Warning to c.redSupergiant
        InsightType.SUGGESTION -> Icons.Default.Lightbulb to c.amberGold
        InsightType.RELATED -> Icons.Default.Info to c.auroraGreen
        InsightType.SUMMARY -> Icons.Default.AutoAwesome to c.iconBlueBright
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceAlt.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = insight.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = c.textTertiary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                text = insight.description,
                fontSize = 10.sp,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 4.dp, start = 22.dp),
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Compact copilot badge — shows when copilot is available.
 */
@Composable
fun AppXCopilotBadge(
    onClick: () -> Unit
) {
    val c = AppXTheme.colors
    Row(
        modifier = Modifier
            .background(c.aiGradientStart)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = c.iconBlueBright
        )
        Text(
            text = "Copilot",
            fontSize = 9.sp,
            color = c.iconBlueBright,
            fontFamily = FontFamily.Monospace
        )
    }
}
