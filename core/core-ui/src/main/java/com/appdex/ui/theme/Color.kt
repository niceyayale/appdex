package com.appdex.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─── Deep Space Dark Theme Colors (Figma-verified) ───
// Original palette inspired by astrophotography

// Primary — Amber Gold
val AmberGold = Color(0xFFE8B547)
val AmberGoldDark = Color(0xFF281D06)
val AmberGoldContainer = Color(0xFF3D2E10)
val OnAmberGoldContainer = Color(0xFFFFD680)
val AmberGoldHighlight = Color(0xFFFFE097)  // selected nav text
val AmberGoldSelectedBg = Color(0xFF3D3012)  // selected nav icon box

// Secondary — Nebula Blue
val NebulaBlue = Color(0xFF5B9BD5)
val NebulaBlueDark = Color(0xFF001A2E)
val NebulaBlueContainer = Color(0xFF0D2A42)
val OnNebulaBlueContainer = Color(0xFF8FCFFF)
val IconBlue = Color(0xFF8FCDF7)  // icon accent blue
val IconBlueBright = Color(0xFF9CD7FF)  // brighter icon blue

// Tertiary — Aurora Green
val AuroraGreen = Color(0xFF7DD3C0)
val AuroraGreenDark = Color(0xFF00382E)
val AuroraGreenContainer = Color(0xFF005142)
val OnAuroraGreenContainer = Color(0xFF99F0DC)
val AuroraGreenBright = Color(0xFF8CE2C8)  // brighter green for scores

// Background — Deep Space Blue
val DeepSpaceBlue = Color(0xFF0B1426)       // inner background
val DeepSpaceOuter = Color(0xFF060D18)      // outer background
val StarlightWhite = Color(0xFFE5EDF7)      // primary text

// Surface — Satellite Blue-Grey
val SatelliteBlue = Color(0xFF101B2E)       // card surface
val SatelliteVariant = Color(0xFF1A2942)    // variant surface
val SurfaceVariant = SatelliteVariant      // alias for convenience
val OnSatelliteVariant = Color(0xFFB4C0D4)
val SurfaceAlt = Color(0xFF101D32)          // alt surface (buttons, cards)
val SurfaceDeep = Color(0xFF101C30)         // deeper surface
val SurfaceInput = Color(0xFF111E33)        // input/search bar bg

// Outline — Asteroid Belt
val AsteroidBelt = Color(0xFF3D5070)
val AsteroidVariant = Color(0xFF243352)
val BorderDefault = Color(0xFF293B55)       // default row border
val BorderLight = Color(0xFF2C3F5A)         // lighter border
val BorderMedium = Color(0xFF344964)        // medium border (inputs)
val BorderAccent = Color(0xFF365675)        // accent border (score card)

// Text — Hierarchy
val TextPrimary = Color(0xFFE5EDF7)         // primary text
val TextSecondary = Color(0xFF8496AE)       // secondary text
val TextTertiary = Color(0xFF8293AA)        // tertiary text
val TextMuted = Color(0xFF7F90A9)           // muted text (placeholders)

// Toast
val ToastBg = Color(0xFF33270E)
val ToastBorder = Color(0xFF85692B)
val ToastText = Color(0xFFFFE4A0)

// Terminal
val TerminalBg = Color(0xFF07101D)
val TerminalText = Color(0xFFB9C9DC)
val TerminalPrompt = Color(0xFFE8B547)

// Error — Red Supergiant
val RedSupergiant = Color(0xFFFF6B6B)
val RedSupergiantDark = Color(0xFF2A0808)
val RedSupergiantContainer = Color(0xFF4D1010)
val OnRedSupergiantContainer = Color(0xFFFFB4B4)

// ─── Light Theme Colors ───
val DeepAmber = Color(0xFFB8860B)
val LightAmberContainer = Color(0xFFFFE9A8)
val OnLightAmberContainer = Color(0xFF2A1D00)

val DeepNebulaBlue = Color(0xFF2E6FA5)
val LightNebulaContainer = Color(0xFFCFE5FF)
val OnLightNebulaContainer = Color(0xFF001D33)

val DeepAuroraGreen = Color(0xFF006B5A)
val LightAuroraContainer = Color(0xFF74F5DD)
val OnLightAuroraContainer = Color(0xFF00201A)

val MoonlightWhite = Color(0xFFFAFBFE)
val PureWhite = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F3FA)
val OnLightSurfaceVariant = Color(0xFF424E5E)

val LightOutline = Color(0xFF7281A0)
val LightOutlineVariant = Color(0xFFC2CAD8)

val ErrorRed = Color(0xFFD92020)
val ErrorRedContainer = Color(0xFFFFE2E2)
val OnErrorRedContainer = Color(0xFF4A0000)

// ─── Extracted hardcoded colors ───
val HeroGradientStart = Color(0xFF142842)
val HeroGradientEnd = Color(0xFF101B30)
val IconBoxBlue = Color(0xFF143451)
val IconBoxDeepBlue = Color(0xFF132D49)
val ToggleOffBg = Color(0xFF46566E)
val ToggleThumbColor = Color(0xFFE5EDF7)
val ScoreCardBg = Color(0xFF102940)
val ScoreLabelBlue = Color(0xFF94D2FC)
val ScoreBarBg = Color(0xFF294865)
val SectionLabelColor = Color(0xFFA4B5C9)
val HexDividerColor = Color(0xFF1A2438)

// ─── Common padding constants ───
val BottomNavPadding = 96.dp
val DefaultBottomPadding = 100.dp
