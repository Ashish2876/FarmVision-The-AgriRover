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
enum class AnalyticsTimeRange(val label: String) {
    Hours24("24 Hours"),
    Days7("7 Days"),
    Days30("30 Days")
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
    val simulatedGps: String = "12.9716°N 74.1672°E"
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
