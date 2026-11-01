package com.farmvision.agrirover.ui.device.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.ui.device.DeviceManagementViewModel
import com.farmvision.agrirover.ui.device.components.ConnectionStateBadge
import com.farmvision.agrirover.ui.device.components.DarkCardBg
import com.farmvision.agrirover.ui.device.components.DarkSurface
import com.farmvision.agrirover.ui.device.components.ParrotGreen
import com.farmvision.agrirover.ui.device.components.TransportTypeSelector
import com.farmvision.agrirover.ui.device.components.WifiSignalBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    viewModel: DeviceManagementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val activeDevice by viewModel.activeDevice.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val statusSnapshot by viewModel.statusSnapshot.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device & Communication",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
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
            // 1. Active Rover Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeDevice.identity.deviceName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeDevice.identity.deviceId,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            WifiSignalBars(rssiDbm = statusSnapshot.wifiRssiDbm)
                        }

                        ConnectionStateBadge(connectionState = connectionState)

                        Text(
                            text = "Transport Mode",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        TransportTypeSelector(
                            selectedType = activeDevice.transportType,
                            onSelect = { viewModel.setTransportType(it) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (connectionState is ConnectionState.Connected) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnect() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Disconnect")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.connect() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (connectionState is ConnectionState.Failed) "Retry Connection" else "Connect",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Hub Navigation Tiles
            item {
                Text(
                    text = "Management & Controls",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavigationTile(
                        icon = Icons.Default.Info,
                        title = "Hardware Specifications & Capabilities",
                        subtitle = "Inspect sensor declarations, MAC address, and firmware",
                        onClick = onNavigateToDetails
                    )
                    NavigationTile(
                        icon = Icons.Default.PlayArrow,
                        title = "Connection Setup Wizard",
                        subtitle = "Configure Wi-Fi SoftAP and Cloud Gateway endpoints",
                        onClick = onNavigateToSetup
                    )
                    NavigationTile(
                        icon = Icons.Default.Settings,
                        title = "Communication Parameters",
                        subtitle = "Tune polling interval, timeouts, and auth tokens",
                        onClick = onNavigateToConfig
                    )
                    NavigationTile(
                        icon = Icons.Default.Build,
                        title = "Diagnostics & Link Health",
                        subtitle = "Latency metrics, packet loss, and Keystore hardware level",
                        onClick = onNavigateToDiagnostics
                    )
                }
            }

            // 3. Registered Device List
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Known Rover Devices",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(knownDevices) { profile ->
                val isSelected = profile.identity.deviceId == activeDevice.identity.deviceId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectDevice(profile.identity.deviceId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ParrotGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = profile.identity.deviceName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${profile.transportType.displayName} • ${profile.baseEndpointUrl}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = ParrotGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ParrotGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ParrotGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

