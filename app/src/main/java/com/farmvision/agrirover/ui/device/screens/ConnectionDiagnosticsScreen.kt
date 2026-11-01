package com.farmvision.agrirover.ui.device.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.device.model.KeystoreHardwareLevel
import com.farmvision.agrirover.ui.device.DeviceManagementViewModel
import com.farmvision.agrirover.ui.device.components.ConnectionStateBadge
import com.farmvision.agrirover.ui.device.components.DarkCardBg
import com.farmvision.agrirover.ui.device.components.DarkSurface
import com.farmvision.agrirover.ui.device.components.DiagnosticMetricTile
import com.farmvision.agrirover.ui.device.components.ParrotGreen
import com.farmvision.agrirover.ui.device.components.WifiSignalBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDiagnosticsScreen(
    viewModel: DeviceManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val statusSnapshot by viewModel.statusSnapshot.collectAsState()
    val latestTelemetry by viewModel.latestTelemetry.collectAsState()

    var showRawPayload by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diagnostics & Link Health",
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
                actions = {
                    IconButton(onClick = { viewModel.refreshDiagnostics() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ParrotGreen
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
            // Live Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Link Operational State",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ConnectionStateBadge(connectionState = connectionState)
                        }
                        WifiSignalBars(rssiDbm = statusSnapshot.wifiRssiDbm)
                    }
                }
            }

            // Metrics Grid
            item {
                Text(
                    text = "Real-time Telemetry Metrics",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticMetricTile(
                        title = "Latency",
                        value = "${statusSnapshot.lastPollLatencyMs} ms",
                        subtitle = "Roundtrip time",
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticMetricTile(
                        title = "Signal (RSSI)",
                        value = "${statusSnapshot.wifiRssiDbm} dBm",
                        subtitle = "Wi-Fi link strength",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticMetricTile(
                        title = "Uptime",
                        value = "${statusSnapshot.uptimeSeconds} s",
                        subtitle = "Continuous runtime",
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticMetricTile(
                        title = "Free Heap",
                        value = "${statusSnapshot.freeHeapBytes / 1024} KB",
                        subtitle = "ESP32 RAM available",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticMetricTile(
                        title = "Packets Received",
                        value = "${statusSnapshot.totalPacketsReceived}",
                        subtitle = "Total inbound",
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticMetricTile(
                        title = "Packets Dropped",
                        value = "${statusSnapshot.failedPacketsCount}",
                        subtitle = "Failure count",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Keystore Security Details
            item {
                Text(
                    text = "Cryptographic Security Level",
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val level = statusSnapshot.keystoreSecurityLevel
                        val (label, desc) = when (level) {
                            KeystoreHardwareLevel.StrongBoxHardware -> Pair(
                                "StrongBox Dedicated Hardware",
                                "Tamper-resistant dedicated hardware security module (HSM)."
                            )
                            KeystoreHardwareLevel.TeeHardware -> Pair(
                                "TEE / Secure Hardware",
                                "Isolated hardware enclave inside main system SoC."
                            )
                            KeystoreHardwareLevel.Software -> Pair(
                                "Software Keystore",
                                "Key material managed in software provider."
                            )
                            KeystoreHardwareLevel.RamOnlyFallback -> Pair(
                                "RAM-Only Ephemeral Fallback",
                                "Token held exclusively in memory; zero disk persistence."
                            )
                        }

                        Text(
                            text = label,
                            color = ParrotGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = desc,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Raw Wire JSON Payload Inspection
            item {
                Text(
                    text = "Raw Wire Telemetry Inspection",
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showRawPayload = !showRawPayload },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showRawPayload) Color.DarkGray else ParrotGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showRawPayload) "Hide Raw JSON Payload" else "Inspect Wire JSON Payload",
                                color = if (showRawPayload) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showRawPayload) {
                            val rawText = latestTelemetry?.rawJson ?: "No telemetry payload captured yet."
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = rawText,
                                    color = ParrotGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

