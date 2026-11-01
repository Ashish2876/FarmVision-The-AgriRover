package com.farmvision.agrirover.ui.assistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences

private val ParrotGreen = Color(0xFF68C000)

@Composable
fun AssistantPrivacyConsentDialog(
    currentPreferences: AssistantConsentPreferences,
    onSavePreferences: (AssistantConsentPreferences) -> Unit,
    onDismiss: () -> Unit,
    onClearConversations: () -> Unit
) {
    var masterConsent by remember { mutableStateOf(currentPreferences.consentGranted) }
    var shareLive by remember { mutableStateOf(currentPreferences.shareLiveTelemetry) }
    var shareAlerts by remember { mutableStateOf(currentPreferences.shareAlerts) }
    var shareHistory by remember { mutableStateOf(currentPreferences.shareHistoricalAnalytics) }
    var shareCrop by remember { mutableStateOf(currentPreferences.shareCropProfile) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = ParrotGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Privacy & Data Controls", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "FarmVision AI Assistant uses cloud intelligence (Google Gemini mediated via our private FastAPI backend) to provide agronomic advice.\n\n" +
                            "No farm sensor or rover data is ever shared without your explicit consent.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Master Toggle
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Cloud Assistant", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Authorize cloud processing of inquiries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = masterConsent,
                            onCheckedChange = { masterConsent = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ParrotGreen)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Granular Farm Data Sharing:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.height(6.dp))

                // Granular 1: Live Telemetry
                ConsentSwitchRow(
                    title = "Live Sensor Telemetry",
                    description = "Shares ambient temp, humidity, raw ADC moisture counts, and uncalibrated pH",
                    checked = shareLive && masterConsent,
                    enabled = masterConsent,
                    onCheckedChange = { shareLive = it }
                )

                // Granular 2: Alerts
                ConsentSwitchRow(
                    title = "Active Farm Alerts",
                    description = "Shares threshold breach events to enable AI root-cause troubleshooting",
                    checked = shareAlerts && masterConsent,
                    enabled = masterConsent,
                    onCheckedChange = { shareAlerts = it }
                )

                // Granular 3: Historical Analytics
                ConsentSwitchRow(
                    title = "Historical Analytics",
                    description = "Shares 24h & 7-day trend summaries for environmental metrics",
                    checked = shareHistory && masterConsent,
                    enabled = masterConsent,
                    onCheckedChange = { shareHistory = it }
                )

                // Granular 4: Crop Profile
                ConsentSwitchRow(
                    title = "Crop & Soil Profile",
                    description = "Shares user-provided crop type, growth stage, and soil type",
                    checked = shareCrop && masterConsent,
                    enabled = masterConsent,
                    onCheckedChange = { shareCrop = it }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Clear History Option
                OutlinedButton(
                    onClick = onClearConversations,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete All Conversation History", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePreferences(
                        AssistantConsentPreferences(
                            consentGranted = masterConsent,
                            shareLiveTelemetry = shareLive && masterConsent,
                            shareAlerts = shareAlerts && masterConsent,
                            shareHistoricalAnalytics = shareHistory && masterConsent,
                            shareCropProfile = shareCrop && masterConsent
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Preferences")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConsentSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            Text(description, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ParrotGreen)
        )
    }
}
