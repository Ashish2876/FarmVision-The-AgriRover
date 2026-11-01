package com.farmvision.agrirover.alert

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.ReadingState
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.domain.alert.engine.DataQualityValidator
import com.farmvision.agrirover.domain.alert.model.DataQualityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataQualityValidatorTest {

    private val now = 1_000_000L

    @Test
    fun testValidTemperatureReading() {
        val reading = SensorReading(
            sensorId = "dht22-temp",
            sensorType = SensorType.AirTemperature,
            value = 25.0f,
            timestampEpochMillis = now - 5_000L,
            dataOrigin = DataOrigin.LiveHardware
        )
        val result = DataQualityValidator.validateReading(reading, now)
        assertEquals(DataQualityState.Valid, result)
    }

    @Test
    fun testMissingReadingWhenNullOrUnavailable() {
        val nullResult = DataQualityValidator.validateReading(null, now)
        assertEquals(DataQualityState.Missing, nullResult)

        val unavailReading = SensorReading(
            sensorId = "sonar",
            sensorType = SensorType.UltrasonicDistance,
            value = 0.0f,
            timestampEpochMillis = now,
            state = ReadingState.Unavailable
        )
        val unavailResult = DataQualityValidator.validateReading(unavailReading, now)
        assertEquals(DataQualityState.Missing, unavailResult)
    }

    @Test
    fun testInvalidReadingWhenNanOrOutOfRange() {
        val nanReading = SensorReading(
            sensorId = "temp",
            sensorType = SensorType.AirTemperature,
            value = Float.NaN,
            timestampEpochMillis = now
        )
        assertEquals(DataQualityState.Invalid, DataQualityValidator.validateReading(nanReading, now))

        val outOfBoundsReading = SensorReading(
            sensorId = "temp",
            sensorType = SensorType.AirTemperature,
            value = 150.0f, // DHT22 max is 70C
            timestampEpochMillis = now
        )
        assertEquals(DataQualityState.Invalid, DataQualityValidator.validateReading(outOfBoundsReading, now))
    }

    @Test
    fun testStaleReadingWhenOlderThanWindow() {
        val oldReading = SensorReading(
            sensorId = "temp",
            sensorType = SensorType.AirTemperature,
            value = 24.0f,
            timestampEpochMillis = now - 70_000L // 70s ago > 60s threshold
        )
        assertEquals(DataQualityState.Stale, DataQualityValidator.validateReading(oldReading, now))
    }

    @Test
    fun testLiveSoilProbeUncalibratedAndSaturationCheck() {
        val normalAdcReading = SensorReading(
            sensorId = "soil-p1",
            sensorType = SensorType.SoilMoisture,
            value = 1800.0f,
            timestampEpochMillis = now,
            dataOrigin = DataOrigin.LiveHardware
        )
        assertEquals(DataQualityState.Uncalibrated, DataQualityValidator.validateReading(normalAdcReading, now))

        val saturatedAdcReading = SensorReading(
            sensorId = "soil-p1",
            sensorType = SensorType.SoilMoisture,
            value = 4080.0f, // >= 4050
            timestampEpochMillis = now,
            dataOrigin = DataOrigin.LiveHardware
        )
        assertEquals(DataQualityState.Invalid, DataQualityValidator.validateReading(saturatedAdcReading, now))
        assertTrue(DataQualityValidator.isAdcElectricallySaturated(4080.0f))
        assertTrue(DataQualityValidator.isAdcElectricallySaturated(20.0f))
    }

    @Test
    fun testLivePhIsMarkedUncalibrated() {
        val phReading = SensorReading(
            sensorId = "soil-ph",
            sensorType = SensorType.SoilPh,
            value = 6.8f,
            timestampEpochMillis = now,
            dataOrigin = DataOrigin.LiveHardware
        )
        assertEquals(DataQualityState.Uncalibrated, DataQualityValidator.validateReading(phReading, now))
    }
}

