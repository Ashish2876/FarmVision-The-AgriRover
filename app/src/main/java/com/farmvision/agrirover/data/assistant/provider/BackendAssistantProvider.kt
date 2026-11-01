package com.farmvision.agrirover.data.assistant.provider

import com.farmvision.agrirover.data.assistant.local.AssistantJsonSerializer
import com.farmvision.agrirover.data.network.HttpResponse
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.RoverNetworkException
import com.farmvision.agrirover.data.network.StandardRoverHttpClient
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantError
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.provider.AssistantProvider

/**
 * Cloud AI Gateway Provider connecting to the FarmVision FastAPI Backend.
 * Strictly mediates all Google Gemini API communication through the backend service.
 * Never connects directly to Gemini; holds zero cloud API keys or credentials.
 */
class BackendAssistantProvider(
    private var baseUrl: String = "http://10.0.2.2:8000",
    private val httpClient: RoverHttpClient = StandardRoverHttpClient(),
    var isForceMockEnabled: Boolean = false,
    var timeoutMs: Long = 180000L
) : AssistantProvider {

    override val providerName: String = "FarmVision FastAPI Cloud Gateway"
    override val isAvailable: Boolean = true

    fun setBaseUrl(url: String) {
        this.baseUrl = url.trimEnd('/')
    }

    override suspend fun sendMessage(
        conversationId: String,
        clientMessageId: String,
        userMessage: String,
        context: AssistantContext
    ): AssistantMessage {
        val endpointUrl = "${baseUrl.trimEnd('/')}/api/v1/assistant/chat"
        val requestJson = AssistantJsonSerializer.serializeChatRequest(
            clientMessageId = clientMessageId,
            conversationId = conversationId,
            userMessage = userMessage,
            context = context,
            forceMock = isForceMockEnabled
        )

        val response: HttpResponse
        try {
            response = httpClient.post(
                url = endpointUrl,
                body = requestJson,
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json"
                ),
                timeoutMs = this.timeoutMs
            )
        } catch (e: RoverNetworkException) {
            if (e.isTimeout) {
                throw AssistantError.TimeoutError()
            } else {
                throw AssistantError.NetworkError("Failed to reach backend at $endpointUrl: ${e.message}")
            }
        } catch (e: Exception) {
            throw AssistantError.NetworkError("Network error: ${e.message}")
        }

        if (response.isSuccessful) {
            val body = response.body ?: ""
            return AssistantJsonSerializer.parseChatResponse(body)
        }

        // Map HTTP error status codes to typed domain errors
        val errorBody = response.body ?: ""
        val errorMsg = AssistantJsonSerializer.parseErrorResponse(errorBody, response.statusCode)

        when (response.statusCode) {
            503 -> throw AssistantError.MissingApiKeyError(errorMsg)
            429 -> throw AssistantError.QuotaExhaustedError(errorMsg)
            400, 422 -> throw AssistantError.SafetyBlockedError(errorMsg)
            else -> throw AssistantError.ServerError(response.statusCode, errorMsg)
        }
    }
}

