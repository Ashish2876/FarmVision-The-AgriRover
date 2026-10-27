package com.farmvision.agrirover.data.model

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTrendDirection
import java.util.Locale

/**
 * Temperature unit options with conversion formulas.
 */
enum class TemperatureUnit(val symbol: String, val label: String) {
    Celsius("°C", "Celsius (°C)"),
    Fahrenheit("°F", "Fahrenheit (°F)");

    fun convertFromCelsius(celsius: Float): Float = when (this) {
        Celsius -> celsius
        Fahrenheit -> (celsius * 9f / 5f) + 32f
    }

    fun format(celsius: Float): String =
        String.format(Locale.US, "%.1f %s", convertFromCelsius(celsius), symbol)

    fun formatValueOnly(celsius: Float): String =
        String.format(Locale.US, "%.1f", convertFromCelsius(celsius))
}

/**
 * Speed and distance unit options with conversion formulas.
 */
enum class SpeedUnit(val symbol: String, val label: String) {
    Metric("km/h", "Metric (km/h)"),
    Imperial("mph", "Imperial (mph)");

    fun convertFromKmh(kmh: Float): Float = when (this) {
        Metric -> kmh
        Imperial -> kmh * 0.621371f
    }

    fun format(kmh: Float): String =
        String.format(Locale.US, "%.1f %s", convertFromKmh(kmh), symbol)

    fun formatValueOnly(kmh: Float): String =
        String.format(Locale.US, "%.1f", convertFromKmh(kmh))
}

/**
 * Time ranges for historical telemetry analytics.
 */
enum class AnalyticsTimeRange(val label: String, val durationMillis: Long) {
    Hours24("24 Hours", 24L * 60 * 60 * 1000),
    Days7("7 Days", 7L * 24 * 60 * 60 * 1000),
    Days30("30 Days", 30L * 24 * 60 * 60 * 1000);

    /**
     * Calculates rolling window [nowEpochMillis - durationMillis, nowEpochMillis], both ends inclusive.
     */
    fun calculateWindow(nowEpochMillis: Long): AnalyticsTimeWindow =
        AnalyticsTimeWindow(this, nowEpochMillis - durationMillis, nowEpochMillis)
}

/**
 * Aggregated summary indicators for the selected analytics range.
 */
data class AnalyticsKpis(
    val avgMoisture: String,
    val thermalRange: String,
    val irrigationVolume: String
)

/**
 * Persisted user settings and preferences.
 */
data class FarmSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
    val speedUnit: SpeedUnit = SpeedUnit.Metric,
    val transportProtocol: String = "Wi-Fi SoftAP",
    val isSimulatedEngineActive: Boolean = true
)

/**
 * Robotics drive modes governing rover actuation behavior.
 * - [Manual]: Joystick and directional controls accepted.
 * - [AutonomousWaypoint]: Simulated waypoint navigation; manual input rejected; no GPS claim.
 * - [HoldPosition]: Rover stationary; neutral stop forced; movement rejected.
 */
enum class RoverDriveMode(val label: String, val allowsManualDrive: Boolean) {
    Manual("Manual", true),
    AutonomousWaypoint("Autonomous Waypoint", false),
    HoldPosition("Hold Position", false);

    companion object {
        fun fromString(value: String): RoverDriveMode = when (value.trim().lowercase()) {
            "manual" -> Manual
            "autonomous", "autonomous waypoint", "autowaypoint" -> AutonomousWaypoint
            "hold", "hold position", "holdposition" -> HoldPosition
            // Backward compatibility with legacy string values "Precision", "Normal", "Turbo"
            else -> Manual
        }
    }
}

/**
 * Software speed presets defining simulated maximum speeds and duty cycle fractions.
 * Mandatory disclaimer: "Software Preset • Uncalibrated Duty Cycle".
 */
enum class SpeedPreset(
    val label: String,
    val maxSpeedKmh: Float,
    val dutyFraction: Float
) {
    Low("Low", 1.5f, 0.30f),
    Medium("Medium", 3.5f, 0.65f),
    High("High", 5.5f, 1.00f);

    companion object {
        const val DISCLAIMER = "Software Preset • Uncalibrated Duty Cycle"

        fun fromString(value: String): SpeedPreset = when (value.trim().lowercase()) {
            "low", "precision" -> Low
            "high", "turbo" -> High
            else -> Medium
        }
    }
}

/**
 * Real-time telemetry snapshot for the agricultural rover and field sensors.
 * Single source of truth shared across Home, Rover, and Analytics screens.
 */
data class RoverTelemetry(
    val roverName: String = "AgriRover Alpha-1",
    val connectionStatus: FarmVisionStatusType = FarmVisionStatusType.DemoData,
    val batteryPercent: Int = 84,
    val batteryVoltage: Float = 12.4f,
    val speedKmh: Float = 0.0f,
    val isEmergencyCutoff: Boolean = false,
    val activeCommand: String = "HOLD POSITION",
    val roverMode: String = "STANDBY",
    val driveMode: String = "Normal",
    val driveModeEnum: RoverDriveMode = RoverDriveMode.Manual,
    val speedPreset: SpeedPreset = SpeedPreset.Medium,
    val joystickVector: com.farmvision.agrirover.ui.rover.controller.JoystickVector = com.farmvision.agrirover.ui.rover.controller.JoystickVector.NEUTRAL,
    val motorTemperature: Float = 34.2f,
    val soilMoisture: Float = 42.5f,
    val soilMoistureTrend: FarmVisionTrendDirection = FarmVisionTrendDirection.Rising,
    val temperature: Float = 26.8f,
    val temperatureTrend: FarmVisionTrendDirection = FarmVisionTrendDirection.Stable,
    val humidity: Float = 62.3f,
    val humidityTrend: FarmVisionTrendDirection = FarmVisionTrendDirection.Falling,
    val lightLux: Int = 48500,
    val lightTrend: FarmVisionTrendDirection = FarmVisionTrendDirection.Rising,
    val lastUpdated: String = "Just now",
    val isSimulation: Boolean = true,
    val isFrontLightOn: Boolean = false,
    val isSideLightOn: Boolean = false,
    val isHornActive: Boolean = false,
    val isSprayActive: Boolean = false,
    val isAuxIrrigationActive: Boolean = false,
    val cameraTiltAngle: Int = 0,
    val fieldName: String = "Field A (Demo)",
    val roverIdentifier: String = "Rover 01",
    val simulatedGps: String = "12.9716°N 74.1672°E",
    val dataOrigin: DataOrigin = DataOrigin.Simulated,
    val soilPh: Float = 6.8f,
    val ultrasonicDistanceCm: Float = 45.0f,
    val sensorReadings: Map<SensorType, SensorReading> = emptyMap(),
    val activeMission: String = "Autonomous Row Scouting",
    val gpsFixStatus: String = "3D Fix (8 Sats)"
)

/**
 * Crop / plant entry in the farm management catalog.
 */
data class PlantItem(
    val id: String,
    val name: String,
    val variety: String,
    val plotLocation: String,
    val growthStage: String,
    val healthPercent: Int,
    val healthStatus: FarmVisionStatusType,
    val currentMoisture: Float,
    val optimalMoistureMin: Float = 35f,
    val optimalMoistureMax: Float = 55f,
    val wateringRecommendation: String,
    val daysToHarvest: Int,
    val isWatered: Boolean = false
)

/**
 * Operational or environmental alert notification.
 */
data class FarmAlert(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val severity: FarmVisionStatusType,
    val isRead: Boolean = false
)

/**
 * Historical time-series data point for sensor analytics.
 */
data class SensorDataPoint(
    val label: String,
    val value: Float,
    val isOptimal: Boolean = true
)
