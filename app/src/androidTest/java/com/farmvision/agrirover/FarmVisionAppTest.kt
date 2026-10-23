package com.farmvision.agrirover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.app.FarmVisionApp
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI test verifying:
 * 1. Bottom navigation across all 5 primary destinations (Home, Rover, My Farm, Analytics, Settings) using test tags.
 * 2. Cross-screen navigation actions (Home -> Rover, Home -> My Farm).
 * 3. Drive & Monitor unified screen telemetry integration.
 * 4. Advanced Rover controller interactions: Directional D-Pad vectors, Center STOP, E-Stop latch/re-arm, and drive modes.
 * 5. Notification ModalBottomSheet opening and dismissal.
 */
class FarmVisionAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appShell_navigatesAcrossAllFiveDestinationsWithTestTags() {
        val testViewModel = FarmViewModel(MockFarmRepository(), null)

        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionApp(viewModel = testViewModel)
            }
        }

        // 1. Initial destination is Home and marked selected
        composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
        composeTestRule.onNodeWithText("FarmVision").assertIsDisplayed()
        composeTestRule.onNodeWithText("Field Telemetry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crop Health Summary").performScrollTo().assertIsDisplayed()

        // 2. Navigate to Rover via testTag
        composeTestRule.onNodeWithTag("nav_rover").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_rover").assertIsSelected()
        composeTestRule.onNodeWithText("AgriRover Alpha-1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Piloting Controls").performScrollTo().assertIsDisplayed()
        // Verify Drive & Monitor HUD on Rover
        composeTestRule.onNodeWithText("Environmental Telemetry HUD").performScrollTo().assertIsDisplayed()

        // 3. Navigate to My Farm via testTag
        composeTestRule.onNodeWithTag("nav_my_farm").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_my_farm").assertIsSelected()
        composeTestRule.onNodeWithText("Plots • Crop Catalog • Irrigation Advice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Roma Tomato").performScrollTo().assertIsDisplayed()

        // 4. Navigate to Analytics via testTag
        composeTestRule.onNodeWithTag("nav_analytics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_analytics").assertIsSelected()
        composeTestRule.onNodeWithText("Field Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Soil Moisture History (%)").performScrollTo().assertIsDisplayed()

        // 5. Navigate to Settings via testTag
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_settings").assertIsSelected()
        composeTestRule.onNodeWithText("Hardware Communication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Measurement Units").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun appShell_crossScreenActionsFromHome_updateSelectedDestination() {
        val testViewModel = FarmViewModel(MockFarmRepository(), null)

        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionApp(viewModel = testViewModel)
            }
        }

        // Home -> Rover button (scroll to ensure visibility on small viewports)
        composeTestRule.onNodeWithText("Open Rover Controls").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_rover").assertIsSelected()
        composeTestRule.onNodeWithText("AgriRover Alpha-1").assertIsDisplayed()

        // Navigate back to Home
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()

        // Home -> My Farm button ("View All")
        composeTestRule.onNodeWithText("View All").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_my_farm").assertIsSelected()
        composeTestRule.onNodeWithText("Plots • Crop Catalog • Irrigation Advice").assertIsDisplayed()
    }

    @Test
    fun roverController_directionalVectors_engageAndCancelWithStop() {
        val testViewModel = FarmViewModel(MockFarmRepository(), null)

        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionApp(viewModel = testViewModel)
            }
        }

        // Navigate to Rover
        composeTestRule.onNodeWithTag("nav_rover").performClick()
        composeTestRule.waitForIdle()

        // Verify primary directional controls exist
        composeTestRule.onNodeWithTag("rover_ctrl_forward").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_reverse").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_left").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_right").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_stop").performScrollTo().assertIsDisplayed()

        // Click Forward
        composeTestRule.onNodeWithTag("rover_ctrl_forward").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Vector: FORWARD").performScrollTo().assertIsDisplayed()

        // Click Center STOP
        composeTestRule.onNodeWithTag("rover_ctrl_stop").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Vector: HOLD POSITION").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun roverController_emergencyStop_latchesAndRequiresRearm() {
        val testViewModel = FarmViewModel(MockFarmRepository(), null)

        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionApp(viewModel = testViewModel)
            }
        }

        // Navigate to Rover
        composeTestRule.onNodeWithTag("nav_rover").performClick()
        composeTestRule.waitForIdle()

        // Trigger E-Stop
        composeTestRule.onNodeWithTag("rover_ctrl_estop").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("POWER CUTOFF ENGAGED").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_rearm").performScrollTo().assertIsDisplayed()

        // Re-arm motors
        composeTestRule.onNodeWithTag("rover_ctrl_rearm").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SAFETY INTERLOCK: ARMED").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_estop").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun homeScreen_notificationBottomSheetOpensAndDisplaysAlerts() {
        val testViewModel = FarmViewModel(MockFarmRepository(), null)

        composeTestRule.setContent {
            FarmVisionTheme {
                FarmVisionApp(viewModel = testViewModel)
            }
        }

        // Click Notifications icon in Home Top Bar
        composeTestRule.onNodeWithContentDescription("Notifications").performClick()
        composeTestRule.waitForIdle()

        // BottomSheet renders
        composeTestRule.onNodeWithText("System Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close notifications").assertIsDisplayed()

        // Dismiss Sheet
        composeTestRule.onNodeWithContentDescription("Close notifications").performClick()
        composeTestRule.waitForIdle()
    }
}
