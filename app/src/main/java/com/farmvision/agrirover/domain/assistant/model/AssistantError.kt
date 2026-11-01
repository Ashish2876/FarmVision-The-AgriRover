package com.farmvision.agrirover.domain.assistant.model

/**
 * Typed domain error hierarchy for the Assistant subsystem.
 * Exposes farmer-friendly explanatory text and eliminates secret leaks.
 */
sealed class AssistantError(open val userMessage: String) : Throwable(userMessage) {

    data class NetworkError(
        override val userMessage: String = "Unable to reach the backend AI gateway. Verify backend is running and network is connected."
    ) : AssistantError(userMessage)

    data class TimeoutError(
        override val userMessage: String = "Request timed out waiting for the AI assistant response. Please try again."
    ) : AssistantError(userMessage)

    data class MissingApiKeyError(
        override val userMessage: String = "Gemini API key is not configured on the backend server. Please configure GEMINI_API_KEY in backend/.env."
    ) : AssistantError(userMessage)

    data class QuotaExhaustedError(
        override val userMessage: String = "AI service rate limit or free-tier quota exceeded. Please wait a few moments before retrying."
    ) : AssistantError(userMessage)

    data class SafetyBlockedError(
        override val userMessage: String = "Request or response was blocked by safety guidelines."
    ) : AssistantError(userMessage)

    data class ServerError(
        val code: Int,
        override val userMessage: String
    ) : AssistantError(userMessage)

    data class StorageError(
        override val userMessage: String = "Failed to load or save conversations in local private storage."
    ) : AssistantError(userMessage)

    data class ActuatorSafetyViolation(
        override val userMessage: String = "Actuator operations are strictly blocked. The AI assistant has zero physical control capability."
    ) : AssistantError(userMessage)

    data class ConsentRequiredError(
        override val userMessage: String = "You must review and grant privacy consent before consulting the AI Assistant."
    ) : AssistantError(userMessage)
}

