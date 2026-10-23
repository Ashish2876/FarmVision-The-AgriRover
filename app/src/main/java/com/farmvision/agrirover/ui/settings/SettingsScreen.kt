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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * Settings & System Preferences Screen.
 * Configures connection protocols, units, simulation toggles, and displays system build metadata.
 * Unit selections are persistently stored in SharedPreferences and dynamically synchronize
 * telemetry rendering across Home, Rover, and Analytics.
 */
@Composable
fun SettingsScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsState()

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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Connection • Preferences • System Info",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FarmVisionDemoBanner(
                message = "PREFERENCES: Settings adjustments operate locally with SharedPreferences persistence. Physical rover connection parameters will activate in Stage D."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // 1. Connection Architecture
            FarmVisionSectionHeader(
                title = "Hardware Communication",
                description = "Rover transport protocol bridge"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Active Transport Protocol",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configures telemetry streaming bridge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                listOf("Wi-Fi SoftAP", "Bluetooth Low Energy (BLE)", "MQTT Broker").forEach { protocol ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.transportProtocol == protocol,
                            onClick = { viewModel.setTransportProtocol(protocol) }
                        )
                        Text(
                            text = protocol,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simulation Mock Engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Generates synthetic telemetry without physical hardware",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isSimulatedEngineActive,
                        onCheckedChange = { viewModel.setSimulatedEngineActive(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // 2. Units & Preferences (Persistent)
            FarmVisionSectionHeader(
                title = "Measurement Units",
                description = "Sensor reading formats and scales (Persisted)"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Temperature Scale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TemperatureUnit.entries.forEach { unit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.temperatureUnit == unit,
                                onClick = { viewModel.setTemperatureUnit(unit) }
                            )
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                Text(
                    text = "Speed & Distance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeedUnit.entries.forEach { unit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.speedUnit == unit,
                                onClick = { viewModel.setSpeedUnit(unit) }
                            )
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // 3. System & Application Metadata
            FarmVisionSectionHeader(
                title = "System Information",
                description = "Build specifications and platform contracts"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(label = "Application", value = "FarmVision AgriRover")
                InfoRow(label = "Package ID", value = "com.farmvision.agrirover")
                InfoRow(label = "Version", value = "1.0.0")
                InfoRow(label = "Target SDK", value = "Android 15 (API 35)")
                InfoRow(label = "UI Architecture", value = "Jetpack Compose • Material 3")
                InfoRow(label = "Shared State", value = "FarmViewModel • StateFlow")
                InfoRow(label = "Safety Watchdog", value = "500ms Heartbeat Cutoff")
                InfoRow(label = "Hardware Target", value = "ESP32 Robotics Platform")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
