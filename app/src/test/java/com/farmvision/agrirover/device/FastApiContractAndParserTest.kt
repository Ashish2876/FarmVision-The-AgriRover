package com.farmvision.agrirover.device

import com.farmvision.agrirover.data.device.dto.FastApiJsonParser
import com.farmvision.agrirover.domain.device.model.JsonRawField
import com.farmvision.agrirover.domain.device.model.JsonTokenType
import com.farmvision.agrirover.domain.device.model.TimestampQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FastApiContractAndParserTest {

    private val receivedAt = 1793534400000L // 2026-11-01T12:00:00Z reference

    @Test
    fun parseTimestamp_case1_fieldAbsent() {
        val rawJson = """{"battery": 80}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertEquals(JsonRawField.Absent, result.sourceField)
        assertNull(result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.Absent, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case2_explicitNull() {
        val rawJson = """{"timestamp": null}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertEquals(JsonRawField.ExplicitNull, result.sourceField)
        assertNull(result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.ExplicitNull, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case3_boolean_typeMismatchNeverCoerced() {
        val rawJson = """{"timestamp": true}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertTrue(result.sourceField is JsonRawField.Present)
        val present = result.sourceField as JsonRawField.Present
        assertEquals(JsonTokenType.BOOLEAN, present.tokenType)
        assertEquals("true", present.rawValue)
        assertNull(result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.TypeMismatch, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case4_object_typeMismatchNeverCoerced() {
        val rawJson = """{"timestamp": {"uptime": 4500}}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertTrue(result.sourceField is JsonRawField.Present)
        val present = result.sourceField as JsonRawField.Present
        assertEquals(JsonTokenType.OBJECT, present.tokenType)
        assertNull(result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.TypeMismatch, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case5_emptyString() {
        val rawJson = """{"timestamp": ""}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertTrue(result.sourceField is JsonRawField.Present)
        assertNull(result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.EmptyString, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case6_numericZeroAndMonotonicUptime() {
        val rawJsonZero = """{"timestamp": 0}"""
        val fieldZero = FastApiJsonParser.extractRawField(rawJsonZero, "timestamp")
        val resultZero = FastApiJsonParser.parseTimestamp(fieldZero, receivedAt)

        assertTrue(resultZero.sourceField is JsonRawField.Present)
        assertNull(resultZero.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.ZeroUptimeSkew, resultZero.timestampQuality)

        val rawJsonUptime = """{"timestamp": 12500}""" // 12.5 seconds boot uptime
        val fieldUptime = FastApiJsonParser.extractRawField(rawJsonUptime, "timestamp")
        val resultUptime = FastApiJsonParser.parseTimestamp(fieldUptime, receivedAt)
        assertNull(resultUptime.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.ZeroUptimeSkew, resultUptime.timestampQuality)
    }

    @Test
    fun parseTimestamp_case7_validUtcIso8601() {
        val rawJson = """{"timestamp": "2026-11-01T12:00:00.000Z"}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertTrue(result.sourceField is JsonRawField.Present)
        assertNotNull(result.sourceTimestampEpochMillis)
        assertEquals(receivedAt, result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.ValidUtc, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case8_futureDrift() {
        // Timestamp is 2 hours ahead of receivedAt
        val futureEpoch = receivedAt + (2 * 3600 * 1000L)
        val rawJson = """{"timestamp": $futureEpoch}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertNotNull(result.sourceTimestampEpochMillis)
        assertEquals(futureEpoch, result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.FutureDrift, result.timestampQuality)
    }

    @Test
    fun parseTimestamp_case9_malformedString() {
        val rawJson = """{"timestamp": "invalid_date_token"}"""
        val field = FastApiJsonParser.extractRawField(rawJson, "timestamp")
        val result = FastApiJsonParser.parseTimestamp(field, receivedAt)

        assertNull(result.sourceTimestampEpochMillis)
        assertEquals(TimestampQuality.Malformed, result.timestampQuality)
    }

    @Test
    fun parseTelemetry_preservesSensorReadingsAndRawAdc() {
        val rawJson = """
            {
                "rover_id": "agrirover-01",
                "timestamp": "2026-11-01T12:00:00Z",
                "battery_percent": 88,
                "battery_voltage": 12.5,
                "soil_moisture_raw": 2100,
                "soil_ph": 6.7,
                "temperature": 28.5,
                "humidity": 55.0,
                "light_lux": 52000,
                "ultrasonic_distance_cm": 65.0
            }
        """.trimIndent()

        val parsed = FastApiJsonParser.parseTelemetry(rawJson, receivedAt).getOrThrow()
        assertEquals("agrirover-01", parsed.roverId)
        assertEquals(88, parsed.batteryPercent)
        assertEquals(12.5f, parsed.batteryVoltage)
        assertEquals(2100, parsed.soilMoistureRawAdc)
        assertEquals(6.7f, parsed.soilPhRaw)
        assertEquals(28.5f, parsed.temperatureCelsius)
        assertEquals(55.0f, parsed.humidityPercent)
        assertEquals(52000, parsed.lightLux)
        assertEquals(65.0f, parsed.ultrasonicDistanceCm)
        assertEquals(rawJson, parsed.rawJson)
    }
}
