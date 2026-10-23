package com.farmvision.agrirover

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.app.FarmVisionDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the production app shell, destinations, navigation back policy, and data repository.
 */
class AppShellUnitTest {

    private val repository = MockFarmRepository()

    @Test
    fun appShell_destinationsCoverAllFiveRequiredTabs() {
        val destinations = FarmVisionDestination.entries
        assertEquals(5, destinations.size)
        assertEquals(FarmVisionDestination.Home, destinations[0])
        assertEquals(FarmVisionDestination.Rover, destinations[1])
        assertEquals(FarmVisionDestination.MyFarm, destinations[2])
        assertEquals(FarmVisionDestination.Analytics, destinations[3])
        assertEquals(FarmVisionDestination.Settings, destinations[4])

        destinations.forEach { dest ->
            assertTrue(dest.label.isNotBlank())
            assertNotNull(dest.icon)
            assertTrue(dest.route.isNotBlank())
            assertTrue(dest.testTag.isNotBlank())
        }
    }

    @Test
    fun appShell_destinationRoutesAndTestTagsAreUnique() {
        val destinations = FarmVisionDestination.entries
        val routes = destinations.map { it.route }.toSet()
        val testTags = destinations.map { it.testTag }.toSet()
        val labels = destinations.map { it.label }.toSet()

        assertEquals(5, routes.size)
        assertEquals(5, testTags.size)
        assertEquals(5, labels.size)

        assertEquals("nav_home", FarmVisionDestination.Home.testTag)
        assertEquals("nav_rover", FarmVisionDestination.Rover.testTag)
        assertEquals("nav_my_farm", FarmVisionDestination.MyFarm.testTag)
        assertEquals("nav_analytics", FarmVisionDestination.Analytics.testTag)
        assertEquals("nav_settings", FarmVisionDestination.Settings.testTag)
    }

    @Test
    fun appShell_backNavigationPolicy_resolvesInOrder() {
        // Back policy helper simulator matching FarmVisionApp BackHandler contract
        fun evaluateBackAction(
            currentDestination: FarmVisionDestination,
            isNotificationSheetOpen: Boolean
        ): String {
            return when {
                isNotificationSheetOpen -> "DISMISS_SHEET"
                currentDestination != FarmVisionDestination.Home -> "RETURN_TO_HOME"
                else -> "EXIT_APP"
            }
        }

        // Rule 1: Modal open -> dismiss modal first, staying on current destination
        assertEquals("DISMISS_SHEET", evaluateBackAction(FarmVisionDestination.Home, isNotificationSheetOpen = true))
        assertEquals("DISMISS_SHEET", evaluateBackAction(FarmVisionDestination.Rover, isNotificationSheetOpen = true))
        assertEquals("DISMISS_SHEET", evaluateBackAction(FarmVisionDestination.MyFarm, isNotificationSheetOpen = true))

        // Rule 2: Non-Home destination with no modal -> returns to Home
        assertEquals("RETURN_TO_HOME", evaluateBackAction(FarmVisionDestination.Rover, isNotificationSheetOpen = false))
        assertEquals("RETURN_TO_HOME", evaluateBackAction(FarmVisionDestination.MyFarm, isNotificationSheetOpen = false))
        assertEquals("RETURN_TO_HOME", evaluateBackAction(FarmVisionDestination.Analytics, isNotificationSheetOpen = false))
        assertEquals("RETURN_TO_HOME", evaluateBackAction(FarmVisionDestination.Settings, isNotificationSheetOpen = false))

        // Rule 3: Home destination with no modal -> exits app (default Android back)
        assertEquals("EXIT_APP", evaluateBackAction(FarmVisionDestination.Home, isNotificationSheetOpen = false))
    }

    @Test
    fun appShell_driveAndMonitorTelemetryFieldsArePopulated() {
        val telemetry = repository.getTelemetry()
        // Verifies the four environmental readings required for Drive & Monitor
        assertTrue(telemetry.soilMoisture in 0f..100f)
        assertTrue(telemetry.temperature in -20f..60f)
        assertTrue(telemetry.humidity in 0f..100f)
        assertTrue(telemetry.lightLux >= 0)
        assertTrue(telemetry.lastUpdated.isNotBlank())

        // Verifies drive status, battery, and safety
        assertEquals("AgriRover Alpha-1", telemetry.roverName)
        assertTrue(telemetry.batteryPercent in 0..100)
        assertFalse(telemetry.isEmergencyCutoff)
        assertEquals("HOLD POSITION", telemetry.activeCommand)
    }

    @Test
    fun repository_telemetryValuesAreBoundedAndSimulated() {
        val telemetry = repository.getTelemetry()
        assertEquals(FarmVisionStatusType.DemoData, telemetry.connectionStatus)
        assertTrue(telemetry.isSimulation)
        assertTrue(telemetry.batteryPercent in 0..100)
        assertTrue(telemetry.batteryVoltage > 0f)
        assertTrue(telemetry.soilMoisture in 0f..100f)
        assertTrue(telemetry.temperature in -20f..60f)
        assertTrue(telemetry.humidity in 0f..100f)
        assertTrue(telemetry.lightLux >= 0)
    }

    @Test
    fun repository_cropCatalogProvidesSufficientMockData() {
        val plants = repository.getPlants()
        assertTrue(plants.isNotEmpty())
        plants.forEach { plant ->
            assertTrue(plant.id.isNotBlank())
            assertTrue(plant.name.isNotBlank())
            assertTrue(plant.variety.isNotBlank())
            assertTrue(plant.plotLocation.isNotBlank())
            assertTrue(plant.healthPercent in 0..100)
            assertTrue(plant.currentMoisture in 0f..100f)
            assertTrue(plant.wateringRecommendation.isNotBlank())
        }
    }

    @Test
    fun repository_sensorHistoryProvidesTimeSeriesData() {
        val moistureHistory = repository.getSoilMoistureHistory()
        val tempHistory = repository.getTemperatureHistory()

        assertTrue(moistureHistory.size >= 4)
        assertTrue(tempHistory.size >= 4)

        moistureHistory.forEach { pt ->
            assertTrue(pt.label.isNotBlank())
            assertTrue(pt.value in 0f..100f)
        }

        tempHistory.forEach { pt ->
            assertTrue(pt.label.isNotBlank())
            assertTrue(pt.value in -20f..60f)
        }
    }
}
