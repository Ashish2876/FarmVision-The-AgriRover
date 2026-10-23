package com.farmvision.agrirover.data.repository

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTrendDirection
import com.farmvision.agrirover.data.model.AnalyticsKpis
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.FarmAlert
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorDataPoint
import com.farmvision.agrirover.data.model.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository interface providing agricultural telemetry, crop profiles, and alert streams.
 */
interface FarmRepository {
    val telemetryFlow: StateFlow<RoverTelemetry>
    val plantsFlow: StateFlow<List<PlantItem>>
    val alertsFlow: StateFlow<List<FarmAlert>>

    fun getTelemetry(): RoverTelemetry
    fun getPlants(): List<PlantItem>
    fun getAlerts(): List<FarmAlert>

    fun updateRoverMovement(command: String, speedKmh: Float)
    fun setDriveMode(mode: String)
    fun setEmergencyStop(engaged: Boolean)
    fun togglePlantIrrigation(plantId: String)
    fun clearAllAlerts()
    fun refreshTelemetry()

    fun toggleFrontLight()
    fun toggleSideLight()
    fun triggerHorn()
    fun toggleSpray()
    fun toggleAuxIrrigation()
    fun setCameraTilt(angle: Int)

    fun getSoilMoistureHistory(range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24): List<SensorDataPoint>
    fun getTemperatureHistory(range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24, tempUnit: TemperatureUnit = TemperatureUnit.Celsius): List<SensorDataPoint>
    fun getAnalyticsKpis(range: AnalyticsTimeRange = AnalyticsTimeRange.Hours24, tempUnit: TemperatureUnit = TemperatureUnit.Celsius): AnalyticsKpis
}

/**
 * High-fidelity Mock Repository maintaining a single source of truth for all screens.
 * State updates (e.g. E-Stop or plant irrigation) immediately emit to all observing screens.
 */
class MockFarmRepository : FarmRepository {

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
        lastUpdated = "Just now",
        isSimulation = true
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

    private val _plantsFlow = MutableStateFlow(initialPlants)
    override val plantsFlow: StateFlow<List<PlantItem>> = _plantsFlow.asStateFlow()

    private val _alertsFlow = MutableStateFlow(initialAlerts)
    override val alertsFlow: StateFlow<List<FarmAlert>> = _alertsFlow.asStateFlow()

    override fun getTelemetry(): RoverTelemetry = _telemetryFlow.value
    override fun getPlants(): List<PlantItem> = _plantsFlow.value
    override fun getAlerts(): List<FarmAlert> = _alertsFlow.value

    override fun updateRoverMovement(command: String, speedKmh: Float) {
        if (_telemetryFlow.value.isEmergencyCutoff) return
        _telemetryFlow.update { current ->
            current.copy(
                activeCommand = command,
                speedKmh = speedKmh,
                roverMode = if (speedKmh > 0f) "MANUAL_DRIVE" else "STANDBY",
                lastUpdated = "Just now"
            )
        }
    }

    override fun setDriveMode(mode: String) {
        _telemetryFlow.update { current ->
            val updatedSpeed = if (current.speedKmh > 0f && !current.isEmergencyCutoff) {
                when (mode) {
                    "Precision" -> 1.5f
                    "Turbo" -> 5.5f
                    else -> 3.5f
                }
            } else {
                0.0f
            }
            current.copy(
                driveMode = mode,
                speedKmh = updatedSpeed,
                lastUpdated = "Just now"
            )
        }
    }

    override fun setEmergencyStop(engaged: Boolean) {
        _telemetryFlow.update { current ->
            if (engaged) {
                current.copy(
                    isEmergencyCutoff = true,
                    activeCommand = "EMERGENCY CUTOFF",
                    speedKmh = 0f,
                    roverMode = "E-STOPPED",
                    lastUpdated = "Just now"
                )
            } else {
                current.copy(
                    isEmergencyCutoff = false,
                    activeCommand = "HOLD POSITION",
                    speedKmh = 0f,
                    roverMode = "STANDBY",
                    lastUpdated = "Just now"
                )
            }
        }
    }

    override fun togglePlantIrrigation(plantId: String) {
        _plantsFlow.update { plants ->
            plants.map { plant ->
                if (plant.id == plantId) {
                    val willWater = !plant.isWatered
                    val newMoisture = if (willWater) (plant.currentMoisture + 8.5f).coerceAtMost(60f) else (plant.currentMoisture - 8.5f).coerceAtLeast(20f)
                    val newHealth = if (willWater) (plant.healthPercent + 10).coerceAtMost(98) else plant.healthPercent
                    plant.copy(
                        isWatered = willWater,
                        currentMoisture = newMoisture,
                        healthPercent = newHealth,
                        healthStatus = if (newMoisture >= plant.optimalMoistureMin) FarmVisionStatusType.Active else FarmVisionStatusType.Warning
                    )
                } else {
                    plant
                }
            }
        }

        // Recalculate average rootzone moisture for shared Home dashboard telemetry
        val avgMoisture = _plantsFlow.value.map { it.currentMoisture }.average().toFloat()
        _telemetryFlow.update { it.copy(soilMoisture = String.format(java.util.Locale.US, "%.1f", avgMoisture).toFloat()) }
    }

    override fun clearAllAlerts() {
        _alertsFlow.value = emptyList()
    }

    override fun refreshTelemetry() {
        _telemetryFlow.update { it.copy(lastUpdated = "Just now") }
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
            AnalyticsTimeRange.Days30 -> listOf(
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
            AnalyticsTimeRange.Days30 -> listOf(
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
            AnalyticsTimeRange.Days30 -> {
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
