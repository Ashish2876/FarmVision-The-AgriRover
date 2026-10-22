package com.farmvision.agrirover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.farmvision.agrirover.ui.components.ComponentCatalogScreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI test verifying the ComponentCatalogScreen renders all design system sections.
 */
class ComponentCatalogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun catalog_rendersAllComponentSections() {
        composeTestRule.setContent {
            FarmVisionTheme {
                ComponentCatalogScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("DESIGN SYSTEM CATALOG").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. Telemetry Link Banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. Semantic Status Chips").assertIsDisplayed()
        composeTestRule.onNodeWithText("3. Telemetry Speed & RPM Gauge").assertIsDisplayed()
        composeTestRule.onNodeWithText("4. Telemetry Metric Cards").assertIsDisplayed()
        composeTestRule.onNodeWithText("5. Emergency Stop (E-Stop) Safety Core").assertIsDisplayed()
        composeTestRule.onNodeWithText("6. Tactile Operational Buttons").assertIsDisplayed()
    }
}
