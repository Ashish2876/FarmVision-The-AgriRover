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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.DiagnosticsState
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import com.farmvision.agrirover.ui.settings.components.SettingsInfoRow
import com.farmvision.agrirover.ui.settings.components.SettingsRadioRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 2 — Rover & Device Settings Screen.
 * Software configuration, transport architecture, sensor catalog, and live diagnostics.
 * Strictly maintains ACTUATORS_ENABLED = 0 (zero physical actuator control capability).
 */
@Composable
fun RoverDeviceSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val diagnostics by viewModel.diagnosticsState.collectAsState()
    val scrollState = rememberScrollState()

    var roverNameInput by remember(settings.roverName) { mutableStateOf(settings.roverName) }
    var backendUrlInput by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "Rover & Device Settings",
                subtitle = "Identity • Architecture • Diagnostics",
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
            // Rover Identity
            FarmVisionSectionHeader(
                title = "Rover Identity",
                description = "Robotics unit naming and identifiers"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = roverNameInput,
                    onValueChange = {
                        roverNameInput = it
                        viewModel.setRoverName(it)
                    },
                    label = { Text("Rover Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                SettingsInfoRow(label = "Hardware Unit ID", value = "AGRI-ROVER-ESP32-01")
                SettingsInfoRow(label = "Firmware Architecture", value = "ESP-IDF FreeRTOS Dual-Core")
                SettingsInfoRow(label = "Safety Watchdog", value = "500ms Heartbeat Timeout")
                SettingsInfoRow(label = "Actuator Invariant", value = "ACTUATORS_ENABLED = 0 (Enforced)")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Transport Protocol Configuration
            FarmVisionSectionHeader(
                title = "Communication Architecture",
                description = "Configures telemetry streaming bridge and backend endpoint"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Active Transport Protocol",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                listOf("Wi-Fi SoftAP", "Bluetooth Low Energy (BLE)", "FastAPI Cloud Gateway").forEach { protocol ->
                    SettingsRadioRow(
                        label = protocol,
                        selected = settings.transportProtocol == protocol,
                        onClick = { viewModel.setTransportProtocol(protocol) }
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                FarmVisionDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                Text(
                    text = "Backend Gateway Endpoint",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Default for Android Emulator is http://10.0.2.2:8000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = {
                        backendUrlInput = it
                        viewModel.setBackendUrl(it)
                    },
                    label = { Text("FastAPI Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Live Diagnostics
            FarmVisionSectionHeader(
                title = "Connection Diagnostics",
                description = "Probe backend health and measure latency"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Verify connection to the FastAPI server running on the host machine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                when (diagnostics) {
                    is DiagnosticsState.Idle -> {
                        Button(
                            onClick = { viewModel.testBackendConnection(backendUrlInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Backend Connection")
                        }
                    }
                    is DiagnosticsState.Testing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Pinging ${backendUrlInput}...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is DiagnosticsState.Success -> {
                        val state = diagnostics as DiagnosticsState.Success
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Connection Successful",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                        Button(
                            onClick = { viewModel.testBackendConnection(backendUrlInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Re-test Connection")
                        }
                    }
                    is DiagnosticsState.Failure -> {
                        val state = diagnostics as DiagnosticsState.Failure
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Connection Failed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                        Button(
                            onClick = { viewModel.testBackendConnection(backendUrlInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Diagnostic Test")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Verified Sensor Capabilities
            FarmVisionSectionHeader(
                title = "Hardware Sensor Inventory",
                description = "Supported telemetry probes and electrical constraints"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(label = "Soil Moisture Probe", value = "Capacitive Analog ADC (0–4095)")
                SettingsInfoRow(label = "Soil pH Probe", value = "Analog Uncalibrated (0.0–14.0)")
                SettingsInfoRow(label = "Air Temperature & Humidity", value = "DHT22 Digital Probe")
                SettingsInfoRow(label = "Solar Panel Monitor", value = "Voltage Divider ADC (0–20V)")
                SettingsInfoRow(label = "Obstacle Detection", value = "HC-SR04 Sonar Rangefinder")
                SettingsInfoRow(label = "Battery Fuel Gauge", value = "Analog Voltage Monitor")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

