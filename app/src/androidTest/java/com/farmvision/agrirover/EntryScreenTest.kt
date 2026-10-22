package com.farmvision.agrirover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.farmvision.agrirover.ui.entry.EntryScreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI test verifying the EntryScreen renders foundational elements.
 */
class EntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entryScreen_displaysAppTitleAndPhaseBadge() {
        composeTestRule.setContent {
            FarmVisionTheme {
                EntryScreen()
            }
        }

        // Verify product branding
        composeTestRule.onNodeWithText("FarmVision").assertIsDisplayed()
        composeTestRule.onNodeWithText("AGRIROVER").assertIsDisplayed()

        // Verify architectural phase badge
        composeTestRule.onNodeWithText("PHASE 2 OF 15 : REUSABLE COMPONENTS ACTIVE").assertIsDisplayed()
    }
}
