package com.myapplication.app.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// OLED-black canvas — true black, not near-black, per the design spec (battery + contrast + premium feel).
val CanvasDeep = Color(0xFF000000)
val CanvasElevated = Color(0xFF1C1C1E) // deep charcoal-grey, the glass base tone

// Primary accent: vivid crimson/safety red — the SOS action color.
val AccentRed = Color(0xFFFF3B30)
val AccentRedDim = Color(0xFF5C1C1C)

// Secondary accent: muted amber/warm orange — secondary sensors/alerts, and the warm end of the
// motion-sensor gradient meter.
val AccentAmber = Color(0xFFFF9500)
val AccentGreen = Color(0xFF35D07F) // "System Active" pulse dot only

// Glass panel tokens: deep charcoal-grey at ~40% opacity, per spec, plus a hairline border to
// mimic glass depth. True backdrop blur is applied separately via RenderEffect where the API allows.
val GlassFillLight = CanvasElevated.copy(alpha = 0.40f)
val GlassFillLighter = CanvasElevated.copy(alpha = 0.55f) // pressed/selected state
val GlassBorder = Color(0x26FFFFFF) // ~15% white hairline
val GlassBorderBright = Color(0x40FFFFFF)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAEAEB2) // muted silver, per spec
val TextTertiary = Color(0xFF6E6E76)
