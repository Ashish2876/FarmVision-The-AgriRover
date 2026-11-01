package com.farmvision.agrirover.domain.assistant.model

import com.farmvision.agrirover.domain.alert.model.AlertItem
import java.util.UUID

/**
 * Veracity and classification hierarchy for information items presented by the assistant.
 */
enum class InformationClass(val displayName: String, val badgeLabel: String) {
    LIVE_TELEMETRY("Live Telemetry", "LIVE"),
    HISTORICAL_TELEMETRY("Historical Analytics", "HISTORICAL"),
    DEMO_SIMULATED("Simulated / Demo", "DEMO"),
    USER_PROVIDED("User Provided", "USER"),
    GENERAL_KNOWLEDGE("General Agronomy", "AGRONOMY"),
    AI_SUGGESTION("AI Synthesis", "AI ADVISORY")
}

/**
 * Traceable metadata explaining the source and honesty qualifications of advice.
 */
data class InformationProvenance(
    val source: String,
    val infoClass: InformationClass,
    val description: String
)

/**
 * Sender role for assistant messages.
 */
enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Message delivery status in the chat UI state machine.
 */
enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}

/**
 * A single conversational turn in an assistant session.
 */
data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val sender: MessageSender,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val provenance: List<InformationProvenance> = emptyList(),
    val status: MessageStatus = MessageStatus.SENT,
    val errorMessage: String? = null,
    val clientMessageId: String = id
)

/**
 * Conversational thread containing up to 50 messages.
 */
data class AssistantConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
    val messages: List<AssistantMessage> = emptyList()
) {
    val lastMessageText: String
        get() = messages.lastOrNull()?.text ?: "No messages"

    val messageCount: Int
        get() = messages.size
}

/**
 * Granular privacy consent controls governing cloud data transmission.
 */
data class AssistantConsentPreferences(
    val consentGranted: Boolean = false,
    val shareLiveTelemetry: Boolean = false,
    val shareAlerts: Boolean = false,
    val shareHistoricalAnalytics: Boolean = false,
    val shareCropProfile: Boolean = false
) {
    val hasAnyDataSharing: Boolean
        get() = consentGranted && (shareLiveTelemetry || shareAlerts || shareHistoricalAnalytics || shareCropProfile)
}

/**
 * User-provided crop and agronomic profile.
 */
data class CropProfile(
    val cropType: String = "",
    val growthStage: String = "",
    val soilType: String = "",
    val plantingDate: String = ""
)

/**
 * Sensor reading snapshot with strict uncalibrated indicators.
 */
data class AssistantSensorContext(
    val batteryPercent: Float? = null,
    val solarVolts: Float? = null,
    val soilMoistureRaw: Int? = null,
    val soilMoistureUncalibrated: Boolean = true,
    val soilMoistureLabel: String? = null,
    val soilTemperature: Float? = null,
    val soilPhRaw: Float? = null,
    val soilPhLabel: String? = "UNCALIBRATED",
    val airTemp: Float? = null,
    val airHumidity: Float? = null,
    val dataOrigin: String? = "UNKNOWN",
    val isStale: Boolean = false,
    val readingAgeSeconds: Long? = null
)

/**
 * Historical metric summary over an analytics window.
 */
data class AssistantHistoricalMetric(
    val metricName: String,
    val rangeLabel: String,
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val avgValue: Float? = null,
    val dataPointsCount: Int = 0,
    val insufficientData: Boolean = false
)

/**
 * Aggregated context package constructed purely and deterministically for assistant queries.
 */
data class AssistantContext(
    val consent: AssistantConsentPreferences,
    val sensorContext: AssistantSensorContext? = null,
    val alertContext: List<AlertItem> = emptyList(),
    val historicalContext: List<AssistantHistoricalMetric> = emptyList(),
    val cropProfile: CropProfile? = null
)

