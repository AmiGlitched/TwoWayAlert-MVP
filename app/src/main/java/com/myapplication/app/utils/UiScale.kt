package com.myapplication.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// Two UI scales: Normal (default, unchanged sizing) and Comfort (elderly-friendly — bigger
// text and bigger touch targets). Rather than rewriting every screen with duplicate layouts,
// this scales density + font scale globally so existing dp/sp values grow together.
enum class UiScale(val densityMultiplier: Float, val fontMultiplier: Float) {
    NORMAL(1.0f, 1.0f),
    COMFORT(1.18f, 1.25f)
}

const val PREFS_KEY_COMFORT_MODE = "comfortModeEnabled"

@Composable
fun ScaledUi(uiScale: UiScale, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val scaledDensity = Density(
        density = base.density * uiScale.densityMultiplier,
        fontScale = base.fontScale * uiScale.fontMultiplier
    )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        content()
    }
}