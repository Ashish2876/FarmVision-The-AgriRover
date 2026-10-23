package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying shared state architecture, E-Stop synchronization,
 * rover directional controls, drive mode envelopes, plant irrigation, unit conversions, and settings behavior.
 */
class FarmViewModelTest {

    private lateinit var repository: MockFarmRepository
    private lateinit var viewModel: FarmViewModel

    @Before
    fun setUp() {
        repository = MockFarmRepository()
        viewModel = FarmViewModel(repository = repository, preferencesManager = null)
    }

    @Test
    fun sharedState_initializesWithConsistentTelemetryAndCropData() {
        val telemetry = viewModel.telemetry.value
        assertEquals("AgriRover Alpha-1", telemetry.roverName)
        assertEquals(FarmVisionStatusType.DemoData, telemetry.connectionStatus)
        assertFalse(telemetry.isEmergencyCutoff)
        assertEquals("HOLD POSITION", telemetry.activeCommand)
        assertEquals("Normal", telemetry.driveMode)
        assertEquals(0.0f, telemetry.speedKmh, 0.01f)
        assertTrue(telemetry.isSimulation)

        val plants = viewModel.plants.value
        assertEquals(4, plants.size)

        val alerts = viewModel.alerts.value
        assertEquals(3, alerts.size)

        val settings = viewModel.settings.value
        assertEquals(TemperatureUnit.Celsius, settings.temperatureUnit)
        assertEquals(SpeedUnit.Metric, settings.speedUnit)
        assertEquals("Wi-Fi SoftAP", settings.transportProtocol)
        assertTrue(settings.isSimulatedEngineActive)
    }

    @Test
    fun emergencyStop_synchronizesAcrossStateAndBlocksPilotingCommands() {
        // Trigger Emergency Stop
        viewModel.setEmergencyStop(true)
        val eStoppedTelemetry = viewModel.telemetry.value
        assertTrue(eStoppedTelemetry.isEmergencyCutoff)
        assertEquals("EMERGENCY CUTOFF", eStoppedTelemetry.activeCommand)
        assertEquals("E-STOPPED", eStoppedTelemetry.roverMode)
        assertEquals(0.0f, eStoppedTelemetry.speedKmh, 0.01f)

        // Attempt movement while E-Stopped — must be rejected by safety watchdog
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        val safetyPreservedTelemetry = viewModel.telemetry.value
        assertTrue(safetyPreservedTelemetry.isEmergencyCutoff)
        assertEquals("EMERGENCY CUTOFF", safetyPreservedTelemetry.activeCommand)
        assertEquals(0.0f, safetyPreservedTelemetry.speedKmh, 0.01f)

        // Attempt reverse while E-Stopped — must also be rejected
        viewModel.updateRoverMovement("REVERSE", 1.8f)
        assertEquals("EMERGENCY CUTOFF", viewModel.telemetry.value.activeCommand)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Re-arm safety core: restores eligibility for movement without starting movement automatically
        viewModel.setEmergencyStop(false)
        val rearmedTelemetry = viewModel.telemetry.value
        assertFalse(rearmedTelemetry.isEmergencyCutoff)
        assertEquals("HOLD POSITION", rearmedTelemetry.activeCommand)
        assertEquals("STANDBY", rearmedTelemetry.roverMode)
        assertEquals(0.0f, rearmedTelemetry.speedKmh, 0.01f)
    }

    @Test
    fun roverMovement_allDirectionsExecuteCorrectly() {
        // Forward
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        assertEquals("FORWARD", viewModel.telemetry.value.activeCommand)
        assertEquals(3.5f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("MANUAL_DRIVE", viewModel.telemetry.value.roverMode)

        // Reverse
        viewModel.updateRoverMovement("REVERSE", 1.8f)
        assertEquals("REVERSE", viewModel.telemetry.value.activeCommand)
        assertEquals(1.8f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Turn Left
        viewModel.updateRoverMovement("TURN LEFT", 1.5f)
        assertEquals("TURN LEFT", viewModel.telemetry.value.activeCommand)
        assertEquals(1.5f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Turn Right
        viewModel.updateRoverMovement("TURN RIGHT", 1.5f)
        assertEquals("TURN RIGHT", viewModel.telemetry.value.activeCommand)
        assertEquals(1.5f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Stop / Hold Position cancels movement immediately
        viewModel.updateRoverMovement("HOLD POSITION", 0.0f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("STANDBY", viewModel.telemetry.value.roverMode)
    }

    @Test
    fun driveMode_selection_doesNotStartMovementWhenStationary() {
        assertEquals("Normal", viewModel.telemetry.value.driveMode)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)

        // Change mode to Precision
        viewModel.setDriveMode("Precision")
        assertEquals("Precision", viewModel.telemetry.value.driveMode)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)

        // Change mode to Turbo
        viewModel.setDriveMode("Turbo")
        assertEquals("Turbo", viewModel.telemetry.value.driveMode)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
    }

    @Test
    fun driveMode_scalesVelocityWhenMoving() {
        viewModel.setDriveMode("Normal")
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        assertEquals(3.5f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Switching to Precision while in motion downscales velocity to 1.5 km/h
        viewModel.setDriveMode("Precision")
        assertEquals("Precision", viewModel.telemetry.value.driveMode)
        assertEquals(1.5f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Switching to Turbo while in motion upscales velocity to 5.5 km/h
        viewModel.setDriveMode("Turbo")
        assertEquals("Turbo", viewModel.telemetry.value.driveMode)
        assertEquals(5.5f, viewModel.telemetry.value.speedKmh, 0.01f)

        // Stopping resets to 0.0f
        viewModel.updateRoverMovement("HOLD POSITION", 0.0f)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
    }

    @Test
    fun telemetry_hud_environmentalReadingsAreAccessible() {
        val telemetry = viewModel.telemetry.value
        assertTrue(telemetry.soilMoisture in 0f..100f)
        assertTrue(telemetry.temperature in -20f..60f)
        assertTrue(telemetry.humidity in 0f..100f)
        assertTrue(telemetry.lightLux >= 0)
        assertTrue(telemetry.lastUpdated.isNotBlank())
        assertTrue(telemetry.isSimulation)
    }

    @Test
    fun irrigation_updatesCropMoistureAndRecalculatesSharedSoilMoisture() {
        val initialSoilMoisture = viewModel.telemetry.value.soilMoisture
        val targetCrop = viewModel.plants.value.first { it.id == "crop-03" }
        assertFalse(targetCrop.isWatered)
        val initialCropMoisture = targetCrop.currentMoisture

        // Irrigating crop-03
        viewModel.togglePlantIrrigation("crop-03")

        val updatedCrops = viewModel.plants.value
        val wateredCrop = updatedCrops.first { it.id == "crop-03" }
        assertTrue(wateredCrop.isWatered)
        assertTrue(wateredCrop.currentMoisture > initialCropMoisture)
        assertEquals(initialCropMoisture + 8.5f, wateredCrop.currentMoisture, 0.1f)

        // Overall rootzone soil moisture observed on Home dashboard must increase
        val updatedSoilMoisture = viewModel.telemetry.value.soilMoisture
        assertTrue(updatedSoilMoisture > initialSoilMoisture)
    }

    @Test
    fun unitConversions_celsiusToFahrenheitFormulasAreAccurate() {
        assertEquals(32.0f, TemperatureUnit.Fahrenheit.convertFromCelsius(0.0f), 0.01f)
        assertEquals(212.0f, TemperatureUnit.Fahrenheit.convertFromCelsius(100.0f), 0.01f)
        assertEquals(77.0f, TemperatureUnit.Fahrenheit.convertFromCelsius(25.0f), 0.01f)

        val formattedC = TemperatureUnit.Celsius.format(26.8f)
        val formattedF = TemperatureUnit.Fahrenheit.format(26.8f)
        assertEquals("26.8 °C", formattedC)
        assertEquals("80.2 °F", formattedF)
    }

    @Test
    fun unitConversions_metricToImperialFormulasAreAccurate() {
        assertEquals(0.0f, SpeedUnit.Imperial.convertFromKmh(0.0f), 0.01f)
        val convertedMph = SpeedUnit.Imperial.convertFromKmh(10.0f)
        assertEquals(6.21371f, convertedMph, 0.01f)

        val formattedMetric = SpeedUnit.Metric.format(3.2f)
        val formattedImperial = SpeedUnit.Imperial.format(3.2f)
        assertEquals("3.2 km/h", formattedMetric)
        assertEquals("2.0 mph", formattedImperial)
    }

    @Test
    fun settingsPreferences_reactivelyPropagate() {
        viewModel.setTemperatureUnit(TemperatureUnit.Fahrenheit)
        assertEquals(TemperatureUnit.Fahrenheit, viewModel.settings.value.temperatureUnit)

        viewModel.setSpeedUnit(SpeedUnit.Imperial)
        assertEquals(SpeedUnit.Imperial, viewModel.settings.value.speedUnit)

        viewModel.setTransportProtocol("Bluetooth Low Energy (BLE)")
        assertEquals("Bluetooth Low Energy (BLE)", viewModel.settings.value.transportProtocol)

        viewModel.setSimulatedEngineActive(false)
        assertFalse(viewModel.settings.value.isSimulatedEngineActive)
    }

    @Test
    fun dynamicAnalytics_datasetsCorrespondToSelectedTimeRange() {
        // 24 Hours Range
        viewModel.setAnalyticsRange(AnalyticsTimeRange.Hours24)
        val h24Points = viewModel.getSoilMoistureHistory()
        assertEquals(6, h24Points.size)
        assertEquals("00:00", h24Points.first().label)
        assertEquals("20:00", h24Points.last().label)

        // 7 Days Range
        viewModel.setAnalyticsRange(AnalyticsTimeRange.Days7)
        val d7Points = viewModel.getSoilMoistureHistory()
        assertEquals(7, d7Points.size)
        assertEquals("Mon", d7Points.first().label)
        assertEquals("Sun", d7Points.last().label)

        // 30 Days Range
        viewModel.setAnalyticsRange(AnalyticsTimeRange.Days30)
        val d30Points = viewModel.getSoilMoistureHistory()
        assertEquals(5, d30Points.size)
        assertEquals("W1", d30Points.first().label)
        assertEquals("Current", d30Points.last().label)

        // KPIs change per range
        val kpis24h = viewModel.getAnalyticsKpis(AnalyticsTimeRange.Hours24, TemperatureUnit.Celsius)
        val kpis7d = viewModel.getAnalyticsKpis(AnalyticsTimeRange.Days7, TemperatureUnit.Celsius)
        val kpis30d = viewModel.getAnalyticsKpis(AnalyticsTimeRange.Days30, TemperatureUnit.Celsius)

        assertEquals("28.5 L", kpis24h.irrigationVolume)
        assertEquals("185.0 L", kpis7d.irrigationVolume)
        assertEquals("740.0 L", kpis30d.irrigationVolume)
    }

    @Test
    fun dynamicAnalytics_temperatureDataRespectsUserUnit() {
        viewModel.setAnalyticsRange(AnalyticsTimeRange.Hours24)

        val celsiusPoints = viewModel.getTemperatureHistory(AnalyticsTimeRange.Hours24, TemperatureUnit.Celsius)
        val fahrenheitPoints = viewModel.getTemperatureHistory(AnalyticsTimeRange.Hours24, TemperatureUnit.Fahrenheit)

        assertEquals(celsiusPoints.size, fahrenheitPoints.size)
        val firstC = celsiusPoints.first().value
        val firstF = fahrenheitPoints.first().value
        assertEquals((firstC * 9f / 5f) + 32f, firstF, 0.05f)

        val kpiFahrenheit = viewModel.getAnalyticsKpis(AnalyticsTimeRange.Hours24, TemperatureUnit.Fahrenheit)
        assertTrue(kpiFahrenheit.thermalRange.contains("°F"))
    }

    @Test
    fun notifications_sheetStateAndAlertClearingWork() {
        assertFalse(viewModel.isNotificationSheetOpen.value)

        viewModel.setNotificationSheetOpen(true)
        assertTrue(viewModel.isNotificationSheetOpen.value)

        viewModel.setNotificationSheetOpen(false)
        assertFalse(viewModel.isNotificationSheetOpen.value)

        assertEquals(3, viewModel.alerts.value.size)
        viewModel.clearAllAlerts()
        assertTrue(viewModel.alerts.value.isEmpty())
    }

    @Test
    fun phase5_quickActions_startFieldScanSetsAutonomousModeAndMovement() {
        viewModel.startFieldScan()
        val telemetry = viewModel.telemetry.value
        assertEquals("FIELD SCAN", telemetry.activeCommand)
        assertEquals("AUTONOMOUS", telemetry.roverMode)
        assertEquals(2.0f, telemetry.speedKmh, 0.01f)
        assertEquals("Autonomous Field Scan", telemetry.activeMission)
    }

    @Test
    fun phase5_quickActions_returnToDockSetsDockingModeAndMovement() {
        viewModel.returnToDock()
        val telemetry = viewModel.telemetry.value
        assertEquals("RETURN TO DOCK", telemetry.activeCommand)
        assertEquals("DOCKING", telemetry.roverMode)
        assertEquals(2.2f, telemetry.speedKmh, 0.01f)
        assertEquals("Returning to Dock", telemetry.activeMission)
    }

    @Test
    fun phase5_quickActions_disarmMotorsEngagesEmergencyCutoff() {
        viewModel.startFieldScan()
        assertEquals("AUTONOMOUS", viewModel.telemetry.value.roverMode)

        // Disarm motors
        viewModel.disarmMotors()
        val eStopped = viewModel.telemetry.value
        assertTrue(eStopped.isEmergencyCutoff)
        assertEquals("E-STOPPED", eStopped.roverMode)
        assertEquals("EMERGENCY CUTOFF", eStopped.activeCommand)
        assertEquals(0.0f, eStopped.speedKmh, 0.01f)
        assertTrue(eStopped.activeMission.contains("Halted"))

        // Attempting quick action while disarmed is blocked
        viewModel.startFieldScan()
        assertTrue(viewModel.telemetry.value.isEmergencyCutoff)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)

        viewModel.returnToDock()
        assertTrue(viewModel.telemetry.value.isEmergencyCutoff)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
    }

    @Test
    fun phase5_roverTelemetry_exposesMissionOverviewAndGpsFix() {
        val telemetry = viewModel.telemetry.value
        assertEquals("Autonomous Row Scouting", telemetry.activeMission)
        assertEquals("3D Fix (8 Sats)", telemetry.gpsFixStatus)
        assertEquals("12.9716°N 74.1672°E", telemetry.simulatedGps)
        assertEquals("Field A (Demo)", telemetry.fieldName)
        assertEquals("Rover 01", telemetry.roverIdentifier)
    }

    @Test
    fun phase5_telemetryBadgeLabels_speedAndGpsLogicEvaluatesCorrectly() {
        // Stationary
        assertEquals("Stationary", viewModel.getSpeedStatusLabel())

        // Cruising when speed > 0
        viewModel.updateRoverMovement("FORWARD", 3.0f)
        assertEquals("Cruising", viewModel.getSpeedStatusLabel())

        // Cutoff when E-Stop engaged
        viewModel.setEmergencyStop(true)
        assertEquals("Cutoff", viewModel.getSpeedStatusLabel())

        // GPS Locked vs No Fix
        assertEquals("Locked", viewModel.getGpsStatusLabel("3D Fix (8 Sats)"))
        assertEquals("Locked", viewModel.getGpsStatusLabel("RTK Fix (12 Sats)"))
        assertEquals("No Fix", viewModel.getGpsStatusLabel("No Fix (0 Sats)"))
        assertEquals("No Fix", viewModel.getGpsStatusLabel("Searching..."))
    }
}
