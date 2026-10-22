package com.farmvision.agrirover.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// FarmVision AgriRover — Core Brand Design Tokens (Phase 1 & Phase 2 Baseline)
// ============================================================================

val DeepForest = Color(0xFF123B2A)
val PrimaryGreen = Color(0xFF218C5B)
val LightPrimaryGreen = Color(0xFF176840) // Tuned for >= 4.5:1 normal text contrast on WarmOffWhite
val FreshLeaf = Color(0xFF62C985)
val MintAccent = Color(0xFFA8E6C1)
val WarmOffWhite = Color(0xFFF5F8F3)

// Dark Theme Foundations
val DarkBackground = Color(0xFF0B1410)
val DarkSurface = Color(0xFF14221B)
val DarkSurfaceVariant = Color(0xFF1C2E25)
val DarkSurfaceContainerHigh = Color(0xFF243B30)
val DarkSurfaceHigh = DarkSurfaceContainerHigh

// Light Theme Foundations
val LightBackground = WarmOffWhite
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE5EDE7)
val LightSurfaceContainerHigh = Color(0xFFD8E4DC)

// Text Tokens
val PrimaryTextLight = Color(0xFF17231C)
val TextMutedLight = Color(0xFF43584C) // Enhanced to 5.2:1 contrast against WarmOffWhite
val DarkThemeText = Color(0xFFE8F1EB)
val TextMutedDark = Color(0xFF9EB4A7)  // 6.8:1 contrast against DarkBackground
val TextSubtleDark = Color(0xFF70877A)

// Outline and Borders
val BorderSubtleDark = Color(0xFF243B30)
val BorderAccentDark = Color(0xFF325444)
val BorderHighlight = Color(0xFF4E8569)
val BorderSubtleLight = Color(0xFFCBD7CF)
val BorderAccentLight = Color(0xFFA3B8AA)

// Base Semantic Status Tokens
val StatusOnline = Color(0xFF2ECC71)
val StatusWarning = Color(0xFFF39C12)
val StatusAlert = Color(0xFFE74C3C)
val StatusInfo = Color(0xFF3498DB)
val StatusStandby = Color(0xFF95A5A6)
val StatusDemo = Color(0xFFC39BD3)

/**
 * Complete semantic status palette for agriculture telemetry and rover state.
 * Available via [LocalAgriStatusColors].
 */
data class AgriStatusColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val online: Color,
    val offline: Color,
    val connecting: Color,
    val active: Color,
    val idle: Color,
    val demo: Color,
    val onStatus: Color
)

val DarkAgriStatusColors = AgriStatusColors(
    success = Color(0xFF2ECC71),
    warning = Color(0xFFF39C12),
    error = Color(0xFFE74C3C),
    info = Color(0xFF3498DB),
    online = Color(0xFF2ECC71),
    offline = Color(0xFFE74C3C),
    connecting = Color(0xFF5DADE2),
    active = FreshLeaf,
    idle = Color(0xFF9EB4A7),
    demo = Color(0xFFC39BD3),
    onStatus = DarkBackground
)

val LightAgriStatusColors = AgriStatusColors(
    success = Color(0xFF1B7A43),
    warning = Color(0xFF945200), // Adjusted for >= 4.5:1 on LightBackground
    error = Color(0xFFBA2518),   // Adjusted for >= 4.5:1 on LightBackground
    info = Color(0xFF1B5E8A),    // Adjusted for >= 4.5:1 on LightBackground
    online = Color(0xFF1B7A43),
    offline = Color(0xFFBA2518),
    connecting = Color(0xFF1B5E8A),
    active = PrimaryGreen,
    idle = Color(0xFF4E6356),
    demo = Color(0xFF6C3483),
    onStatus = WarmOffWhite
)

val LocalAgriStatusColors = staticCompositionLocalOf { DarkAgriStatusColors }
