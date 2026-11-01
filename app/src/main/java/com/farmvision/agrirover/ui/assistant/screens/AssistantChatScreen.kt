package com.farmvision.agrirover.ui.assistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.assistant.AssistantViewModel
import com.farmvision.agrirover.ui.assistant.components.AssistantInputBar
import com.farmvision.agrirover.ui.assistant.components.MessageBubble
import com.farmvision.agrirover.ui.assistant.components.OfflineSensorSnapshotCard
import com.farmvision.agrirover.ui.assistant.components.SuggestedPromptsRow

private val ParrotGreen = Color(0xFF68C000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChatScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to bottom when message list expands
    LaunchedEffect(uiState.currentMessages.size) {
        if (uiState.currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.currentMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FarmVision AI Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Cloud Advisory • Gemini via FastAPI",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = ParrotGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Conversation", tint = ParrotGreen)
                    }
                    IconButton(onClick = { viewModel.openConsentDialog() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Privacy & Consent Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                if (uiState.currentMessages.isEmpty()) {
                    SuggestedPromptsRow(
                        prompts = uiState.suggestedPrompts,
                        onSelectPrompt = { prompt ->
                            viewModel.onInputTextChanged(prompt)
                        }
                    )
                }
                AssistantInputBar(
                    inputText = uiState.inputText,
                    isLoading = uiState.isLoading,
                    onInputChanged = { viewModel.onInputTextChanged(it) },
                    onSend = { viewModel.sendMessage() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sensor Telemetry Context & Sharing Indicator
            OfflineSensorSnapshotCard(
                sensorContext = null, // In live run, bound via ViewModel
                isSharingEnabled = uiState.consentPreferences.shareLiveTelemetry,
                onOpenConsent = { viewModel.openConsentDialog() }
            )

            // Active Alert Context Banner (if user navigated via "Explain this alert")
            if (uiState.activeAlertContext != null) {
                val alert = uiState.activeAlertContext!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Diagnosing Alert: ${alert.title}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Severity: ${alert.severity.name} • ${alert.sourceSensor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Message List
            if (uiState.currentMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🌱",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "FarmVision Agronomic Advisor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask questions regarding crop care, soil moisture readings, irrigation timing, and active alerts.\n\nAll rover controls remain manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(uiState.currentMessages) { message ->
                        MessageBubble(
                            message = message,
                            onRetry = { viewModel.retryFailedMessage(message) }
                        )
                    }
                }
            }
        }
    }

    // Consent Dialog
    if (uiState.showConsentDialog) {
        AssistantPrivacyConsentDialog(
            currentPreferences = uiState.consentPreferences,
            onSavePreferences = { viewModel.updateConsent(it) },
            onDismiss = { viewModel.dismissConsentDialog() },
            onClearConversations = { viewModel.clearAllConversations() }
        )
    }
}

