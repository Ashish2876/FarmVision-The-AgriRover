package com.farmvision.agrirover.data.model

import java.util.Locale

/**
 * Explicit origin indicator for all telemetry and state within FarmVision.
 * In Phase 4, the runtime engine exclusively produces [Simulated] data.
 */
enum class DataOrigin(val label: String) {
    /** Telemetry generated locally via software simulation without physical hardware. */
    Simulated("Simulated / Demo Data"),

    /** Telemetry received from physical hardware (ESP32 / real sensors). Not active in Phase 4. */
    LiveHardware("Live Hardware Telemetry")
}

/**
 * Operational state classification for an individual sensor reading.
 * Out-of-bounds readings are marked [Invalid] and must never be rendered as valid measurements.
 */
enum class ReadingState {
    /** Reading is present, within physical validity bounds, and freshly polled. */
    Available,

    /** Reading exceeds the configured freshness threshold age. */
    Stale,

    /** Sensor is offline, unpolled, or communication timed out. */
    Unavailable,

    /** Reading falls outside physical/agronomical bounds; invalid telemetry. */
    Invalid
}

/**
 * Agricultural sensor types supported by the FarmVision domain layer.
 * Defines canonical units and physical/agronomical validity boundaries.
 */
enum class SensorType(
    val displayName: String,
    val canonicalUnit: String,
    val minValid: Float,
    val maxValid: Float,
    val description: String
) {
    SoilMoisture(
        displayName = "Soil Moisture",
        canonicalUnit = "%",
        minValid = 0.0f,
        maxValid = 100.0f,
        description = "Capacitive volumetric soil water content percentage"
    ),
    AirTemperature(
        displayName = "Air Temperature",
        canonicalUnit = "°C",
        minValid = -20.0f,
        maxValid = 70.0f,
        description = "Ambient atmospheric temperature from DHT22"
    ),
    Humidity(
        displayName = "Relative Humidity",
        canonicalUnit = "%",
        minValid = 0.0f,
        maxValid = 100.0f,
        description = "Ambient relative atmospheric humidity percentage from DHT22"
    ),
    LightIntensity(
        displayName = "Solar Light Intensity",
        canonicalUnit = "lx",
        minValid = 0.0f,
        maxValid = 150000.0f,
        description = "Simulated ambient illuminance in lux (no physical sensor in inventory)"
    ),
    SoilPh(
        displayName = "Soil pH",
        canonicalUnit = "pH",
        minValid = 0.0f,
        maxValid = 14.0f,
        description = "Soil acidity / alkalinity on standard 0-14 pH scale"
    ),
    UltrasonicDistance(
        displayName = "Obstacle Distance",
        canonicalUnit = "cm",
        minValid = 2.0f,
        maxValid = 400.0f,
        description = "HC-SR04 sonar detection distance in centimeters (2-400cm nominal)"
    );

    /**
     * Verifies if a given raw numerical value falls within the physical operating limits.
     */
    fun isWithinValidRange(value: Float): Boolean = value in minValid..maxValid
}

/**
 * Canonical domain model for a single agricultural sensor reading.
 * Stored exclusively in canonical units (°C, %, lx, pH, cm). Display conversions
 * (e.g. °F, mph) occur only at the presentation boundary.
 */
data class SensorReading(
    val sensorId: String,
    val sensorType: SensorType,
    val value: Float,
    val canonicalUnit: String = sensorType.canonicalUnit,
    val timestampEpochMillis: Long,
    val state: ReadingState = ReadingState.Available,
    val dataOrigin: DataOrigin = DataOrigin.Simulated
) {
    /**
     * Evaluates whether this reading is valid according to physical sensor boundaries.
     */
    fun isValuePhysicallyValid(): Boolean = sensorType.isWithinValidRange(value)

    /**
     * Determines whether this reading has grown stale relative to the provided current time.
     * Default freshness threshold is 60 seconds (60,000 ms).
     */
    fun isStale(nowMillis: Long, freshnessThresholdMillis: Long = 60_000L): Boolean {
        if (state == ReadingState.Unavailable || state == ReadingState.Invalid) return false
        val age = nowMillis - timestampEpochMillis
        return age > freshnessThresholdMillis
    }

    /**
     * Formats the reading value with its canonical unit, or returns "--" if unavailable or invalid.
     */
    fun formatDisplay(): String {
        return when (state) {
            ReadingState.Unavailable -> "-- ${sensorType.canonicalUnit} (Unavailable)"
            ReadingState.Invalid -> "ERR (Invalid Range)"
            ReadingState.Stale -> String.format(Locale.US, "%.1f %s (Stale)", value, canonicalUnit)
            ReadingState.Available -> String.format(Locale.US, "%.1f %s", value, canonicalUnit)
        }
    }

    companion object {
        /**
         * Factory function creating a validated [SensorReading], automatically marking
         * out-of-range values as [ReadingState.Invalid].
         */
        fun createValidated(
            sensorId: String,
            sensorType: SensorType,
            value: Float,
            timestampEpochMillis: Long,
            nowEpochMillis: Long = timestampEpochMillis,
            freshnessThresholdMillis: Long = 60_000L,
            dataOrigin: DataOrigin = DataOrigin.Simulated
        ): SensorReading {
            val state = when {
                !sensorType.isWithinValidRange(value) -> ReadingState.Invalid
                (nowEpochMillis - timestampEpochMillis) > freshnessThresholdMillis -> ReadingState.Stale
                else -> ReadingState.Available
            }
            return SensorReading(
                sensorId = sensorId,
                sensorType = sensorType,
                value = value,
                canonicalUnit = sensorType.canonicalUnit,
                timestampEpochMillis = timestampEpochMillis,
                state = state,
                dataOrigin = dataOrigin
            )
        }

        /**
         * Factory for an explicitly unavailable reading.
         */
        fun createUnavailable(
            sensorId: String,
            sensorType: SensorType,
            timestampEpochMillis: Long
        ): SensorReading = SensorReading(
            sensorId = sensorId,
            sensorType = sensorType,
            value = 0.0f,
            canonicalUnit = sensorType.canonicalUnit,
            timestampEpochMillis = timestampEpochMillis,
            state = ReadingState.Unavailable,
            dataOrigin = DataOrigin.Simulated
        )
    }
}

/**
 * Domain mapping rule for the two capacitive soil moisture sensors.
 *
 * The inventory contains two distinct capacitive soil probes:
 * - Probe 1: Primary rootzone surface probe (depth ~5cm)
 * - Probe 2: Deep substrate probe (depth ~15cm)
 *
 * This mapping consolidates both probe readings into a single representative rootzone value
 * for the legacy single-reading UI without hiding the underlying multi-sensor architecture.
 */
object SoilMoistureDualProbeMapping {

    const val PROBE_1_ID = "sensor-soil-moisture-1"
    const val PROBE_2_ID = "sensor-soil-moisture-2"

    /**
     * Resolves a unified soil moisture reading from both probes.
     * Rule:
     * - If both probes are available and valid, computes weighted average (60% primary rootzone, 40% deep substrate).
     * - If one probe is available, falls back gracefully to that probe.
     * - If neither is available, produces an unavailable reading.
     */
    fun computeUnifiedMoisture(
        probe1: SensorReading?,
        probe2: SensorReading?,
        nowEpochMillis: Long
    ): SensorReading {
        val p1Valid = probe1 != null && probe1.state != ReadingState.Invalid && probe1.state != ReadingState.Unavailable
        val p2Valid = probe2 != null && probe2.state != ReadingState.Invalid && probe2.state != ReadingState.Unavailable

        return when {
            p1Valid && p2Valid -> {
                val weightedValue = (probe1!!.value * 0.6f) + (probe2!!.value * 0.4f)
                val isAnyStale = probe1.state == ReadingState.Stale || probe2.state == ReadingState.Stale
                SensorReading(
                    sensorId = "unified-rootzone-moisture",
                    sensorType = SensorType.SoilMoisture,
                    value = String.format(Locale.US, "%.1f", weightedValue).toFloat(),
                    canonicalUnit = "%",
                    timestampEpochMillis = maxOf(probe1.timestampEpochMillis, probe2.timestampEpochMillis),
                    state = if (isAnyStale) ReadingState.Stale else ReadingState.Available,
                    dataOrigin = DataOrigin.Simulated
                )
            }
            p1Valid -> probe1!!
            p2Valid -> probe2!!
            else -> SensorReading.createUnavailable(
                sensorId = "unified-rootzone-moisture",
                sensorType = SensorType.SoilMoisture,
                timestampEpochMillis = nowEpochMillis
            )
        }
    }
}
