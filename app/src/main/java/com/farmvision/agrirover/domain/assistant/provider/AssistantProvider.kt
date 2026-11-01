package com.farmvision.agrirover.domain.assistant.provider

import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage

/**
 * Pluggable provider interface for AI Agriculture Assistant services.
 */
interface AssistantProvider {
    val providerName: String
    val isAvailable: Boolean

    /**
     * Sends a user query to the AI provider and returns the synthesized assistant turn.
     */
    suspend fun sendMessage(
        conversationId: String,
        clientMessageId: String,
        userMessage: String,
        context: AssistantContext
    ): AssistantMessage
}

