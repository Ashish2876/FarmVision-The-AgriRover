package com.farmvision.agrirover.assistant

import com.farmvision.agrirover.data.assistant.provider.BackendAssistantProvider
import com.farmvision.agrirover.data.network.HttpResponse
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.RoverNetworkException
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantError
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackendAssistantProviderTest {

    private class FakeRoverHttpClient(
        var responseHandler: (url: String, body: String) -> HttpResponse
    ) : RoverHttpClient {
        override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse {
            throw UnsupportedOperationException()
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse {
            return responseHandler(url, body)
        }
    }

    private val emptyContext = AssistantContext(consent = AssistantConsentPreferences())

    @Test
    fun `successful Gemini response parses model, provenance, and delivered status`() = runBlocking {
        val fakeJson = """
            {
                "client_message_id": "msg-001",
                "conversation_id": "conv-100",
                "reply_text": "Wheat requires balanced moisture during tillering.",
                "provenance": [
                    {
                        "source": "Google Gemini (gemini-2.5-flash)",
                        "information_class": "AI_SUGGESTION",
                        "description": "Cloud generative reasoning."
                    }
                ],
                "model_used": "gemini-2.5-flash"
            }
        """.trimIndent()

        val fakeClient = FakeRoverHttpClient { _, _ ->
            HttpResponse(statusCode = 200, body = fakeJson, latencyMs = 120L)
        }

        val provider = BackendAssistantProvider(httpClient = fakeClient)
        val reply = provider.sendMessage(
            conversationId = "conv-100",
            clientMessageId = "msg-001",
            userMessage = "What is wheat watering guide?",
            context = emptyContext
        )

        assertEquals("conv-100", reply.conversationId)
        assertEquals("msg-001", reply.clientMessageId)
        assertEquals(MessageSender.ASSISTANT, reply.sender)
        assertEquals(MessageStatus.SENT, reply.status)
        assertEquals("Wheat requires balanced moisture during tillering.", reply.text)
        assertEquals(1, reply.provenance.size)
        assertEquals("Google Gemini (gemini-2.5-flash)", reply.provenance.first().source)
    }

    @Test
    fun `backend 503 missing API key maps cleanly to MissingApiKeyError`() = runBlocking {
        val errorJson = """
            {
                "error_code": "MISSING_API_KEY",
                "message": "Google Gemini API key is not configured on the backend server.",
                "details": {"status_code": 503}
            }
        """.trimIndent()

        val fakeClient = FakeRoverHttpClient { _, _ ->
            HttpResponse(statusCode = 503, body = errorJson, latencyMs = 15L)
        }

        val provider = BackendAssistantProvider(httpClient = fakeClient)
        try {
            provider.sendMessage(
                conversationId = "conv-100",
                clientMessageId = "msg-001",
                userMessage = "Hello",
                context = emptyContext
            )
            fail("Expected AssistantError.MissingApiKeyError")
        } catch (e: AssistantError.MissingApiKeyError) {
            assertTrue(e.message?.contains("Google Gemini API key is not configured") == true)
        }
    }

    @Test
    fun `backend 429 quota exhausted maps cleanly to QuotaExhaustedError`() = runBlocking {
        val errorJson = """
            {
                "error_code": "QUOTA_EXHAUSTED",
                "message": "Gemini rate limit or quota exceeded.",
                "details": {"status_code": 429}
            }
        """.trimIndent()

        val fakeClient = FakeRoverHttpClient { _, _ ->
            HttpResponse(statusCode = 429, body = errorJson, latencyMs = 20L)
        }

        val provider = BackendAssistantProvider(httpClient = fakeClient)
        try {
            provider.sendMessage("conv-100", "msg-001", "Hello", emptyContext)
            fail("Expected AssistantError.QuotaExhaustedError")
        } catch (e: AssistantError.QuotaExhaustedError) {
            assertTrue(e.message?.contains("rate limit") == true)
        }
    }

    @Test
    fun `network timeout maps cleanly to TimeoutError`() = runBlocking {
        val fakeClient = FakeRoverHttpClient { _, _ ->
            throw RoverNetworkException("Socket timeout", isTimeout = true)
        }

        val provider = BackendAssistantProvider(httpClient = fakeClient)
        try {
            provider.sendMessage("conv-100", "msg-001", "Hello", emptyContext)
            fail("Expected AssistantError.TimeoutError")
        } catch (e: AssistantError.TimeoutError) {
            assertNotNull(e)
        }
    }
}
