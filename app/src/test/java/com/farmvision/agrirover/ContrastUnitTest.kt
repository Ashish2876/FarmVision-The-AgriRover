package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.theme.ContrastCalculator
import com.farmvision.agrirover.ui.theme.DarkAgriStatusColors
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkThemeText
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.LightAgriStatusColors
import com.farmvision.agrirover.ui.theme.LightBackground
import com.farmvision.agrirover.ui.theme.LightSurface
import com.farmvision.agrirover.ui.theme.PrimaryGreen
import com.farmvision.agrirover.ui.theme.PrimaryTextLight
import com.farmvision.agrirover.ui.theme.TextMutedDark
import com.farmvision.agrirover.ui.theme.TextMutedLight
import com.farmvision.agrirover.ui.theme.WarmOffWhite
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mathematical WCAG 2.1 contrast ratio verification test suite.
 * Enforces >= 4.5:1 for normal text and >= 3.0:1 for large text / UI graphical boundaries.
 */
class ContrastUnitTest {

    @Test
    fun darkTheme_bodyTextOnBackgroundAndSurface_exceedsWcagAAA() {
        val onBgRatio = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkBackground)
        val onSurfaceRatio = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkSurface)

        println("Dark Theme Body on Background: ${"%.2f".format(onBgRatio)}:1")
        println("Dark Theme Body on Surface: ${"%.2f".format(onSurfaceRatio)}:1")

        // WCAG AAA requires 7.0:1. Our tokens achieve > 10:1
        assertTrue("Expected >= 7.0:1 but got $onBgRatio", onBgRatio >= 7.0)
        assertTrue("Expected >= 7.0:1 but got $onSurfaceRatio", onSurfaceRatio >= 7.0)
    }

    @Test
    fun darkTheme_mutedText_satisfiesWcagAA() {
        val ratio = ContrastCalculator.calculateContrastRatio(TextMutedDark, DarkBackground)
        println("Dark Theme Muted on Background: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 4.5:1 but got $ratio", ratio >= 4.5)
    }

    @Test
    fun darkTheme_primaryButtonTextOnContainer_satisfiesWcagAA() {
        val ratio = ContrastCalculator.calculateContrastRatio(DarkBackground, FreshLeaf)
        println("Dark Theme Primary Button Text on FreshLeaf Container: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 4.5:1 but got $ratio", ratio >= 4.5)
    }

    @Test
    fun lightTheme_bodyTextOnBackgroundAndSurface_exceedsWcagAAA() {
        val onBgRatio = ContrastCalculator.calculateContrastRatio(PrimaryTextLight, LightBackground)
        val onSurfaceRatio = ContrastCalculator.calculateContrastRatio(PrimaryTextLight, LightSurface)

        println("Light Theme Body on Background: ${"%.2f".format(onBgRatio)}:1")
        println("Light Theme Body on Surface: ${"%.2f".format(onSurfaceRatio)}:1")

        assertTrue("Expected >= 7.0:1 but got $onBgRatio", onBgRatio >= 7.0)
        assertTrue("Expected >= 7.0:1 but got $onSurfaceRatio", onSurfaceRatio >= 7.0)
    }

    @Test
    fun lightTheme_mutedText_satisfiesWcagAA() {
        val ratio = ContrastCalculator.calculateContrastRatio(TextMutedLight, LightBackground)
        println("Light Theme Muted on Background: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 4.5:1 but got $ratio", ratio >= 4.5)
    }

    @Test
    fun lightTheme_primaryButtonTextOnContainer_satisfiesWcagAA() {
        val ratio = ContrastCalculator.calculateContrastRatio(WarmOffWhite, com.farmvision.agrirover.ui.theme.LightPrimaryGreen)
        println("Light Theme Primary Button Text on LightPrimaryGreen: ${"%.2f".format(ratio)}:1")
        assertTrue("Expected >= 4.5:1 but got $ratio", ratio >= 4.5)
    }

    @Test
    fun semanticStatus_textOnBackground_satisfiesContrast() {
        val darkOnline = ContrastCalculator.calculateContrastRatio(DarkAgriStatusColors.online, DarkBackground)
        val lightOnline = ContrastCalculator.calculateContrastRatio(LightAgriStatusColors.online, LightBackground)

        println("Dark Online on Background: ${"%.2f".format(darkOnline)}:1")
        println("Light Online on Background: ${"%.2f".format(lightOnline)}:1")

        assertTrue(darkOnline >= 3.0)
        assertTrue(lightOnline >= 4.5)
    }
}
