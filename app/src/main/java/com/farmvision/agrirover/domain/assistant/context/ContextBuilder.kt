package com.farmvision.agrirover.domain.assistant.context

import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantHistoricalMetric
import com.farmvision.agrirover.domain.assistant.model.AssistantSensorContext
import com.farmvision.agrirover.domain.assistant.model.CropProfile

/**
 * Pure, deterministic context aggregator for the AI Assistant.
 * Strictly enforces user consent boundaries, freshness flags, and sensor honesty invariants.
 */
object ContextBuilder {

    /**
     * Builds an [AssistantContext] payload governed strictly by user consent.
     */
    fun buildContext(
        consent: AssistantConsentPreferences,
        telemetry: RoverTelemetry? = null,
        isTelemetryStale: Boolean = false,
        telemetryAgeSeconds: Long? = null,
        activeAlerts: List<AlertItem> = emptyList(),
        historicalMetrics: List<AssistantHistoricalMetric> = emptyList(),
        cropProfile: CropProfile? = null
    ): AssistantContext {
        // Master switch: If consent has not been granted, transmit zero farm data.
        if (!consent.consentGranted) {
            return AssistantContext(
                consent = consent,
                sensorContext = null,
                alertContext = emptyList(),
                historicalContext = emptyList(),
                cropProfile = null
            )
        }

        // 1. Live Telemetry Context (only if granular consent granted)
        val sensorContext = if (consent.shareLiveTelemetry && telemetry != null) {
            AssistantSensorContext(
                batteryPercent = telemetry.batteryPercent.toFloat(),
                solarVolts = null,
                soilMoistureRaw = telemetry.soilMoisture.toInt(),
                soilMoistureUncalibrated = true,
                soilMoistureLabel = "UNCALIBRATED_ADC",
                soilTemperature = telemetry.temperature,
                soilPhRaw = telemetry.soilPh,
                soilPhLabel = "UNCALIBRATED",
                airTemp = telemetry.temperature,
                airHumidity = telemetry.humidity,
                dataOrigin = telemetry.dataOrigin.name,
                isStale = isTelemetryStale,
                readingAgeSeconds = telemetryAgeSeconds
            )
        } else {
            null
        }

        // 2. Active Alerts Context (only if granular consent granted; max 5 most critical)
        val alertContext = if (consent.shareAlerts) {
            activeAlerts
                .filter { it.isActive }
                .sortedByDescending { it.severity.level }
                .take(5)
        } else {
            emptyList()
        }

        // 3. Historical Analytics Context (only if granular consent granted)
        val historicalContext = if (consent.shareHistoricalAnalytics) {
            historicalMetrics
        } else {
            emptyList()
        }

        // 4. Crop Profile (only if granular consent granted)
        val profile = if (consent.shareCropProfile) {
            cropProfile
        } else {
            null
        }

        return AssistantContext(
            consent = consent,
            sensorContext = sensorContext,
            alertContext = alertContext,
            historicalContext = historicalContext,
            cropProfile = profile
        )
    }
}

