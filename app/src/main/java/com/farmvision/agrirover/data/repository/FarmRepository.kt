package com.farmvision.agrirover.data.repository

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTrendDirection
import com.farmvision.agrirover.data.datasource.CommandSink
import com.farmvision.agrirover.data.datasource.HardwareCatalogSource
import com.farmvision.agrirover.data.datasource.StaticHardwareCatalogSource
import com.farmvision.agrirover.data.datasource.TelemetrySource
import com.farmvision.agrirover.data.model.AnalyticsKpis
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.FarmAlert
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorDataPoint
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.data.model.SoilMoistureDualProbeMapping
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.time.SystemTimeProvider
import com.farmvision.agrirover.data.time.TimeProvider
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.ui.rover.controller.JoystickMath
import com.farmvision.agrirover.ui.rover.controller.JoystickVector
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.alert.repository.AlertRepository
import com.farmvision.agrirover.domain.repository.FarmManagementRepository
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository
import com.farmvision.agrirover.data.repository.InMemoryFarmManagementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Unified Agricultural Repository interface.
 * Implements [TelemetrySource] and [CommandSink] boundaries, providing a single source of truth
 * for rover telemetry, multi-sensor streams, crop health, alerts, hardware inventory,
 * and the Farm -> Plot -> Planting hierarchy.
 */
interface FarmRepository : TelemetrySource, CommandSink {
    /** Hardware component catalog and device profiles source. */
    val hardwareCatalog: HardwareCatalogSource

    /** Agricultural farm, plot, and planting single source of truth. */
    val farmManagement: FarmManagementRepository

    /** Historical telemetry repository boundary. */
    val telemetryHistory: TelemetryHistoryRepository

    /** Reactive stream of crop/plant profiles. */
    val plantsFlow: StateFlow<List<PlantItem>>

    /** Reactive stream of operational and environmental alerts. */
    val alertsFlow: StateFlow<List<FarmAlert>>

    /** Device registry repository boundary for Phase 11 communication architecture. */
    val deviceRegistry: com.farmvision.agrirover.domain.device.repository.DeviceRegistryRepository? get() = null

    fun getPlants(): List<PlantItem>
    fun getAlerts(): List<FarmAlert>
    fun getSensorReading(type: SensorType): SensorReading?

    fun togglePlantIrrigation(plantId: String)
    fun clearAllAlerts()

    fun getSoilMoistureHistory(range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24): List<SensorDataPoint>
    fun getTemperatureHistory(range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24, tempUnit: TemperatureUnit = TemperatureUnit.Celsius): List<SensorDataPoint>
    fun getAnalyticsKpis(range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24, tempUnit: TemperatureUnit = TemperatureUnit.Celsius): AnalyticsKpis
}

/**
 * High-fidelity Mock Repository maintaining a single source of truth for all screens.
 * Backed by deterministic [TimeProvider] and static [HardwareCatalogSource].
 * State updates (e.g. E-Stop or plant irrigation) immediately emit to all observing screens.
 */
class MockFarmRepository(
    val timeProvider: TimeProvider = SystemTimeProvider(),
    override val hardwareCatalog: HardwareCatalogSource = StaticHardwareCatalogSource(),
    override val farmManagement: FarmManagementRepository = InMemoryFarmManagementRepository(timeProvider = timeProvider),
    override val telemetryHistory: TelemetryHistoryRepository = InMemoryTelemetryHistoryRepository(),
    val alertRepository: AlertRepository? = null,
    val deviceCommunicationRepository: com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository = com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository()
) : FarmRepository {

    override val deviceRegistry: com.farmvision.agrirover.domain.device.repository.DeviceRegistryRepository = deviceCommunicationRepository

    private val initialPlants = listOf(
        PlantItem(
            id = "crop-01",
            name = "Roma Tomato",
            variety = "Solanum lycopersicum",
            plotLocation = "Sector A • Plot 2",
            growthStage = "Flowering (Day 42)",
            healthPercent = 94,
            healthStatus = FarmVisionStatusType.Active,
            currentMoisture = 44.2f,
            wateringRecommendation = "Hydration optimal • Next scan at 08:00 AM",
            daysToHarvest = 35,
            isWatered = false
        ),
        PlantItem(
            id = "crop-02",
            name = "Sweet Corn",
            variety = "Zea mays var. saccharata",
            plotLocation = "Sector B • Plot 1",
            growthStage = "Tasseling (Day 56)",
            healthPercent = 88,
            healthStatus = FarmVisionStatusType.Active,
            currentMoisture = 38.6f,
            wateringRecommendation = "Soil drying moderately • Water 400ml tomorrow",
            daysToHarvest = 28,
            isWatered = false
        ),
        PlantItem(
            id = "crop-03",
            name = "Bell Pepper",
            variety = "Capsicum annuum",
            plotLocation = "Sector B • Plot 4",
            growthStage = "Fruit Setting (Day 38)",
            healthPercent = 72,
            healthStatus = FarmVisionStatusType.Warning,
            currentMoisture = 31.4f,
            wateringRecommendation = "Low moisture threshold reached • Irrigate within 4 hours",
            daysToHarvest = 45,
            isWatered = false
        ),
        PlantItem(
            id = "crop-04",
            name = "Golden Soybean",
            variety = "Glycine max",
            plotLocation = "Sector C • Plot 3",
            growthStage = "Pod Development (Day 64)",
            healthPercent = 91,
            healthStatus = FarmVisionStatusType.Active,
            currentMoisture = 46.8f,
            wateringRecommendation = "Optimal rootzone saturation",
            daysToHarvest = 22,
            isWatered = false
        )
    )

    private val initialMoisture: Float =
        String.format(java.util.Locale.US, "%.1f", initialPlants.map { it.currentMoisture }.average()).toFloat()

    private val nowEpoch = timeProvider.nowEpochMillis()

    // Canonical sensor readings initialization
    private val probe1Initial = SensorReading.createValidated(
        sensorId = SoilMoistureDualProbeMapping.PROBE_1_ID,
        sensorType = SensorType.SoilMoisture,
        value = 43.5f,
        timestampEpochMillis = nowEpoch,
        nowEpochMillis = nowEpoch
    )
    private val probe2Initial = SensorReading.createValidated(
        sensorId = SoilMoistureDualProbeMapping.PROBE_2_ID,
        sensorType = SensorType.SoilMoisture,
        value = 41.0f,
        timestampEpochMillis = nowEpoch,
        nowEpochMillis = nowEpoch
    )
    private val unifiedMoistureInitial = SoilMoistureDualProbeMapping.computeUnifiedMoisture(
        probe1Initial, probe2Initial, nowEpoch
    )

    private val initialSensorReadings: Map<SensorType, SensorReading> = mapOf(
        SensorType.SoilMoisture to unifiedMoistureInitial,
        SensorType.AirTemperature to SensorReading.createValidated(
            sensorId = "sensor-dht22-temp",
            sensorType = SensorType.AirTemperature,
            value = 26.8f,
            timestampEpochMillis = nowEpoch,
            nowEpochMillis = nowEpoch
        ),
        SensorType.Humidity to SensorReading.createValidated(
            sensorId = "sensor-dht22-humidity",
            sensorType = SensorType.Humidity,
            value = 62.3f,
            timestampEpochMillis = nowEpoch,
            nowEpochMillis = nowEpoch
        ),
        // NOTE: No physical light sensor in inventory; explicitly marked as simulated with no hardware source
        SensorType.LightIntensity to SensorReading.createValidated(
            sensorId = "simulated-ambient-lux",
            sensorType = SensorType.LightIntensity,
            value = 48500.0f,
            timestampEpochMillis = nowEpoch,
            nowEpochMillis = nowEpoch,
            dataOrigin = DataOrigin.Simulated
        ),
        SensorType.SoilPh to SensorReading.createValidated(
            sensorId = "sensor-soil-ph",
            sensorType = SensorType.SoilPh,
            value = 6.8f,
            timestampEpochMillis = nowEpoch,
            nowEpochMillis = nowEpoch
        ),
        SensorType.UltrasonicDistance to SensorReading.createValidated(
            sensorId = "sensor-hc-sr04-distance",
            sensorType = SensorType.UltrasonicDistance,
            value = 45.0f,
            timestampEpochMillis = nowEpoch,
            nowEpochMillis = nowEpoch
        )
    )

    private val initialTelemetry = RoverTelemetry(
        roverName = "AgriRover Alpha-1",
        connectionStatus = FarmVisionStatusType.DemoData,
        batteryPercent = 84,
        batteryVoltage = 12.4f,
        speedKmh = 0.0f,
        isEmergencyCutoff = false,
        activeCommand = "HOLD POSITION",
        roverMode = "STANDBY",
        motorTemperature = 34.2f,
        soilMoisture = initialMoisture,
        soilMoistureTrend = FarmVisionTrendDirection.Rising,
        temperature = 26.8f,
        temperatureTrend = FarmVisionTrendDirection.Stable,
        humidity = 62.3f,
        humidityTrend = FarmVisionTrendDirection.Falling,
        lightLux = 48500,
        lightTrend = FarmVisionTrendDirection.Rising,
        lastUpdated = timeProvider.formatRelativeTime(nowEpoch),
        isSimulation = true,
        dataOrigin = DataOrigin.Simulated,
        soilPh = 6.8f,
        ultrasonicDistanceCm = 45.0f,
        sensorReadings = initialSensorReadings
    )

    private val initialAlerts = listOf(
        FarmAlert(
            id = "alert-01",
            title = "Sector B-4 Moisture Advisory",
            message = "Capacitance probe reports rootzone moisture at 31.4% (Threshold: 35%).",
            timestamp = "14m ago",
            severity = FarmVisionStatusType.Warning
        ),
        FarmAlert(
            id = "alert-02",
            title = "Solar Radiation Peak",
            message = "Peak solar intensity reached 48,500 lux across Sector A plots.",
            timestamp = "1h ago",
            severity = FarmVisionStatusType.Active
        ),
        FarmAlert(
            id = "alert-03",
            title = "Waypoint Scan Completed",
            message = "Autonomous perimeter check logged 42 sampling points with zero obstacle alerts.",
            timestamp = "3h ago",
            severity = FarmVisionStatusType.DemoData
        )
    )

    private val _telemetryFlow = MutableStateFlow(initialTelemetry)
    override val telemetryFlow: StateFlow<RoverTelemetry> = _telemetryFlow.asStateFlow()

    private val _sensorReadingsFlow = MutableStateFlow(initialSensorReadings)
    override val sensorReadingsFlow: StateFlow<Map<SensorType, SensorReading>> = _sensorReadingsFlow.asStateFlow()

    data class CropSimulationState(
        val currentMoisture: Float,
        val isWatered: Boolean = false,
        val wateringRecommendation: String = "Optimal rootzone saturation"
    )

    private val initialSimulations: Map<String, CropSimulationState> = mapOf(
        "crop-01" to CropSimulationState(44.2f, false, "Hydration optimal • Next scan at 08:00 AM"),
        "crop-02" to CropSimulationState(38.6f, false, "Soil drying moderately • Water 400ml tomorrow"),
        "crop-03" to CropSimulationState(31.4f, false, "Low moisture threshold reached • Irrigate within 4 hours"),
        "crop-04" to CropSimulationState(46.8f, false, "Optimal rootzone saturation")
    )
    private val _simulationsFlow = MutableStateFlow(initialSimulations)

    private val _plantsFlow = MutableStateFlow(initialPlants)
    override val plantsFlow: StateFlow<List<PlantItem>> = _plantsFlow.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        syncPlants()
        scope.launch {
            combine(
                farmManagement.plantingsFlow,
                farmManagement.plotsFlow
            ) { _, _ ->
                syncPlants()
            }.collect()
        }
    }

    private fun buildPlantItems(): List<PlantItem> {
        val plots = farmManagement.getPlots().associateBy { it.id }
        val plantings = farmManagement.getPlantings()
        val simMap = _simulationsFlow.value

        return plantings.map { p ->
            val plotName = plots[p.plotId]?.name ?: "Sector A • Plot 2"
            val sim = simMap[p.id] ?: initialSimulations[p.id] ?: CropSimulationState(42.0f, false, "Hydration optimal")
            val healthPercent = if (sim.isWatered) {
                when (p.id) {
                    "crop-01" -> 98
                    "crop-02" -> 98
                    "crop-03" -> 82
                    "crop-04" -> 98
                    else -> 94
                }
            } else {
                when (p.id) {
                    "crop-01" -> 94
                    "crop-02" -> 88
                    "crop-03" -> 72
                    "crop-04" -> 91
                    else -> if (sim.currentMoisture >= 35f) 90 else 70
                }
            }
            val healthStatus = if (sim.currentMoisture >= 35f && p.userHealthStatus != PlantHealthStatus.NeedsAttention && p.userHealthStatus != PlantHealthStatus.Critical) {
                FarmVisionStatusType.Active
            } else {
                FarmVisionStatusType.Warning
            }

            PlantItem(
                id = p.id,
                name = p.cropName,
                variety = p.variety ?: "",
                plotLocation = plotName,
                growthStage = p.growthStage.label,
                healthPercent = healthPercent,
                healthStatus = healthStatus,
                currentMoisture = sim.currentMoisture,
                optimalMoistureMin = 35f,
                optimalMoistureMax = 55f,
                wateringRecommendation = if (sim.isWatered) "Hydration logged. Rootzone absorption active." else sim.wateringRecommendation,
                daysToHarvest = p.daysToHarvest ?: 30,
                isWatered = sim.isWatered
            )
        }
    }

    private fun syncPlants() {
        val items = buildPlantItems()
        _plantsFlow.value = items

        val avgMoisture = if (items.isNotEmpty()) {
            items.map { it.currentMoisture }.average().toFloat()
        } else {
            40.0f
        }
        val formattedMoisture = String.format(java.util.Locale.US, "%.1f", avgMoisture).toFloat()
        val now = timeProvider.nowEpochMillis()

        val updatedMoistureReading = SensorReading.createValidated(
            sensorId = "unified-rootzone-moisture",
            sensorType = SensorType.SoilMoisture,
            value = formattedMoisture,
            timestampEpochMillis = now,
            nowEpochMillis = now
        )

        _sensorReadingsFlow.update { it + (SensorType.SoilMoisture to updatedMoistureReading) }
        _telemetryFlow.update {
            it.copy(
                soilMoisture = formattedMoisture,
                sensorReadings = _sensorReadingsFlow.value,
                lastUpdated = timeProvider.formatRelativeTime(now)
            )
        }
    }

    private val _alertsFlow = MutableStateFlow(initialAlerts)
    override val alertsFlow: StateFlow<List<FarmAlert>> = _alertsFlow.asStateFlow()

    override fun getTelemetry(): RoverTelemetry = _telemetryFlow.value
    override fun getPlants(): List<PlantItem> = _plantsFlow.value
    override fun getAlerts(): List<FarmAlert> = _alertsFlow.value
    override fun getSensorReading(type: SensorType): SensorReading? = _sensorReadingsFlow.value[type]

    override fun updateRoverMovement(command: String, speedKmh: Float) {
        if (_telemetryFlow.value.isEmergencyCutoff) return
        val currentMode = _telemetryFlow.value.driveModeEnum
        if (currentMode != RoverDriveMode.Manual && speedKmh > 0f) return
        val now = timeProvider.nowEpochMillis()
        _telemetryFlow.update { current ->
            val mission = when (command) {
                "FIELD SCAN" -> "Autonomous Field Scan"
                "RETURN TO DOCK" -> "Returning to Dock"
                "FORWARD", "REVERSE", "TURN LEFT", "TURN RIGHT" -> "Manual Navigation"
                else -> if (speedKmh > 0f) "Active Mission" else "Standby • Ready for Dispatch"
            }
            val mode = when (command) {
                "FIELD SCAN" -> "AUTONOMOUS"
                "RETURN TO DOCK" -> "DOCKING"
                else -> if (speedKmh > 0f) "MANUAL_DRIVE" else "STANDBY"
            }
            current.copy(
                activeCommand = command,
                speedKmh = speedKmh,
                roverMode = mode,
                activeMission = mission,
                lastUpdated = timeProvider.formatRelativeTime(now)
            )
        }
    }

    override fun updateRoverMovement(vector: JoystickVector, speedKmh: Float) {
        if (_telemetryFlow.value.isEmergencyCutoff) return
        val currentMode = _telemetryFlow.value.driveModeEnum
        if (currentMode != RoverDriveMode.Manual && !vector.isNeutral) return

        val sanitized = JoystickMath.sanitize(vector)
        val now = timeProvider.nowEpochMillis()
        _telemetryFlow.update { current ->
            if (sanitized.isNeutral) {
                current.copy(
                    activeCommand = "HOLD POSITION",
                    speedKmh = 0.0f,
                    roverMode = "STANDBY",
                    activeMission = "Standby • Ready for Dispatch",
                    joystickVector = JoystickVector.NEUTRAL,
                    lastUpdated = timeProvider.formatRelativeTime(now)
                )
            } else {
                val dirLabel = when {
                    sanitized.y > 0.35f && kotlin.math.abs(sanitized.x) <= 0.35f -> "FORWARD"
                    sanitized.y < -0.35f && kotlin.math.abs(sanitized.x) <= 0.35f -> "REVERSE"
                    sanitized.x < -0.35f && kotlin.math.abs(sanitized.y) <= 0.35f -> "TURN LEFT"
                    sanitized.x > 0.35f && kotlin.math.abs(sanitized.y) <= 0.35f -> "TURN RIGHT"
                    else -> String.format(java.util.Locale.US, "VECTOR (%.2f, %.2f)", sanitized.x, sanitized.y)
                }
                current.copy(
                    activeCommand = dirLabel,
                    speedKmh = speedKmh,
                    roverMode = "MANUAL_DRIVE",
                    activeMission = "Manual Navigation (Vector)",
                    joystickVector = sanitized,
                    lastUpdated = timeProvider.formatRelativeTime(now)
                )
            }
        }
    }

    override fun setDriveMode(mode: String) {
        val now = timeProvider.nowEpochMillis()
        _telemetryFlow.update { current ->
            val driveModeEnum = when (mode.trim().lowercase()) {
                "autonomous", "autonomous waypoint", "autowaypoint" -> RoverDriveMode.AutonomousWaypoint
                "hold", "hold position", "holdposition" -> RoverDriveMode.HoldPosition
                else -> RoverDriveMode.Manual
            }
            val speedPreset = when (mode.trim().lowercase()) {
                "precision", "low" -> SpeedPreset.Low
                "turbo", "high" -> SpeedPreset.High
                else -> SpeedPreset.Medium
            }
            val updatedSpeed = if (driveModeEnum == RoverDriveMode.Manual && current.speedKmh > 0f && !current.isEmergencyCutoff) {
                when (mode) {
                    "Precision" -> 1.5f
                    "Turbo" -> 5.5f
                    "Normal" -> 3.5f
                    else -> speedPreset.maxSpeedKmh
                }
            } else {
                0.0f
            }
            val updatedCmd = if (driveModeEnum == RoverDriveMode.HoldPosition) "HOLD POSITION" else current.activeCommand
            val updatedMode = if (driveModeEnum == RoverDriveMode.Manual && updatedSpeed > 0f) "MANUAL_DRIVE" else "STANDBY"
            val updatedVector = if (driveModeEnum == RoverDriveMode.Manual && updatedSpeed > 0f) current.joystickVector else JoystickVector.NEUTRAL

            current.copy(
                driveMode = mode,
                driveModeEnum = driveModeEnum,
                speedPreset = speedPreset,
                speedKmh = updatedSpeed,
                activeCommand = updatedCmd,
                roverMode = updatedMode,
                joystickVector = updatedVector,
                lastUpdated = timeProvider.formatRelativeTime(now)
            )
        }
    }

    override fun setDriveMode(mode: RoverDriveMode) {
        setDriveMode(mode.label)
    }

    override fun setSpeedPreset(preset: SpeedPreset) {
        val now = timeProvider.nowEpochMillis()
        _telemetryFlow.update { current ->
            // Changing a preset never starts movement from stationary
            val updatedSpeed = if (current.speedKmh > 0f && !current.isEmergencyCutoff && current.driveModeEnum == RoverDriveMode.Manual) {
                val scale = if (!current.joystickVector.isNeutral) current.joystickVector.magnitude else 1.0f
                preset.maxSpeedKmh * scale
            } else {
                0.0f
            }
            current.copy(
                speedPreset = preset,
                speedKmh = updatedSpeed,
                lastUpdated = timeProvider.formatRelativeTime(now)
            )
        }
    }

    override fun setEmergencyStop(engaged: Boolean) {
        val now = timeProvider.nowEpochMillis()
        val relTime = timeProvider.formatRelativeTime(now)
        _telemetryFlow.update { current ->
            if (engaged) {
                current.copy(
                    isEmergencyCutoff = true,
                    activeCommand = "EMERGENCY CUTOFF",
                    speedKmh = 0f,
                    roverMode = "E-STOPPED",
                    activeMission = "Mission Halted (E-Stop Engaged)",
                    joystickVector = JoystickVector.NEUTRAL,
                    lastUpdated = relTime
                )
            } else {
                current.copy(
                    isEmergencyCutoff = false,
                    activeCommand = "HOLD POSITION",
                    speedKmh = 0f,
                    roverMode = "STANDBY",
                    activeMission = "Standby • Ready for Dispatch",
                    joystickVector = JoystickVector.NEUTRAL,
                    lastUpdated = relTime
                )
            }
        }
    }

    override fun togglePlantIrrigation(plantId: String) {
        _simulationsFlow.update { map ->
            val current = map[plantId] ?: initialSimulations[plantId] ?: CropSimulationState(42.0f, false, "Optimal rootzone saturation")
            val willWater = !current.isWatered
            val newMoisture = if (willWater) (current.currentMoisture + 8.5f).coerceAtMost(60f) else (current.currentMoisture - 8.5f).coerceAtLeast(20f)
            val newRec = if (willWater) "Hydration logged. Rootzone absorption active." else "Hydration optimal • Next scan at 08:00 AM"
            map + (plantId to current.copy(currentMoisture = newMoisture, isWatered = willWater, wateringRecommendation = newRec))
        }

        syncPlants()
    }

    override fun clearAllAlerts() {
        _alertsFlow.value = emptyList()
    }

    override fun refreshTelemetry() {
        val now = timeProvider.nowEpochMillis()
        _telemetryFlow.update { it.copy(lastUpdated = timeProvider.formatRelativeTime(now)) }
    }

    override fun toggleFrontLight() {
        _telemetryFlow.update { it.copy(isFrontLightOn = !it.isFrontLightOn) }
    }

    override fun toggleSideLight() {
        _telemetryFlow.update { it.copy(isSideLightOn = !it.isSideLightOn) }
    }

    override fun triggerHorn() {
        _telemetryFlow.update { it.copy(isHornActive = !it.isHornActive) }
    }

    override fun toggleSpray() {
        _telemetryFlow.update { it.copy(isSprayActive = !it.isSprayActive) }
    }

    override fun toggleAuxIrrigation() {
        _telemetryFlow.update { it.copy(isAuxIrrigationActive = !it.isAuxIrrigationActive) }
    }

    override fun setCameraTilt(angle: Int) {
        _telemetryFlow.update { it.copy(cameraTiltAngle = angle.coerceIn(-30, 30)) }
    }

    override fun getSoilMoistureHistory(range: AnalyticsTimeRange): List<SensorDataPoint> {
        return when (range) {
            AnalyticsTimeRange.Hours1,
            AnalyticsTimeRange.Hours6,
            AnalyticsTimeRange.Hours24 -> listOf(
                SensorDataPoint("00:00", 46.5f),
                SensorDataPoint("04:00", 45.2f),
                SensorDataPoint("08:00", 43.8f),
                SensorDataPoint("12:00", 39.4f),
                SensorDataPoint("16:00", 41.2f),
                SensorDataPoint("20:00", 42.5f)
            )
            AnalyticsTimeRange.Days7 -> listOf(
                SensorDataPoint("Mon", 48.0f),
                SensorDataPoint("Tue", 44.5f),
                SensorDataPoint("Wed", 41.0f),
                SensorDataPoint("Thu", 46.2f),
                SensorDataPoint("Fri", 43.8f),
                SensorDataPoint("Sat", 40.5f),
                SensorDataPoint("Sun", 42.5f)
            )
            AnalyticsTimeRange.Days30,
            AnalyticsTimeRange.Custom -> listOf(
                SensorDataPoint("W1", 49.2f),
                SensorDataPoint("W2", 46.0f),
                SensorDataPoint("W3", 43.5f),
                SensorDataPoint("W4", 41.8f),
                SensorDataPoint("Current", 42.5f)
            )
        }
    }

    override fun getTemperatureHistory(range: AnalyticsTimeRange, tempUnit: TemperatureUnit): List<SensorDataPoint> {
        val basePoints = when (range) {
            AnalyticsTimeRange.Hours1,
            AnalyticsTimeRange.Hours6,
            AnalyticsTimeRange.Hours24 -> listOf(
                SensorDataPoint("00:00", 21.4f),
                SensorDataPoint("04:00", 19.8f),
                SensorDataPoint("08:00", 23.5f),
                SensorDataPoint("12:00", 28.6f),
                SensorDataPoint("16:00", 27.2f),
                SensorDataPoint("20:00", 24.1f)
            )
            AnalyticsTimeRange.Days7 -> listOf(
                SensorDataPoint("Mon", 24.5f),
                SensorDataPoint("Tue", 25.8f),
                SensorDataPoint("Wed", 27.2f),
                SensorDataPoint("Thu", 26.0f),
                SensorDataPoint("Fri", 24.8f),
                SensorDataPoint("Sat", 26.5f),
                SensorDataPoint("Sun", 26.8f)
            )
            AnalyticsTimeRange.Days30,
            AnalyticsTimeRange.Custom -> listOf(
                SensorDataPoint("W1", 23.8f),
                SensorDataPoint("W2", 25.2f),
                SensorDataPoint("W3", 26.4f),
                SensorDataPoint("W4", 27.0f),
                SensorDataPoint("Current", 26.8f)
            )
        }

        return basePoints.map { pt ->
            pt.copy(value = tempUnit.convertFromCelsius(pt.value))
        }
    }

    override fun getAnalyticsKpis(range: AnalyticsTimeRange, tempUnit: TemperatureUnit): AnalyticsKpis {
        return when (range) {
            AnalyticsTimeRange.Hours1,
            AnalyticsTimeRange.Hours6,
            AnalyticsTimeRange.Hours24 -> {
                val minT = tempUnit.formatValueOnly(19.8f)
                val maxT = tempUnit.format(28.6f)
                AnalyticsKpis(
                    avgMoisture = "43.1%",
                    thermalRange = "$minT - $maxT",
                    irrigationVolume = "28.5 L"
                )
            }
            AnalyticsTimeRange.Days7 -> {
                val minT = tempUnit.formatValueOnly(18.5f)
                val maxT = tempUnit.format(29.2f)
                AnalyticsKpis(
                    avgMoisture = "43.8%",
                    thermalRange = "$minT - $maxT",
                    irrigationVolume = "185.0 L"
                )
            }
            AnalyticsTimeRange.Days30,
            AnalyticsTimeRange.Custom -> {
                val minT = tempUnit.formatValueOnly(17.2f)
                val maxT = tempUnit.format(31.4f)
                AnalyticsKpis(
                    avgMoisture = "44.6%",
                    thermalRange = "$minT - $maxT",
                    irrigationVolume = "740.0 L"
                )
            }
        }
    }
}
