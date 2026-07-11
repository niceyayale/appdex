package com.appdex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ─── Deep Space Dark Color Scheme ───
val DeepSpaceDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = AmberGoldDark,
    primaryContainer = AmberGoldContainer,
    onPrimaryContainer = OnAmberGoldContainer,

    secondary = NebulaBlue,
    onSecondary = NebulaBlueDark,
    secondaryContainer = NebulaBlueContainer,
    onSecondaryContainer = OnNebulaBlueContainer,

    tertiary = AuroraGreen,
    onTertiary = AuroraGreenDark,
    tertiaryContainer = AuroraGreenContainer,
    onTertiaryContainer = OnAuroraGreenContainer,

    background = DeepSpaceBlue,
    onBackground = StarlightWhite,

    surface = SatelliteBlue,
    onSurface = StarlightWhite,
    surfaceVariant = SatelliteVariant,
    onSurfaceVariant = OnSatelliteVariant,

    outline = AsteroidBelt,
    outlineVariant = AsteroidVariant,

    error = RedSupergiant,
    onError = RedSupergiantDark,
    errorContainer = RedSupergiantContainer,
    onErrorContainer = OnRedSupergiantContainer,
)

// ─── Deep Space Light Color Scheme ───
val DeepSpaceLightColorScheme = lightColorScheme(
    primary = DeepAmber,
    onPrimary = PureWhite,
    primaryContainer = LightAmberContainer,
    onPrimaryContainer = OnLightAmberContainer,

    secondary = DeepNebulaBlue,
    onSecondary = PureWhite,
    secondaryContainer = LightNebulaContainer,
    onSecondaryContainer = OnLightNebulaContainer,

    tertiary = DeepAuroraGreen,
    onTertiary = PureWhite,
    tertiaryContainer = LightAuroraContainer,
    onTertiaryContainer = OnLightAuroraContainer,

    background = MoonlightWhite,
    onBackground = DeepSpaceBlue,

    surface = PureWhite,
    onSurface = DeepSpaceBlue,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
)

@Composable
fun AppDexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DeepSpaceDarkColorScheme
    } else {
        DeepSpaceLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppDexTypography,
        shapes = AppDexShapes,
        content = content
    )
}
