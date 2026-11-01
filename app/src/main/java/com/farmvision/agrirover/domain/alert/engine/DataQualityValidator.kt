package com.farmvision.agrirover.domain.alert.engine

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.ReadingState
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.domain.alert.model.DataQualityState

/**
 * Validates sensor telemetry before numeric alert threshold evaluation.
 * Invariant: Only [DataQualityState.Valid] readings are compared against numeric thresholds.
 */
object DataQualityValidator {

    const val DEFAULT_FRESHNESS_WINDOW_MS = 60_000L

    // Hardware Electrical Saturation Boundaries (Raw 12-bit ADC 0-4095 counts)
    const val ADC_SATURATION_LOW_COUNT = 50.0f
    const val ADC_SATURATION_HIGH_COUNT = 4050.0f

    /**
     * Evaluates the data quality of a [SensorReading].
     */
    fun validateReading(
        reading: SensorReading?,
        nowEpochMillis: Long,
        freshnessWindowMillis: Long = DEFAULT_FRESHNESS_WINDOW_MS
    ): DataQualityState {
        if (reading == null || reading.state == ReadingState.Unavailable) {
            return DataQualityState.Missing
        }

        val value = reading.value
        if (value.isNaN() || value.isInfinite()) {
            return DataQualityState.Invalid
        }

        // Verify physical bounds
        if (reading.dataOrigin == DataOrigin.LiveHardware && reading.sensorType == com.farmvision.agrirover.data.model.SensorType.SoilMoisture) {
            // Live capacitive probe measures raw 12-bit ADC counts (0..4095), not 0..100%
            if (value !in 0.0f..4095.0f || isAdcElectricallySaturated(value)) {
                return DataQualityState.Invalid
            }
            return DataQualityState.Uncalibrated
        } else if (!reading.sensorType.isWithinValidRange(value)) {
            return DataQualityState.Invalid
        }

        // Check age freshness
        val sampleAge = nowEpochMillis - reading.timestampEpochMillis
        if (sampleAge > freshnessWindowMillis) {
            return DataQualityState.Stale
        }

        // Live uncalibrated probe handling
        if (reading.dataOrigin == DataOrigin.LiveHardware) {
            if (reading.sensorType == com.farmvision.agrirover.data.model.SensorType.SoilPh) {
                return DataQualityState.Uncalibrated
            }
        }

        return DataQualityState.Valid
    }

    /**
     * Evaluates whether a raw ADC count falls within the electrical saturation margin.
     */
    fun isAdcElectricallySaturated(adcCount: Float): Boolean {
        return adcCount <= ADC_SATURATION_LOW_COUNT || adcCount >= ADC_SATURATION_HIGH_COUNT
    }
}
