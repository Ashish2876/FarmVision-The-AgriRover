package com.farmvision.agrirover.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.farmvision.agrirover.data.local.SettingsPreferencesManager
import com.farmvision.agrirover.data.model.AnalyticsKpis
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.FarmAlert
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorDataPoint
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.repository.FarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel for FarmVision AgriRover providing a single source of truth
 * for rover telemetry, emergency stop status, crop irrigation state, dynamic analytics,
 * and user preferences across all application screens.
 */
class FarmViewModel(
    private val repository: FarmRepository,
    private val preferencesManager: SettingsPreferencesManager? = null
) : ViewModel() {

    // Shared StateFlows backed by the repository
    val telemetry: StateFlow<RoverTelemetry> = repository.telemetryFlow
    val plants: StateFlow<List<PlantItem>> = repository.plantsFlow
    val alerts: StateFlow<List<FarmAlert>> = repository.alertsFlow

    // Reactive persisted user settings
    private val _fallbackSettings = MutableStateFlow(FarmSettings())
    val settings: StateFlow<FarmSettings> = preferencesManager?.settingsFlow ?: _fallbackSettings.asStateFlow()

    // Reactive analytics time range selection
    private val _analyticsRange = MutableStateFlow(AnalyticsTimeRange.Hours24)
    val analyticsRange: StateFlow<AnalyticsTimeRange> = _analyticsRange.asStateFlow()

    // Notification bottom sheet visibility
    private val _isNotificationSheetOpen = MutableStateFlow(false)
    val isNotificationSheetOpen: StateFlow<Boolean> = _isNotificationSheetOpen.asStateFlow()

    // --- Rover Control Actions ---

    fun setEmergencyStop(engaged: Boolean) {
        repository.setEmergencyStop(engaged)
    }

    fun updateRoverMovement(command: String, speedKmh: Float) {
        repository.updateRoverMovement(command, speedKmh)
    }

    fun setDriveMode(mode: String) {
        repository.setDriveMode(mode)
    }

    fun toggleFrontLight() {
        repository.toggleFrontLight()
    }

    fun toggleSideLight() {
        repository.toggleSideLight()
    }

    fun triggerHorn() {
        repository.triggerHorn()
    }

    fun toggleSpray() {
        repository.toggleSpray()
    }

    fun toggleAuxIrrigation() {
        repository.toggleAuxIrrigation()
    }

    fun setCameraTilt(angle: Int) {
        repository.setCameraTilt(angle)
    }

    // --- Crop & Farm Actions ---

    fun togglePlantIrrigation(plantId: String) {
        repository.togglePlantIrrigation(plantId)
    }

    // --- Alert & Telemetry Actions ---

    fun clearAllAlerts() {
        repository.clearAllAlerts()
    }

    fun refreshTelemetry() {
        repository.refreshTelemetry()
    }

    fun setNotificationSheetOpen(open: Boolean) {
        _isNotificationSheetOpen.value = open
    }

    // --- Analytics Range Action ---

    fun setAnalyticsRange(range: AnalyticsTimeRange) {
        _analyticsRange.value = range
    }

    // --- Settings Preferences Actions ---

    fun setTemperatureUnit(unit: TemperatureUnit) {
        preferencesManager?.updateTemperatureUnit(unit) ?: run {
            _fallbackSettings.value = _fallbackSettings.value.copy(temperatureUnit = unit)
        }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        preferencesManager?.updateSpeedUnit(unit) ?: run {
            _fallbackSettings.value = _fallbackSettings.value.copy(speedUnit = unit)
        }
    }

    fun setTransportProtocol(protocol: String) {
        preferencesManager?.updateTransportProtocol(protocol) ?: run {
            _fallbackSettings.value = _fallbackSettings.value.copy(transportProtocol = protocol)
        }
    }

    fun setSimulatedEngineActive(active: Boolean) {
        preferencesManager?.updateSimulatedEngine(active) ?: run {
            _fallbackSettings.value = _fallbackSettings.value.copy(isSimulatedEngineActive = active)
        }
    }

    // --- Dynamic Analytics Queries ---

    fun getSoilMoistureHistory(range: AnalyticsTimeRange = _analyticsRange.value): List<SensorDataPoint> {
        return repository.getSoilMoistureHistory(range)
    }

    fun getTemperatureHistory(
        range: AnalyticsTimeRange = _analyticsRange.value,
        tempUnit: TemperatureUnit = settings.value.temperatureUnit
    ): List<SensorDataPoint> {
        return repository.getTemperatureHistory(range, tempUnit)
    }

    fun getAnalyticsKpis(
        range: AnalyticsTimeRange = _analyticsRange.value,
        tempUnit: TemperatureUnit = settings.value.temperatureUnit
    ): AnalyticsKpis {
        return repository.getAnalyticsKpis(range, tempUnit)
    }
}
