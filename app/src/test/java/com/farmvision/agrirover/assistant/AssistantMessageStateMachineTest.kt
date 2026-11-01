package com.farmvision.agrirover.assistant

import com.farmvision.agrirover.data.assistant.local.AssistantJsonSerializer
import com.farmvision.agrirover.data.assistant.repository.JsonFileAssistantRepository
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantConversation
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import com.farmvision.agrirover.domain.assistant.model.InformationClass
import com.farmvision.agrirover.domain.assistant.model.InformationProvenance
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AssistantMessageStateMachineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `conversation creation and message addition updates state and respects limits`() = runBlocking {
        val testFile = File(tempFolder.root, "assistant_test.json")
        val repository = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)

        val conv = repository.createConversation("Crop Diagnosis")
        assertEquals("Crop Diagnosis", conv.title)

        val userMsg = AssistantMessage(
            conversationId = conv.id,
            sender = MessageSender.USER,
            text = "Why are lower tomato leaves yellowing?",
            status = MessageStatus.SENT
        )
        val updatedConv1 = repository.addMessage(conv.id, userMsg)
        assertEquals(1, updatedConv1.messageCount)

        val assistantMsg = AssistantMessage(
            conversationId = conv.id,
            sender = MessageSender.ASSISTANT,
            text = "Yellowing lower leaves often indicate nitrogen mobility or over-irrigation.",
            status = MessageStatus.SENT,
            provenance = listOf(
                InformationProvenance(
                    source = "Agronomy KB",
                    infoClass = InformationClass.GENERAL_KNOWLEDGE,
                    description = "Nitrogen deficiency symptoms"
                )
            )
        )
        val updatedConv2 = repository.addMessage(conv.id, assistantMsg)
        assertEquals(2, updatedConv2.messageCount)
        assertEquals(MessageSender.ASSISTANT, updatedConv2.messages.last().sender)

        // Verify state is re-read cleanly from disk
        val reloadedRepo = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)
        val loadedConvs = reloadedRepo.getConversations()
        assertEquals(1, loadedConvs.size)
        assertEquals(2, loadedConvs.first().messages.size)
        assertEquals("Why are lower tomato leaves yellowing?", loadedConvs.first().messages.first().text)
    }

    @Test
    fun `message status transition to FAILED with error message is persisted`() = runBlocking {
        val testFile = File(tempFolder.root, "assistant_error_test.json")
        val repository = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)

        val conv = repository.createConversation("Status Test")
        val msg = AssistantMessage(
            conversationId = conv.id,
            sender = MessageSender.USER,
            text = "Testing error handling",
            status = MessageStatus.SENDING
        )
        repository.addMessage(conv.id, msg)

        // Transition to FAILED
        repository.updateMessageStatus(
            conversationId = conv.id,
            messageId = msg.id,
            status = MessageStatus.FAILED,
            errorMessage = "Backend timeout"
        )

        val updated = repository.getConversation(conv.id)
        assertNotNull(updated)
        val targetMsg = updated!!.messages.first { it.id == msg.id }
        assertEquals(MessageStatus.FAILED, targetMsg.status)
        assertEquals("Backend timeout", targetMsg.errorMessage)
    }

    @Test
    fun `repository enforces maximum 20 conversations limit`() = runBlocking {
        val testFile = File(tempFolder.root, "assistant_limits_test.json")
        val repository = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)

        for (i in 1..25) {
            repository.createConversation("Thread #$i")
        }

        val allConvs = repository.getConversations()
        assertEquals("Must strictly cap conversations at 20", 20, allConvs.size)
        assertEquals("Thread #25", allConvs.first().title)
    }

    @Test
    fun `repository enforces maximum 50 messages limit per conversation`() = runBlocking {
        val testFile = File(tempFolder.root, "assistant_msg_limits_test.json")
        val repository = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)

        val conv = repository.createConversation("Long Thread")
        for (i in 1..60) {
            val m = AssistantMessage(
                conversationId = conv.id,
                sender = if (i % 2 == 1) MessageSender.USER else MessageSender.ASSISTANT,
                text = "Message index $i"
            )
            repository.addMessage(conv.id, m)
        }

        val loaded = repository.getConversation(conv.id)
        assertNotNull(loaded)
        assertEquals("Must strictly cap messages at 50", 50, loaded!!.messageCount)
        assertEquals("Message index 60", loaded.messages.last().text)
    }

    @Test
    fun `chat request and response json serialization roundtrip preserves provenance`() {
        val context = AssistantContext(
            consent = AssistantConsentPreferences(consentGranted = true, shareLiveTelemetry = true),
            cropProfile = CropProfile("Maize", "Silking")
        )

        val requestJson = AssistantJsonSerializer.serializeChatRequest(
            clientMessageId = "req-99",
            conversationId = "conv-88",
            userMessage = "What is optimal moisture for maize during silking?",
            context = context,
            forceMock = true
        )

        assertTrue(requestJson.contains("\"client_message_id\": \"req-99\""))
        assertTrue(requestJson.contains("\"force_mock\": true"))
        assertTrue(requestJson.contains("\"crop_type\": \"Maize\""))

        // Simulate backend JSON response
        val mockResponseJson = """
            {
                "client_message_id": "req-99",
                "conversation_id": "conv-88",
                "reply_text": "Maize requires adequate soil moisture during silking to avoid poor kernel set.",
                "provenance": [
                    {
                        "source": "Agronomy Guide",
                        "information_class": "GENERAL_KNOWLEDGE",
                        "description": "Maize silking water requirements"
                    }
                ],
                "model_used": "mock-deterministic-v1"
            }
        """.trimIndent()

        val parsedMsg = AssistantJsonSerializer.parseChatResponse(mockResponseJson)
        assertEquals("conv-88", parsedMsg.conversationId)
        assertEquals("req-99", parsedMsg.clientMessageId)
        assertEquals(MessageSender.ASSISTANT, parsedMsg.sender)
        assertEquals(1, parsedMsg.provenance.size)
        assertEquals(InformationClass.GENERAL_KNOWLEDGE, parsedMsg.provenance.first().infoClass)
    }
}

