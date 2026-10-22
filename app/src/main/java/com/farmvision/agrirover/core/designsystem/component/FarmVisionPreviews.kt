package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.theme.FarmVisionTheme

/**
 * Master Preview Collection for FarmVision AgriRover Design System.
 * Covers dark mode, light mode, accessibility large text scale (2.0f),
 * and narrow screen width constraints (320dp).
 */

@Preview(name = "Buttons — Dark Theme", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun PreviewButtonsDark() {
    FarmVisionTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FarmVisionPrimaryButton(text = "Primary Trigger", onClick = {}, leadingIcon = Icons.Rounded.CheckCircle)
            FarmVisionSecondaryButton(text = "Secondary Tonal", onClick = {})
            FarmVisionOutlinedButton(text = "Outlined Action", onClick = {})
            FarmVisionTextButton(text = "Text Auxiliary Action", onClick = {})
            FarmVisionPrimaryButton(text = "Processing State", onClick = {}, isLoading = true)
        }
    }
}

@Preview(name = "Buttons — Light Theme", showBackground = true, backgroundColor = 0xFFF5F8F3)
@Composable
private fun PreviewButtonsLight() {
    FarmVisionTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FarmVisionPrimaryButton(text = "Primary Trigger", onClick = {}, leadingIcon = Icons.Rounded.CheckCircle)
            FarmVisionSecondaryButton(text = "Secondary Tonal", onClick = {})
            FarmVisionOutlinedButton(text = "Outlined Action", onClick = {})
            FarmVisionTextButton(text = "Text Auxiliary Action", onClick = {})
        }
    }
}

@Preview(name = "Buttons — 2.0x Large Font Accessibility Scale", fontScale = 2.0f, showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun PreviewButtonsLargeFontScale() {
    FarmVisionTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FarmVisionPrimaryButton(text = "Large Text Accessible", onClick = {})
            FarmVisionSecondaryButton(text = "Secondary Tonal", onClick = {})
        }
    }
}

@Preview(name = "Status Badges — All Variants", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun PreviewStatusBadges() {
    FarmVisionTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmVisionStatusBadge(status = FarmVisionStatusType.Online)
                FarmVisionStatusBadge(status = FarmVisionStatusType.Offline)
                FarmVisionStatusBadge(status = FarmVisionStatusType.Connecting)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmVisionStatusBadge(status = FarmVisionStatusType.Active)
                FarmVisionStatusBadge(status = FarmVisionStatusType.Idle)
                FarmVisionStatusBadge(status = FarmVisionStatusType.Warning)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmVisionStatusBadge(status = FarmVisionStatusType.Error)
                FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
            }
        }
    }
}

@Preview(name = "Cards & Headers — Dark Theme", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun PreviewCardsDark() {
    FarmVisionTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FarmVisionCard {
                FarmVisionCardHeader(
                    title = "Sensor Subsystem",
                    supportingText = "Rootzone capacitive array",
                    leadingIcon = Icons.Rounded.Sensors,
                    trailingAction = {
                        FarmVisionStatusBadge(status = FarmVisionStatusType.Active)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Subsurface soil metrics streaming at 10Hz. Automatic threshold protection active.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FarmVisionCardFooter(
                    statusContent = {
                        Text(text = "Sample Readings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    },
                    actionContent = {
                        FarmVisionTextButton(text = "View History", onClick = {})
                    }
                )
            }
        }
    }
}

@Preview(name = "Sensor Metrics — Narrow 320dp Screen", widthDp = 320, showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun PreviewMetricsNarrow() {
    FarmVisionTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FarmVisionMetricCard(
                label = FarmVisionMetricType.SoilMoisture.defaultLabel,
                value = "41.6",
                unit = FarmVisionMetricType.SoilMoisture.defaultUnit,
                icon = FarmVisionMetricType.SoilMoisture.icon,
                trend = FarmVisionTrendDirection.Rising,
                trendLabel = "+1.8%",
                isSampleData = true
            )
            FarmVisionMetricCard(
                label = FarmVisionMetricType.Temperature.defaultLabel,
                value = "26.4",
                unit = FarmVisionMetricType.Temperature.defaultUnit,
                icon = FarmVisionMetricType.Temperature.icon,
                trend = FarmVisionTrendDirection.Stable,
                trendLabel = "Stable",
                isSampleData = true
            )
        }
    }
}

@Preview(name = "State Components & Demo Banner", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun PreviewStates() {
    FarmVisionTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FarmVisionDemoBanner()
            FarmVisionLoadingState(message = "Acquiring satellite RTK lock...")
            FarmVisionErrorState(
                title = "Sensor Disconnected",
                message = "The I2C bus failed to acknowledge probe 0x48.",
                onRetry = {}
            )
            FarmVisionEmptyState(
                title = "No Past Mission Logs",
                message = "Completed field scans will be archived here.",
                actionText = "Create Mission",
                onActionClick = {}
            )
        }
    }
}
