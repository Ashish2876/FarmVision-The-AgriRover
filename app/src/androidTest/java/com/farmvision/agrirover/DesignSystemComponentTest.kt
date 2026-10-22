package com.farmvision.agrirover

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.farmvision.agrirover.core.designsystem.component.FarmVisionErrorState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionMetricCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI test suite verifying interactive behavior, accessibility semantics,
 * and rendering resilience of Phase 2 design system components.
 */
class DesignSystemComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun button_clickCallbackTriggers_whenEnabled() {
        var clicked = false
        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionPrimaryButton(
                    text = "Execute Action",
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Execute Action")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun button_doesNotClick_whenDisabled() {
        var clicked = false
        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionPrimaryButton(
                    text = "Disabled Action",
                    onClick = { clicked = true },
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithText("Disabled Action")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        assertEquals(false, clicked)
    }

    @Test
    fun statusBadge_exposesCorrectAccessibilitySemantics() {
        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionStatusBadge(status = FarmVisionStatusType.Offline)
            }
        }

        composeTestRule.onNodeWithContentDescription("Status: Offline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline").assertIsDisplayed()
    }

    @Test
    fun metricCard_rendersValueAndUnitWithoutCrashing() {
        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionMetricCard(
                    label = "Soil Moisture",
                    value = "42.8",
                    unit = "%",
                    isSampleData = true
                )
            }
        }

        composeTestRule.onNodeWithText("Soil Moisture").assertIsDisplayed()
        composeTestRule.onNodeWithText("42.8").assertIsDisplayed()
        composeTestRule.onNodeWithText("%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Demo Data").assertIsDisplayed()
    }

    @Test
    fun errorState_triggersRetryCallbackOnButtonClick() {
        var retryTriggered = false
        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionErrorState(
                    title = "Connection Timeout",
                    message = "Telemetry stream timed out after 500ms.",
                    onRetry = { retryTriggered = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Connection Timeout").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry Action").performClick()

        assertTrue(retryTriggered)
    }
}
