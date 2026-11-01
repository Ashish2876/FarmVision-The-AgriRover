package com.farmvision.agrirover.ui.hardware.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.repository.JsonFileTelemetryHistoryRepository
import com.farmvision.agrirover.data.repository.SharedPreferencesHardwareSettingsRepository
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository
import com.farmvision.agrirover.ui.hardware.components.ControllerEndpointCard
import com.farmvision.agrirover.ui.hardware.components.HardwareCategoryFilterBar
import com.farmvision.agrirover.ui.hardware.components.HardwareComponentCard
import com.farmvision.agrirover.ui.hardware.components.HardwareSummaryRow
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.viewmodel.HardwareCenterViewModel
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Hardware Center Screen for the FarmVision AgriRover.
 * Displays the verified 23-component hardware catalog, independent controller endpoint statuses,
 * truthful connection metrics, persistent connection settings, and search/filtering capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareCenterScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HardwareCenterViewModel? = null,
    telemetryHistoryRepository: TelemetryHistoryRepository? = null
) {
    val context = LocalContext.current
    val hardwareViewModel = viewModel ?: remember {
        val repo = SharedPreferencesHardwareSettingsRepository(context)
        val historyRepo = telemetryHistoryRepository ?: JsonFileTelemetryHistoryRepository(context)
        HardwareCenterViewModel(settingsRepository = repo, telemetryHistoryRepository = historyRepo)
    }

    val uiState by hardwareViewModel.uiState.collectAsState()
    val statusColors = LocalAgriStatusColors.current

    // Detail Screen Navigation Handling
    if (uiState.selectedComponentId != null) {
        val selectedItem = uiState.items.firstOrNull { it.component.id == uiState.selectedComponentId }
        if (selectedItem != null) {
            BackHandler {
                hardwareViewModel.onComponentSelected(null)
            }
            ComponentDetailScreen(
                item = selectedItem,
                onNavigateBack = { hardwareViewModel.onComponentSelected(null) }
            )
            return
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hardware Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isDemoMode) {
                                "Simulation Active • 23 Components"
                            } else {
                                "Live Hardware • 23 Components"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isDemoMode) statusColors.demo else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { hardwareViewModel.refreshTelemetry() }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Telemetry Status"
                        )
                    }
                    IconButton(
                        onClick = { hardwareViewModel.setConnectionDialogOpen(true) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configure Hardware Connection"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = FarmVisionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                // Mode Banner
                Surface(
                    shape = RoundedCornerShape(FarmVisionSpacing.small),
                    color = if (uiState.isDemoMode) statusColors.demo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (uiState.isDemoMode) statusColors.demo.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmVisionSpacing.medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (uiState.isDemoMode) statusColors.demo else statusColors.warning,
                                modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
                            )
                            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                            Text(
                                text = if (uiState.isDemoMode) {
                                    "DEMO MODE ACTIVE — NOT CONNECTED TO HARDWARE"
                                } else {
                                    "LIVE HARDWARE MODE — DISCONNECTED"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isDemoMode) statusColors.demo else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isDemoMode) {
                                "Telemetry values are simulated for UI development. No commands are sent to physical microcontrollers."
                            } else {
                                "Searching for ESP32 DevKit V1 and ESP32-CAM on Wi-Fi AP. Components show Unknown until an authenticated response arrives."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Controller Endpoints Strip
            item {
                Text(
                    text = "Microcontroller Endpoints",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                Column(verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)) {
                    ControllerEndpointCard(
                        target = ControllerTarget.Esp32DevKitV1,
                        title = "ESP32 DevKit V1 (Main Controller)",
                        subtitle = "Sensors, Motors, Servos, Irrigation Relay",
                        connectionStatus = if (uiState.isDemoMode) EndpointConnectionStatus.Unknown else EndpointConnectionStatus.Disconnected,
                        endpointAddress = "${uiState.settings.mainEsp32Host}:${uiState.settings.mainEsp32Port}",
                        firmwareVersion = if (uiState.isDemoMode) "v1.0.0-demo (Simulated)" else null,
                        lastSeenText = if (uiState.isDemoMode) "Simulated" else "Never",
                        onClick = { hardwareViewModel.setConnectionDialogOpen(true) }
                    )

                    ControllerEndpointCard(
                        target = ControllerTarget.Esp32Cam,
                        title = "ESP32-CAM (Vision Controller)",
                        subtitle = "OV2640 Image Stream & Camera Endpoint",
                        connectionStatus = if (uiState.isDemoMode) EndpointConnectionStatus.Unknown else EndpointConnectionStatus.Disconnected,
                        endpointAddress = "${uiState.settings.esp32CamHost}:${uiState.settings.esp32CamPort}",
                        firmwareVersion = if (uiState.isDemoMode) "v1.0.0-demo (Simulated)" else null,
                        lastSeenText = if (uiState.isDemoMode) "Simulated" else "Never",
                        onClick = { hardwareViewModel.setConnectionDialogOpen(true) }
                    )
                }
            }

            // Truthful Summary Counter Row (Section 8 rules)
            item {
                Text(
                    text = "Device Health & Connection Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                HardwareSummaryRow(counts = uiState.summaryCounts)
            }

            // Search and Category Filter Strip
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { hardwareViewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search 23 inventory items (e.g., DHT22, TB6612, Servo)...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { hardwareViewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HardwareCategoryFilterBar(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { hardwareViewModel.onCategorySelected(it) }
                )
            }

            // Component Cards List
            if (uiState.filteredItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = FarmVisionSpacing.large),
                        shape = RoundedCornerShape(FarmVisionSpacing.medium),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FarmVisionSpacing.extraLarge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No matching hardware components",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try clearing your search query or selecting 'All (23)'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredItems, key = { it.component.id }) { item ->
                    HardwareComponentCard(
                        item = item,
                        onClick = { hardwareViewModel.onComponentSelected(item.component.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
            }
        }
    }

    // Connection Settings Dialog
    if (uiState.isConnectionDialogOpen) {
        HardwareConnectionDialog(
            initialSettings = uiState.settings,
            isTesting = uiState.isTestingConnection,
            diagnosticResult = uiState.diagnosticResult,
            onDismissRequest = { hardwareViewModel.setConnectionDialogOpen(false) },
            onTestConnection = { hardwareViewModel.testConnection(it) },
            onResetDefaults = { hardwareViewModel.resetSettingsToDefaults() },
            onSaveSettings = { updated ->
                hardwareViewModel.updateSettings(updated)
            }
        )
    }

    // Explicit Mode Switch Confirmation Dialog (Rule: DEMO never silently becomes LIVE)
    if (uiState.isModeSwitchConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { hardwareViewModel.cancelModeSwitch() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = statusColors.warning
                    )
                    Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                    Text(
                        text = "Confirm Switch to Live Hardware Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Switching to LIVE HARDWARE MODE requires an active Wi-Fi connection to the FarmVision Rover Access Point (SSID: FarmVision-Rover).\n\n" +
                        "In Live Mode, the app will dispatch network requests to configured microcontrollers. Unverified components will display as Unknown or Disconnected until an authenticated response arrives.\n\n" +
                        "Do you want to proceed with Live Hardware Mode?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { hardwareViewModel.confirmModeSwitch() }
                ) {
                    Text("Switch to Live Mode")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { hardwareViewModel.cancelModeSwitch() }
                ) {
                    Text("Stay in Demo Mode")
                }
            }
        )
    }
}
