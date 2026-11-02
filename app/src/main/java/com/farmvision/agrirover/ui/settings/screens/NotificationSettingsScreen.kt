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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.farmvision.agrirover.ui.settings.components.SettingsToggleRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 5 — Notification Settings Screen.
 * Configures Android notification channels, audio/vibration feedback, and threshold triggers.
 */
@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAlertSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val alertThresholds by viewModel.alertThresholds.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "Notification Settings",
                subtitle = "Alert Channels • Sound • Vibration",
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
            // Audio & Vibration Preferences
            FarmVisionSectionHeader(
                title = "Feedback Preferences",
                description = "Audio tone and haptic feedback for farm alerts"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsToggleRow(
                    title = "Alert Sound",
                    subtitle = "Play audible notification chimes for warnings and critical alerts",
                    checked = settings.isNotificationSoundEnabled,
                    onCheckedChange = { viewModel.setNotificationSound(it) }
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                SettingsToggleRow(
                    title = "Haptic Vibration",
                    subtitle = "Vibrate device when new threshold breaches are confirmed",
                    checked = settings.isNotificationVibrationEnabled,
                    onCheckedChange = { viewModel.setNotificationVibration(it) }
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Notification Channels
            FarmVisionSectionHeader(
                title = "Android Notification Channels",
                description = "Registered platform priority categories"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(label = "Critical Hardware Alerts", value = "High Priority (Heads-Up & Alarm)")
                SettingsInfoRow(label = "Agronomic Warnings", value = "Default Priority (Sound & Badge)")
                SettingsInfoRow(label = "System Diagnostics", value = "Low Priority (Silent in Shade)")
                SettingsInfoRow(label = "Repeat Cooldown", value = "5 min (Critical) • 15 min (Warning)")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Active Threshold Rules Summary
            FarmVisionSectionHeader(
                title = "Configured Alert Triggers",
                description = "Active electrical and environmental safety boundaries"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    label = "Soil Moisture Critical Minimum",
                    value = "< ${alertThresholds.soilMoistureMinCritical}% (Warn < ${alertThresholds.soilMoistureMinWarning}%)"
                )
                SettingsInfoRow(
                    label = "Air Temperature Range",
                    value = "${alertThresholds.tempMinWarning}°C – ${alertThresholds.tempMaxWarning}°C"
                )
                SettingsInfoRow(
                    label = "Air Relative Humidity Range",
                    value = "${alertThresholds.humidityMinWarning}% – ${alertThresholds.humidityMaxWarning}%"
                )
                SettingsInfoRow(
                    label = "Obstacle Proximity Warning",
                    value = "< ${alertThresholds.obstacleWarningCm} cm (Crit < ${alertThresholds.obstacleCriticalCm} cm)"
                )
                SettingsInfoRow(
                    label = "Soil pH Alert Status",
                    value = "Suppressed (Uncalibrated Analog Probe)"
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                OutlinedButton(
                    onClick = onNavigateToAlertSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tune Alert Limits & Debounce Counts")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

