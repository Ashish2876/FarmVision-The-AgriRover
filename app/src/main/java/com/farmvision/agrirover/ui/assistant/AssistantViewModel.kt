package com.farmvision.agrirover.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.assistant.context.ContextBuilder
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantConversation
import com.farmvision.agrirover.domain.assistant.model.AssistantError
import com.farmvision.agrirover.domain.assistant.model.AssistantHistoricalMetric
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import com.farmvision.agrirover.domain.assistant.provider.AssistantProvider
import com.farmvision.agrirover.domain.assistant.repository.AssistantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI state for the AI Agriculture Assistant screen.
 */
data class AssistantUiState(
    val conversations: List<AssistantConversation> = emptyList(),
    val currentConversationId: String? = null,
    val currentMessages: List<AssistantMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val consentPreferences: AssistantConsentPreferences = AssistantConsentPreferences(),
    val cropProfile: CropProfile = CropProfile(),
    val showConsentDialog: Boolean = false,
    val activeAlertContext: AlertItem? = null,
    val activeHistoricalContext: List<AssistantHistoricalMetric> = emptyList(),
    val suggestedPrompts: List<String> = listOf(
        "Explain my soil moisture ADC reading",
        "What is the best irrigation timing for my crops?",
        "How do I balance uncalibrated soil pH safely?",
        "Are there any active rover telemetry risks?"
    )
)

/**
 * ViewModel managing state transitions, concurrency locking, and context aggregation.
 */
class AssistantViewModel(
    private val repository: AssistantRepository,
    private val assistantProvider: AssistantProvider,
    private val telemetryProvider: () -> RoverTelemetry? = { null },
    private val isStaleProvider: () -> Boolean = { false },
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    // Supports explicit scope injection for clean JVM unit tests without Looper mocks
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        // Observe conversations and preferences from repository
        scope.launch {
            repository.getConversationsFlow().collect { convs ->
                _uiState.update { state ->
                    val activeId = state.currentConversationId ?: convs.firstOrNull()?.id
                    val activeConv = convs.firstOrNull { it.id == activeId }
                    state.copy(
                        conversations = convs,
                        currentConversationId = activeId,
                        currentMessages = activeConv?.messages ?: emptyList()
                    )
                }
            }
        }

        scope.launch {
            repository.getConsentPreferencesFlow().collect { prefs ->
                _uiState.update { it.copy(consentPreferences = prefs) }
            }
        }

        scope.launch {
            repository.getCropProfileFlow().collect { profile ->
                _uiState.update { it.copy(cropProfile = profile) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun selectConversation(conversationId: String) {
        val conv = _uiState.value.conversations.firstOrNull { it.id == conversationId }
        _uiState.update {
            it.copy(
                currentConversationId = conversationId,
                currentMessages = conv?.messages ?: emptyList(),
                error = null
            )
        }
    }

    fun startNewConversation() {
        scope.launch {
            val newConv = repository.createConversation("New Advisory Session")
            _uiState.update {
                it.copy(
                    currentConversationId = newConv.id,
                    currentMessages = emptyList(),
                    activeAlertContext = null,
                    error = null
                )
            }
        }
    }

    fun setAlertForDiagnosis(alert: AlertItem) {
        _uiState.update {
            it.copy(
                activeAlertContext = alert,
                inputText = "Explain this alert: ${alert.title} (Severity: ${alert.severity.name})"
            )
        }
    }

    fun setHistoricalAnalyticsContext(metrics: List<AssistantHistoricalMetric>) {
        _uiState.update {
            it.copy(
                activeHistoricalContext = metrics,
                inputText = "Analyze the historical telemetry trends for our field metrics."
            )
        }
    }

    fun openConsentDialog() {
        _uiState.update { it.copy(showConsentDialog = true) }
    }

    fun dismissConsentDialog() {
        _uiState.update { it.copy(showConsentDialog = false) }
    }

    fun updateConsent(preferences: AssistantConsentPreferences) {
        scope.launch {
            repository.updateConsentPreferences(preferences)
            _uiState.update { it.copy(consentPreferences = preferences, showConsentDialog = false) }
        }
    }

    fun updateCropProfile(profile: CropProfile) {
        scope.launch {
            repository.updateCropProfile(profile)
            _uiState.update { it.copy(cropProfile = profile) }
        }
    }

    fun deleteConversation(conversationId: String) {
        scope.launch {
            repository.deleteConversation(conversationId)
            val remaining = repository.getConversations()
            val nextActive = remaining.firstOrNull()
            _uiState.update {
                it.copy(
                    conversations = remaining,
                    currentConversationId = nextActive?.id,
                    currentMessages = nextActive?.messages ?: emptyList()
                )
            }
        }
    }

    fun clearAllConversations() {
        scope.launch {
            repository.clearAllConversations()
            _uiState.update {
                it.copy(
                    conversations = emptyList(),
                    currentConversationId = null,
                    currentMessages = emptyList()
                )
            }
        }
    }

    /**
     * Sends user inquiry with single in-flight lock, duplicate submit prevention, and idempotent retry.
     */
    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank() || state.isLoading) return

        // Check if consent has never been reviewed; prompt dialog if desired
        if (!state.consentPreferences.consentGranted && state.consentPreferences.hasAnyDataSharing) {
            _uiState.update { it.copy(showConsentDialog = true) }
            return
        }

        val convId = state.currentConversationId ?: UUID.randomUUID().toString()
        val clientMsgId = UUID.randomUUID().toString()

        val userMessage = AssistantMessage(
            id = clientMsgId,
            conversationId = convId,
            sender = MessageSender.USER,
            text = text,
            status = MessageStatus.SENT,
            clientMessageId = clientMsgId
        )

        // Clear input immediately and set loading
        _uiState.update {
            it.copy(
                inputText = "",
                isLoading = true,
                error = null,
                currentConversationId = convId
            )
        }

        scope.launch {
            // Persist user turn
            repository.addMessage(convId, userMessage)

            // Construct deterministic context governed strictly by user consent
            val context = ContextBuilder.buildContext(
                consent = _uiState.value.consentPreferences,
                telemetry = telemetryProvider(),
                isTelemetryStale = isStaleProvider(),
                activeAlerts = listOfNotNull(_uiState.value.activeAlertContext),
                historicalMetrics = _uiState.value.activeHistoricalContext,
                cropProfile = _uiState.value.cropProfile
            )

            try {
                val reply = assistantProvider.sendMessage(
                    conversationId = convId,
                    clientMessageId = clientMsgId,
                    userMessage = text,
                    context = context
                )
                repository.addMessage(convId, reply)
                _uiState.update { it.copy(isLoading = false, activeAlertContext = null) }
            } catch (e: AssistantError) {
                val failMsg = AssistantMessage(
                    conversationId = convId,
                    sender = MessageSender.ASSISTANT,
                    text = "⚠️ Advisory generation failed: ${e.userMessage}",
                    status = MessageStatus.FAILED,
                    errorMessage = e.userMessage,
                    clientMessageId = clientMsgId
                )
                repository.addMessage(convId, failMsg)
                _uiState.update { it.copy(isLoading = false, error = e.userMessage) }
            } catch (e: Exception) {
                val failMsg = AssistantMessage(
                    conversationId = convId,
                    sender = MessageSender.ASSISTANT,
                    text = "⚠️ Communication error: ${e.message ?: "Unknown error"}",
                    status = MessageStatus.FAILED,
                    errorMessage = e.message,
                    clientMessageId = clientMsgId
                )
                repository.addMessage(convId, failMsg)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun retryFailedMessage(message: AssistantMessage) {
        val state = _uiState.value
        if (state.isLoading) return

        val convId = message.conversationId
        _uiState.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            val conv = repository.getConversation(convId)
            val userText = conv?.messages?.lastOrNull { it.sender == MessageSender.USER }?.text
                ?: "Please provide agricultural guidance."

            val context = ContextBuilder.buildContext(
                consent = _uiState.value.consentPreferences,
                telemetry = telemetryProvider(),
                isTelemetryStale = isStaleProvider(),
                activeAlerts = listOfNotNull(_uiState.value.activeAlertContext),
                historicalMetrics = _uiState.value.activeHistoricalContext,
                cropProfile = _uiState.value.cropProfile
            )

            try {
                val reply = assistantProvider.sendMessage(
                    conversationId = convId,
                    clientMessageId = message.clientMessageId,
                    userMessage = userText,
                    context = context
                )
                repository.deleteConversation(convId)
                repository.addMessage(convId, reply)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
