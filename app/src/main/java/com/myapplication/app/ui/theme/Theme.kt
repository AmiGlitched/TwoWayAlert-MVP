package com.myapplication.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TintedGlassDarkScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = TextPrimary,
    secondary = AccentGreen,
    background = CanvasDeep,
    onBackground = TextPrimary,
    surface = CanvasElevated,
    onSurface = TextPrimary,
    surfaceVariant = GlassFillLight,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = AccentRed
)

@Composable
fun TwoWayAlertTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TintedGlassDarkScheme,
        typography = Typography,
        content = content
    )
}