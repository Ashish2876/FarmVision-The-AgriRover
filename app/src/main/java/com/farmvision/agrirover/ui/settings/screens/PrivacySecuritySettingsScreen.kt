package com.farmvision.agrirover.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import com.farmvision.agrirover.ui.settings.components.SettingsInfoRow
import com.farmvision.agrirover.ui.settings.components.SettingsToggleRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 4 — Privacy & Security Settings Screen.
 * Centralized consent controls directly reusing AssistantConsentPreferences.
 * Granular toggles govern what data is packed into the AI ContextBuilder payload.
 */
@Composable
fun PrivacySecuritySettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consent by viewModel.consentPreferences.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "Privacy & Security",
                subtitle = "Data Controls • Context Permissions",
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
            // Master AI Consent
            FarmVisionSectionHeader(
                title = "AI Assistant Consent",
                description = "Master permission for sharing farm context with Gemini AI"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsToggleRow(
                    title = "Allow AI Farm Context Sharing",
                    subtitle = if (consent.consentGranted) "Farm context enabled for AI agronomic advisory" else "No telemetry data will be shared with AI",
                    checked = consent.consentGranted,
                    onCheckedChange = { viewModel.toggleConsentGranted(it) }
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Granular Data Categories
            FarmVisionSectionHeader(
                title = "Granular Context Permissions",
                description = "Choose specific telemetry streams included in AI consultations"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsToggleRow(
                    title = "Live Sensor Telemetry",
                    subtitle = "Soil moisture ADC, pH, air temp, humidity, battery and solar levels",
                    checked = consent.shareLiveTelemetry,
                    onCheckedChange = { viewModel.toggleShareLiveTelemetry(it) },
                    enabled = consent.consentGranted
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                SettingsToggleRow(
                    title = "Active Farm Alerts",
                    subtitle = "Current threshold breaches and system diagnostics",
                    checked = consent.shareAlerts,
                    onCheckedChange = { viewModel.toggleShareAlerts(it) },
                    enabled = consent.consentGranted
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                SettingsToggleRow(
                    title = "Historical Analytics",
                    subtitle = "24-hour and 7-day rolling aggregates and sensor trends",
                    checked = consent.shareHistoricalAnalytics,
                    onCheckedChange = { viewModel.toggleShareHistoricalAnalytics(it) },
                    enabled = consent.consentGranted
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                SettingsToggleRow(
                    title = "User Crop Profile",
                    subtitle = "Plant varieties, growth stages, soil type, and planting dates",
                    checked = consent.shareCropProfile,
                    onCheckedChange = { viewModel.toggleShareCropProfile(it) },
                    enabled = consent.consentGranted
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Security Architecture & Data Protection
            FarmVisionSectionHeader(
                title = "Security & Storage Guarantees",
                description = "Hardware isolation and private local storage policies"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(label = "Cloud Credential Storage", value = "FastAPI Backend Only (Never on Phone)")
                SettingsInfoRow(label = "Local Chat Persistence", value = "Private App Files Directory (Encrypted)")
                SettingsInfoRow(label = "Actuator Circuit Isolation", value = "ACTUATORS_ENABLED = 0 (Physical Lock)")
                SettingsInfoRow(label = "Third-Party Telemetry Tracking", value = "None (Zero Analytics Trackers)")

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                Text(
                    text = "When consent is revoked or specific categories are disabled, the Assistant ContextBuilder completely omits those data blocks from the outbound JSON payload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

