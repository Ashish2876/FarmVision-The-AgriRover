package com.farmvision.agrirover.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Scoped color palette for the premium Drone-Style Rover Control Station.
 * Anchored to the primary design system tokens (DarkBackground, ParrotGreen, DarkThemeText)
 * and conforms to WCAG 2.1 AA outdoor high-contrast standards.
 */
data class ControlStationColors(
    val background: Color = DarkBackground,
    val surfaceGlass: Color = Color(0xD9132218),
    val surfaceGlassDeep: Color = Color(0xF20D1710),
    val surfaceTile: Color = Color(0x991A2E20),
    val surfacePill: Color = Color(0xB3101E14),
    val surfacePillActive: Color = ParrotGreen,
    val borderGlow: Color = Color(0x5978E000),
    val borderGlowHigh: Color = Color(0xB378E000),
    val borderSubtle: Color = BorderSubtleDark,
    val textPrimary: Color = DarkThemeText,
    val textMuted: Color = TextMutedDark,
    val textSubtle: Color = TextSubtleDark,
    val textOnAccent: Color = TextPrimary,
    val accentGreen: Color = ParrotGreen,
    val accentGreenDeep: Color = ParrotGreenDeep,
    val accentGreenGlow: Color = Color(0x4078E000),
    val eStopRed: Color = Color(0xFFD32F2F),
    val eStopRedGlow: Color = Color(0x80D32F2F),
    val eStopRedPressed: Color = Color(0xFFB71C1C),
    val stickKnob: Color = Color(0xFFE8EFE8),
    val stickKnobIcon: Color = DarkGreen,
    val stickKnobBorder: Color = Color(0xFFFFFFFF),
    val stickRing: Color = ParrotGreen,
    val stickRingGlow: Color = Color(0x4D78E000),
    val stickChevron: Color = Color(0xFFE8EFE8),
    val stickChevronDimmed: Color = Color(0x4DE8EFE8),
    val mapWater: Color = Color(0xFF1B3B2B),
    val mapFieldGrid: Color = Color(0x3378E000)
)

val DefaultControlStationColors = ControlStationColors()

val LocalControlStationColors = staticCompositionLocalOf { DefaultControlStationColors }

object ControlStationTheme {
    val colors: ControlStationColors
        @Composable
        @ReadOnlyComposable
        get() = LocalControlStationColors.current
}

