package com.farmvision.agrirover.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.components.SettingsCategoryCard

/**
 * Settings & Configuration Management Center — Hub Screen.
 * Presents organized navigation into 8 functional modules:
 * 1. General Settings
 * 2. Rover & Device Settings
 * 3. AI Assistant Settings
 * 4. Privacy & Security
 * 5. Notification Settings
 * 6. Data Management
 * 7. Help & Support
 * 8. About FarmVision
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToGeneral: () -> Unit,
    onNavigateToRoverDevice: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHardwareCenter: (() -> Unit)? = null,
    onNavigateToAlertCenter: (() -> Unit)? = null,
    onNavigateToDeviceManagement: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Settings & Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Control Center • Hardware • AI • Privacy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FarmVisionDemoBanner(
                message = "SETTINGS CENTER: Centralized control for themes, device endpoints, Google Gemini 3.5 Flash Lite, privacy consent, and data storage."
            )

            // Primary Configuration Categories
            FarmVisionSectionHeader(
                title = "Application & Device Controls",
                description = "Core appearance, communication endpoints, and unit options"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 1. General Settings
            SettingsCategoryCard(
                title = "General Settings",
                description = "Visual theme (Light/Dark/System), language, measurement units, and preferences reset",
                icon = Icons.Rounded.Tune,
                onClick = onNavigateToGeneral
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 2. Rover & Device Settings
            SettingsCategoryCard(
                title = "Rover & Device Settings",
                description = "Rover identity, communication transport, ESP32 architecture, and backend diagnostics",
                icon = Icons.Rounded.Agriculture,
                onClick = onNavigateToRoverDevice
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // AI & Security Section
            FarmVisionSectionHeader(
                title = "Intelligence & Data Protection",
                description = "Google Gemini AI provider, privacy consent, and notifications"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 3. AI Assistant Settings
            SettingsCategoryCard(
                title = "AI Assistant Settings",
                description = "Google Gemini integration, quota tracking, and agronomic veracity policies",
                icon = Icons.Rounded.Psychology,
                badgeText = "gemini-3.5-flash-lite",
                onClick = onNavigateToAiAssistant
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 4. Privacy & Security
            SettingsCategoryCard(
                title = "Privacy & Security",
                description = "Granular context sharing permissions (sensors, alerts, analytics, crops)",
                icon = Icons.Rounded.Security,
                onClick = onNavigateToPrivacy
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 5. Notification Settings
            SettingsCategoryCard(
                title = "Notification Settings",
                description = "Alert sound, vibration feedback, Android channels, and threshold boundaries",
                icon = Icons.Rounded.Notifications,
                onClick = onNavigateToNotifications
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Storage & Information Section
            FarmVisionSectionHeader(
                title = "Data, Guides & Support",
                description = "Storage management, user documentation, and application specifications"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 6. Data Management
            SettingsCategoryCard(
                title = "Data Management",
                description = "Storage audit, AI conversation clearing, telemetry log pruning, and cache cleaning",
                icon = Icons.Rounded.Storage,
                onClick = onNavigateToDataManagement
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 7. Help & Support
            SettingsCategoryCard(
                title = "Help & Support",
                description = "User guides, network/AI troubleshooting, agronomic FAQ, and GitHub issue tracker",
                icon = Icons.Rounded.HelpOutline,
                onClick = onNavigateToHelpSupport
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 8. About FarmVision
            SettingsCategoryCard(
                title = "About FarmVision",
                description = "Version 1.0.0, engineering stack, open source notices, and system specs",
                icon = Icons.Rounded.Info,
                onClick = onNavigateToAbout
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Quick Diagnostic Links
            FarmVisionSectionHeader(
                title = "Hardware & Alert Portals",
                description = "Direct access to component inventory and active alert center"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            if (onNavigateToHardwareCenter != null) {
                SettingsCategoryCard(
                    title = "Hardware Center & Device Inventory",
                    description = "Inspect 23 catalog components, dual ESP32 microcontroller status, and pinouts",
                    icon = Icons.Rounded.Agriculture,
                    onClick = onNavigateToHardwareCenter
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            }

            if (onNavigateToAlertCenter != null) {
                SettingsCategoryCard(
                    title = "Alert Centre & Threshold Settings",
                    description = "Configure sensor limits, debounce counts, audit logs, and notification channels",
                    icon = Icons.Rounded.Notifications,
                    onClick = onNavigateToAlertCenter
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}
