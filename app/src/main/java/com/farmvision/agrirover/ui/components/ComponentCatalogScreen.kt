package com.farmvision.agrirover.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.StatusAlert

/**
 * Phase 2 Interactive Component Catalog Showcase.
 * Demonstrates all reusable UI components in live action with dynamic state updates.
 */
@Composable
fun ComponentCatalogScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Interactive Demo States
    var simulatedSpeed by remember { mutableFloatStateOf(3.8f) }
    var isEmergencyCutoff by remember { mutableStateOf(false) }
    var buttonLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DESIGN SYSTEM CATALOG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = DarkGreen
                    )
                    Text(
                        text = "Phase 2 Reusable Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                TactileButton(
                    text = "Foundation Screen",
                    onClick = onNavigateBack,
                    variant = TactileButtonVariant.Secondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Connection State Banner Component
            SectionHeader(title = "1. Telemetry Link Banner")
            ConnectionStateBanner(
                protocol = if (isConnected) ConnectionProtocol.WiFiSoftAP else ConnectionProtocol.Disconnected,
                isConnected = isConnected,
                latencyMs = if (isConnected) 24 else 0,
                signalDbm = if (isConnected) -58 else 0,
                quality = if (isConnected) LinkQuality.Excellent else LinkQuality.Offline,
                onReconnectClick = { isConnected = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Semantic AgriStatusChips Component
            SectionHeader(title = "2. Semantic Status Chips")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgriStatusChip(type = AgriStatusType.Nominal)
                AgriStatusChip(type = AgriStatusType.Warning)
                AgriStatusChip(type = AgriStatusType.Alert)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgriStatusChip(type = AgriStatusType.Info, customLabel = "SYNCING (10Hz)")
                AgriStatusChip(type = AgriStatusType.Standby, customLabel = "MOTORS DISARMED")
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Rover Speed Gauge Component
            SectionHeader(title = "3. Telemetry Speed & RPM Gauge")
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoverSpeedGauge(
                        currentValue = if (isEmergencyCutoff) 0.0f else simulatedSpeed,
                        maxValue = 6.0f,
                        unit = "km/h",
                        title = if (isEmergencyCutoff) "HALTED" else "Ground Speed",
                        secondaryNote = if (isEmergencyCutoff) "E-STOP ENGAGED" else "TARGET: 4.5 km/h"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Adjust Simulated Velocity (Interactive Slider)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = simulatedSpeed,
                        onValueChange = {
                            simulatedSpeed = it
                            if (it > 0f) isEmergencyCutoff = false
                        },
                        valueRange = 0f..6f,
                        colors = SliderDefaults.colors(
                            thumbColor = DarkGreen,
                            activeTrackColor = ParrotGreen,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Telemetry Metric Cards
            SectionHeader(title = "4. Telemetry Metric Cards")
            TelemetryMetricCard(
                title = "Rootzone Soil Moisture",
                value = "41.6",
                unit = "%",
                icon = Icons.Rounded.WaterDrop,
                trend = MetricTrend.Rising,
                trendLabel = "+1.8% / 1h",
                status = AgriStatusType.Nominal,
                progressFraction = 0.68f
            )

            Spacer(modifier = Modifier.height(12.dp))

            TelemetryMetricCard(
                title = "Battery Pack SoC",
                value = "88",
                unit = "%",
                icon = Icons.Rounded.BatteryChargingFull,
                trend = MetricTrend.Falling,
                trendLabel = "-2.4% / 1h",
                status = AgriStatusType.Nominal,
                progressFraction = 0.88f
            )

            Spacer(modifier = Modifier.height(12.dp))

            TelemetryMetricCard(
                title = "Motor Driver Temp",
                value = "49.2",
                unit = "°C",
                icon = Icons.Rounded.DeviceThermostat,
                trend = MetricTrend.Stable,
                trendLabel = "Nominal Range",
                status = AgriStatusType.Warning,
                progressFraction = 0.58f
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Emergency Stop Station Component
            SectionHeader(title = "5. Emergency Stop (E-Stop) Safety Core")
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isEmergencyCutoff) StatusAlert else MaterialTheme.colorScheme.outlineVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEmergencyCutoff) "POWER CUTOFF ACTIVE" else "AUTONOMOUS ACTUATOR ARMED",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (isEmergencyCutoff) StatusAlert else DarkGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    EmergencyStopButton(
                        onEmergencyStopTriggered = {
                            isEmergencyCutoff = true
                        },
                        isEngaged = isEmergencyCutoff
                    )

                    if (isEmergencyCutoff) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TactileButton(
                            text = "Reset & Re-arm Motors",
                            onClick = { isEmergencyCutoff = false },
                            variant = TactileButtonVariant.Secondary,
                            leadingIcon = Icons.Rounded.Refresh
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 6. Tactile Operational Buttons Gallery
            SectionHeader(title = "6. Tactile Operational Buttons")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TactileButton(
                    text = "Start Autonomous Scan Mission",
                    onClick = {},
                    variant = TactileButtonVariant.Primary,
                    leadingIcon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )

                TactileButton(
                    text = "Calibrate Soil Moisture Probe",
                    onClick = { buttonLoading = !buttonLoading },
                    variant = TactileButtonVariant.Tonal,
                    leadingIcon = Icons.Rounded.CheckCircle,
                    isLoading = buttonLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                TactileButton(
                    text = "Simulate Telemetry Link Severance",
                    onClick = { isConnected = !isConnected },
                    variant = TactileButtonVariant.Danger,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(ParrotGreen, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(name = "Catalog Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun ComponentCatalogScreenPreview() {
    FarmVisionTheme {
        ComponentCatalogScreen(onNavigateBack = {})
    }
}
