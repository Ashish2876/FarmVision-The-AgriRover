package com.farmvision.agrirover.ui.device.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.device.model.DeviceCommunicationConfig
import com.farmvision.agrirover.domain.device.model.KeystoreHardwareLevel
import com.farmvision.agrirover.ui.device.DeviceManagementViewModel
import com.farmvision.agrirover.ui.device.components.DarkCardBg
import com.farmvision.agrirover.ui.device.components.DarkSurface
import com.farmvision.agrirover.ui.device.components.ParrotGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConfigurationScreen(
    viewModel: DeviceManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val config by viewModel.communicationConfig.collectAsState()
    val statusSnapshot by viewModel.statusSnapshot.collectAsState()

    var pollingInterval by remember(config.pollingIntervalMs) {
        mutableFloatStateOf(config.pollingIntervalMs.toFloat())
    }
    var requestTimeout by remember(config.requestTimeoutMs) {
        mutableFloatStateOf(config.requestTimeoutMs.toFloat())
    }
    var maxRetries by remember(config.maxRetries) {
        mutableFloatStateOf(config.maxRetries.toFloat())
    }

    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var tokenStatusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Communication Parameters",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkSurface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Polling Rate & Timeout
            item {
                Text(
                    text = "Polling & Timing Parameters",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Telemetry Polling Frequency: ${pollingInterval.toInt()} ms (${String.format("%.1f", 1000f / pollingInterval)} Hz)",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = pollingInterval,
                            onValueChange = { pollingInterval = it },
                            valueRange = 500f..5000f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = ParrotGreen,
                                activeTrackColor = ParrotGreen
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "HTTP Request Timeout: ${requestTimeout.toInt()} ms",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = requestTimeout,
                            onValueChange = { requestTimeout = it },
                            valueRange = 1000f..10000f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = ParrotGreen,
                                activeTrackColor = ParrotGreen
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Maximum Retry Budget: ${maxRetries.toInt()} attempts",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = maxRetries,
                            onValueChange = { maxRetries = it },
                            valueRange = 1f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = ParrotGreen,
                                activeTrackColor = ParrotGreen
                            )
                        )

                        Button(
                            onClick = {
                                viewModel.updateConfig(
                                    DeviceCommunicationConfig(
                                        pollingIntervalMs = pollingInterval.toLong(),
                                        requestTimeoutMs = requestTimeout.toLong(),
                                        maxRetries = maxRetries.toInt(),
                                        baseBackoffMs = config.baseBackoffMs,
                                        maxBackoffMs = config.maxBackoffMs,
                                        jitterFactor = config.jitterFactor
                                    )
                                )
                                tokenStatusMessage = "Configuration parameters applied."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply Parameters", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Security & Keystore Token Management
            item {
                Text(
                    text = "Security & Bearer Token",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val hardwareLevelText = when (statusSnapshot.keystoreSecurityLevel) {
                            KeystoreHardwareLevel.StrongBoxHardware -> "StrongBox Dedicated Hardware (Tamper-Resistant)"
                            KeystoreHardwareLevel.TeeHardware -> "TEE / Secure Hardware Backed"
                            KeystoreHardwareLevel.Software -> "Software Keystore Implementation"
                            KeystoreHardwareLevel.RamOnlyFallback -> "Volatile RAM Fallback (Memory Only)"
                        }

                        Text(
                            text = "Keystore Backing: $hardwareLevelText",
                            color = ParrotGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("FastAPI Bearer Token") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ParrotGreen,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (tokenInput.isNotBlank()) {
                                        viewModel.saveBearerToken(tokenInput.trim())
                                        tokenStatusMessage = "Token encrypted and stored in Android Keystore."
                                        tokenInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Store Token", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.clearBearerToken()
                                    tokenStatusMessage = "Token wiped from Keystore."
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Token")
                            }
                        }

                        if (tokenStatusMessage != null) {
                            Text(
                                text = tokenStatusMessage!!,
                                color = ParrotGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

