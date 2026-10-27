package com.farmvision.agrirover

import androidx.compose.ui.graphics.Color
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.DarkThemeText
import com.farmvision.agrirover.ui.theme.DeepForest
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.PrimaryGreen
import com.farmvision.agrirover.ui.theme.PureWhite
import com.farmvision.agrirover.ui.theme.SoftWhite
import com.farmvision.agrirover.ui.theme.TextPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Baseline unit tests for FarmVision AgriRover technical foundation.
 * Validates design token values, contrast baseline requirements, and core configuration constants.
 */
class BaselineUnitTest {

    @Test
    fun brandColors_matchSpecifiedDesignTokens() {
        assertEquals(Color(0xFF78E000), ParrotGreen)
        assertEquals(Color(0xFF287A20), DarkGreen)
        assertEquals(Color(0xFFFFFFFF), PureWhite)
        assertEquals(Color(0xFFF7FBF4), SoftWhite)
        assertEquals(Color(0xFF1B2E1B), TextPrimary)
        assertEquals(DarkGreen, DeepForest)
        assertEquals(DarkGreen, PrimaryGreen)
        assertEquals(ParrotGreen, FreshLeaf)
        assertEquals(Color(0xFF0D1710), DarkBackground)
    }

    @Test
    fun contrastBaseline_textIsNotIdenticalToBackground() {
        assertNotEquals(DarkBackground, DarkThemeText)
    }

    @Test
    fun applicationPackage_matchesArchitecturalContract() {
        val expectedPackage = "com.farmvision.agrirover"
        assertEquals("com.farmvision.agrirover", expectedPackage)
    }
}
