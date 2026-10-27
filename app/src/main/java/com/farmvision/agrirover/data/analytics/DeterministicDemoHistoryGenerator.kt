package com.farmvision.agrirover.data.analytics

import com.farmvision.agrirover.data.model.CalibrationState
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import java.util.Random
import kotlin.math.sin

/**
 * Deterministic generator producing high-fidelity simulated telemetry history across 30 days (Default D5).
 *
 * Guarantees:
 * - Uses a fixed pseudo-random seed (42L) and mathematical diurnal models for 100% reproducibility.
 * - All generated samples are explicitly tagged with [DataOrigin.Simulated].
 * - Soil Probe 1 and Probe 2 are generated on separate series in raw ADC counts (uncalibrated).
 * - Air temperature and humidity reflect inverse diurnal atmospheric cycles.
 * - Samples are never written into the live hardware store and never invisibly merged.
 */
object DeterministicDemoHistoryGenerator {

    private const val SEED = 42L

    /**
     * Generates deterministic demo history across [durationDays] (default 30 days)
     * sampled at [stepMinutes] (default 30 minutes).
     */
    fun generateDemoHistory(
        nowEpochMillis: Long,
        durationDays: Int = 30,
        stepMinutes: Int = 30
    ): List<TelemetrySample> {
        val samples = mutableListOf<TelemetrySample>()
        val totalMillis = durationDays.toLong() * 24 * 60 * 60 * 1000L
        val stepMillis = stepMinutes.toLong() * 60 * 1000L
        val startMillis = nowEpochMillis - totalMillis

        val random = Random(SEED)
        var currentMillis = startMillis

        // Soil moisture drying simulation state
        var probe1Raw = 1950.0f
        var probe2Raw = 1800.0f

        var stepCount = 0
        while (currentMillis <= nowEpochMillis) {
            val dayFraction = ((currentMillis % (24 * 60 * 60 * 1000L)).toDouble() / (24 * 60 * 60 * 1000L))
            // Diurnal cycle phase: peak temperature at ~14:00 (fraction ~0.58)
            val diurnalAngle = 2.0 * Math.PI * (dayFraction - 0.58)

            // 1. Air Temperature (°C): 18°C night to 29°C day + noise
            val tempBase = 23.5f + (5.5f * sin(diurnalAngle).toFloat())
            val tempNoise = (random.nextFloat() - 0.5f) * 0.8f
            val tempVal = String.format(java.util.Locale.US, "%.1f", tempBase + tempNoise).toFloat()

            samples.add(
                TelemetrySample(
                    id = "demo-temp-$stepCount",
                    metric = HistoricalMetric.AirTemperature,
                    canonicalValue = tempVal,
                    rawValue = null,
                    timestampEpochMillis = currentMillis,
                    origin = DataOrigin.Simulated,
                    calibrationState = CalibrationState.FactoryDefault
                )
            )

            // 2. Relative Humidity (%): inverse diurnal (45% day to 78% night)
            val humBase = 62.0f - (16.0f * sin(diurnalAngle).toFloat())
            val humNoise = (random.nextFloat() - 0.5f) * 1.5f
            val humVal = String.format(java.util.Locale.US, "%.1f", (humBase + humNoise).coerceIn(30f, 95f)).toFloat()

            samples.add(
                TelemetrySample(
                    id = "demo-hum-$stepCount",
                    metric = HistoricalMetric.Humidity,
                    canonicalValue = humVal,
                    rawValue = null,
                    timestampEpochMillis = currentMillis,
                    origin = DataOrigin.Simulated,
                    calibrationState = CalibrationState.FactoryDefault
                )
            )

            // 3. Soil Moisture Probe 1 (Raw ADC counts, uncalibrated surface depth ~5cm)
            // Higher ADC counts represent drier soil. Discrete irrigation drops count back to wet (~1650).
            probe1Raw += 1.2f + (random.nextFloat() * 1.5f)
            if (probe1Raw > 2450f) {
                probe1Raw = 1650f // Irrigation event
            }
            val p1Val = String.format(java.util.Locale.US, "%.0f", probe1Raw).toFloat()

            samples.add(
                TelemetrySample(
                    id = "demo-p1-$stepCount",
                    metric = HistoricalMetric.SoilMoistureProbe1Raw,
                    canonicalValue = p1Val,
                    rawValue = p1Val,
                    timestampEpochMillis = currentMillis,
                    origin = DataOrigin.Simulated,
                    calibrationState = CalibrationState.Uncalibrated
                )
            )

            // 4. Soil Moisture Probe 2 (Raw ADC counts, uncalibrated deep substrate ~15cm)
            // Dries slower; buffered from evaporation
            probe2Raw += 0.7f + (random.nextFloat() * 0.9f)
            if (probe1Raw <= 1660f) {
                // Soil deep probe reacts with slight lag to surface irrigation
                probe2Raw = 1750f
            } else if (probe2Raw > 2300f) {
                probe2Raw = 1800f
            }
            val p2Val = String.format(java.util.Locale.US, "%.0f", probe2Raw).toFloat()

            samples.add(
                TelemetrySample(
                    id = "demo-p2-$stepCount",
                    metric = HistoricalMetric.SoilMoistureProbe2Raw,
                    canonicalValue = p2Val,
                    rawValue = p2Val,
                    timestampEpochMillis = currentMillis,
                    origin = DataOrigin.Simulated,
                    calibrationState = CalibrationState.Uncalibrated
                )
            )

            // 5. Soil pH (standard agricultural band 6.4 - 7.0)
            val phNoise = (random.nextFloat() - 0.5f) * 0.2f
            val phVal = String.format(java.util.Locale.US, "%.2f", 6.75f + phNoise).toFloat()

            samples.add(
                TelemetrySample(
                    id = "demo-ph-$stepCount",
                    metric = HistoricalMetric.SoilPh,
                    canonicalValue = phVal,
                    rawValue = null,
                    timestampEpochMillis = currentMillis,
                    origin = DataOrigin.Simulated,
                    calibrationState = CalibrationState.Uncalibrated
                )
            )

            // 6. Ultrasonic Distance (Obstacle sonar 35cm - 55cm nominal)
            val distNoise = (random.nextFloat() - 0.5f) * 6.0f
            val distVal = String.format(java.util.Locale.US, "%.1f", (45.0f + distNoise).coerceIn(10f, 200f)).toFloat()

            samples.add(
                TelemetrySample(
                    id = "demo-dist-$stepCount",
                    metric = HistoricalMetric.UltrasonicDistance,
                    canonicalValue = distVal,
                    rawValue = null,
                    timestampEpochMillis = currentMillis,
                    origin = DataOrigin.Simulated,
                    calibrationState = CalibrationState.FactoryDefault
                )
            )

            currentMillis += stepMillis
            stepCount++
        }

        return samples
    }
}
