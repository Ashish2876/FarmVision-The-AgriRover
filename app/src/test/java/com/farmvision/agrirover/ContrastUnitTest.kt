package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.theme.ContrastCalculator
import com.farmvision.agrirover.ui.theme.DarkAgriStatusColors
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.DarkThemeText
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.LightAgriStatusColors
import com.farmvision.agrirover.ui.theme.LightBackground
import com.farmvision.agrirover.ui.theme.LightLime
import com.farmvision.agrirover.ui.theme.LightSurface
import com.farmvision.agrirover.ui.theme.PaleGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.ParrotGreenDeep
import com.farmvision.agrirover.ui.theme.PrimaryGreen
import com.farmvision.agrirover.ui.theme.PrimaryTextLight
import com.farmvision.agrirover.ui.theme.PureWhite
import com.farmvision.agrirover.ui.theme.SoftWhite
import com.farmvision.agrirover.ui.theme.TextMutedDark
import com.farmvision.agrirover.ui.theme.TextMutedLight
import com.farmvision.agrirover.ui.theme.TextPrimary
import com.farmvision.agrirover.ui.theme.TextSecondary
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

    // ========================================================================
    // Contrast Guard Tests (Milestone U1)
    // ========================================================================

    @Test
    fun contrastGuard_parrotGreenFills_carryDarkTextPassingAAA() {
        // Enforces D2: Parrot Green (#78E000) fills must carry dark text, NEVER white text
        val darkTextRatio = ContrastCalculator.calculateContrastRatio(TextPrimary, ParrotGreen)
        println("Contrast Guard - TextPrimary on ParrotGreen: ${"%.2f".format(darkTextRatio)}:1")
        assertTrue("Dark text on ParrotGreen must exceed 7.0:1 (AAA), got $darkTextRatio", darkTextRatio >= 7.0)

        val whiteTextRatio = ContrastCalculator.calculateContrastRatio(PureWhite, ParrotGreen)
        println("Contrast Guard - PureWhite on ParrotGreen: ${"%.2f".format(whiteTextRatio)}:1")
        assertTrue("White text on ParrotGreen must fail (< 3.0:1), got $whiteTextRatio", whiteTextRatio < 3.0)
    }

    @Test
    fun contrastGuard_darkGreenOnLightSurfaces_satisfiesAA() {
        // Enforces D2: DarkGreen (#287A20) is the green for text and icons on white/pale surfaces
        val onWhiteRatio = ContrastCalculator.calculateContrastRatio(DarkGreen, PureWhite)
        val onSoftWhiteRatio = ContrastCalculator.calculateContrastRatio(DarkGreen, SoftWhite)
        val onPaleGreenRatio = ContrastCalculator.calculateContrastRatio(DarkGreen, PaleGreen)
        val onLightLimeRatio = ContrastCalculator.calculateContrastRatio(DarkGreen, LightLime)

        println("Contrast Guard - DarkGreen on PureWhite: ${"%.2f".format(onWhiteRatio)}:1")
        println("Contrast Guard - DarkGreen on SoftWhite: ${"%.2f".format(onSoftWhiteRatio)}:1")
        println("Contrast Guard - DarkGreen on PaleGreen: ${"%.2f".format(onPaleGreenRatio)}:1")
        println("Contrast Guard - DarkGreen on LightLime: ${"%.2f".format(onLightLimeRatio)}:1")

        assertTrue("DarkGreen on PureWhite must be >= 4.5:1, got $onWhiteRatio", onWhiteRatio >= 4.5)
        assertTrue("DarkGreen on SoftWhite must be >= 4.5:1, got $onSoftWhiteRatio", onSoftWhiteRatio >= 4.5)
        assertTrue("DarkGreen on PaleGreen must be >= 4.5:1, got $onPaleGreenRatio", onPaleGreenRatio >= 4.5)
        assertTrue("DarkGreen on LightLime must be >= 4.5:1, got $onLightLimeRatio", onLightLimeRatio >= 4.5)
    }

    @Test
    fun contrastGuard_textSecondaryOnLightSurfaces_satisfiesAA() {
        val onWhiteRatio = ContrastCalculator.calculateContrastRatio(TextSecondary, PureWhite)
        val onSoftWhiteRatio = ContrastCalculator.calculateContrastRatio(TextSecondary, SoftWhite)
        val onPaleGreenRatio = ContrastCalculator.calculateContrastRatio(TextSecondary, PaleGreen)

        assertTrue("TextSecondary on PureWhite must be >= 4.5:1, got $onWhiteRatio", onWhiteRatio >= 4.5)
        assertTrue("TextSecondary on SoftWhite must be >= 4.5:1, got $onSoftWhiteRatio", onSoftWhiteRatio >= 4.5)
        assertTrue("TextSecondary on PaleGreen must be >= 4.5:1, got $onPaleGreenRatio", onPaleGreenRatio >= 4.5)
    }

    @Test
    fun contrastGuard_lightSemanticStatusColors_satisfyAAForText() {
        // Enforces that status text on White and PaleGreen meets WCAG AA (>= 4.5:1)
        val colors = LightAgriStatusColors
        val successRatio = ContrastCalculator.calculateContrastRatio(colors.success, PureWhite)
        val warningRatio = ContrastCalculator.calculateContrastRatio(colors.warning, PureWhite)
        val errorRatio = ContrastCalculator.calculateContrastRatio(colors.error, PureWhite)
        val infoRatio = ContrastCalculator.calculateContrastRatio(colors.info, PureWhite)

        val successPale = ContrastCalculator.calculateContrastRatio(colors.success, PaleGreen)
        val warningPale = ContrastCalculator.calculateContrastRatio(colors.warning, PaleGreen)
        val errorPale = ContrastCalculator.calculateContrastRatio(colors.error, PaleGreen)
        val infoPale = ContrastCalculator.calculateContrastRatio(colors.info, PaleGreen)

        assertTrue("Light success on White must be >= 4.5:1, got $successRatio", successRatio >= 4.5)
        assertTrue("Light warning on White must be >= 4.5:1, got $warningRatio", warningRatio >= 4.5)
        assertTrue("Light error on White must be >= 4.5:1, got $errorRatio", errorRatio >= 4.5)
        assertTrue("Light info on White must be >= 4.5:1, got $infoRatio", infoRatio >= 4.5)

        assertTrue("Light success on PaleGreen must be >= 4.5:1, got $successPale", successPale >= 4.5)
        assertTrue("Light warning on PaleGreen must be >= 4.5:1, got $warningPale", warningPale >= 4.5)
        assertTrue("Light error on PaleGreen must be >= 4.5:1, got $errorPale", errorPale >= 4.5)
        assertTrue("Light info on PaleGreen must be >= 4.5:1, got $infoPale", infoPale >= 4.5)
    }

    @Test
    fun contrastGuard_darkThemeTokens_satisfyAAAndAAA() {
        val textOnBg = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkBackground)
        val textOnSurface = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkSurface)
        val textOnVariant = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkSurfaceVariant)
        val mutedOnBg = ContrastCalculator.calculateContrastRatio(TextMutedDark, DarkBackground)
        val parrotOnBg = ContrastCalculator.calculateContrastRatio(ParrotGreen, DarkBackground)

        assertTrue(textOnBg >= 7.0)
        assertTrue(textOnSurface >= 7.0)
        assertTrue(textOnVariant >= 7.0)
        assertTrue(mutedOnBg >= 4.5)
        assertTrue(parrotOnBg >= 3.0)
    }
}
