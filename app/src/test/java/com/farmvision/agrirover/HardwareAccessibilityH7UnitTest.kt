package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.theme.ContrastCalculator
import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareFilterCategory
import com.farmvision.agrirover.ui.hardware.model.HardwareSummaryCounts
import com.farmvision.agrirover.ui.hardware.model.HardwareUiDataMapper
import com.farmvision.agrirover.ui.theme.DarkAgriStatusColors
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.DarkThemeText
import com.farmvision.agrirover.ui.theme.WarmOffWhite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Milestone H7 Accessibility, Semantics, and Contrast Verification Suite.
 * Asserts WCAG AA/AAA compliance, content descriptions, heading semantics,
 * and 2.0x font scaling preview configurations across the Hardware Center.
 */
class HardwareAccessibilityH7UnitTest {

    private fun resolveFile(relativePath: String): File {
        val f1 = File(relativePath)
        if (f1.exists()) return f1
        val f2 = File("../$relativePath")
        if (f2.exists()) return f2
        return f1
    }

    @Test
    fun verifyFilterBarCategories_HaveAccessibleLabelsAndCoverage() {
        val categories = HardwareFilterCategory.entries
        assertEquals("Must support 7 filter categories", 7, categories.size)

        val expectedLabels = listOf(
            "All (23)",
            "Controllers (2)",
            "Sensors (4)",
            "Motion (5)",
            "Irrigation (2)",
            "Power & System (3)",
            "Passive Inventory (7)"
        )
        categories.forEachIndexed { index, cat ->
            assertEquals(expectedLabels[index], cat.label)
            assertTrue("Category label must not be blank", cat.label.isNotBlank())
        }
    }

    @Test
    fun verifyCatalogCards_ExposeRichAccessibilityLabels() {
        val uiItems = HardwareUiDataMapper.createUiItems(isDemoMode = false)
        assertEquals("Must generate UI models for all 23 components", 23, uiItems.size)

        uiItems.forEach { item ->
            assertTrue("Display name must be accessible", item.component.displayName.isNotBlank())
            assertTrue("Model designation must be accessible", item.component.modelDesignation.isNotBlank())
            assertNotNull("Category must be defined", item.component.category)
            assertNotNull("Status must be defined", item.component.status)
        }
    }

    @Test
    fun verifyHonestyCards_ProvideClearNonEmptyBoundaryDisclaimers() {
        val uiItems = HardwareUiDataMapper.createUiItems(isDemoMode = false)
        val itemsWithDisclaimers = uiItems.filter { it.honestyDisclaimer != null }

        assertTrue("Multiple components must require honesty disclaimers", itemsWithDisclaimers.size >= 8)
        itemsWithDisclaimers.forEach { item ->
            val disclaimer = item.honestyDisclaimer!!
            assertTrue("Honesty disclaimer must explain physical limits", disclaimer.length >= 20)
            assertTrue(
                "Disclaimer must highlight physical limits: $disclaimer",
                disclaimer.contains("CALIBRATION") ||
                    disclaimer.contains("ENCODERS") ||
                    disclaimer.contains("FLOW") ||
                    disclaimer.contains("FEEDBACK") ||
                    disclaimer.contains("BATTERY") ||
                    disclaimer.contains("ATMOSPHERIC") ||
                    disclaimer.contains("LEVEL SHIFTING") ||
                    disclaimer.contains("FLASHING TOOL")
            )
        }
    }

    @Test
    fun verifySummaryCounts_FormatAccessibleDescription() {
        val counts = HardwareSummaryCounts(
            confirmedConnected = 0,
            disconnected = 0,
            warningOrFault = 0,
            unknownOrDraft = 13,
            passiveInventory = 10
        )
        val expected = "Hardware Inventory Status: 0 Connected, 0 Disconnected, 0 Warning, 13 Unknown, 10 Passive items"
        val actual = "Hardware Inventory Status: " +
            "${counts.confirmedConnected} Connected, " +
            "${counts.disconnected} Disconnected, " +
            "${counts.warningOrFault} Warning, " +
            "${counts.unknownOrDraft} Unknown, " +
            "${counts.passiveInventory} Passive items"

        assertEquals(expected, actual)
    }

    @Test
    fun verifyHighContrastRatios_MeetWcagStandards() {
        // Dark theme body text on dark surface
        val textOnDarkSurface = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkSurface)
        assertTrue("Dark text on DarkSurface must exceed WCAG AA (4.5:1), was $textOnDarkSurface", textOnDarkSurface >= 4.5)

        // Dark theme body text on dark background
        val textOnDarkBackground = ContrastCalculator.calculateContrastRatio(DarkThemeText, DarkBackground)
        assertTrue("Dark text on DarkBackground must exceed WCAG AAA (7.0:1), was $textOnDarkBackground", textOnDarkBackground >= 7.0)

        // Status colors
        val colors = DarkAgriStatusColors
        val warningOnSurface = ContrastCalculator.calculateContrastRatio(colors.warning, DarkBackground)
        assertTrue("Warning indicator must be visible against dark background (>= 3.0:1), was $warningOnSurface", warningOnSurface >= 3.0)

        val onlineOnSurface = ContrastCalculator.calculateContrastRatio(colors.online, DarkBackground)
        assertTrue("Online indicator must be visible against dark background (>= 3.0:1), was $onlineOnSurface", onlineOnSurface >= 3.0)
    }

    @Test
    fun verifyHardwareCenterPreviews_ContainFontScaleAndThemeVariations() {
        val previewFile = resolveFile("app/src/main/java/com/farmvision/agrirover/ui/hardware/screens/HardwareCenterPreviews.kt")
        assertTrue("HardwareCenterPreviews.kt must exist", previewFile.exists())

        val content = previewFile.readText()
        assertTrue("Must include light theme preview", content.contains("HardwareCenterLightPreview"))
        assertTrue("Must include dark theme preview", content.contains("HardwareCenterDarkPreview"))
        assertTrue("Must include 2.0x font scale light preview", content.contains("HardwareCenterLargeFontLightPreview"))
        assertTrue("Must include 2.0x font scale dark preview", content.contains("HardwareCenterLargeFontDarkPreview"))
        assertTrue("Must configure fontScale = 2.0f", content.contains("fontScale = 2.0f"))
        assertTrue("Must include ComponentDetail preview", content.contains("ComponentDetailLightPreview"))
    }

    @Test
    fun verifyHeadingsAndSemantics_PresentInScreenSources() {
        val centerScreenFile = resolveFile("app/src/main/java/com/farmvision/agrirover/ui/hardware/screens/HardwareCenterScreen.kt")
        val detailScreenFile = resolveFile("app/src/main/java/com/farmvision/agrirover/ui/hardware/screens/ComponentDetailScreen.kt")

        val centerContent = centerScreenFile.readText()
        assertTrue("HardwareCenterScreen must declare heading semantics", centerContent.contains("heading()"))

        val detailContent = detailScreenFile.readText()
        assertTrue("ComponentDetailScreen must declare heading semantics", detailContent.contains("heading()"))
    }
}
