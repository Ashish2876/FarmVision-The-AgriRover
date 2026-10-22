package com.farmvision.agrirover.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized spacing, elevation, border, and touch dimension tokens
 * for the FarmVision AgriRover design system.
 * Adheres strictly to the 4dp grid convention and minimum 48dp accessible touch targets.
 */
object FarmVisionSpacing {
    val xxSmall: Dp = 2.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 24.dp
    val xxLarge: Dp = 32.dp
    val huge: Dp = 48.dp

    // Standard touch targets & interactive component dimensions
    val minTouchTarget: Dp = 48.dp
    val buttonHeight: Dp = 48.dp
    val buttonHeightCompact: Dp = 40.dp
    val iconSizeSmall: Dp = 16.dp
    val iconSizeMedium: Dp = 20.dp
    val iconSizeLarge: Dp = 24.dp

    // Standard border dimensions
    val borderThin: Dp = 1.dp
    val borderMedium: Dp = 2.dp

    // Standard elevation
    val elevationNone: Dp = 0.dp
    val elevationLow: Dp = 2.dp
    val elevationMedium: Dp = 4.dp
}
