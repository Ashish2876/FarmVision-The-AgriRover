package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.ui.rover.controller.RoverHudPresenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RoverHudPresenter].
 * Validates honesty rules for battery, distance, runtime, and simulated markers.
 */
class RoverHudPresenterTest {

    @Test
    fun formatDuration_formatsHoursMinutesSeconds() {
        assertEquals("00:00:00", RoverHudPresenter.formatDuration(0))
        assertEquals("00:01:05", RoverHudPresenter.formatDuration(65))
        assertEquals("00:18:24", RoverHudPresenter.formatDuration(1104))
        assertEquals("02:05:30", RoverHudPresenter.formatDuration(7530))
    }

    @Test
    fun demoMode_displaysSimulatedTags() {
        val telemetry = RoverTelemetry(
            isSimulation = true,
            connectionStatus = FarmVisionStatusType.DemoData,
            batteryPercent = 84,
            batteryVoltage = 12.4f,
            driveModeEnum = RoverDriveMode.Manual
        )
        val settings = FarmSettings()
        val hud = RoverHudPresenter.present(telemetry, settings, 1104)

        assertTrue("Demo mode connection subtitle", hud.connectionSubtitle.contains("Demo"))
        assertTrue("Camera title should show Sim", hud.cameraTitle.contains("Sim"))
        assertTrue("Battery value must contain [SIM]", hud.batteryValue.contains("[SIM]"))
        assertTrue("Distance value must contain [SIM]", hud.distanceValue.contains("[SIM]"))
        assertTrue("Signal value must contain [SIM]", hud.signalValue.contains("[SIM]"))
        assertEquals("00:18:24", hud.runtimeFormatted)
        assertTrue(hud.isBatteryAvailable)
    }

    @Test
    fun liveHardwareMode_batteryAndDistanceAreTruthfullyUnavailable() {
        // Hardware firmware lacks battery ADC and odometry sensors.
        // Presenter must not invent fake battery percentages or fake distance.
        val telemetry = RoverTelemetry(
            isSimulation = false,
            connectionStatus = FarmVisionStatusType.Online,
            batteryPercent = 84, // ignored in live mode
            driveModeEnum = RoverDriveMode.Manual
        )
        val settings = FarmSettings()
        val hud = RoverHudPresenter.present(telemetry, settings, 500)

        assertFalse("Live hardware battery is unavailable", hud.isBatteryAvailable)
        assertEquals("Unavailable", hud.batteryValue)
        assertEquals("Unavailable", hud.distanceValue)
        assertFalse(hud.cameraTitle.contains("Sim"))
    }
}
