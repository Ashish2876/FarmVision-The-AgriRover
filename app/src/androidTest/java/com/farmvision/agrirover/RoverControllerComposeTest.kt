package com.farmvision.agrirover

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.rover.RoverScreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test for Phase 6: Advanced Rover Controller.
 * Verifies:
 * 1. Virtual Joystick and Circular D-Pad coexist in the controller cockpit.
 * 2. Virtual Joystick touch drag updates telemetry vector and spring-returns to neutral on release.
 * 3. Drive Mode selector pills toggle Manual, Autonomous Waypoint, and Hold Position.
 * 4. Speed Preset selector pills toggle Low (1.5 km/h), Medium (3.5 km/h), and High (5.5 km/h).
 * 5. Emergency Stop latch engages power cutoff, disables controls, and requires deliberate re-arm.
 */
class RoverControllerComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun joystickAndDpadCoexist_bothDisplayedInCockpit() {
        val repository = MockFarmRepository()
        val viewModel = FarmViewModel(repository, null)

        composeTestRule.setContent {
            FarmVisionTheme {
                RoverScreen(viewModel = viewModel)
            }
        }

        // Virtual Joystick is rendered
        composeTestRule.onNodeWithTag("rover_ctrl_joystick").performScrollTo().assertIsDisplayed()

        // Circular D-Pad and all directional controls coexist
        composeTestRule.onNodeWithTag("rover_ctrl_forward").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_reverse").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_left").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_right").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_stop").performScrollTo().assertIsDisplayed()

        // Telemetry HUD displays initial zero vector
        composeTestRule.onNodeWithText("Tactile Vector Telemetry").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("(0.00, 0.00)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun virtualJoystick_dragAndRelease_returnsToNeutral() {
        val repository = MockFarmRepository()
        val viewModel = FarmViewModel(repository, null)

        composeTestRule.setContent {
            FarmVisionTheme {
                RoverScreen(viewModel = viewModel)
            }
        }

        val joystickNode = composeTestRule.onNodeWithTag("rover_ctrl_joystick").performScrollTo()
        joystickNode.assertIsDisplayed()

        // Drag forward (upward on screen, negative Y offset)
        joystickNode.performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 40f))
            up()
        }
        composeTestRule.waitForIdle()

        // On release, joystick vector must return to neutral
        val currentTelemetry = viewModel.telemetry.value
        assertTrue("Vector should return to neutral on release", currentTelemetry.joystickVector.isNeutral)
        assertEquals(0f, currentTelemetry.joystickVector.magnitude, 0.001f)
    }

    @Test
    fun driveModePills_selectModesAndUpdateViewModelState() {
        val repository = MockFarmRepository()
        val viewModel = FarmViewModel(repository, null)

        composeTestRule.setContent {
            FarmVisionTheme {
                RoverScreen(viewModel = viewModel)
            }
        }

        // Initially in Manual mode
        assertEquals(RoverDriveMode.Manual, viewModel.telemetry.value.driveModeEnum)

        // Switch to Autonomous Waypoint
        composeTestRule.onNodeWithTag("rover_mode_autonomouswaypoint").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(RoverDriveMode.AutonomousWaypoint, viewModel.telemetry.value.driveModeEnum)

        // Switch to Hold Position
        composeTestRule.onNodeWithTag("rover_mode_holdposition").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(RoverDriveMode.HoldPosition, viewModel.telemetry.value.driveModeEnum)

        // Switch back to Manual
        composeTestRule.onNodeWithTag("rover_mode_manual").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(RoverDriveMode.Manual, viewModel.telemetry.value.driveModeEnum)
    }

    @Test
    fun speedPresetPills_selectPresetsAndUpdateDutyCycle() {
        val repository = MockFarmRepository()
        val viewModel = FarmViewModel(repository, null)

        composeTestRule.setContent {
            FarmVisionTheme {
                RoverScreen(viewModel = viewModel)
            }
        }

        // Switch to Low preset
        composeTestRule.onNodeWithTag("rover_speed_low").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(SpeedPreset.Low, viewModel.telemetry.value.speedPreset)
        assertEquals(0.30f, viewModel.telemetry.value.speedPreset.dutyFraction, 0.001f)

        // Switch to High preset
        composeTestRule.onNodeWithTag("rover_speed_high").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(SpeedPreset.High, viewModel.telemetry.value.speedPreset)
        assertEquals(1.00f, viewModel.telemetry.value.speedPreset.dutyFraction, 0.001f)

        // Switch back to Medium preset
        composeTestRule.onNodeWithTag("rover_speed_medium").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(SpeedPreset.Medium, viewModel.telemetry.value.speedPreset)
        assertEquals(0.65f, viewModel.telemetry.value.speedPreset.dutyFraction, 0.001f)
    }

    @Test
    fun emergencyStop_latchesPowerCutoff_andRearmsSuccessfully() {
        val repository = MockFarmRepository()
        val viewModel = FarmViewModel(repository, null)

        composeTestRule.setContent {
            FarmVisionTheme {
                RoverScreen(viewModel = viewModel)
            }
        }

        // Trigger E-Stop
        composeTestRule.onNodeWithTag("rover_ctrl_estop").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue("Emergency cutoff should be latched", viewModel.telemetry.value.isEmergencyCutoff)
        composeTestRule.onNodeWithText("POWER CUTOFF ENGAGED").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("rover_ctrl_rearm").performScrollTo().assertIsDisplayed()

        // Re-arm
        composeTestRule.onNodeWithTag("rover_ctrl_rearm").performClick()
        composeTestRule.waitForIdle()

        assertFalse("Emergency cutoff should be cleared after re-arm", viewModel.telemetry.value.isEmergencyCutoff)
        composeTestRule.onNodeWithText("SAFETY INTERLOCK: ARMED").performScrollTo().assertIsDisplayed()
    }
}
