package com.farmvision.agrirover.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
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
import com.farmvision.agrirover.data.analytics.DeterministicDemoHistoryGenerator
import com.farmvision.agrirover.data.model.AbnormalReadingsSummary
import com.farmvision.agrirover.data.model.AnalyticsOverviewMetrics
import com.farmvision.agrirover.data.model.AnalyticsTimeWindow
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.HistoricalSeries
import com.farmvision.agrirover.data.model.HistoricalTrendDirection
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.data.model.TelemetryStatistics
import com.farmvision.agrirover.data.model.TrendSummary
import com.farmvision.agrirover.domain.analytics.AnalyticsMathEngine
import com.farmvision.agrirover.ui.analytics.model.AnalyticsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import com.farmvision.agrirover.domain.alert.engine.AlertEngine
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.domain.alert.lifecycle.AlertLifecycleManager
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.repository.AlertRepository
import com.farmvision.agrirover.domain.alert.repository.AlertSettingsRepository
import com.farmvision.agrirover.ui.alert.notification.AlertNotificationManager

/**
 * Shared ViewModel for FarmVision AgriRover providing a single source of truth
 * for rover telemetry, emergency stop status, crop irrigation state, dynamic analytics,
 * and user preferences across all application screens.
 */
class FarmViewModel(
    private val repository: FarmRepository,
    private val preferencesManager: SettingsPreferencesManager? = null,
    val alertRepository: AlertRepository? = null,
    val alertSettingsRepository: AlertSettingsRepository? = null,
    val alertNotificationManager: AlertNotificationManager? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    val alertEngine = AlertEngine(
        config = alertSettingsRepository?.settingsFlow?.value ?: AlertThresholdConfig()
    )
    val alertLifecycleManager = AlertLifecycleManager()

    private val _dynamicAlerts = MutableStateFlow<List<FarmAlert>>(emptyList())

    // Shared StateFlows backed by the repository
    val telemetry: StateFlow<RoverTelemetry> = repository.telemetryFlow
    val plants: StateFlow<List<PlantItem>> = repository.plantsFlow
    val alerts: StateFlow<List<FarmAlert>> = if (alertRepository != null) {
        _dynamicAlerts.asStateFlow()
    } else {
        repository.alertsFlow
    }
    val sensorReadings: StateFlow<Map<SensorType, SensorReading>> = repository.sensorReadingsFlow
    val hardwareCatalog = repository.hardwareCatalog
    val farmManagement = repository.farmManagement
    val telemetryHistory = repository.telemetryHistory
    val deviceRegistry = repository.deviceRegistry

    // Reactive persisted user settings
    private val _fallbackSettings = MutableStateFlow(FarmSettings())
    val settings: StateFlow<FarmSettings> = preferencesManager?.settingsFlow ?: _fallbackSettings.asStateFlow()

    // Reactive analytics time range and metric selection
    private val _analyticsRange = MutableStateFlow(AnalyticsTimeRange.Hours24)
    val analyticsRange: StateFlow<AnalyticsTimeRange> = _analyticsRange.asStateFlow()

    private val _selectedMetric = MutableStateFlow(HistoricalMetric.SoilMoistureProbe1Raw)
    val selectedMetric: StateFlow<HistoricalMetric> = _selectedMetric.asStateFlow()

    private val _isDemoModeActive = MutableStateFlow(true)
    val isDemoModeActive: StateFlow<Boolean> = _isDemoModeActive.asStateFlow()

    private val _isTableViewSelected = MutableStateFlow(false)
    val isTableViewSelected: StateFlow<Boolean> = _isTableViewSelected.asStateFlow()

    private val _recordsLimit = MutableStateFlow(100)
    val recordsLimit: StateFlow<Int> = _recordsLimit.asStateFlow()
    private val _customStartMillis = MutableStateFlow<Long?>(null)
    val customStartMillis: StateFlow<Long?> = _customStartMillis.asStateFlow()
    private val _customEndMillis = MutableStateFlow<Long?>(null)
    val customEndMillis: StateFlow<Long?> = _customEndMillis.asStateFlow()
    private val _isCustomDatePickerOpen = MutableStateFlow(false)
    val isCustomDatePickerOpen: StateFlow<Boolean> = _isCustomDatePickerOpen.asStateFlow()

    private val _analyticsUiState = MutableStateFlow(buildAnalyticsState())
    val analyticsUiState: StateFlow<AnalyticsUiState> = _analyticsUiState.asStateFlow()

    private var analyticsJob: Job? = null

    init {
        viewModelScope.launch(defaultDispatcher) {
            telemetry.collect { tel ->
                val connStatus = tel.connectionStatus.defaultLabel
                val isConnected = tel.connectionStatus == FarmVisionStatusType.Online || tel.connectionStatus == FarmVisionStatusType.Active
                if (_analyticsUiState.value.connectionStatus != connStatus || _analyticsUiState.value.isRoverConnected != isConnected) {
                    _analyticsUiState.value = _analyticsUiState.value.copy(
                        connectionStatus = connStatus,
                        isRoverConnected = isConnected
                    )
                }
            }
        }
        recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }

        if (alertRepository != null) {
            viewModelScope.launch(defaultDispatcher) {
                alertSettingsRepository?.settingsFlow?.collect { newConfig ->
                    alertEngine.updateConfig(newConfig)
                }
            }

            viewModelScope.launch(defaultDispatcher) {
                alertLifecycleManager.restoreState(
                    alertRepository.activeAlertsFlow.value,
                    alertRepository.resolvedAlertsFlow.value
                )

                alertLifecycleManager.activeAlertsFlow.collect { items ->
                    _dynamicAlerts.value = items.map { item ->
                        FarmAlert(
                            id = item.id,
                            title = item.title,
                            message = item.message,
                            timestamp = "Active",
                            severity = when (item.severity) {
                                AlertSeverity.Critical -> FarmVisionStatusType.Error
                                AlertSeverity.Warning -> FarmVisionStatusType.Warning
                                AlertSeverity.Information -> FarmVisionStatusType.Online
                            },
                            isRead = item.isRead
                        )
                    }
                }
            }

            viewModelScope.launch(defaultDispatcher) {
                combine(telemetry, sensorReadings) { tel, readings ->
                    val now = System.currentTimeMillis()
                    val output = alertEngine.evaluate(
                        telemetry = tel,
                        sensorReadings = readings,
                        nowEpochMillis = now
                    )
                    val events = alertLifecycleManager.processEvaluationOutput(output, now)
                    if (events.isNotEmpty() || output.newAlerts.isNotEmpty() || output.repeatKeys.isNotEmpty() || output.resolvedKeys.isNotEmpty()) {
                        alertRepository.saveAlerts(
                            active = alertLifecycleManager.activeAlertsFlow.value,
                            resolved = alertLifecycleManager.resolvedAlertsFlow.value
                        )
                        events.forEach { event ->
                            alertNotificationManager?.handleLifecycleEvent(event, now)
                        }
                    }
                }.collect()
            }
        }
    }

    // Notification bottom sheet visibility
    private val _isNotificationSheetOpen = MutableStateFlow(false)
    val isNotificationSheetOpen: StateFlow<Boolean> = _isNotificationSheetOpen.asStateFlow()

    // Session runtime ticker for Controller HUD
    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

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
        viewModelScope.launch(defaultDispatcher) {
            alertRepository?.clearHistory()
        }
    }

    fun refreshTelemetry() {
        repository.refreshTelemetry()
    }

    fun setNotificationSheetOpen(open: Boolean) {
        _isNotificationSheetOpen.value = open
    }

    // --- Analytics Actions ---

    fun setAnalyticsRange(range: AnalyticsTimeRange) {
        if (_analyticsRange.value == range) {
            if (range == AnalyticsTimeRange.Custom) {
                setCustomDatePickerOpen(true)
            }
            return
        }
        _analyticsRange.value = range
        if (range == AnalyticsTimeRange.Custom) {
            setCustomDatePickerOpen(true)
        } else {
            recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }
        }
    }

    fun setAnalyticsMetric(metric: HistoricalMetric) {
        if (_selectedMetric.value == metric) return
        _selectedMetric.value = metric
        recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }
    }

    fun toggleAnalyticsDemoMode(enable: Boolean) {
        if (_isDemoModeActive.value == enable) return
        _isDemoModeActive.value = enable
        recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _customStartMillis.value = startMillis
        _customEndMillis.value = endMillis
        _analyticsRange.value = AnalyticsTimeRange.Custom
        _isCustomDatePickerOpen.value = false
        recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }
    }

    fun setCustomDatePickerOpen(open: Boolean) {
        _isCustomDatePickerOpen.value = open
        _analyticsUiState.value = _analyticsUiState.value.copy(isCustomDateRangePickerOpen = open)
    }

    fun setAnalyticsTableView(enabled: Boolean) {
        _isTableViewSelected.value = enabled
        _analyticsUiState.value = _analyticsUiState.value.copy(isTableViewSelected = enabled)
    }

    fun loadMoreRecentRecords() {
        _recordsLimit.value += 100
        recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }
    }

    fun refreshAnalytics() {
        recomputeAnalytics()
        viewModelScope.launch(defaultDispatcher) {
            while (true) {
                delay(1000L)
                _sessionDurationSeconds.value += 1L
            }
        }
    }

    private fun recomputeAnalytics() {
        analyticsJob?.cancel()
        analyticsJob = viewModelScope.launch(defaultDispatcher) {
            val metric = _selectedMetric.value
            val range = _analyticsRange.value
            val isDemo = _isDemoModeActive.value
            val limit = _recordsLimit.value
            val isTable = _isTableViewSelected.value
            val customStart = _customStartMillis.value
            val customEnd = _customEndMillis.value
            val isPickerOpen = _isCustomDatePickerOpen.value
            val currentTelemetry = telemetry.value
            val connStatus = currentTelemetry.connectionStatus.defaultLabel
            val isConnected = currentTelemetry.connectionStatus == FarmVisionStatusType.Online || currentTelemetry.connectionStatus == FarmVisionStatusType.Active
            val now = System.currentTimeMillis()
            val window: AnalyticsTimeWindow = if (range == AnalyticsTimeRange.Custom && customStart != null && customEnd != null) {
                AnalyticsMathEngine.createCustomWindow(customStart, customEnd, now).getOrDefault(range.calculateWindow(now))
            } else {
                range.calculateWindow(now)
            }

            try {
                val newState = if (isDemo) {
                    buildAnalyticsState(
                        metric = metric,
                        range = range,
                        isDemo = true,
                        isTable = isTable,
                        limit = limit,
                        now = now,
                        customStart = customStart,
                        customEnd = customEnd,
                        isPickerOpen = isPickerOpen,
                        connectionStatus = connStatus,
                        isRoverConnected = isConnected
                    )
                } else {
                    val liveSamples = repository.telemetryHistory.getSamples(metric, window, DataOrigin.LiveHardware)
                    val liveSec = if (metric == HistoricalMetric.SoilMoistureProbe1Raw) {
                        repository.telemetryHistory.getSamples(HistoricalMetric.SoilMoistureProbe2Raw, window, DataOrigin.LiveHardware)
                    } else emptyList()
                    val recent = repository.telemetryHistory.getRecentRecords(limit = limit + 1, offset = 0)

                    val allLiveSamplesForWindow = mutableListOf<TelemetrySample>()
                    allLiveSamplesForWindow.addAll(liveSamples)
                    if (metric != HistoricalMetric.SoilMoistureProbe1Raw) {
                        allLiveSamplesForWindow.addAll(repository.telemetryHistory.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window, DataOrigin.LiveHardware))
                    }
                    if (metric != HistoricalMetric.SoilMoistureProbe2Raw) {
                        allLiveSamplesForWindow.addAll(if (liveSec.isNotEmpty()) liveSec else repository.telemetryHistory.getSamples(HistoricalMetric.SoilMoistureProbe2Raw, window, DataOrigin.LiveHardware))
                    }
                    if (metric != HistoricalMetric.AirTemperature) {
                        allLiveSamplesForWindow.addAll(repository.telemetryHistory.getSamples(HistoricalMetric.AirTemperature, window, DataOrigin.LiveHardware))
                    }
                    if (metric != HistoricalMetric.Humidity) {
                        allLiveSamplesForWindow.addAll(repository.telemetryHistory.getSamples(HistoricalMetric.Humidity, window, DataOrigin.LiveHardware))
                    }
                    if (metric != HistoricalMetric.SoilPh) {
                        allLiveSamplesForWindow.addAll(repository.telemetryHistory.getSamples(HistoricalMetric.SoilPh, window, DataOrigin.LiveHardware))
                    }

                    buildAnalyticsState(
                        metric = metric,
                        range = range,
                        isDemo = false,
                        isTable = isTable,
                        limit = limit,
                        now = now,
                        liveSamples = liveSamples,
                        liveProbe2Samples = liveSec,
                        liveRecentRecords = recent,
                        allLiveSamplesForWindow = allLiveSamplesForWindow,
                        customStart = customStart,
                        customEnd = customEnd,
                        isPickerOpen = isPickerOpen,
                        connectionStatus = connStatus,
                        isRoverConnected = isConnected
                    )
                }
                _analyticsUiState.value = newState
            } catch (_: CancellationException) {
                // Cancelled by a rapid selector change
            } catch (e: Exception) {
                _analyticsUiState.value = _analyticsUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to process analytics"
                )
            }
        }
    }

    companion object {
        fun buildAnalyticsState(
            metric: HistoricalMetric = HistoricalMetric.SoilMoistureProbe1Raw,
            range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24,
            isDemo: Boolean = true,
            isTable: Boolean = false,
            limit: Int = 100,
            now: Long = System.currentTimeMillis(),
            liveSamples: List<TelemetrySample> = emptyList(),
            liveProbe2Samples: List<TelemetrySample> = emptyList(),
            liveRecentRecords: List<TelemetrySample> = emptyList(),
            allLiveSamplesForWindow: List<TelemetrySample> = emptyList(),
            customStart: Long? = null,
            customEnd: Long? = null,
            isPickerOpen: Boolean = false,
            connectionStatus: String = "OFFLINE",
            isRoverConnected: Boolean = false
        ): AnalyticsUiState {
            val window: AnalyticsTimeWindow = if (range == AnalyticsTimeRange.Custom && customStart != null && customEnd != null) {
                AnalyticsMathEngine.createCustomWindow(customStart, customEnd, now).getOrDefault(range.calculateWindow(now))
            } else {
                range.calculateWindow(now)
            }
            val primarySeries: HistoricalSeries
            val secondarySeries: HistoricalSeries?
            val allRecentRecords: List<TelemetrySample>
            val provenanceLabel: String
            val abnormalSummary: AbnormalReadingsSummary
            val overviewMetrics: AnalyticsOverviewMetrics
            val lastUpdatedEpochMillis: Long?

            if (isDemo) {
                provenanceLabel = "DEMO DATA"
                val allDemoSamples = DeterministicDemoHistoryGenerator.generateDemoHistory(now, durationDays = 30)

                val primaryDemo = allDemoSamples.filter { it.metric == metric }
                val (validPrimary, excludedPrimary) = AnalyticsMathEngine.filterAndValidateWindow(primaryDemo, window, now)
                val statsPrimary = AnalyticsMathEngine.computeStatistics(validPrimary, excludedPrimary)
                val trendPrimary = AnalyticsMathEngine.computeTrend(validPrimary, metric)
                val bucketsPrimary = AnalyticsMathEngine.computeBuckets(validPrimary, window, AnalyticsMathEngine.MAX_CHART_POINTS)

                primarySeries = HistoricalSeries(
                    metric = metric,
                    origin = DataOrigin.Simulated,
                    buckets = bucketsPrimary,
                    statistics = statsPrimary,
                    trend = trendPrimary,
                    totalValidSamples = validPrimary
                )

                secondarySeries = if (metric == HistoricalMetric.SoilMoistureProbe1Raw) {
                    val secondaryDemo = allDemoSamples.filter { it.metric == HistoricalMetric.SoilMoistureProbe2Raw }
                    val (validSec, excludedSec) = AnalyticsMathEngine.filterAndValidateWindow(secondaryDemo, window, now)
                    val statsSec = AnalyticsMathEngine.computeStatistics(validSec, excludedSec)
                    val trendSec = AnalyticsMathEngine.computeTrend(validSec, HistoricalMetric.SoilMoistureProbe2Raw)
                    val bucketsSec = AnalyticsMathEngine.computeBuckets(validSec, window, AnalyticsMathEngine.MAX_CHART_POINTS)
                    HistoricalSeries(
                        metric = HistoricalMetric.SoilMoistureProbe2Raw,
                        origin = DataOrigin.Simulated,
                        buckets = bucketsSec,
                        statistics = statsSec,
                        trend = trendSec,
                        totalValidSamples = validSec
                    )
                } else null

                allRecentRecords = allDemoSamples.sortedByDescending { it.timestampEpochMillis }
                abnormalSummary = AnalyticsMathEngine.computeAbnormalReadings(validPrimary, excludedPrimary, metric)
                val demoValidInWindow = allDemoSamples.filter { it.timestampEpochMillis in window.startEpochMillis..window.endEpochMillis }
                overviewMetrics = AnalyticsMathEngine.computeOverviewMetrics(demoValidInWindow, "DEMO DATA")
                lastUpdatedEpochMillis = validPrimary.maxOfOrNull { it.timestampEpochMillis } ?: now
            } else {
                provenanceLabel = "LIVE HARDWARE"
                val (validLive, excludedLive) = AnalyticsMathEngine.filterAndValidateWindow(liveSamples, window, now)
                val statsLive = AnalyticsMathEngine.computeStatistics(validLive, excludedLive)
                val trendLive = AnalyticsMathEngine.computeTrend(validLive, metric)
                val bucketsLive = AnalyticsMathEngine.computeBuckets(validLive, window, AnalyticsMathEngine.MAX_CHART_POINTS)

                primarySeries = HistoricalSeries(
                    metric = metric,
                    origin = DataOrigin.LiveHardware,
                    buckets = bucketsLive,
                    statistics = statsLive,
                    trend = trendLive,
                    totalValidSamples = validLive
                )

                secondarySeries = if (metric == HistoricalMetric.SoilMoistureProbe1Raw) {
                    val (validSec, excludedSec) = AnalyticsMathEngine.filterAndValidateWindow(liveProbe2Samples, window, now)
                    val statsSec = AnalyticsMathEngine.computeStatistics(validSec, excludedSec)
                    val trendSec = AnalyticsMathEngine.computeTrend(validSec, HistoricalMetric.SoilMoistureProbe2Raw)
                    val bucketsSec = AnalyticsMathEngine.computeBuckets(validSec, window, AnalyticsMathEngine.MAX_CHART_POINTS)
                    HistoricalSeries(
                        metric = HistoricalMetric.SoilMoistureProbe2Raw,
                        origin = DataOrigin.LiveHardware,
                        buckets = bucketsSec,
                        statistics = statsSec,
                        trend = trendSec,
                        totalValidSamples = validSec
                    )
                } else null

                allRecentRecords = liveRecentRecords
                abnormalSummary = AnalyticsMathEngine.computeAbnormalReadings(validLive, excludedLive, metric)
                overviewMetrics = AnalyticsMathEngine.computeOverviewMetrics(allLiveSamplesForWindow, "LIVE HARDWARE")
                lastUpdatedEpochMillis = allLiveSamplesForWindow.maxOfOrNull { it.timestampEpochMillis } ?: liveRecentRecords.firstOrNull()?.timestampEpochMillis
            }

            val pagedRecords = allRecentRecords.take(limit)
            val hasMore = allRecentRecords.size > limit

            return AnalyticsUiState(
                selectedMetric = metric,
                selectedRange = range,
                isDemoModeActive = isDemo,
                isLoading = false,
                errorMessage = null,
                primarySeries = primarySeries,
                secondarySeries = secondarySeries,
                statistics = primarySeries.statistics,
                trend = primarySeries.trend,
                recentRecords = pagedRecords,
                totalRecordCount = allRecentRecords.size,
                recordsLimit = limit,
                hasMoreRecords = hasMore,
                provenanceLabel = provenanceLabel,
                isTableViewSelected = isTable,
                abnormalSummary = abnormalSummary,
                overviewMetrics = overviewMetrics,
                connectionStatus = connectionStatus,
                isRoverConnected = isRoverConnected,
                lastUpdatedEpochMillis = lastUpdatedEpochMillis,
                customStartEpochMillis = customStart,
                customEndEpochMillis = customEnd,
                isCustomDateRangePickerOpen = isPickerOpen
            )
        }
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
        analyticsJob?.cancel()
        commandPipeline.cancelPending()
        commandPipeline.submitVector(JoystickVector.NEUTRAL)
    }
}
