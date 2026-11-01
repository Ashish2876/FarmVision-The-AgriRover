package com.farmvision.agrirover.alert

import com.farmvision.agrirover.data.alert.repository.InMemoryAlertRepository
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.AlertStatus
import com.farmvision.agrirover.domain.alert.repository.AlertSettingsRepository
import com.farmvision.agrirover.ui.alert.AlertViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlertViewModelTest {

    private lateinit var alertRepo: InMemoryAlertRepository
    private lateinit var settingsRepo: AlertSettingsRepository
    private lateinit var viewModel: AlertViewModel

    private val sampleAlertLive = AlertItem(
        id = "live-1",
        dedupKey = "env:temp:dht22:LiveHardware",
        category = AlertCategory.Environment,
        ruleId = "env-temp-high-warning",
        severity = AlertSeverity.Warning,
        title = "Live High Temp",
        message = "38C",
        sourceSensor = "dht22",
        origin = DataOrigin.LiveHardware,
        createdTimestampEpochMillis = 100_000L
    )

    private val sampleAlertSim = AlertItem(
        id = "sim-1",
        dedupKey = "env:temp:dht22:Simulated",
        category = AlertCategory.Environment,
        ruleId = "env-temp-high-critical",
        severity = AlertSeverity.Critical,
        title = "Sim High Temp",
        message = "44C",
        sourceSensor = "dht22",
        origin = DataOrigin.Simulated,
        createdTimestampEpochMillis = 100_000L
    )

    @Before
    fun setUp() {
        alertRepo = InMemoryAlertRepository(
            initialActive = listOf(sampleAlertLive, sampleAlertSim),
            initialResolved = emptyList()
        )
        settingsRepo = object : AlertSettingsRepository {
            private val _flow = MutableStateFlow(AlertThresholdConfig())
            override val settingsFlow: StateFlow<AlertThresholdConfig> = _flow.asStateFlow()
            override suspend fun updateSettings(config: AlertThresholdConfig): Result<Unit> {
                _flow.value = config
                return Result.success(Unit)
            }
            override suspend fun restoreDefaults(): Result<Unit> {
                _flow.value = AlertThresholdConfig()
                return Result.success(Unit)
            }
        }
        viewModel = AlertViewModel(
            alertRepository = alertRepo,
            settingsRepository = settingsRepo,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testModeIsolationFiltersAlertsByDataOrigin() = runBlocking {
        // Default mode is Simulated
        viewModel.setDataOrigin(DataOrigin.Simulated)

        val activeSim = viewModel.filteredActiveAlerts.value
        assertEquals(1, activeSim.size)
        assertEquals("sim-1", activeSim.first().id)

        // Switch mode to LiveHardware
        viewModel.setDataOrigin(DataOrigin.LiveHardware)

        val activeLive = viewModel.filteredActiveAlerts.value
        assertEquals(1, activeLive.size)
        assertEquals("live-1", activeLive.first().id)
    }

    @Test
    fun testSearchQueryFiltersAlerts() = runBlocking {
        viewModel.setDataOrigin(DataOrigin.Simulated)
        viewModel.searchQuery.value = "NonExistent"

        val filtered = viewModel.filteredActiveAlerts.value
        assertTrue(filtered.isEmpty())

        viewModel.searchQuery.value = "Sim High"

        val matched = viewModel.filteredActiveAlerts.value
        assertEquals(1, matched.size)
        assertEquals("sim-1", matched.first().id)
    }

    @Test
    fun testAcknowledgeAlertUpdatesStatus() = runBlocking {
        viewModel.setDataOrigin(DataOrigin.Simulated)
        viewModel.acknowledgeAlert("sim-1")

        val alert = alertRepo.getAlertById("sim-1")
        assertEquals(AlertStatus.Acknowledged, alert?.status)
        assertTrue(alert?.isRead == true)
    }
}
