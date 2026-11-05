package com.droidpilot.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── DroidPilot terminal palette — matches mockup CSS variables ──
val DpBackground    = Color(0xFF0D1117)  // --bg
val DpSurface       = Color(0xFF161B26)  // --surf  (cards, bars)
val DpSurface2      = Color(0xFF1E2336)  // --surf2 (elevated, hover)
val DpCodeSurface   = Color(0xFF0A0D14)  // editor / log pane

val DpBorder        = Color(0xFF2D3348)  // --border
val DpBorderAccent  = Color(0xFF3D4A70)  // --border2 (primary-tinted border)

val DpPrimary       = Color(0xFF7B9EFF)  // --primary  (blue-purple)
val DpPrimaryDim    = Color(0xFF1C2D52)  // --pdim     (primary container)

val DpGreen         = Color(0xFF52D68A)  // --green   (PASS / success)
val DpGreenDim      = Color(0xFF1A3A2A)  // --gdim    (success container)

val DpRed           = Color(0xFFFF6B6B)  // --red     (FAIL / error)
val DpRedDim        = Color(0xFF2A1A1A)  // --rdim    (error container)

val DpYellow        = Color(0xFFFFB347)  // --yellow  (warning / running)
val DpYellowDim     = Color(0xFF3D2800)

val DpText          = Color(0xFFDEE3F0)  // --text    (primary on-dark)
val DpText2         = Color(0xFFCDD5E8)  // --text2   (secondary text)
val DpMuted         = Color(0xFF7B87A8)  // --muted   (placeholder / timestamp)
val DpLineNumber    = Color(0xFF3D4A70)  // gutter line numbers

// Light scheme (accessibility fallback)
val LightBg         = Color(0xFFF0F4FA)
val LightSurface    = Color(0xFFFFFFFF)
val LightOnBg       = Color(0xFF0D1520)
val LightPrimary    = Color(0xFF3A5BC8)
val LightOutline    = Color(0xFFB0C4E0)
