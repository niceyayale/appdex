package com.appdex.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * AppX 自定义颜色方案 — 包含所有自定义颜色
 *
 * 深色主题：Deep Space 配色
 * 亮色主题：Moonlight 配色
 */
data class AppXColors(
    // Background
    val background: Color,
    val backgroundOuter: Color,

    // Surface
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceAlt: Color,
    val surfaceDeep: Color,
    val surfaceInput: Color,

    // Border
    val borderDefault: Color,
    val borderLight: Color,
    val borderMedium: Color,
    val borderAccent: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,

    // Accent — Amber Gold
    val amberGold: Color,
    val amberGoldDark: Color,
    val amberGoldContainer: Color,
    val amberGoldHighlight: Color,
    val amberGoldSelectedBg: Color,

    // Secondary — Blue
    val nebulaBlue: Color,
    val nebulaBlueContainer: Color,
    val iconBlue: Color,
    val iconBlueBright: Color,
    val iconBoxBlue: Color,

    // Tertiary — Green
    val auroraGreen: Color,
    val auroraGreenContainer: Color,
    val auroraGreenBright: Color,

    // Error — Red
    val redSupergiant: Color,
    val redSupergiantContainer: Color,

    // Special
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val aiGradientStart: Color,
    val aiGradientEnd: Color,
    val scoreCardBg: Color,
    val scoreLabelBlue: Color,
    val scoreBarBg: Color,
    val toggleOffBg: Color,
    val toggleThumbColor: Color,
    val sectionLabelColor: Color,

    // Terminal
    val terminalBg: Color,
    val terminalText: Color,
    val terminalPrompt: Color,
)

// ─── Dark Theme Colors ───
val DarkAppXColors = AppXColors(
    background = DeepSpaceBlue,
    backgroundOuter = DeepSpaceOuter,

    surface = SatelliteBlue,
    surfaceVariant = SatelliteVariant,
    surfaceAlt = SurfaceAlt,
    surfaceDeep = SurfaceDeep,
    surfaceInput = SurfaceInput,

    borderDefault = BorderDefault,
    borderLight = BorderLight,
    borderMedium = BorderMedium,
    borderAccent = BorderAccent,

    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    textMuted = TextMuted,

    amberGold = AmberGold,
    amberGoldDark = AmberGoldDark,
    amberGoldContainer = AmberGoldContainer,
    amberGoldHighlight = AmberGoldHighlight,
    amberGoldSelectedBg = AmberGoldSelectedBg,

    nebulaBlue = NebulaBlue,
    nebulaBlueContainer = NebulaBlueContainer,
    iconBlue = IconBlue,
    iconBlueBright = IconBlueBright,
    iconBoxBlue = IconBoxBlue,

    auroraGreen = AuroraGreen,
    auroraGreenContainer = AuroraGreenContainer,
    auroraGreenBright = AuroraGreenBright,

    redSupergiant = RedSupergiant,
    redSupergiantContainer = RedSupergiantContainer,

    heroGradientStart = HeroGradientStart,
    heroGradientEnd = HeroGradientEnd,
    aiGradientStart = Color(0xFF142842),
    aiGradientEnd = Color(0xFF0D1B30),
    scoreCardBg = ScoreCardBg,
    scoreLabelBlue = ScoreLabelBlue,
    scoreBarBg = ScoreBarBg,
    toggleOffBg = ToggleOffBg,
    toggleThumbColor = ToggleThumbColor,
    sectionLabelColor = SectionLabelColor,

    terminalBg = TerminalBg,
    terminalText = TerminalText,
    terminalPrompt = TerminalPrompt,
)

// ─── Light Theme Colors ───
val LightAppXColors = AppXColors(
    background = MoonlightWhite,
    backgroundOuter = Color(0xFFF0F3FA),

    surface = PureWhite,
    surfaceVariant = LightSurfaceVariant,
    surfaceAlt = Color(0xFFF5F7FB),
    surfaceDeep = Color(0xFFECEFF5),
    surfaceInput = Color(0xFFF0F3FA),

    borderDefault = Color(0xFFD8DEE8),
    borderLight = Color(0xFFE0E5EE),
    borderMedium = Color(0xFFC8D0DD),
    borderAccent = Color(0xFFB8C8D8),

    textPrimary = Color(0xFF1A2332),
    textSecondary = Color(0xFF5A6A7E),
    textTertiary = Color(0xFF7A8A9E),
    textMuted = Color(0xFF9AAABE),

    amberGold = DeepAmber,
    amberGoldDark = Color(0xFFFFFFFF),
    amberGoldContainer = LightAmberContainer,
    amberGoldHighlight = DeepAmber,
    amberGoldSelectedBg = LightAmberContainer,

    nebulaBlue = DeepNebulaBlue,
    nebulaBlueContainer = LightNebulaContainer,
    iconBlue = DeepNebulaBlue,
    iconBlueBright = Color(0xFF1E5A8A),
    iconBoxBlue = Color(0xFFE3EDF7),

    auroraGreen = DeepAuroraGreen,
    auroraGreenContainer = LightAuroraContainer,
    auroraGreenBright = DeepAuroraGreen,

    redSupergiant = ErrorRed,
    redSupergiantContainer = ErrorRedContainer,

    heroGradientStart = Color(0xFFF0F3FA),
    heroGradientEnd = Color(0xFFE8EDF5),
    aiGradientStart = Color(0xFFEDF2F8),
    aiGradientEnd = Color(0xFFE0E8F0),
    scoreCardBg = Color(0xFFF0F5FA),
    scoreLabelBlue = DeepNebulaBlue,
    scoreBarBg = Color(0xFFD8E0EC),
    toggleOffBg = Color(0xFFC0C8D4),
    toggleThumbColor = Color(0xFF1A2332),
    sectionLabelColor = Color(0xFF4A5A6E),

    terminalBg = Color(0xFF1A1F2E),
    terminalText = Color(0xFFB9C9DC),
    terminalPrompt = DeepAmber,
)

// ─── Composition Local ───
val LocalAppXColors = staticCompositionLocalOf { DarkAppXColors }

/**
 * 便捷访问器 — 在 Composable 中使用 AppXTheme.colors.xxx
 */
object AppXTheme {
    val colors: AppXColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppXColors.current
}
