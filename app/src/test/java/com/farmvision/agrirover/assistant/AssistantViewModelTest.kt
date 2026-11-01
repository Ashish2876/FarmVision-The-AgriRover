package com.farmvision.agrirover.assistant

import com.farmvision.agrirover.data.assistant.provider.MockAssistantProvider
import com.farmvision.agrirover.data.assistant.repository.JsonFileAssistantRepository
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantHistoricalMetric
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AssistantViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: JsonFileAssistantRepository
    private lateinit var provider: MockAssistantProvider
    private lateinit var viewModel: AssistantViewModel

    private val sampleTelemetry = RoverTelemetry(
        batteryPercent = 82,
        soilMoisture = 2700f,
        temperature = 25.0f,
        humidity = 60.0f,
        soilPh = 6.8f,
        dataOrigin = DataOrigin.LiveHardware
    )

    @Before
    fun setUp() {
        val file = File(tempFolder.root, "assistant_vm_test.json")
        repository = JsonFileAssistantRepository(file, Dispatchers.Unconfined)
        provider = MockAssistantProvider()
        viewModel = AssistantViewModel(
            repository = repository,
            assistantProvider = provider,
            telemetryProvider = { sampleTelemetry },
            isStaleProvider = { false },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `initial uiState contains default suggested prompts and no active conversation`() {
        val state = viewModel.uiState.value
        assertTrue("Suggested prompts must be provided", state.suggestedPrompts.isNotEmpty())
        assertFalse("Consent dialog must start closed", state.showConsentDialog)
        assertFalse("Must not start in loading state", state.isLoading)
        assertTrue("Must start with blank input", state.inputText.isBlank())
    }

    @Test
    fun `setAlertForDiagnosis populates active alert context and input text`() {
        val alert = AlertItem(
            id = "ALT-VM-01",
            dedupKey = "soil-low",
            category = AlertCategory.Soil,
            ruleId = "rule-1",
            severity = AlertSeverity.Critical,
            title = "Rootzone Moisture Alert",
            message = "Moisture below threshold",
            sourceSensor = "Soil Moisture",
            triggerValue = 3400f,
            configuredThreshold = 3000f,
            createdTimestampEpochMillis = 1730467200000L
        )

        viewModel.setAlertForDiagnosis(alert)
        val state = viewModel.uiState.value
        assertNotNull(state.activeAlertContext)
        assertEquals("ALT-VM-01", state.activeAlertContext?.id)
        assertTrue(state.inputText.contains("Rootzone Moisture Alert"))
    }

    @Test
    fun `setHistoricalAnalyticsContext populates historical metrics context`() {
        val metric = AssistantHistoricalMetric(
            metricName = "Air Temperature",
            rangeLabel = "7 Days",
            minValue = 18f,
            maxValue = 32f,
            avgValue = 24.5f,
            dataPointsCount = 168,
            insufficientData = false
        )

        viewModel.setHistoricalAnalyticsContext(listOf(metric))
        val state = viewModel.uiState.value
        assertEquals(1, state.activeHistoricalContext.size)
        assertTrue(state.inputText.contains("historical telemetry trends"))
    }

    @Test
    fun `dialog controls open and dismiss correctly`() {
        assertFalse(viewModel.uiState.value.showConsentDialog)
        viewModel.openConsentDialog()
        assertTrue(viewModel.uiState.value.showConsentDialog)
        viewModel.dismissConsentDialog()
        assertFalse(viewModel.uiState.value.showConsentDialog)
    }

    @Test
    fun `onInputTextChanged updates input state and clears previous error`() {
        viewModel.onInputTextChanged("Hello assistant")
        assertEquals("Hello assistant", viewModel.uiState.value.inputText)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `deleteConversation and clearAllConversations remove records from state`() = runBlocking {
        viewModel.startNewConversation()
        delay(50)

        val convId = viewModel.uiState.value.currentConversationId
        assertNotNull(convId)

        viewModel.deleteConversation(convId!!)
        delay(50)

        assertTrue(viewModel.uiState.value.conversations.isEmpty())
        assertNull(viewModel.uiState.value.currentConversationId)
    }
}
