package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.ReadingState
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.data.model.SoilMoistureDualProbeMapping
import com.farmvision.agrirover.data.time.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Phase 4 sensor domain models, canonical unit boundaries,
 * physical range validators, deterministic staleness calculations, and the dual-probe mapping rule.
 */
class SensorDomainUnitTest {

    @Test
    fun sensorType_canonicalValidityRanges_enforcedCorrectly() {
        // Soil Moisture (0% - 100%)
        assertTrue(SensorType.SoilMoisture.isWithinValidRange(0.0f))
        assertTrue(SensorType.SoilMoisture.isWithinValidRange(50.0f))
        assertTrue(SensorType.SoilMoisture.isWithinValidRange(100.0f))
        assertFalse(SensorType.SoilMoisture.isWithinValidRange(-0.1f))
        assertFalse(SensorType.SoilMoisture.isWithinValidRange(100.1f))

        // Air Temperature (-20°C - 70°C)
        assertTrue(SensorType.AirTemperature.isWithinValidRange(-20.0f))
        assertTrue(SensorType.AirTemperature.isWithinValidRange(25.0f))
        assertTrue(SensorType.AirTemperature.isWithinValidRange(70.0f))
        assertFalse(SensorType.AirTemperature.isWithinValidRange(-20.1f))
        assertFalse(SensorType.AirTemperature.isWithinValidRange(70.5f))

        // Relative Humidity (0% - 100%)
        assertTrue(SensorType.Humidity.isWithinValidRange(0.0f))
        assertTrue(SensorType.Humidity.isWithinValidRange(65.0f))
        assertTrue(SensorType.Humidity.isWithinValidRange(100.0f))
        assertFalse(SensorType.Humidity.isWithinValidRange(-1.0f))
        assertFalse(SensorType.Humidity.isWithinValidRange(101.0f))

        // Light Intensity (0 - 150,000 lx)
        assertTrue(SensorType.LightIntensity.isWithinValidRange(0.0f))
        assertTrue(SensorType.LightIntensity.isWithinValidRange(48500.0f))
        assertTrue(SensorType.LightIntensity.isWithinValidRange(150000.0f))
        assertFalse(SensorType.LightIntensity.isWithinValidRange(-1.0f))

        // Soil pH (0 - 14)
        assertTrue(SensorType.SoilPh.isWithinValidRange(0.0f))
        assertTrue(SensorType.SoilPh.isWithinValidRange(6.8f))
        assertTrue(SensorType.SoilPh.isWithinValidRange(14.0f))
        assertFalse(SensorType.SoilPh.isWithinValidRange(-0.5f))
        assertFalse(SensorType.SoilPh.isWithinValidRange(14.1f))

        // Ultrasonic Distance (2 - 400 cm)
        assertTrue(SensorType.UltrasonicDistance.isWithinValidRange(2.0f))
        assertTrue(SensorType.UltrasonicDistance.isWithinValidRange(45.0f))
        assertTrue(SensorType.UltrasonicDistance.isWithinValidRange(400.0f))
        assertFalse(SensorType.UltrasonicDistance.isWithinValidRange(1.9f))
        assertFalse(SensorType.UltrasonicDistance.isWithinValidRange(400.5f))
    }

    @Test
    fun sensorReading_validatedFactory_marksOutOfRangeAsInvalid() {
        val validReading = SensorReading.createValidated(
            sensorId = "test-sensor",
            sensorType = SensorType.SoilMoisture,
            value = 45.0f,
            timestampEpochMillis = 1000L
        )
        assertEquals(ReadingState.Available, validReading.state)
        assertTrue(validReading.isValuePhysicallyValid())
        assertEquals("45.0 %", validReading.formatDisplay())

        // Out-of-range reading must be marked Invalid and NEVER formatted as a valid measurement
        val invalidReading = SensorReading.createValidated(
            sensorId = "test-sensor",
            sensorType = SensorType.SoilMoisture,
            value = 150.0f,
            timestampEpochMillis = 1000L
        )
        assertEquals(ReadingState.Invalid, invalidReading.state)
        assertFalse(invalidReading.isValuePhysicallyValid())
        assertEquals("ERR (Invalid Range)", invalidReading.formatDisplay())
    }

    @Test
    fun sensorReading_stalenessEvaluation_withFakeTimeProvider() {
        val fakeClock = FakeTimeProvider(currentMillis = 100_000L)
        val initialTimestamp = 100_000L

        val reading = SensorReading.createValidated(
            sensorId = "dht22-temp",
            sensorType = SensorType.AirTemperature,
            value = 24.5f,
            timestampEpochMillis = initialTimestamp,
            nowEpochMillis = fakeClock.nowEpochMillis(),
            freshnessThresholdMillis = 60_000L // 60 seconds
        )

        // At 30 seconds: not stale
        fakeClock.advanceMillis(30_000L)
        assertFalse(reading.isStale(fakeClock.nowEpochMillis(), freshnessThresholdMillis = 60_000L))

        // At 61 seconds: stale
        fakeClock.advanceMillis(31_000L) // Total 61s elapsed
        assertTrue(reading.isStale(fakeClock.nowEpochMillis(), freshnessThresholdMillis = 60_000L))

        // Factory automatically marks stale if now - timestamp > threshold
        val staleReading = SensorReading.createValidated(
            sensorId = "dht22-temp",
            sensorType = SensorType.AirTemperature,
            value = 24.5f,
            timestampEpochMillis = initialTimestamp,
            nowEpochMillis = fakeClock.nowEpochMillis(),
            freshnessThresholdMillis = 60_000L
        )
        assertEquals(ReadingState.Stale, staleReading.state)
        assertEquals("24.5 °C (Stale)", staleReading.formatDisplay())
    }

    @Test
    fun sensorReading_unavailableReading_rendersSafelyWithoutCrashing() {
        val unavailable = SensorReading.createUnavailable(
            sensorId = "probe-unplugged",
            sensorType = SensorType.SoilPh,
            timestampEpochMillis = 50_000L
        )
        assertEquals(ReadingState.Unavailable, unavailable.state)
        assertEquals("-- pH (Unavailable)", unavailable.formatDisplay())
    }

    @Test
    fun soilMoistureDualProbeMapping_computesWeightedAverageCorrectly() {
        val t = 1_000_000L
        val probe1 = SensorReading.createValidated(
            sensorId = SoilMoistureDualProbeMapping.PROBE_1_ID,
            sensorType = SensorType.SoilMoisture,
            value = 50.0f,
            timestampEpochMillis = t
        )
        val probe2 = SensorReading.createValidated(
            sensorId = SoilMoistureDualProbeMapping.PROBE_2_ID,
            sensorType = SensorType.SoilMoisture,
            value = 40.0f,
            timestampEpochMillis = t
        )

        // 60% of probe 1 (30.0) + 40% of probe 2 (16.0) = 46.0%
        val unified = SoilMoistureDualProbeMapping.computeUnifiedMoisture(probe1, probe2, t)
        assertEquals(46.0f, unified.value, 0.05f)
        assertEquals(ReadingState.Available, unified.state)
        assertEquals("%", unified.canonicalUnit)
        assertEquals(DataOrigin.Simulated, unified.dataOrigin)
    }

    @Test
    fun soilMoistureDualProbeMapping_fallsBackGracefullyIfOneProbeFails() {
        val t = 1_000_000L
        val probe1Valid = SensorReading.createValidated(
            sensorId = SoilMoistureDualProbeMapping.PROBE_1_ID,
            sensorType = SensorType.SoilMoisture,
            value = 42.0f,
            timestampEpochMillis = t
        )
        val probe2Invalid = SensorReading.createValidated(
            sensorId = SoilMoistureDualProbeMapping.PROBE_2_ID,
            sensorType = SensorType.SoilMoisture,
            value = -10.0f, // Invalid
            timestampEpochMillis = t
        )

        // Should fall back to probe 1
        val unified = SoilMoistureDualProbeMapping.computeUnifiedMoisture(probe1Valid, probe2Invalid, t)
        assertEquals(42.0f, unified.value, 0.05f)
        assertEquals(ReadingState.Available, unified.state)

        // If both are unavailable / null
        val unavailableUnified = SoilMoistureDualProbeMapping.computeUnifiedMoisture(null, null, t)
        assertEquals(ReadingState.Unavailable, unavailableUnified.state)
    }
}
