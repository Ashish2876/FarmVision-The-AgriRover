package com.farmvision.agrirover.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.repository.AlertRepository
import com.farmvision.agrirover.domain.alert.repository.AlertSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * KPI summary state representing counts of active and historical alerts.
 */
data class AlertSummaryUiState(
    val totalActive: Int = 0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0,
    val resolvedCount: Int = 0
)

/**
 * ViewModel managing UI state, filters, search, and operator actions for the Alert Centre.
 */
class AlertViewModel(
    private val alertRepository: AlertRepository,
    private val settingsRepository: AlertSettingsRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    customScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = customScope ?: viewModelScope

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedSeverityFilter = MutableStateFlow<AlertSeverity?>(null)
    val selectedCategoryFilter = MutableStateFlow<AlertCategory?>(null)
    val selectedTab = MutableStateFlow(0) // 0 = Active, 1 = Resolved

    // Target data mode: defaults to Simulated in demo mode, LiveHardware in live mode
    val activeDataOrigin = MutableStateFlow(DataOrigin.Simulated)

    // Raw streams from repository
    val rawActiveAlerts: StateFlow<List<AlertItem>> = alertRepository.activeAlertsFlow
    val rawResolvedAlerts: StateFlow<List<AlertItem>> = alertRepository.resolvedAlertsFlow
    val settingsConfig: StateFlow<AlertThresholdConfig> = settingsRepository.settingsFlow

    // Currently selected alert for Details destination
    private val _selectedAlert = MutableStateFlow<AlertItem?>(null)
    val selectedAlert: StateFlow<AlertItem?> = _selectedAlert.asStateFlow()

    // Filtered Active Alerts
    val filteredActiveAlerts: StateFlow<List<AlertItem>> = combine(
        rawActiveAlerts,
        searchQuery,
        selectedSeverityFilter,
        selectedCategoryFilter,
        activeDataOrigin
    ) { list, query, severity, category, origin ->
        filterAlertList(list, query, severity, category, origin)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Filtered Resolved Alerts
    val filteredResolvedAlerts: StateFlow<List<AlertItem>> = combine(
        rawResolvedAlerts,
        searchQuery,
        selectedSeverityFilter,
        selectedCategoryFilter,
        activeDataOrigin
    ) { list, query, severity, category, origin ->
        filterAlertList(list, query, severity, category, origin)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Summary counters
    val summaryState: StateFlow<AlertSummaryUiState> = combine(
        rawActiveAlerts,
        rawResolvedAlerts,
        activeDataOrigin
    ) { active, resolved, origin ->
        val modeActive = active.filter { it.origin == origin }
        val modeResolved = resolved.filter { it.origin == origin }

        AlertSummaryUiState(
            totalActive = modeActive.size,
            criticalCount = modeActive.count { it.severity == AlertSeverity.Critical },
            warningCount = modeActive.count { it.severity == AlertSeverity.Warning },
            resolvedCount = modeResolved.size
        )
    }.stateIn(scope, SharingStarted.Eagerly, AlertSummaryUiState())

    private fun filterAlertList(
        list: List<AlertItem>,
        query: String,
        severity: AlertSeverity?,
        category: AlertCategory?,
        origin: DataOrigin
    ): List<AlertItem> {
        return list.filter { alert ->
            val matchesOrigin = alert.origin == origin
            val matchesSeverity = severity == null || alert.severity == severity
            val matchesCategory = category == null || alert.category == category
            val matchesQuery = query.isBlank() ||
                alert.title.contains(query, ignoreCase = true) ||
                alert.message.contains(query, ignoreCase = true) ||
                alert.sourceSensor.contains(query, ignoreCase = true)

            matchesOrigin && matchesSeverity && matchesCategory && matchesQuery
        }
    }

    fun acknowledgeAlert(id: String) {
        scope.launch(defaultDispatcher) {
            alertRepository.acknowledgeAlert(id)
            if (_selectedAlert.value?.id == id) {
                _selectedAlert.value = alertRepository.getAlertById(id)
            }
        }
    }

    fun clearResolvedHistory() {
        scope.launch(defaultDispatcher) {
            alertRepository.clearHistory()
        }
    }

    fun selectAlert(alert: AlertItem?) {
        _selectedAlert.value = alert
    }

    fun selectAlertById(id: String) {
        scope.launch(defaultDispatcher) {
            _selectedAlert.value = alertRepository.getAlertById(id)
        }
    }

    fun updateThresholds(config: AlertThresholdConfig, onResult: (Result<Unit>) -> Unit) {
        scope.launch(defaultDispatcher) {
            val res = settingsRepository.updateSettings(config)
            onResult(res)
        }
    }

    fun restoreDefaultThresholds(onResult: (Result<Unit>) -> Unit) {
        scope.launch(defaultDispatcher) {
            val res = settingsRepository.restoreDefaults()
            onResult(res)
        }
    }

    fun setDataOrigin(origin: DataOrigin) {
        activeDataOrigin.value = origin
    }
}
