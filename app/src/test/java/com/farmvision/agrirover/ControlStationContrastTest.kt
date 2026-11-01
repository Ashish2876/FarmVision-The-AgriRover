package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.theme.ContrastCalculator
import com.farmvision.agrirover.ui.theme.DefaultControlStationColors
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.1 Contrast verification test for [DefaultControlStationColors].
 * Verifies high readability in dark outdoor environments:
 * - Text on background >= 4.5:1 (normal text)
 * - Muted text on background >= 4.5:1
 * - Active text on accent green >= 4.5:1
 * - Graphical accents on dark surface >= 3.0:1
 */
class ControlStationContrastTest {

    @Test
    fun controlStation_primaryTextOnBackground_exceedsWcagAA() {
        val colors = DefaultControlStationColors
        val ratio = ContrastCalculator.calculateContrastRatio(colors.textPrimary, colors.background)
        println("ControlStation TextPrimary on Background: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 7.0:1 AAA, got $ratio", ratio >= 7.0)
    }

    @Test
    fun controlStation_mutedTextOnBackground_satisfiesWcagAA() {
        val colors = DefaultControlStationColors
        val ratio = ContrastCalculator.calculateContrastRatio(colors.textMuted, colors.background)
        println("ControlStation TextMuted on Background: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 4.5:1 AA, got $ratio", ratio >= 4.5)
    }

    @Test
    fun controlStation_textOnAccentPill_satisfiesWcagAA() {
        val colors = DefaultControlStationColors
        val ratio = ContrastCalculator.calculateContrastRatio(colors.textOnAccent, colors.accentGreen)
        println("ControlStation TextOnAccent on AccentGreen: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 4.5:1 AA, got $ratio", ratio >= 4.5)
    }

    @Test
    fun controlStation_accentGlow_satisfiesGraphicalBoundary() {
        val colors = DefaultControlStationColors
        val ratio = ContrastCalculator.calculateContrastRatio(colors.accentGreen, colors.background)
        println("ControlStation AccentGreen on Background: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 3.0:1 for graphical UI element, got $ratio", ratio >= 3.0)
    }
}

