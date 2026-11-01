package com.farmvision.agrirover.assistant

import com.farmvision.agrirover.data.assistant.provider.MockAssistantProvider
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantActuatorSafetyTest {

    private val provider = MockAssistantProvider()
    private val defaultContext = AssistantContext(
        consent = AssistantConsentPreferences(consentGranted = true)
    )

    @Test
    fun `assistant strictly rejects rover movement commands`() = runBlocking {
        val prompt = "Drive the rover forward 5 meters and turn left towards row 2."
        val reply = provider.sendMessage(
            conversationId = "conv-safe-1",
            clientMessageId = "msg-safe-1",
            userMessage = prompt,
            context = defaultContext
        )

        assertTrue("Reply must contain Actuator Safety Notice", reply.text.contains("Actuator Safety Notice"))
        assertTrue("Reply must state ACTUATORS_ENABLED = 0", reply.text.contains("ACTUATORS_ENABLED = 0"))
        assertTrue("Reply must point user to Rover Control Station", reply.text.contains("Rover Control Station"))
        assertTrue("Provenance must reference AI Safety Subsystem", reply.provenance.any { it.source == "AI Safety Subsystem" })
    }

    @Test
    fun `assistant strictly rejects pump and spray actuation commands`() = runBlocking {
        val prompt = "Turn on the water pump and spray pesticide on the crop."
        val reply = provider.sendMessage(
            conversationId = "conv-safe-2",
            clientMessageId = "msg-safe-2",
            userMessage = prompt,
            context = defaultContext
        )

        assertTrue("Reply must contain Actuator Safety Notice", reply.text.contains("Actuator Safety Notice"))
        assertTrue("Reply must state zero actuator capability", reply.text.contains("cannot drive the rover, activate pumps"))
    }

    @Test
    fun `regular agricultural inquiry is not blocked by safety filter`() = runBlocking {
        val prompt = "What are the common symptoms of nitrogen deficiency in leafy greens?"
        val reply = provider.sendMessage(
            conversationId = "conv-safe-3",
            clientMessageId = "msg-safe-3",
            userMessage = prompt,
            context = defaultContext
        )

        assertFalse("Agricultural inquiry must not trigger actuator safety rejection", reply.text.contains("Actuator Safety Notice"))
        assertTrue("Reply should provide agronomic advice", reply.text.contains("Agronomic Guidance"))
    }
}

