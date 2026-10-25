package com.farmvision.agrirover.ui.hardware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.HardwareConnectionSettings
import com.farmvision.agrirover.data.network.RoverProtocolClient
import com.farmvision.agrirover.data.repository.HardwareSettingsRepository
import com.farmvision.agrirover.ui.hardware.diagnostic.ConnectionTester
import com.farmvision.agrirover.ui.hardware.diagnostic.DefaultConnectionTester
import com.farmvision.agrirover.ui.hardware.model.ConnectionDiagnosticResult
import com.farmvision.agrirover.ui.hardware.model.HardwareFilterCategory
import com.farmvision.agrirover.ui.hardware.model.HardwareOperationMode
import com.farmvision.agrirover.ui.hardware.model.HardwareSummaryCounts
import com.farmvision.agrirover.ui.hardware.model.HardwareUiDataMapper
import com.farmvision.agrirover.ui.hardware.model.HardwareUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Hardware Center screen and connection settings.
 */
data class HardwareCenterUiState(
    val settings: HardwareConnectionSettings = HardwareConnectionSettings(),
    val isDemoMode: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: HardwareFilterCategory = HardwareFilterCategory.All,
    val selectedComponentId: String? = null,
    val isConnectionDialogOpen: Boolean = false,
    val isModeSwitchConfirmationOpen: Boolean = false,
    val pendingOperationMode: HardwareOperationMode? = null,
    val diagnosticResult: ConnectionDiagnosticResult? = null,
    val isTestingConnection: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<HardwareUiItem> = emptyList(),
    val filteredItems: List<HardwareUiItem> = emptyList(),
    val summaryCounts: HardwareSummaryCounts = HardwareSummaryCounts(0, 0, 0, 13, 10)
)

/**
 * ViewModel managing Hardware Center inventory inspection, connection settings persistence,
 * truthful Section 8 counting rules, and explicit mode switching.
 */
class HardwareCenterViewModel(
    private val settingsRepository: HardwareSettingsRepository,
    private val connectionTester: ConnectionTester = DefaultConnectionTester(),
    private val protocolClient: RoverProtocolClient = RoverProtocolClient(),
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(HardwareCenterUiState())
    val uiState: StateFlow<HardwareCenterUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val isDemo = settings.operationMode == HardwareOperationMode.Demo
                val uiItems = HardwareUiDataMapper.createUiItems(isDemo)
                val counts = HardwareUiDataMapper.computeSummaryCounts(uiItems)

                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        isDemoMode = isDemo,
                        items = uiItems,
                        summaryCounts = counts,
                        filteredItems = filterItems(uiItems, current.selectedCategory, current.searchQuery)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredItems = filterItems(current.items, current.selectedCategory, query)
            )
        }
    }

    fun onCategorySelected(category: HardwareFilterCategory) {
        _uiState.update { current ->
            current.copy(
                selectedCategory = category,
                filteredItems = filterItems(current.items, category, current.searchQuery)
            )
        }
    }

    fun onComponentSelected(componentId: String?) {
        _uiState.update { it.copy(selectedComponentId = componentId) }
    }

    fun setConnectionDialogOpen(isOpen: Boolean) {
        _uiState.update {
            it.copy(
                isConnectionDialogOpen = isOpen,
                diagnosticResult = if (!isOpen) null else it.diagnosticResult
            )
        }
    }

    /**
     * Request switching operation mode.
     * Enforces safety rule: Switching to LIVE mode requires explicit confirmation dialog.
     * Demo mode never silently becomes Live mode.
     */
    fun requestModeSwitch(targetMode: HardwareOperationMode) {
        val currentMode = _uiState.value.settings.operationMode
        if (targetMode == currentMode) return

        if (targetMode == HardwareOperationMode.Live) {
            _uiState.update {
                it.copy(
                    isModeSwitchConfirmationOpen = true,
                    pendingOperationMode = targetMode
                )
            }
        } else {
            // Switching back to demo is immediately safe
            settingsRepository.setOperationMode(HardwareOperationMode.Demo)
        }
    }

    fun confirmModeSwitch() {
        val pending = _uiState.value.pendingOperationMode ?: return
        settingsRepository.setOperationMode(pending)
        _uiState.update {
            it.copy(
                isModeSwitchConfirmationOpen = false,
                pendingOperationMode = null
            )
        }
    }

    fun cancelModeSwitch() {
        _uiState.update {
            it.copy(
                isModeSwitchConfirmationOpen = false,
                pendingOperationMode = null
            )
        }
    }

    fun updateSettings(settings: HardwareConnectionSettings) {
        settingsRepository.updateSettings(settings)
    }

    fun resetSettingsToDefaults() {
        settingsRepository.resetToDefaults()
    }

    fun testConnection(target: ControllerTarget) {
        val state = _uiState.value
        val host = if (target == ControllerTarget.Esp32Cam) state.settings.esp32CamHost else state.settings.mainEsp32Host
        val port = if (target == ControllerTarget.Esp32Cam) state.settings.esp32CamPort else state.settings.mainEsp32Port

        _uiState.update { it.copy(isTestingConnection = true) }
        scope.launch {
            val result = connectionTester.testConnection(
                target = target,
                host = host,
                port = port,
                timeoutMs = state.settings.connectionTimeoutMs,
                isDemoMode = state.isDemoMode,
                authToken = state.settings.authToken
            )
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    diagnosticResult = result
                )
            }
        }
    }

    fun clearDiagnosticResult() {
        _uiState.update { it.copy(diagnosticResult = null) }
    }

    fun refreshTelemetry() {
        val state = _uiState.value
        _uiState.update { it.copy(isRefreshing = true) }

        if (state.isDemoMode) {
            val freshItems = HardwareUiDataMapper.createUiItems(true)
            val counts = HardwareUiDataMapper.computeSummaryCounts(freshItems)
            _uiState.update { current ->
                current.copy(
                    isRefreshing = false,
                    items = freshItems,
                    summaryCounts = counts,
                    filteredItems = filterItems(freshItems, current.selectedCategory, current.searchQuery)
                )
            }
        } else {
            scope.launch {
                val baseUrl = "http://${state.settings.mainEsp32Host}:${state.settings.mainEsp32Port}"
                val telemetryResult = protocolClient.fetchTelemetry(
                    baseUrl = baseUrl,
                    authToken = state.settings.authToken,
                    timeoutMs = state.settings.connectionTimeoutMs
                )
                val baseItems = if (state.items.isEmpty()) HardwareUiDataMapper.createUiItems(false) else state.items
                val updatedItems = telemetryResult.fold(
                    onSuccess = { packet ->
                        HardwareUiDataMapper.updateWithLiveTelemetry(baseItems, packet, System.currentTimeMillis())
                    },
                    onFailure = { error ->
                        HardwareUiDataMapper.markLiveTelemetryDisconnected(baseItems, error.message ?: "Connection failed")
                    }
                )
                val counts = HardwareUiDataMapper.computeSummaryCounts(updatedItems)
                _uiState.update { current ->
                    current.copy(
                        isRefreshing = false,
                        items = updatedItems,
                        summaryCounts = counts,
                        filteredItems = filterItems(updatedItems, current.selectedCategory, current.searchQuery)
                    )
                }
            }
        }
    }

    private fun filterItems(
        items: List<HardwareUiItem>,
        category: HardwareFilterCategory,
        query: String
    ): List<HardwareUiItem> {
        return items.filter { item ->
            val matchesCategory = when (category) {
                HardwareFilterCategory.All -> true
                HardwareFilterCategory.Controllers -> item.component.category == ComponentCategory.Controller || item.component.category == ComponentCategory.Camera
                HardwareFilterCategory.Sensors -> item.component.category == ComponentCategory.Sensor
                HardwareFilterCategory.Motion -> item.component.category == ComponentCategory.Motor || item.component.category == ComponentCategory.MotorDriver || item.component.category == ComponentCategory.Servo
                HardwareFilterCategory.Irrigation -> item.component.category == ComponentCategory.Irrigation
                HardwareFilterCategory.Power -> item.component.category == ComponentCategory.Power
                HardwareFilterCategory.PassiveInventory -> !item.isElectronic
            }
            val matchesQuery = query.isBlank() ||
                item.component.displayName.contains(query, ignoreCase = true) ||
                item.component.modelDesignation.contains(query, ignoreCase = true) ||
                item.component.intendedRole.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }
}
