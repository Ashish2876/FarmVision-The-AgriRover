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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.device.model.TransportType
import com.farmvision.agrirover.ui.device.DeviceManagementViewModel
import com.farmvision.agrirover.ui.device.components.ConnectionStateBadge
import com.farmvision.agrirover.ui.device.components.DarkCardBg
import com.farmvision.agrirover.ui.device.components.DarkSurface
import com.farmvision.agrirover.ui.device.components.ParrotGreen
import com.farmvision.agrirover.ui.device.components.TransportTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSetupScreen(
    viewModel: DeviceManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val activeDevice by viewModel.activeDevice.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var endpointUrl by remember(activeDevice.baseEndpointUrl) {
        mutableStateOf(activeDevice.baseEndpointUrl)
    }
    var qrMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Connection Setup Wizard",
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
            // Mode Selection
            item {
                Text(
                    text = "Select Transport Architecture",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TransportTypeSelector(
                    selectedType = activeDevice.transportType,
                    onSelect = { viewModel.setTransportType(it) }
                )
            }

            // Endpoint Setup Card
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
                            text = if (activeDevice.transportType == TransportType.DirectEsp32SoftAp)
                                "Direct Wi-Fi SoftAP Configuration"
                            else
                                "FastAPI Cloud Gateway Configuration",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (activeDevice.transportType == TransportType.DirectEsp32SoftAp)
                                "Connect Android device Wi-Fi to rover SoftAP ('AgriRover-AP'). Default IP is 192.168.4.1."
                            else
                                "Connects over HTTPS to cloud FastAPI gateway. Requires valid Bearer auth token.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = endpointUrl,
                            onValueChange = { endpointUrl = it },
                            label = { Text("Base Endpoint URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ParrotGreen,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val updated = activeDevice.copy(baseEndpointUrl = endpointUrl)
                                    viewModel.communicationRepository.updateConfig(
                                        viewModel.communicationConfig.value
                                    )
                                    viewModel.connect()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save & Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    qrMessage = "QR Scanner active: Point camera at rover QR tag"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Scan Rover QR", color = ParrotGreen)
                            }
                        }

                        if (qrMessage != null) {
                            Text(
                                text = qrMessage!!,
                                color = ParrotGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Connection Status Feedback
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
                        Text(
                            text = "Live Connection State",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        ConnectionStateBadge(connectionState = connectionState)
                    }
                }
            }
        }
    }
}

