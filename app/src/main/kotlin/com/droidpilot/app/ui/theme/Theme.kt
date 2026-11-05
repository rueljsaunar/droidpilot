package com.droidpilot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Extended colors not covered by M3 slots — accessed via LocalDpColors.current
data class DpColors(
    val codeSurface: Color,
    val border: Color,
    val borderAccent: Color,
    val green: Color,
    val greenDim: Color,
    val red: Color,
    val redDim: Color,
    val yellow: Color,
    val yellowDim: Color,
    val text2: Color,
    val muted: Color,
    val lineNumber: Color,
)

val LocalDpColors = staticCompositionLocalOf {
    DpColors(
        codeSurface   = DpCodeSurface,
        border        = DpBorder,
        borderAccent  = DpBorderAccent,
        green         = DpGreen,
        greenDim      = DpGreenDim,
        red           = DpRed,
        redDim        = DpRedDim,
        yellow        = DpYellow,
        yellowDim     = DpYellowDim,
        text2         = DpText2,
        muted         = DpMuted,
        lineNumber    = DpLineNumber,
    )
}

private val DarkColorScheme = darkColorScheme(
    primary              = DpPrimary,
    onPrimary            = DpBackground,
    primaryContainer     = DpPrimaryDim,
    onPrimaryContainer   = DpText,

    secondary            = DpGreen,
    onSecondary          = DpBackground,
    secondaryContainer   = DpGreenDim,
    onSecondaryContainer = DpGreen,

    tertiary             = DpYellow,
    onTertiary           = DpBackground,
    tertiaryContainer    = DpYellowDim,
    onTertiaryContainer  = DpYellow,

    background           = DpBackground,
    onBackground         = DpText,

    surface              = DpSurface,
    onSurface            = DpText,
    surfaceVariant       = DpSurface2,
    onSurfaceVariant     = DpMuted,

    error                = DpRed,
    onError              = DpBackground,
    errorContainer       = DpRedDim,
    onErrorContainer     = DpRed,

    outline              = DpBorder,
    outlineVariant       = DpBorderAccent,

    inverseSurface       = DpText,
    inverseOnSurface     = DpBackground,
    inversePrimary       = DpPrimaryDim,
)

private val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightSurface,
    primaryContainer     = LightPrimary.copy(alpha = 0.12f),
    onPrimaryContainer   = LightOnBg,

    background           = LightBg,
    onBackground         = LightOnBg,

    surface              = LightSurface,
    onSurface            = LightOnBg,
    surfaceVariant       = LightBg,
    onSurfaceVariant     = LightOnBg.copy(alpha = 0.6f),

    outline              = LightOutline,
    outlineVariant       = LightOutline.copy(alpha = 0.5f),

    error                = DpRed,
    onError              = LightSurface,
    errorContainer       = DpRed.copy(alpha = 0.12f),
    onErrorContainer     = DpRed,
)

@Composable
fun DroidPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDpColors provides LocalDpColors.current) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography  = DroidPilotTypography,
            content     = content,
        )
    }
}
