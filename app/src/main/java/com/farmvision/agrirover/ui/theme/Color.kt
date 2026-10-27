package com.farmvision.agrirover.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// FarmVision AgriRover — Core Brand Design Tokens (Parrot Green & White Redesign)
// ============================================================================

val ParrotGreen = Color(0xFF78E000)      // Primary action fills, hero header, selected indicator
val ParrotGreenDeep = Color(0xFF3CB300)  // Pressed/hover fills, strong accents
val DarkGreen = Color(0xFF287A20)        // Green text/icons on white, focus rings, borders on selection
val LightLime = Color(0xFFD4F8A8)        // Selected surfaces, icon containers
val PaleGreen = Color(0xFFEFFBE8)        // Secondary background, selected card surface

val PureWhite = Color(0xFFFFFFFF)        // Content cards, sheets, dialogs, nav bar
val SoftWhite = Color(0xFFF7FBF4)        // App background (outdoor anti-glare canvas)
val TextPrimary = Color(0xFF1B2E1B)      // Headings and high-contrast body (8.57:1 on ParrotGreen, 14.45:1 on White)
val TextSecondary = Color(0xFF5C6B5C)    // Supporting text (5.66:1 on White, 5.40:1 on SoftWhite)
val BorderSubtle = Color(0xFFDDE8D5)     // Thin borders and dividers

// Aliases for backwards compatibility with earlier Phase code
val FreshLeaf = ParrotGreen
val DeepForest = DarkGreen
val PrimaryGreen = DarkGreen
val LightPrimaryGreen = DarkGreen
val MintAccent = LightLime
val WarmOffWhite = SoftWhite
val PrimaryTextLight = TextPrimary
val TextMutedLight = TextSecondary
val LightBackground = SoftWhite
val LightSurface = PureWhite
val LightSurfaceVariant = PaleGreen
val LightSurfaceContainerHigh = Color(0xFFE2F0DB)
val BorderSubtleLight = BorderSubtle
val BorderAccentLight = DarkGreen

// Coordinated Deep-Green Dark Theme Foundations (Default D5)
val DarkBackground = Color(0xFF0D1710)
val DarkSurface = Color(0xFF132218)
val DarkSurfaceVariant = Color(0xFF1A2E20)
val DarkSurfaceContainerHigh = Color(0xFF223C2B)
val DarkSurfaceHigh = DarkSurfaceContainerHigh

val DarkThemeText = Color(0xFFF2F8F1)
val TextMutedDark = Color(0xFF9EB5A2)
val TextSubtleDark = Color(0xFF728A76)

val BorderSubtleDark = Color(0xFF243B2A)
val BorderAccentDark = Color(0xFF33553C)
val BorderHighlight = Color(0xFF4C7D59)

// Base Semantic Status Tokens
val StatusOnline = Color(0xFF2ECC71)
val StatusWarning = Color(0xFFF39C12)
val StatusAlert = Color(0xFFE53935)
val StatusInfo = Color(0xFF3498DB)
val StatusStandby = Color(0xFF95A5A6)
val StatusDemo = Color(0xFFC39BD3)

// Chart Series Tokens
val ChartSeriesPrimary = DarkGreen
val ChartSeriesSecondary = Color(0xFFD97706)
val ChartSeriesTertiary = Color(0xFF2563EB)

data class ChartSeriesColors(
    val series1: Color,
    val series2: Color,
    val series3: Color
)

val DefaultChartSeriesColors = ChartSeriesColors(
    series1 = ChartSeriesPrimary,
    series2 = ChartSeriesSecondary,
    series3 = ChartSeriesTertiary
)

val LocalChartSeriesColors = staticCompositionLocalOf { DefaultChartSeriesColors }

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
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
    info = Color(0xFF60A5FA),
    online = Color(0xFF34D399),
    offline = Color(0xFFF87171),
    connecting = Color(0xFF60A5FA),
    active = ParrotGreen,
    idle = Color(0xFF9EB5A2),
    demo = Color(0xFFC39BD3),
    onStatus = DarkBackground
)

val LightAgriStatusColors = AgriStatusColors(
    success = Color(0xFF1B7A43), // 5.37:1 on White, 5.02:1 on PaleGreen
    warning = Color(0xFF945200), // 6.06:1 on White, 5.66:1 on PaleGreen
    error = Color(0xFFBA2518),   // 6.24:1 on White, 5.83:1 on PaleGreen
    info = Color(0xFF1B5E8A),    // 6.97:1 on White, 6.51:1 on PaleGreen
    online = Color(0xFF1B7A43),
    offline = Color(0xFFBA2518),
    connecting = Color(0xFF1B5E8A),
    active = DarkGreen,
    idle = TextSecondary,
    demo = Color(0xFF6C3483),
    onStatus = PureWhite
)

val LocalAgriStatusColors = staticCompositionLocalOf { LightAgriStatusColors }
