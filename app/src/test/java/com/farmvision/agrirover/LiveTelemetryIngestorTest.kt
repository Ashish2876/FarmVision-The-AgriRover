package com.farmvision.agrirover

import com.farmvision.agrirover.data.analytics.LiveTelemetryIngestor
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.CalibrationState
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.repository.InMemoryTelemetryHistoryRepository
import com.farmvision.agrirover.domain.protocol.RoverTelemetryPacket
import com.farmvision.agrirover.domain.protocol.SensorReadingPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTelemetryIngestorTest {

    private val fixedNow = 1_700_000_000_000L

    @Test
    fun testLiveIngestion_validPacket() = runBlocking {
        val repo = InMemoryTelemetryHistoryRepository(writeIntervalSeconds = 0L)

        val packet = RoverTelemetryPacket(
            protocolVersion = "1.0.0",
            deviceId = "esp32-rover-main",
            firmwareVersion = "1.0.0",
            sequenceNumber = 42L,
            uptimeMs = 120_000L,
            diagnostics = mapOf("battery" to "nominal"),
            readings = mapOf(
                "hw-sensor-soil-moisture-probes" to SensorReadingPayload(
                    value = 45.0,
                    rawValue = 2150.0,
                    unit = "%",
                    isValid = true,
                    quality = "NOMINAL"
                ),
                "hw-sensor-soil-moisture-probe-2" to SensorReadingPayload(
                    value = 0.0, // Firmware provides 0.0 value for uncalibrated probe 2
                    rawValue = 1920.0,
                    unit = "counts",
                    isValid = true,
                    quality = "NOMINAL"
                ),
                "hw-sensor-dht22" to SensorReadingPayload(
                    value = 24.8,
                    rawValue = 24.8,
                    unit = "°C",
                    isValid = true,
                    quality = "NOMINAL"
                ),
                "hw-sensor-dht22-humidity" to SensorReadingPayload(
                    value = 58.5,
                    rawValue = 58.5,
                    unit = "%",
                    isValid = true,
                    quality = "NOMINAL"
                ),
                "hw-sensor-soil-ph" to SensorReadingPayload(
                    value = 6.8,
                    rawValue = 6.8,
                    unit = "pH",
                    isValid = true,
                    quality = "NOMINAL"
                ),
                "hw-sensor-ultrasonic" to SensorReadingPayload(
                    value = 42.0,
                    rawValue = 42.0,
                    unit = "cm",
                    isValid = true,
                    quality = "NOMINAL"
                )
            )
        )

        val ingestedCount = LiveTelemetryIngestor.ingestPacket(packet, repo, fixedNow)
        assertEquals(6, ingestedCount)

        val window = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)

        // Verify Probe 1: raw ADC count, uncalibrated, live
        val p1Samples = repo.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window)
        assertEquals(1, p1Samples.size)
        assertEquals(2150.0f, p1Samples[0].canonicalValue, 0.01f)
        assertEquals(2150.0f, p1Samples[0].rawValue!!, 0.01f)
        assertEquals(CalibrationState.Uncalibrated, p1Samples[0].calibrationState)
        assertEquals(DataOrigin.LiveHardware, p1Samples[0].origin)

        // Verify Probe 2: raw ADC count, uncalibrated, live (separate series from Probe 1!)
        val p2Samples = repo.getSamples(HistoricalMetric.SoilMoistureProbe2Raw, window)
        assertEquals(1, p2Samples.size)
        assertEquals(1920.0f, p2Samples[0].canonicalValue, 0.01f)
        assertEquals(1920.0f, p2Samples[0].rawValue!!, 0.01f)
        assertEquals(CalibrationState.Uncalibrated, p2Samples[0].calibrationState)
        assertEquals(DataOrigin.LiveHardware, p2Samples[0].origin)

        // Verify Air Temp
        val tempSamples = repo.getSamples(HistoricalMetric.AirTemperature, window)
        assertEquals(1, tempSamples.size)
        assertEquals(24.8f, tempSamples[0].canonicalValue, 0.01f)
    }

    @Test
    fun testLiveIngestion_saturatedAdcAndInvalidExcluded() = runBlocking {
        val repo = InMemoryTelemetryHistoryRepository(writeIntervalSeconds = 0L)

        val packet = RoverTelemetryPacket(
            protocolVersion = "1.0.0",
            deviceId = "esp32-rover-main",
            firmwareVersion = "1.0.0",
            sequenceNumber = 43L,
            uptimeMs = 125_000L,
            diagnostics = mapOf("battery" to "nominal"),
            readings = mapOf(
                // Saturated ADC counts (<= 50)
                "hw-sensor-soil-moisture-probes" to SensorReadingPayload(
                    value = 0.0,
                    rawValue = 25.0, // Saturated low
                    unit = "%",
                    isValid = true,
                    quality = "SATURATED"
                ),
                // Saturated ADC counts (>= 4050)
                "hw-sensor-soil-moisture-probe-2" to SensorReadingPayload(
                    value = 0.0,
                    rawValue = 4085.0, // Saturated high
                    unit = "counts",
                    isValid = true,
                    quality = "SATURATED"
                ),
                // Invalid reading
                "hw-sensor-dht22" to SensorReadingPayload(
                    value = 99.0, // Out of bounds temp
                    rawValue = 99.0,
                    unit = "°C",
                    isValid = false, // Explicitly invalid
                    quality = "FAULT"
                )
            )
        )

        val ingestedCount = LiveTelemetryIngestor.ingestPacket(packet, repo, fixedNow)
        assertEquals(0, ingestedCount)

        val window = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)
        assertTrue(repo.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window).isEmpty())
        assertTrue(repo.getSamples(HistoricalMetric.SoilMoistureProbe2Raw, window).isEmpty())
        assertTrue(repo.getSamples(HistoricalMetric.AirTemperature, window).isEmpty())
    }
}
