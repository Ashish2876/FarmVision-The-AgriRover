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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.device.DeviceManagementViewModel
import com.farmvision.agrirover.ui.device.components.DarkCardBg
import com.farmvision.agrirover.ui.device.components.DarkSurface
import com.farmvision.agrirover.ui.device.components.ParrotGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    viewModel: DeviceManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val activeDevice by viewModel.activeDevice.collectAsState()
    val caps = activeDevice.capabilities

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hardware Specifications",
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
            // Identity Section
            item {
                Text(
                    text = "Device Identification",
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
                        DetailRow(label = "Device Name", value = activeDevice.identity.deviceName)
                        DetailRow(label = "Device ID", value = activeDevice.identity.deviceId)
                        DetailRow(label = "MAC Address", value = activeDevice.identity.macAddress)
                        DetailRow(label = "Hardware Revision", value = activeDevice.identity.hardwareRevision)
                        DetailRow(label = "Firmware Build", value = activeDevice.identity.firmwareVersion)
                        DetailRow(label = "Default Transport", value = activeDevice.transportType.displayName)
                    }
                }
            }

            // Capabilities Section
            item {
                Text(
                    text = "Declared Capabilities",
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
                        CapabilityItem(name = "Telemetry Streaming (/api/v1/telemetry)", supported = caps.supportsTelemetry)
                        CapabilityItem(name = "Capabilities Query (/api/v1/capabilities)", supported = caps.supportsCapabilities)
                        CapabilityItem(name = "Status & Diagnostics (/api/v1/status)", supported = caps.supportsStatus)
                        DetailRow(label = "Maximum Polling Rate", value = "${caps.maxPollingHz} Hz")
                        CapabilityItem(
                            name = "Actuator & Motor Control",
                            supported = false,
                            subtitle = "Safety Interlocked (Zero Actuator Capability in Phase 11)"
                        )
                    }
                }
            }

            // Supported Sensors Section
            item {
                Text(
                    text = "Declared Sensors",
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
                        caps.availableSensors.forEach { sensor ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ParrotGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sensor,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CapabilityItem(name: String, supported: Boolean, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = Color.White, fontSize = 13.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Icon(
            imageVector = if (supported) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (supported) ParrotGreen else Color.Red,
            modifier = Modifier.size(18.dp)
        )
    }
}

