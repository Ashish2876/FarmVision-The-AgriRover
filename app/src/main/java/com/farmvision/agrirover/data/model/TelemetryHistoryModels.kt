package com.farmvision.agrirover.data.model

import java.util.Locale

/**
 * Supported metrics for historical analytics adhering strictly to Section 6.1.
 * Only metrics supported by the hardware inventory and domain model are included.
 * Light intensity, soil temperature, water flow, servo position, wheel speed,
 * battery voltage/percentage, and GPS are deliberately omitted from measured history.
 */
enum class HistoricalMetric(
    val id: String,
    val displayName: String,
    val canonicalUnit: String,
    val isRaw: Boolean,
    val minValid: Float,
    val maxValid: Float,
    val axisLabel: String,
    val description: String
) {
    SoilMoistureProbe1Raw(
        id = "soil_moisture_p1_raw",
        displayName = "Soil Moisture Probe 1 (Raw ADC)",
        canonicalUnit = "counts",
        isRaw = true,
        minValid = 0f,
        maxValid = 4095f,
        axisLabel = "raw ADC counts (uncalibrated)",
        description = "Raw 12-bit ADC reading from surface capacitive soil moisture probe (depth ~5cm)"
    ),
    SoilMoistureProbe2Raw(
        id = "soil_moisture_p2_raw",
        displayName = "Soil Moisture Probe 2 (Raw ADC)",
        canonicalUnit = "counts",
        isRaw = true,
        minValid = 0f,
        maxValid = 4095f,
        axisLabel = "raw ADC counts (uncalibrated)",
        description = "Raw 12-bit ADC reading from deep substrate capacitive soil moisture probe (depth ~15cm)"
    ),
    AirTemperature(
        id = "air_temperature",
        displayName = "Air Temperature",
        canonicalUnit = "°C",
        isRaw = false,
        minValid = -20f,
        maxValid = 70f,
        axisLabel = "Air Temperature (°C)",
        description = "Ambient atmospheric air temperature from DHT22 digital sensor"
    ),
    Humidity(
        id = "relative_humidity",
        displayName = "Relative Humidity",
        canonicalUnit = "%",
        isRaw = false,
        minValid = 0f,
        maxValid = 100f,
        axisLabel = "Relative Humidity (%)",
        description = "Ambient relative atmospheric humidity percentage from DHT22 digital sensor"
    ),
    SoilPh(
        id = "soil_ph",
        displayName = "Soil pH",
        canonicalUnit = "pH",
        isRaw = false,
        minValid = 0f,
        maxValid = 14f,
        axisLabel = "Soil pH (uncalibrated)",
        description = "Soil acidity / alkalinity on standard 0-14 pH scale"
    ),
    UltrasonicDistance(
        id = "ultrasonic_distance",
        displayName = "Obstacle Distance",
        canonicalUnit = "cm",
        isRaw = false,
        minValid = 2f,
        maxValid = 400f,
        axisLabel = "Obstacle Distance (cm)",
        description = "HC-SR04 ultrasonic sonar distance in centimeters (2-400cm nominal)"
    );

    fun isWithinValidRange(value: Float): Boolean {
        if (value.isNaN() || value.isInfinite()) return false
        if (isRaw) {
            // Saturated ADC counts (<= 50 or >= 4050) are treated as invalid/suspect per Section 6.3 & Track H
            if (value <= 50f || value >= 4050f) return false
        }
        return value in minValid..maxValid
    }
}

/**
 * Calibration state indicator for historical samples.
 */
enum class CalibrationState(val label: String) {
    Uncalibrated("Uncalibrated"),
    Calibrated("Calibrated"),
    FactoryDefault("Factory Default")
}

/**
 * Canonical telemetry sample representation stored in history.
 * Contains metric identifier, canonical value, raw ADC value where relevant,
 * UTC receipt timestamp, data origin (Live or Demo), and calibration state.
 */
data class TelemetrySample(
    val id: String,
    val metric: HistoricalMetric,
    val canonicalValue: Float,
    val rawValue: Float? = null,
    val timestampEpochMillis: Long,
    val origin: DataOrigin,
    val calibrationState: CalibrationState = if (metric.isRaw) CalibrationState.Uncalibrated else CalibrationState.FactoryDefault
)

/**
 * Rolling time window representation [startEpochMillis, endEpochMillis], both ends inclusive.
 */
data class AnalyticsTimeWindow(
    val range: AnalyticsTimeRange,
    val startEpochMillis: Long,
    val endEpochMillis: Long
)

/**
 * Factual descriptive trend classification adhering strictly to Section 6.5 & Default D8.
 * No agricultural advice, yield claims, or 'significant' terminology.
 */
enum class HistoricalTrendDirection(val label: String) {
    Rising("Rising"),
    Falling("Falling"),
    Steady("Steady"),
    InsufficientData("Not enough data")
}

/**
 * Summary of calculated trend direction and neutral factual observation text.
 */
data class TrendSummary(
    val direction: HistoricalTrendDirection,
    val displayText: String,
    val slopePerDay: Float? = null
)

/**
 * Honest statistics computed over valid samples only.
 * Plainly states that the mean is a sample mean.
 */
data class TelemetryStatistics(
    val count: Int,
    val latestValidSample: TelemetrySample?,
    val sampleMean: Float?,
    val minValue: Float?,
    val maxValue: Float?,
    val excludedCount: Int
) {
    companion object {
        fun empty(excludedCount: Int = 0): TelemetryStatistics = TelemetryStatistics(
            count = 0,
            latestValidSample = null,
            sampleMean = null,
            minValue = null,
            maxValue = null,
            excludedCount = excludedCount
        )
    }
}

/**
 * Time-bucketed point for bounded chart rendering (maximum 200 points per series).
 * Represents minimum, average, and maximum within a discrete temporal bucket.
 */
data class HistoricalDataBucket(
    val bucketIndex: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val centerTimeMillis: Long,
    val min: Float?,
    val avg: Float?,
    val max: Float?,
    val sampleCount: Int,
    val isGap: Boolean
)

/**
 * Filtered series combining bucketed chart data, statistics, and trend for a single metric & origin.
 */
data class HistoricalSeries(
    val metric: HistoricalMetric,
    val origin: DataOrigin,
    val buckets: List<HistoricalDataBucket>,
    val statistics: TelemetryStatistics,
    val trend: TrendSummary,
    val totalValidSamples: List<TelemetrySample>
)
