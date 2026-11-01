package com.farmvision.agrirover.assistant

import com.farmvision.agrirover.data.assistant.provider.BackendAssistantProvider
import com.farmvision.agrirover.data.assistant.repository.JsonFileAssistantRepository
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.network.StandardRoverHttpClient
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import com.farmvision.agrirover.ui.assistant.AssistantViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LiveBackendIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun isLocalBackendRunning(): Boolean {
        return try {
            val url = URL("http://127.0.0.1:8000/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    @Test
    fun `when backend is unreachable, Android UI transitions cleanly to error state`() = runBlocking {
        val testFile = File(tempFolder.root, "assistant_unreachable_test.json")
        val repository = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)
        val httpClient = StandardRoverHttpClient()
        val provider = BackendAssistantProvider(
            baseUrl = "http://127.0.0.1:9999", // Unreachable port
            httpClient = httpClient,
            isForceMockEnabled = false,
            timeoutMs = 1000L
        )

        val viewModel = AssistantViewModel(
            repository = repository,
            assistantProvider = provider,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.onInputTextChanged("What is optimal soil moisture for winter wheat?")
        viewModel.sendMessage()
        delay(1500)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.inputText)

        val messages = state.currentMessages
        assertTrue("Expected user message and failed assistant message", messages.size >= 2)
        val assistantTurn = messages.first { it.sender == MessageSender.ASSISTANT }
        assertEquals(MessageStatus.FAILED, assistantTurn.status)
        assertNotNull(state.error)
    }

    @Test
    fun `when local backend is running in mock mode, Android UI receives mock response with provenance`() = runBlocking {
        if (!isLocalBackendRunning()) {
            println("FastAPI backend is not running on port 8000. Skipping live socket test.")
            return@runBlocking
        }

        val testFile = File(tempFolder.root, "assistant_mock_live_test.json")
        val repository = JsonFileAssistantRepository(testFile, Dispatchers.Unconfined)
        val httpClient = StandardRoverHttpClient()
        val provider = BackendAssistantProvider(
            baseUrl = "http://127.0.0.1:8000",
            httpClient = httpClient,
            isForceMockEnabled = true // Explicit mock
        )

        val sampleTelemetry = RoverTelemetry(
            batteryPercent = 88,
            soilMoisture = 2800f,
            temperature = 24.0f,
            humidity = 55.0f,
            soilPh = 6.8f,
            dataOrigin = DataOrigin.LiveHardware
        )

        val viewModel = AssistantViewModel(
            repository = repository,
            assistantProvider = provider,
            telemetryProvider = { sampleTelemetry },
            isStaleProvider = { false },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.updateConsent(
            AssistantConsentPreferences(
                consentGranted = true,
                shareLiveTelemetry = true
            )
        )

        viewModel.onInputTextChanged("Explain my soil moisture")
        viewModel.sendMessage()
        delay(500)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(null, state.error)

        val assistantTurn = state.currentMessages.first { it.sender == MessageSender.ASSISTANT }
        assertEquals(MessageStatus.SENT, assistantTurn.status)
        assertTrue(assistantTurn.text.isNotBlank())
        assertTrue("Provenance should contain telemetry origin", assistantTurn.provenance.isNotEmpty())
    }
}

