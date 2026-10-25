package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.hardware.ConfigurationStatus
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.HardwareConnectionSettings
import com.farmvision.agrirover.data.repository.InMemoryHardwareSettingsRepository
import com.farmvision.agrirover.ui.hardware.diagnostic.FakeConnectionTester
import com.farmvision.agrirover.ui.hardware.model.ActuatorCommandState
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareFilterCategory
import com.farmvision.agrirover.ui.hardware.model.HardwareOperationMode
import com.farmvision.agrirover.ui.hardware.model.HardwareUiDataMapper
import com.farmvision.agrirover.ui.hardware.model.InventoryPresence
import com.farmvision.agrirover.ui.hardware.model.PhysicalFeedbackState
import com.farmvision.agrirover.ui.hardware.model.SensorHealthStatus
import com.farmvision.agrirover.ui.hardware.model.TelemetryFreshness
import com.farmvision.agrirover.ui.hardware.model.evaluateTelemetryFreshness
import com.farmvision.agrirover.ui.hardware.viewmodel.HardwareCenterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive JVM unit tests for Milestone H2:
 * - 7-axis status model completeness
 * - Telemetry freshness calculation with injected clock
 * - Section 8 counting rules across diverse operational states
 * - Connection settings persistence & secret redaction
 * - Explicit Demo/Live mode switching and safety confirmation gate
 * - ViewModel search, filtering, and link diagnostic probe execution
 */
class HardwareCenterH2UnitTest {

    private fun createTestScope(): CoroutineScope =
        CoroutineScope(Job() + Dispatchers.Unconfined)

    // ==========================================
    // 1. Status Model 7-Axis Completeness
    // ==========================================

    @Test
    fun statusAxes_allSevenAxesExistAndHaveRequiredValues() {
        // Axis 1: Endpoint Connection Status
        val connValues = EndpointConnectionStatus.values().map { it.name }
        assertTrue(connValues.containsAll(listOf("Connecting", "Connected", "Disconnected", "Stale", "Unknown", "NotConfigured")))

        // Axis 2: Telemetry Freshness
        val freshValues = TelemetryFreshness.values().map { it.name }
        assertTrue(freshValues.containsAll(listOf("Fresh", "Stale", "Unavailable", "Unknown")))

        // Axis 3: Sensor Health Status
        val healthValues = SensorHealthStatus.values().map { it.name }
        assertTrue(healthValues.containsAll(listOf("Healthy", "Warning", "Fault", "Unavailable", "Uncalibrated", "Unknown")))

        // Axis 4: Actuator Command State
        val actValues = ActuatorCommandState.values().map { it.name }
        assertTrue(actValues.containsAll(listOf("Idle", "Commanded", "Acknowledged", "Rejected", "TimedOut", "Expired", "HaltedByEStop", "Unknown")))

        // Axis 5: Physical Feedback State
        val feedbackValues = PhysicalFeedbackState.values().map { it.name }
        assertTrue(feedbackValues.containsAll(listOf("Available", "NotAvailable")))

        // Axis 6: Configuration Maturity Lifecycle
        val configValues = ConfigurationStatus.values().map { it.name }
        assertTrue(configValues.containsAll(listOf("NotConfigured", "Draft", "Verified")))

        // Axis 7: Inventory Presence
        val invValues = InventoryPresence.values().map { it.name }
        assertTrue(invValues.contains("Cataloged"))
    }

    // ==========================================
    // 2. Telemetry Freshness Evaluation
    // ==========================================

    @Test
    fun evaluateTelemetryFreshness_correctlyClassifiesByInjectedClock() {
        val clockMs = 100_000L

        // Fresh reading: 1.5 seconds old
        assertEquals(
            TelemetryFreshness.Fresh,
            evaluateTelemetryFreshness(clockMs - 1500L, clockMs, freshThresholdMs = 3000L, staleThresholdMs = 10000L)
        )

        // Stale reading: 6 seconds old
        assertEquals(
            TelemetryFreshness.Stale,
            evaluateTelemetryFreshness(clockMs - 6000L, clockMs, freshThresholdMs = 3000L, staleThresholdMs = 10000L)
        )

        // Unavailable reading: 15 seconds old
        assertEquals(
            TelemetryFreshness.Unavailable,
            evaluateTelemetryFreshness(clockMs - 15000L, clockMs, freshThresholdMs = 3000L, staleThresholdMs = 10000L)
        )

        // Missing / unpolled timestamp
        assertEquals(
            TelemetryFreshness.Unknown,
            evaluateTelemetryFreshness(null, clockMs)
        )

        // Negative / clock skew timestamp
        assertEquals(
            TelemetryFreshness.Unknown,
            evaluateTelemetryFreshness(clockMs + 5000L, clockMs)
        )
    }

    // ==========================================
    // 3. Section 8 Counting Rules
    // ==========================================

    @Test
    fun countingRules_strictlyExcludesPassiveItemsFromConnectivityMetrics() {
        val items = HardwareUiDataMapper.createUiItems(isDemoMode = false)
        val counts = HardwareUiDataMapper.computeSummaryCounts(items)

        // Exactly 13 electronic components, 10 passive
        assertEquals(0, counts.confirmedConnected)
        assertEquals(0, counts.disconnected)
        assertEquals(0, counts.warningOrFault)
        assertEquals(13, counts.unknownOrDraft)
        assertEquals(10, counts.passiveInventory)

        // The sum of electronic counts must equal 13
        val totalElectronicEvaluated = counts.confirmedConnected + counts.disconnected + counts.unknownOrDraft
        assertEquals(13, totalElectronicEvaluated)
    }

    @Test
    fun countingRules_dynamicSimulation_reflectsTrueCounts() {
        val baseItems = HardwareUiDataMapper.createUiItems(isDemoMode = false)

        // Simulate 2 sensors reporting connected & healthy, 1 sensor reporting fault, and 1 sensor reporting disconnected
        val simulatedItems = baseItems.map { uiItem ->
            when (uiItem.component.id) {
                "hw-sensor-soil-moisture-probes" -> uiItem.copy(
                    connectionStatus = EndpointConnectionStatus.Connected,
                    healthStatus = SensorHealthStatus.Healthy
                )
                "hw-sensor-dht22" -> uiItem.copy(
                    connectionStatus = EndpointConnectionStatus.Connected,
                    healthStatus = SensorHealthStatus.Healthy
                )
                "hw-sensor-soil-ph" -> uiItem.copy(
                    connectionStatus = EndpointConnectionStatus.Connected,
                    healthStatus = SensorHealthStatus.Fault
                )
                "hw-sensor-ultrasonic" -> uiItem.copy(
                    connectionStatus = EndpointConnectionStatus.Disconnected,
                    healthStatus = SensorHealthStatus.Unavailable
                )
                else -> uiItem
            }
        }

        val counts = HardwareUiDataMapper.computeSummaryCounts(simulatedItems)

        // 3 items connected (soil moisture, dht22, soil ph)
        assertEquals(3, counts.confirmedConnected)
        // 1 item disconnected (ultrasonic)
        assertEquals(1, counts.disconnected)
        // 1 item in fault/warning (soil ph)
        assertEquals(1, counts.warningOrFault)
        // Remaining electronic items (13 - 3 connected - 1 disconnected = 9 unknown)
        assertEquals(9, counts.unknownOrDraft)
        // Passive inventory untouched
        assertEquals(10, counts.passiveInventory)
    }

    // ==========================================
    // 4. Connection Settings & Secret Redaction
    // ==========================================

    @Test
    fun hardwareConnectionSettings_secretRedactedInToString() {
        val rawSecret = "super-secret-rover-token-98765"
        val settings = HardwareConnectionSettings(
            operationMode = HardwareOperationMode.Live,
            authToken = rawSecret
        )

        val stringRep = settings.toString()
        assertFalse("toString must NEVER reveal the cleartext auth token", stringRep.contains(rawSecret))
        assertTrue("toString must display masked <redacted> token label", stringRep.contains("authToken=<redacted>"))
        assertTrue("isAuthTokenSet must be true for non-empty token", settings.isAuthTokenSet)
    }

    @Test
    fun hardwareSettingsRepository_persistsAndRestoresDefaults() {
        val repo = InMemoryHardwareSettingsRepository()
        assertEquals(HardwareOperationMode.Demo, repo.getSettings().operationMode)

        val custom = HardwareConnectionSettings(
            operationMode = HardwareOperationMode.Live,
            mainEsp32Host = "192.168.1.100",
            mainEsp32Port = 8080,
            esp32CamHost = "192.168.1.101",
            esp32CamPort = 8081,
            authToken = "rover-bearer-abc"
        )
        repo.updateSettings(custom)
        assertEquals(HardwareOperationMode.Live, repo.getSettings().operationMode)
        assertEquals("192.168.1.100", repo.getSettings().mainEsp32Host)
        assertEquals(8080, repo.getSettings().mainEsp32Port)

        repo.resetToDefaults()
        assertEquals(HardwareOperationMode.Demo, repo.getSettings().operationMode)
        assertEquals("192.168.4.1", repo.getSettings().mainEsp32Host)
        assertEquals(80, repo.getSettings().mainEsp32Port)
    }

    // ==========================================
    // 5. ViewModel & Safety Confirmation Gate
    // ==========================================

    @Test
    fun viewModel_modeSwitchToLiveRequiresConfirmationGate() {
        val scope = createTestScope()
        val repo = InMemoryHardwareSettingsRepository()
        val viewModel = HardwareCenterViewModel(
            settingsRepository = repo,
            externalScope = scope
        )

        // Initially Demo mode
        assertTrue(viewModel.uiState.value.isDemoMode)
        assertFalse(viewModel.uiState.value.isModeSwitchConfirmationOpen)

        // Request mode switch to Live
        viewModel.requestModeSwitch(HardwareOperationMode.Live)

        // Safety rule: Mode MUST NOT change immediately. Confirmation dialog must open!
        assertTrue("Confirmation dialog must be open", viewModel.uiState.value.isModeSwitchConfirmationOpen)
        assertEquals(HardwareOperationMode.Live, viewModel.uiState.value.pendingOperationMode)
        assertEquals("Repository must still be in Demo mode prior to confirmation", HardwareOperationMode.Demo, repo.getSettings().operationMode)

        // If user cancels:
        viewModel.cancelModeSwitch()
        assertFalse(viewModel.uiState.value.isModeSwitchConfirmationOpen)
        assertNull(viewModel.uiState.value.pendingOperationMode)
        assertTrue(viewModel.uiState.value.isDemoMode)

        // Request again and confirm:
        viewModel.requestModeSwitch(HardwareOperationMode.Live)
        viewModel.confirmModeSwitch()

        assertFalse(viewModel.uiState.value.isModeSwitchConfirmationOpen)
        assertFalse("Should now be in Live mode", viewModel.uiState.value.isDemoMode)
        assertEquals(HardwareOperationMode.Live, repo.getSettings().operationMode)

        // Switching back to Demo does not require a hazard confirmation
        viewModel.requestModeSwitch(HardwareOperationMode.Demo)
        assertTrue(viewModel.uiState.value.isDemoMode)
        assertEquals(HardwareOperationMode.Demo, repo.getSettings().operationMode)
    }

    @Test
    fun viewModel_searchAndCategoryFiltering() {
        val scope = createTestScope()
        val repo = InMemoryHardwareSettingsRepository()
        val viewModel = HardwareCenterViewModel(
            settingsRepository = repo,
            externalScope = scope
        )

        assertEquals(23, viewModel.uiState.value.filteredItems.size)

        // Filter by Sensors (should be 4 items: moisture probes, dht22, soil ph, ultrasonic)
        viewModel.onCategorySelected(HardwareFilterCategory.Sensors)
        assertEquals(4, viewModel.uiState.value.filteredItems.size)

        // Search query "DHT22"
        viewModel.onCategorySelected(HardwareFilterCategory.All)
        viewModel.onSearchQueryChanged("DHT22")
        assertEquals(1, viewModel.uiState.value.filteredItems.size)
        assertEquals("hw-sensor-dht22", viewModel.uiState.value.filteredItems.first().component.id)

        // Search non-existent query
        viewModel.onSearchQueryChanged("NonExistentComponent12345")
        assertTrue(viewModel.uiState.value.filteredItems.isEmpty())
    }

    @Test
    fun viewModel_testConnection_executesProbeAndUpdatesDiagnosticResult() = runBlocking {
        val scope = createTestScope()
        val repo = InMemoryHardwareSettingsRepository()
        val fakeTester = FakeConnectionTester(simulatedSuccess = true, simulatedLatencyMs = 22L)
        val viewModel = HardwareCenterViewModel(
            settingsRepository = repo,
            connectionTester = fakeTester,
            externalScope = scope
        )

        // In Demo mode: probe informs user that physical link is skipped
        viewModel.testConnection(ControllerTarget.Esp32DevKitV1)

        val demoResult = viewModel.uiState.value.diagnosticResult
        assertNotNull(demoResult)
        assertFalse("Demo probe does not claim real success", demoResult!!.success)
        assertTrue(demoResult.message.contains("DEMO MODE ACTIVE"))

        // In Live mode:
        repo.setOperationMode(HardwareOperationMode.Live)

        viewModel.testConnection(ControllerTarget.Esp32Cam)

        val liveResult = viewModel.uiState.value.diagnosticResult
        assertNotNull(liveResult)
        assertTrue(liveResult!!.success)
        assertEquals(ControllerTarget.Esp32Cam, liveResult.target)
        assertEquals(22L, liveResult.latencyMs)
    }
}
