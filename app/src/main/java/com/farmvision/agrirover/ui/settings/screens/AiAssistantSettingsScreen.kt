package com.farmvision.agrirover.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import com.farmvision.agrirover.ui.settings.components.SettingsInfoRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 3 — AI Assistant Settings Screen.
 * Displays Google Gemini configuration, active production model (gemini-3.5-flash-lite),
 * FastAPI backend mediation, and agronomic veracity policies.
 */
@Composable
fun AiAssistantSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "AI Assistant Settings",
                subtitle = "Google Gemini • Backend Gateway • Quota",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Production Model & Gateway
            FarmVisionSectionHeader(
                title = "AI Cloud Provider",
                description = "Configured generative intelligence engine"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(FarmVisionSpacing.medium))
                    Column {
                        Text(
                            text = "Google Gemini Cloud",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active Model: gemini-3.5-flash-lite",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                SettingsInfoRow(label = "Cloud Provider", value = "Google AI Studio")
                SettingsInfoRow(label = "Configured Model", value = "gemini-3.5-flash-lite")
                SettingsInfoRow(label = "Response Latency", value = "~3.4s (High Responsiveness)")
                SettingsInfoRow(label = "Backend Gateway", value = settings.backendUrl)
                SettingsInfoRow(label = "Direct Client Key Access", value = "Blocked (Zero Secrets on Client)")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Agronomic Veracity & Sensor Honesty
            FarmVisionSectionHeader(
                title = "Sensor Honesty & Veracity Rules",
                description = "Strict constraints governing AI agronomic recommendations"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "• Raw Capacitive Soil Moisture: Analog ADC counts (0–4095) measure dielectric permittivity and are NEVER interpreted as calibrated water percentages without custom soil-texture curves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Soil pH Probe: Treated as an uncalibrated analog signal. FarmVision AI reminds farmers to verify with chemical test strips before critical soil acidification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Actuator Safety Guarantee: The AI operates strictly as an advisory assistant and has ZERO capability to drive motors, spray chemicals, or trigger irrigation valves (ACTUATORS_ENABLED = 0).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Quota & Rate Limit Information
            FarmVisionSectionHeader(
                title = "Free-Tier Quota & Rate Limits",
                description = "Google AI Studio API usage tier"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(label = "Plan Tier", value = "Google AI Studio Free Tier")
                SettingsInfoRow(label = "Quota Partitioning", value = "Per Model ID (Separate Allocations)")
                SettingsInfoRow(label = "Daily Request Cap", value = "20 Requests / Day per Model")
                SettingsInfoRow(label = "Automatic Reset", value = "Midnight UTC (~13h Cycle)")

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                Text(
                    text = "When free-tier limits are reached, the assistant presents a friendly reset notice with the exact retry duration calculated from Google's RetryInfo payload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Privacy Link Card
            FarmVisionSectionHeader(
                title = "Privacy & Consent Settings",
                description = "Manage granular data sharing with the AI Assistant"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToPrivacy
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(FarmVisionSpacing.medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage AI Data Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toggle live sensors, alerts, and historical trend context sharing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

