package com.appdex.ui.theme

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
// AppX Spacing System — Consistent spacing scale
// ═══════════════════════════════════════════════════════════════
// Use these constants instead of arbitrary dp values
// for consistent visual rhythm across the app.
// ═══════════════════════════════════════════════════════════════

object AppXSpacing {
    // Micro spacing — for tight elements (badges, inline icons)
    val xs = 2.dp
    val sm = 4.dp

    // Base spacing — default padding, gaps
    val base = 8.dp
    val md = 12.dp
    val lg = 16.dp

    // Section spacing — between sections, major divisions
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    // Screen-level spacing
    val screen = 16.dp      // horizontal screen padding
    val sectionGap = 20.dp  // gap between sections
    val itemGap = 8.dp      // gap between items in a list

    // Component sizes
    val iconSm = 12.dp
    val iconMd = 16.dp
    val iconLg = 24.dp
    val iconXl = 32.dp
    val iconBox = 36.dp
    val iconBoxSm = 28.dp

    // Bar heights
    val barHeight = 56.dp
    val bottomBarHeight = 64.dp

    // Card padding
    val cardPadding = 12.dp
    val cardPaddingLg = 16.dp

    // Border widths
    val borderThin = 0.5.dp
    val borderDefault = 1.dp
    val borderThick = 2.dp
}
