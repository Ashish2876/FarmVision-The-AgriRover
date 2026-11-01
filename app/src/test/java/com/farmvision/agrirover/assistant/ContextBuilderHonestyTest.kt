package com.farmvision.agrirover.assistant

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.assistant.context.ContextBuilder
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantHistoricalMetric
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBuilderHonestyTest {

    private val sampleTelemetry = RoverTelemetry(
        batteryPercent = 78,
        soilMoisture = 2850f, // Raw capacitive ADC value
        temperature = 26.4f,
        humidity = 58.2f,
        soilPh = 6.7f,
        dataOrigin = DataOrigin.LiveHardware
    )

    private val sampleAlert = AlertItem(
        id = "ALT-001",
        dedupKey = "soil-moist-low",
        category = AlertCategory.Soil,
        ruleId = "rule-soil-1",
        severity = AlertSeverity.Critical,
        title = "Critically Low Moisture",
        message = "Soil moisture ADC count is below safety baseline",
        sourceSensor = "Soil Moisture",
        triggerValue = 3200f,
        configuredThreshold = 3000f,
        origin = DataOrigin.LiveHardware,
        createdTimestampEpochMillis = 1730467200000L
    )

    @Test
    fun `when master consent is not granted, all context is strictly withheld`() {
        val consent = AssistantConsentPreferences(
            consentGranted = false,
            shareLiveTelemetry = true,
            shareAlerts = true,
            shareHistoricalAnalytics = true,
            shareCropProfile = true
        )

        val context = ContextBuilder.buildContext(
            consent = consent,
            telemetry = sampleTelemetry,
            activeAlerts = listOf(sampleAlert),
            cropProfile = CropProfile("Tomato", "Flowering")
        )

        assertNull("Sensor context must be null when master consent is false", context.sensorContext)
        assertTrue("Alert context must be empty when master consent is false", context.alertContext.isEmpty())
        assertTrue("Historical context must be empty when master consent is false", context.historicalContext.isEmpty())
        assertNull("Crop profile must be null when master consent is false", context.cropProfile)
    }

    @Test
    fun `when granular telemetry consent is denied, sensor context is null`() {
        val consent = AssistantConsentPreferences(
            consentGranted = true,
            shareLiveTelemetry = false,
            shareAlerts = true
        )

        val context = ContextBuilder.buildContext(
            consent = consent,
            telemetry = sampleTelemetry,
            activeAlerts = listOf(sampleAlert)
        )

        assertNull("Sensor context must be null when shareLiveTelemetry is false", context.sensorContext)
        assertEquals("Alert context should still be present", 1, context.alertContext.size)
    }

    @Test
    fun `sensor honesty enforces uncalibrated labels for moisture and pH`() {
        val consent = AssistantConsentPreferences(
            consentGranted = true,
            shareLiveTelemetry = true
        )

        val context = ContextBuilder.buildContext(
            consent = consent,
            telemetry = sampleTelemetry
        )

        val sc = context.sensorContext
        assertNotNull("Sensor context must be populated", sc)
        assertTrue("Moisture must be explicitly marked uncalibrated", sc!!.soilMoistureUncalibrated)
        assertEquals("UNCALIBRATED", sc.soilPhLabel)
        assertEquals(2850, sc.soilMoistureRaw)
        assertEquals(DataOrigin.LiveHardware.name, sc.dataOrigin)
        assertFalse("Reading is fresh", sc.isStale)
    }

    @Test
    fun `stale telemetry is explicitly tagged as stale`() {
        val consent = AssistantConsentPreferences(
            consentGranted = true,
            shareLiveTelemetry = true
        )

        val context = ContextBuilder.buildContext(
            consent = consent,
            telemetry = sampleTelemetry,
            isTelemetryStale = true,
            telemetryAgeSeconds = 120L
        )

        val sc = context.sensorContext
        assertNotNull(sc)
        assertTrue("Stale telemetry must set isStale = true", sc!!.isStale)
        assertEquals(120L, sc.readingAgeSeconds)
    }

    @Test
    fun `historical metrics and crop profile are filtered by granular consent`() {
        val metric = AssistantHistoricalMetric("Soil Moisture", "24 Hours", 1500f, 3200f, 2400f, 48, false)
        val profile = CropProfile("Capsicum", "Vegetative", "Loam", "2026-10-15")

        val consentWithProfileOnly = AssistantConsentPreferences(
            consentGranted = true,
            shareHistoricalAnalytics = false,
            shareCropProfile = true
        )

        val context = ContextBuilder.buildContext(
            consent = consentWithProfileOnly,
            historicalMetrics = listOf(metric),
            cropProfile = profile
        )

        assertTrue(context.historicalContext.isEmpty())
        assertNotNull(context.cropProfile)
        assertEquals("Capsicum", context.cropProfile?.cropType)
    }
}
