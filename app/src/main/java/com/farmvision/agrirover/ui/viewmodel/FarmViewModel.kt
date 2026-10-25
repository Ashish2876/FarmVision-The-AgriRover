package com.farmvision.agrirover.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.data.local.SettingsPreferencesManager
import com.farmvision.agrirover.data.model.AnalyticsKpis
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.FarmAlert
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorDataPoint
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.repository.FarmRepository
import com.farmvision.agrirover.ui.rover.controller.JoystickVector
import com.farmvision.agrirover.ui.rover.controller.RoverCommandPipeline
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
    val sensorReadings: StateFlow<Map<SensorType, SensorReading>> = repository.sensorReadingsFlow
    val hardwareCatalog = repository.hardwareCatalog
    val farmManagement = repository.farmManagement

    // Reactive persisted user settings
    private val _fallbackSettings = MutableStateFlow(FarmSettings())
    val settings: StateFlow<FarmSettings> = preferencesManager?.settingsFlow ?: _fallbackSettings.asStateFlow()

    // Reactive analytics time range selection
    private val _analyticsRange = MutableStateFlow(AnalyticsTimeRange.Hours24)
    val analyticsRange: StateFlow<AnalyticsTimeRange> = _analyticsRange.asStateFlow()

    // Notification bottom sheet visibility
    private val _isNotificationSheetOpen = MutableStateFlow(false)
    val isNotificationSheetOpen: StateFlow<Boolean> = _isNotificationSheetOpen.asStateFlow()

    // Phase 6 Rover Command Pipeline with 20 Hz rate limiter and trailing emission
    val commandPipeline = RoverCommandPipeline(
        commandSink = repository,
        scope = viewModelScope
    )

    // --- Rover Control Actions ---

    fun setEmergencyStop(engaged: Boolean) {
        commandPipeline.submitEmergencyStop(engaged)
    }

    fun updateRoverMovement(command: String, speedKmh: Float) {
        commandPipeline.cancelPending()
        repository.updateRoverMovement(command, speedKmh)
    }

    fun onJoystickMoved(vector: JoystickVector) {
        commandPipeline.submitVector(vector, telemetry.value.speedPreset)
    }

    fun onJoystickMoved(x: Float, y: Float) {
        onJoystickMoved(JoystickVector(x, y))
    }

    fun onJoystickReleased() {
        commandPipeline.submitVector(JoystickVector.NEUTRAL, telemetry.value.speedPreset)
    }

    fun setSpeedPreset(preset: SpeedPreset) {
        commandPipeline.setSpeedPreset(preset)
    }

    fun setDriveMode(mode: RoverDriveMode) {
        commandPipeline.cancelPending()
        repository.setDriveMode(mode)
    }

    fun setDriveMode(mode: String) {
        commandPipeline.cancelPending()
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

    // --- Phase 5 Home Dashboard Quick Actions ---

    fun startFieldScan() {
        if (!telemetry.value.isEmergencyCutoff) {
            repository.updateRoverMovement("FIELD SCAN", 2.0f)
        }
    }

    fun returnToDock() {
        if (!telemetry.value.isEmergencyCutoff) {
            repository.updateRoverMovement("RETURN TO DOCK", 2.2f)
        }
    }

    fun disarmMotors() {
        repository.setEmergencyStop(true)
    }

    fun getSpeedStatusLabel(speedKmh: Float = telemetry.value.speedKmh, isEmergencyCutoff: Boolean = telemetry.value.isEmergencyCutoff): String {
        return when {
            isEmergencyCutoff -> "Cutoff"
            speedKmh > 0f -> "Cruising"
            else -> "Stationary"
        }
    }

    fun getGpsStatusLabel(gpsFixStatus: String = telemetry.value.gpsFixStatus): String {
        return if (gpsFixStatus.contains("Fix", ignoreCase = true) && !gpsFixStatus.contains("No Fix", ignoreCase = true)) "Locked" else "No Fix"
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

    override fun onCleared() {
        super.onCleared()
        commandPipeline.cancelPending()
        commandPipeline.submitVector(JoystickVector.NEUTRAL)
    }
}
