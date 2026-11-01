package com.farmvision.agrirover.ui.assistant.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.AssistantSensorContext
import com.farmvision.agrirover.domain.assistant.model.InformationClass
import com.farmvision.agrirover.domain.assistant.model.InformationProvenance
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ParrotGreenPrimary = Color(0xFF68C000)
private val ParrotGreenLight = Color(0xFF78E000)
private val NeutralCardDark = Color(0xFF1E241E)
private val NeutralCardLight = Color(0xFFF4F6F4)

@Composable
fun MessageBubble(
    message: AssistantMessage,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val isFailed = message.status == MessageStatus.FAILED
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = if (isUser) {
        ParrotGreenPrimary.copy(alpha = 0.85f)
    } else if (isFailed) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isUser) {
        Color.White
    } else if (isFailed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header (Assistant label or User label)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else "FarmVision AI",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatTimestamp(message.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = textColor
                )

                // Provenance Chips (for Assistant turns)
                if (!isUser && message.provenance.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ProvenanceSection(provenanceList = message.provenance)
                }

                // Error banner & Retry button
                if (isFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.errorMessage ?: "Failed to deliver",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Footer for Assistant: Copy action & Advisory note
                if (!isUser && !isFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advisory only • Controls manual",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = textColor.copy(alpha = 0.5f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    copied = true
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = "Copy text",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copied) "Copied" else "Copy",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProvenanceSection(provenanceList: List<InformationProvenance>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Information Provenance & Truth Sources:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            provenanceList.forEach { p ->
                ProvenanceChip(item = p)
            }
        }
    }
}

@Composable
fun ProvenanceChip(item: InformationProvenance) {
    val (chipBg, chipFg) = when (item.infoClass) {
        InformationClass.LIVE_TELEMETRY -> Color(0xFF2E7D32) to Color.White
        InformationClass.DEMO_SIMULATED -> Color(0xFFEF6C00) to Color.White
        InformationClass.HISTORICAL_TELEMETRY -> Color(0xFF1565C0) to Color.White
        InformationClass.USER_PROVIDED -> Color(0xFF00838F) to Color.White
        InformationClass.GENERAL_KNOWLEDGE -> Color(0xFF455A64) to Color.White
        InformationClass.AI_SUGGESTION -> ParrotGreenPrimary to Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(chipBg.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${item.infoClass.badgeLabel} • ${item.source}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = chipFg
        )
    }
}

@Composable
fun SuggestedPromptsRow(
    prompts: List<String>,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prompts) { prompt ->
            SuggestionChip(
                onClick = { onSelectPrompt(prompt) },
                label = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun OfflineSensorSnapshotCard(
    sensorContext: AssistantSensorContext?,
    isSharingEnabled: Boolean,
    onOpenConsent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSharingEnabled && sensorContext != null) Color(0xFF4CAF50) else Color(0xFFFFA000))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSharingEnabled) "Farm Context Active" else "Context Sharing Off",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = if (expanded) "Hide details ▲" else "View details ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParrotGreenPrimary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (!isSharingEnabled || sensorContext == null) {
                        Text(
                            text = "Telemetry sharing is currently withheld. Tap to configure permissions in Privacy Controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onOpenConsent,
                            colors = ButtonDefaults.buttonColors(containerColor = ParrotGreenPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Configure Sharing Consent", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text(
                            text = "• Soil Moisture: ADC ${sensorContext.soilMoistureRaw ?: "N/A"} (UNCALIBRATED)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Soil pH: ${sensorContext.soilPhRaw?.let { String.format("%.1f", it) } ?: "N/A"} (UNCALIBRATED)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Battery: ${sensorContext.batteryPercent?.let { String.format("%.0f%%", it) } ?: "N/A"} | Air: ${sensorContext.airTemp?.let { String.format("%.1f°C", it) } ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Origin: ${sensorContext.dataOrigin} | Stale: ${sensorContext.isStale}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantInputBar(
    inputText: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        "Ask about crops, soil readings, alerts...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ParrotGreenPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !isLoading) ParrotGreenPrimary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send inquiry",
                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
