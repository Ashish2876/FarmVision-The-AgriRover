package com.farmvision.agrirover.data.assistant.provider

import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.InformationClass
import com.farmvision.agrirover.domain.assistant.model.InformationProvenance
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import com.farmvision.agrirover.domain.assistant.provider.AssistantProvider
import java.util.UUID

/**
 * Deterministic offline / demo provider for the FarmVision AI Assistant.
 * Strictly preserves sensor honesty and strictly rejects actuator operations.
 */
class MockAssistantProvider : AssistantProvider {

    override val providerName: String = "Mock / Demo Assistant Provider"
    override val isAvailable: Boolean = true

    private val actuatorKeywords = listOf(
        "drive", "move", "forward", "backward", "reverse", "turn",
        "steer", "pump", "water", "spray", "relay", "motor", "servo", "actuate", "speed"
    )

    override suspend fun sendMessage(
        conversationId: String,
        clientMessageId: String,
        userMessage: String,
        context: AssistantContext
    ): AssistantMessage {
        val lower = userMessage.lowercase().trim()

        // 1. STRICT ACTUATOR SAFETY INVARIANT
        if (actuatorKeywords.any { lower.contains(it) }) {
            return AssistantMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = MessageSender.ASSISTANT,
                text = "⚠️ **Actuator Safety Notice**: The AI Agriculture Assistant operates strictly in ADVISORY mode " +
                        "with zero actuator capability (`ACTUATORS_ENABLED = 0`).\n\n" +
                        "I cannot drive the rover, activate pumps, or toggle relays. " +
                        "Please use the manual Rover Control Station or Hardware Center interface for hardware operations.",
                timestampMillis = System.currentTimeMillis(),
                provenance = listOf(
                    InformationProvenance(
                        source = "AI Safety Subsystem",
                        infoClass = InformationClass.AI_SUGGESTION,
                        description = "Actuator isolation check: Command execution strictly forbidden."
                    )
                ),
                status = MessageStatus.SENT,
                clientMessageId = clientMessageId
            )
        }

        // 2. ALERT EXPLANATION
        if (context.consent.shareAlerts && context.alertContext.isNotEmpty()) {
            val alert = context.alertContext.first()
            return AssistantMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = MessageSender.ASSISTANT,
                text = "### Alert Diagnosis: ${alert.title}\n" +
                        "- **Severity**: ${alert.severity.name}\n" +
                        "- **Metric**: ${alert.sourceSensor} (Observed: ${alert.triggerValue ?: "N/A"} ${alert.canonicalUnit}, Threshold: ${alert.configuredThreshold ?: "N/A"})\n\n" +
                        "**Agronomic Meaning**: This alert indicates that ${alert.sourceSensor} has crossed configured threshold bounds. " +
                        "Prolonged deviation can cause crop stress.\n\n" +
                        "**Recommended Next Steps**:\n" +
                        "1. Visually check root zone probes for loose soil contact.\n" +
                        "2. Verify ambient weather and soil compaction.\n" +
                        "3. Do not apply heavy irrigation without confirming drainage.",
                timestampMillis = System.currentTimeMillis(),
                provenance = listOf(
                    InformationProvenance(
                        source = "AlertRepository (${alert.id})",
                        infoClass = if (alert.isSimulated) InformationClass.DEMO_SIMULATED else InformationClass.LIVE_TELEMETRY,
                        description = "Active farm alert context provided with user consent."
                    )
                ),
                status = MessageStatus.SENT,
                clientMessageId = clientMessageId
            )
        }

        // 3. SENSOR READING INTERPRETATION
        val sc = context.sensorContext
        if (context.consent.shareLiveTelemetry && sc != null) {
            val moistureDesc = sc.soilMoistureRaw?.let {
                "Raw ADC $it (Uncalibrated analog count; requires soil texture calibration curve)"
            } ?: "Unavailable"
            val phDesc = sc.soilPhRaw?.let {
                String.format("%.1f (Uncalibrated analog probe; verify with chemical test strips)", it)
            } ?: "Unavailable"
            val tempDesc = sc.airTemp?.let { String.format("%.1f °C", it) } ?: "Unavailable"
            val humDesc = sc.airHumidity?.let { String.format("%.1f %%", it) } ?: "Unavailable"
            val staleNotice = if (sc.isStale) "\n> ⚠️ *Notice: Telemetry data is stale and may not reflect current field conditions.*" else ""
            val originTag = sc.dataOrigin?.let { " [$it Mode]" } ?: ""

            return AssistantMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = MessageSender.ASSISTANT,
                text = "### Current Sensor Observations$originTag$staleNotice\n\n" +
                        "- **Soil Moisture**: $moistureDesc\n" +
                        "- **Soil pH**: $phDesc\n" +
                        "- **Air Temperature**: $tempDesc\n" +
                        "- **Relative Humidity**: $humDesc\n\n" +
                        "**Data Honesty Note**: Capacitive soil moisture readings reflect dielectric permittivity, not guaranteed volumetric water content. " +
                        "Do not interpret raw ADC counts as calibrated moisture percentages.",
                timestampMillis = System.currentTimeMillis(),
                provenance = listOf(
                    InformationProvenance(
                        source = "FarmVision Telemetry Subsystem",
                        infoClass = if (sc.dataOrigin == "Simulated" || sc.dataOrigin == "DEMO") InformationClass.DEMO_SIMULATED else InformationClass.LIVE_TELEMETRY,
                        description = "Sensor reading snapshot (origin: ${sc.dataOrigin}, stale: ${sc.isStale})."
                    )
                ),
                status = MessageStatus.SENT,
                clientMessageId = clientMessageId
            )
        }

        // 4. HISTORICAL TREND INTERPRETATION
        if (context.consent.shareHistoricalAnalytics && context.historicalContext.isNotEmpty()) {
            val trend = context.historicalContext.first()
            return AssistantMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = MessageSender.ASSISTANT,
                text = "### Historical Trend: ${trend.metricName} (${trend.rangeLabel})\n\n" +
                        "- Data points analyzed: ${trend.dataPointsCount}\n" +
                        "- Average: ${trend.avgValue?.let { String.format("%.2f", it) } ?: "N/A"}\n" +
                        "- Range: [${trend.minValue?.let { String.format("%.2f", it) } ?: "N/A"} to ${trend.maxValue?.let { String.format("%.2f", it) } ?: "N/A"}]\n\n" +
                        "**Observation**: Fluctuations follow expected diurnal environmental patterns. Maintain consistent monitoring.",
                timestampMillis = System.currentTimeMillis(),
                provenance = listOf(
                    InformationProvenance(
                        source = "TelemetryHistoryRepository",
                        infoClass = InformationClass.HISTORICAL_TELEMETRY,
                        description = "Historical trend aggregation over ${trend.rangeLabel}."
                    )
                ),
                status = MessageStatus.SENT,
                clientMessageId = clientMessageId
            )
        }

        // 5. GENERAL AGRONOMIC GUIDANCE
        return AssistantMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            sender = MessageSender.ASSISTANT,
            text = "Thank you for consulting the FarmVision AI Agriculture Assistant.\n\n" +
                    "Regarding your inquiry about: *\"$userMessage\"*\n\n" +
                    "**Agronomic Guidance**:\n" +
                    "- **Irrigation Scheduling**: Water early in the morning (6:00 AM – 8:00 AM) to optimize transpiration and minimize fungal spore germination.\n" +
                    "- **Soil Management**: Apply a 5–7 cm layer of organic mulch around rootzones to buffer thermal fluctuations and preserve moisture.\n" +
                    "- **Nutrient Balance**: Base fertilizer applications on periodic soil testing rather than routine feeding.\n\n" +
                    "*(Advisory note: General agronomic principles. Confirm with your regional extension specialist.)*",
            timestampMillis = System.currentTimeMillis(),
            provenance = listOf(
                InformationProvenance(
                    source = "General Agronomy Knowledge Base",
                    infoClass = InformationClass.GENERAL_KNOWLEDGE,
                    description = "Standard agricultural practice principles."
                )
            ),
            status = MessageStatus.SENT,
            clientMessageId = clientMessageId
        )
    }
}

