package com.farmvision.agrirover.ui.hardware.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.HardwareConnectionSettings
import com.farmvision.agrirover.ui.hardware.model.ConnectionDiagnosticResult
import com.farmvision.agrirover.ui.hardware.model.HardwareOperationMode
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Manual Hardware Connection Settings Dialog.
 * Allows configuring endpoints for both Main ESP32 and ESP32-CAM, auth bearer token,
 * communication timeouts, link diagnostic probes, and switching between Demo and Live modes.
 */
@Composable
fun HardwareConnectionDialog(
    initialSettings: HardwareConnectionSettings = HardwareConnectionSettings(),
    isTesting: Boolean = false,
    diagnosticResult: ConnectionDiagnosticResult? = null,
    onDismissRequest: () -> Unit,
    onTestConnection: (ControllerTarget) -> Unit,
    onResetDefaults: () -> Unit,
    onSaveSettings: (HardwareConnectionSettings) -> Unit
) {
    var mainHost by remember { mutableStateOf(initialSettings.mainEsp32Host) }
    var mainPortText by remember { mutableStateOf(initialSettings.mainEsp32Port.toString()) }
    var camHost by remember { mutableStateOf(initialSettings.esp32CamHost) }
    var camPortText by remember { mutableStateOf(initialSettings.esp32CamPort.toString()) }
    var authToken by remember { mutableStateOf(initialSettings.authToken) }
    var timeoutText by remember { mutableStateOf(initialSettings.connectionTimeoutMs.toString()) }
    var pollingText by remember { mutableStateOf(initialSettings.telemetryPollingIntervalMs.toString()) }
    var isDemoMode by remember { mutableStateOf(initialSettings.operationMode == HardwareOperationMode.Demo) }
    var isTokenVisible by remember { mutableStateOf(false) }

    val statusColors = LocalAgriStatusColors.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                Text(
                    text = "Hardware Connection Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Configure microcontroller endpoints, bearer authentication, and transport parameters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                // Operation Mode Toggle Card
                Surface(
                    shape = RoundedCornerShape(FarmVisionSpacing.small),
                    color = if (isDemoMode) statusColors.demo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, if (isDemoMode) statusColors.demo.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FarmVisionSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDemoMode) "Demo Simulation Mode" else "Live Hardware Mode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDemoMode) statusColors.demo else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isDemoMode) "Simulates telemetry locally" else "Requires physical Wi-Fi connection",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !isDemoMode,
                            onCheckedChange = { isDemoMode = !it }
                        )
                    }
                }

                if (!isDemoMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NOTE: Live mode requires direct connection to rover Wi-Fi AP (SSID: FarmVision-Rover).",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColors.warning
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                // Section: Main ESP32 DevKit V1
                Text(
                    text = "Main ESP32 DevKit V1 (Core Rover)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    OutlinedTextField(
                        value = mainHost,
                        onValueChange = { mainHost = it },
                        label = { Text("Host / IP") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = mainPortText,
                        onValueChange = { mainPortText = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                // Section: ESP32-CAM
                Text(
                    text = "ESP32-CAM (Vision Endpoint)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    OutlinedTextField(
                        value = camHost,
                        onValueChange = { camHost = it },
                        label = { Text("Camera Host / IP") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = camPortText,
                        onValueChange = { camPortText = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                // Section: Security & Authentication
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("Bearer Token (X-AgriRover-Token)") },
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (isTokenVisible) "Hide token" else "Show token"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                // Section: Timing & Timeouts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    OutlinedTextField(
                        value = timeoutText,
                        onValueChange = { timeoutText = it },
                        label = { Text("Timeout (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pollingText,
                        onValueChange = { pollingText = it },
                        label = { Text("Poll Rate (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                // Section: Link Diagnostic Probes
                Text(
                    text = "Endpoint Diagnostic Probes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    OutlinedButton(
                        onClick = { onTestConnection(ControllerTarget.Esp32DevKitV1) },
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Probe Main ESP32")
                    }
                    OutlinedButton(
                        onClick = { onTestConnection(ControllerTarget.Esp32Cam) },
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Probe Camera")
                    }
                }

                if (isTesting) {
                    Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                        Text("Testing network link...", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Diagnostic Result Box
                if (diagnosticResult != null) {
                    Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                    Surface(
                        shape = RoundedCornerShape(FarmVisionSpacing.small),
                        color = if (diagnosticResult.success) statusColors.online.copy(alpha = 0.10f) else statusColors.offline.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, if (diagnosticResult.success) statusColors.online.copy(alpha = 0.35f) else statusColors.offline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(FarmVisionSpacing.small),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (diagnosticResult.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (diagnosticResult.success) statusColors.online else statusColors.offline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                            Column {
                                Text(
                                    text = "${diagnosticResult.target.name}: ${if (diagnosticResult.success) "Reachable" else "Unreachable"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diagnosticResult.success) statusColors.online else statusColors.offline
                                )
                                Text(
                                    text = diagnosticResult.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (diagnosticResult.latencyMs != null) {
                                    Text(
                                        text = "Round-trip Latency: ${diagnosticResult.latencyMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedMainPort = mainPortText.toIntOrNull() ?: HardwareConnectionSettings.DEFAULT_MAIN_ESP32_PORT
                    val parsedCamPort = camPortText.toIntOrNull() ?: HardwareConnectionSettings.DEFAULT_ESP32_CAM_PORT
                    val parsedTimeout = timeoutText.toLongOrNull() ?: HardwareConnectionSettings.DEFAULT_TIMEOUT_MS
                    val parsedPolling = pollingText.toLongOrNull() ?: HardwareConnectionSettings.DEFAULT_POLLING_INTERVAL_MS

                    val updated = HardwareConnectionSettings(
                        operationMode = if (isDemoMode) HardwareOperationMode.Demo else HardwareOperationMode.Live,
                        mainEsp32Host = mainHost.trim(),
                        mainEsp32Port = parsedMainPort.coerceIn(1, 65535),
                        esp32CamHost = camHost.trim(),
                        esp32CamPort = parsedCamPort.coerceIn(1, 65535),
                        authToken = authToken.trim(),
                        connectionTimeoutMs = parsedTimeout.coerceAtLeast(100L),
                        telemetryPollingIntervalMs = parsedPolling.coerceAtLeast(50L)
                    )
                    onSaveSettings(updated)
                    onDismissRequest()
                }
            ) {
                Text("Apply & Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onResetDefaults) {
                    Text("Reset Defaults")
                }
                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}
