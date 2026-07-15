package com.appdex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.ui.theme.*

// ─── AppDexBar — Top header bar ───
@Composable
fun AppDexBar(
    title: String,
    back: Boolean = false,
    onBack: (() -> Unit)? = null,
    showBell: Boolean = false
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
                        .clickable(onClick = onBack),
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
                    text = "APPDEX",
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

// ─── AppDexBar with subtitle and actions slot ───
@Composable
fun AppDexBar(
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
                        .clickable(onClick = onBack),
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
                    text = "APPDEX",
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

// ─── AppDexRow — List row with icon, title, detail, badge ───
@Composable
fun AppDexRow(
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
            .clickable(enabled = onClick != null) { onClick?.invoke() }
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

// ─── AppDexSection — Section with label ───
@Composable
fun AppDexSection(
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

// ─── AppDexButton — Primary amber button ───
@Composable
fun AppDexButton(
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
            .clickable(enabled = enabled, onClick = onClick),
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

// ─── AppDexCard — Bordered card ───
@Composable
fun AppDexCard(
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

// ─── AppDexSearchBar — Search input bar ───
@Composable
fun AppDexSearchBar(
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

// ─── AppDexFAB — Floating action button ───
@Composable
fun AppDexFAB(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(AmberGold)
            .clickable(onClick = onClick),
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

// ─── AppDexDivider — Horizontal divider ───
@Composable
fun AppDexDivider(
    color: Color = BorderDefault
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

// ─── AppDexToggle — Toggle switch ───
@Composable
fun AppDexToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) AuroraGreen else ToggleOffBg)
            .clickable { onCheckedChange(!checked) },
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

// ─── AppDexIconBox — Icon box for quick access tiles ───
@Composable
fun AppDexIconBox(
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
object AppDexIcons {
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

// ─── AppDexTabRow — Unified tab row ───
@Composable
fun AppDexTabRow(
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

// ─── AppDexSnackbarHost — Unified snackbar host ───
@Composable
fun AppDexSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    )
}
