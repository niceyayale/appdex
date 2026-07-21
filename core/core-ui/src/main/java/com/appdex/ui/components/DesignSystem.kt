package com.appdex.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.ui.theme.*

// ─── AppXBar — Top header bar ───

/**
 * Current APK display name — provided globally so every AppXBar shows
 * which APK the user is working on. null = no APK loaded.
 */
val LocalCurrentApkName = compositionLocalOf<String?> { null }

/**
 * RC4: Workspace Activity Reporter — provided globally so any tool screen
 * can report user activity (selections, actions) to the SessionManager.
 * This feeds into AI context so the Copilot knows what the user just did.
 */
interface WorkspaceReporter {
    fun report(
        panel: String? = null,
        selection: String? = null,
        action: String? = null,
        timelineType: String? = null,
        timelineTitle: String? = null,
        timelineDetail: String? = null
    )
}

val LocalWorkspaceReporter = compositionLocalOf<WorkspaceReporter?> { null }

/**
 * RC5: WorkspaceController — the brain of the Workspace OS.
 * Provided globally so any composable can read workspace state and emit events.
 */
val LocalWorkspaceController = staticCompositionLocalOf<com.appdex.data.workspace.WorkspaceController?> { null }

/**
 * RC5: WorkspaceEventBus — the central nervous system.
 * Provided globally so any composable can emit events.
 */
val LocalWorkspaceEventBus = staticCompositionLocalOf<com.appdex.data.workspace.WorkspaceEventBus?> { null }

@Composable
fun AppXBar(
    title: String,
    back: Boolean = false,
    onBack: (() -> Unit)? = null,
    showBell: Boolean = false
) {
    val apkName = LocalCurrentApkName.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (back && onBack != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .bounceClick(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(21.dp),
                        tint = TextPrimary
                    )
                }
            }
            Column {
                Text(
                    text = apkName ?: "AppX",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = AmberGold,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
        if (showBell) {
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = TextTertiary
                    )
                }
        }
    }
}

// ─── AppXBar with subtitle and actions slot ───
@Composable
fun AppXBar(
    title: String,
    subtitle: String? = null,
    back: Boolean = false,
    onBack: (() -> Unit)? = null,
    showBell: Boolean = false,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (back && onBack != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .bounceClick(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(21.dp),
                        tint = TextPrimary
                    )
                }
            }
            Column {
                val apkName = LocalCurrentApkName.current
                Text(
                    text = apkName ?: "AppX",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = AmberGold,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (actions != null) {
                actions()
            }
            if (showBell) {
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = TextTertiary
                    )
                }
            }
        }
    }
}

// ─── AppXRow — List row with icon, title, detail, badge ───
@Composable
fun AppXRow(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = true,
    iconTint: Color = IconBlue
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint
            )
        }
        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detail != null) {
                Text(
                    text = detail,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // Badge
        if (badge != null) {
            Text(
                text = badge,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = IconBlueBright
            )
        }
        // Chevron
        if (showChevron && onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TextTertiary
            )
        }
    }
}

// ─── AppXSection — Section with label ───
@Composable
fun AppXSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            color = SectionLabelColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

// ─── AppXButton — Primary amber button ───
@Composable
fun AppXButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(if (enabled) AmberGold else AmberGold.copy(alpha = 0.3f))
                .bounceClick(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AmberGoldDark
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AmberGoldDark
        )
    }
}

// ─── AppXCard — Bordered card ───
@Composable
fun AppXCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderLight,
    backgroundColor: Color = SurfaceAlt,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .border(1.dp, borderColor)
            .background(backgroundColor)
    ) {
        content()
    }
}

// ─── AppXSearchBar — Search input bar ───
@Composable
fun AppXSearchBar(
    placeholder: String = "搜索",
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .border(1.dp, BorderMedium)
            .background(SurfaceInput)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextTertiary
        )
        if (onQueryChange != null) {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = placeholder,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = AmberGold
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp
                )
            )
        } else {
            Text(
                text = placeholder,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

// ─── AppXFAB — Floating action button ───
@Composable
fun AppXFAB(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(AmberGold)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            modifier = Modifier.size(25.dp),
            tint = AmberGoldDark
        )
    }
}

// ─── AppXDivider — Horizontal divider ───
@Composable
fun AppXDivider(
    color: Color = BorderDefault
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

// ─── AppXToggle — Toggle switch ───
@Composable
fun AppXToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) AuroraGreen else ToggleOffBg)
            .bounceClick { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .padding(start = if (checked) 26.dp else 4.dp, top = 4.dp, bottom = 4.dp)
                .clip(CircleShape)
                .background(ToggleThumbColor)
        )
    }
}

// ─── AppXIconBox — Icon box for quick access tiles ───
@Composable
fun AppXIconBox(
    icon: ImageVector,
    size: Int = 36,
    iconSize: Int = 18,
    bgColor: Color = SurfaceAlt,
    iconTint: Color = IconBlue
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize.dp),
            tint = iconTint
        )
    }
}

// ─── Navigation icon mapping ───
val NavHomeIcon = Icons.Default.Home
val NavApkIcon = Icons.Default.Apps
val NavFolderIcon = Icons.Default.Folder
val NavSettingsIcon = Icons.Default.Settings
val NavToolsIcon = Icons.Default.Build
val NavAiIcon = Icons.Default.Psychology
val NavTaskIcon = Icons.Default.Assessment

// ─── Common icon mappings for the design system ───
object AppXIcons {
    val Home = Icons.Default.Home
    val Apk = Icons.Default.Apps
    val Folder = Icons.Default.Folder
    val Settings = Icons.Default.Settings
    val Ai = Icons.Default.Psychology
    val Task = Icons.Default.Assessment
    val Plus = Icons.Default.Add
    val Search = Icons.Default.Search
    val Bell = Icons.Default.Notifications
    val More = Icons.Default.Build
    val Arrow = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Shield = Icons.Default.Security
    val Code = Icons.Default.Code
    val Key = Icons.Default.VpnKey
    val Scan = Icons.Default.Devices
    val File = Icons.Default.Source
    val Chevron = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val Check = Icons.Default.Check
    val Warning = Icons.Default.BugReport
    val Refresh = Icons.Default.Refresh
    val Moon = Icons.Default.DarkMode
    val Info = Icons.Default.Info
    val Close = Icons.Default.Close
    val Terminal = Icons.Default.Terminal
    val Edit = Icons.Default.Edit
    val Network = Icons.Default.Cloud
    val Plugin = Icons.Default.Extension
    val Play = Icons.Default.PlayArrow
    val Storage = Icons.Default.Storage
    val Copy = Icons.Default.Source
}

// ─── AppXTabRow — Unified tab row ───
@Composable
fun AppXTabRow(
    selectedTabIndex: Int,
    tabs: List<Pair<String, ImageVector?>>,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = SurfaceDeep,
        contentColor = AmberGold,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 2.dp,
                color = AmberGold
            )
        }
    ) {
        tabs.forEachIndexed { index, (title, icon) ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTabIndex == index) AmberGoldHighlight else TextTertiary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                icon = if (icon != null) {
                    {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTabIndex == index) AmberGold else TextTertiary
                        )
                    }
                } else null
            )
        }
    }
}

// ─── AppXSnackbarHost — Unified snackbar host ───
@Composable
fun AppXSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    )
}

// ─── AppXCard — Consistent card wrapper ───
@Composable
fun AppXCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 12.dp,
    border: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .then(if (border) Modifier.border(AppXSpacing.borderDefault, BorderLight) else Modifier)
            .background(SurfaceAlt)
            .padding(padding)
    ) {
        content()
    }
}

// ─── ShimmerBox — Loading shimmer effect ───
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 0.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColor = SurfaceVariant.copy(alpha = 0.4f)
    val shimmerHighlight = SurfaceVariant.copy(alpha = 0.7f)
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(shimmerColor, shimmerHighlight, shimmerColor),
        start = androidx.compose.ui.geometry.Offset(translateAnim * 300f - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim * 300f, 0f)
    )
    Box(
        modifier = modifier
            .then(if (cornerRadius > 0.dp) Modifier.clip(RoundedCornerShape(cornerRadius)) else Modifier)
            .background(brush)
    )
}

// ─── AppXShimmerCard — Shimmer loading placeholder ───
@Composable
fun AppXShimmerCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(AppXSpacing.borderDefault, BorderLight)
            .background(SurfaceAlt)
            .padding(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp))
    }
}

// ─── AppXGradientLine — Decorative gradient divider ───
@Composable
fun AppXGradientLine(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BorderMedium.copy(alpha = 0.5f),
                        AmberGold.copy(alpha = 0.3f),
                        BorderMedium.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
    )
}

// ─── CopilotButton — Floating AI Copilot entry point ───
@Composable
fun CopilotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "问 AI"
) {
    val c = AppXTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "copilot_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(c.aiGradientStart, c.aiGradientEnd)
                )
            )
            .border(1.dp, c.iconBlue.copy(alpha = pulseAlpha), RoundedCornerShape(24.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = "AI Copilot",
            modifier = Modifier.size(18.dp),
            tint = c.iconBlueBright
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.iconBlueBright
        )
    }
}
