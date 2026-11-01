package com.farmvision.agrirover.ui.alert.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.alert.repository.PreferencesAlertSettingsRepository
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.ui.alert.AlertViewModel
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusWarning
import kotlinx.coroutines.launch

/**
 * Settings configuration screen for alert thresholds, debounce counts, and notification options.
 * Enforces strict input validation and restores factory development baselines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreen(
    viewModel: AlertViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val persistedConfig by viewModel.settingsConfig.collectAsState()

    // Editable draft configuration
    var draft by remember(persistedConfig) { mutableStateOf(persistedConfig) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    // Validation state
    val validationErrors = remember(draft) { PreferencesAlertSettingsRepository.validate(draft) }
    val isValid = validationErrors.isEmpty()

    // Permission launcher for Android 13+
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        scope.launch {
            if (granted) {
                snackbarHostState.showSnackbar("Notification permission granted.")
            } else {
                snackbarHostState.showSnackbar("Notification permission denied. In-app alerts remain active.")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alert Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRestoreDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Restore Defaults"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small)
        ) {
            // Agronomic Caveat Banner
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = DarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Development Baselines: All threshold settings are adjustable testing baselines, NOT universal agronomic truths. Real crop health depends on soil taxonomy, crop variety, growth stage, ambient climate, and physical probe calibration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notification Permission Card
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = if (hasNotifPermission) DarkGreen else StatusWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Push Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen, contentColor = DarkGreen),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Grant Permission", fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = if (hasNotifPermission) "Enabled" else "Disabled in OS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasNotifPermission) DarkGreen else StatusWarning
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Validation Error Box (if invalid)
            if (!isValid) {
                FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = StatusAlert,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configuration Validation Errors",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusAlert
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        validationErrors.forEach { err ->
                            Text(
                                text = "• $err",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAlert
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 1. Temperature Section
            SettingsSectionHeader(title = "Air Temperature (°C)")
            ThresholdInputPair(
                labelMin = "Min Critical (°C)",
                valueMin = draft.tempMinCritical.toString(),
                onMinChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(tempMinCritical = v) } },
                labelMax = "Max Critical (°C)",
                valueMax = draft.tempMaxCritical.toString(),
                onMaxChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(tempMaxCritical = v) } }
            )
            ThresholdInputPair(
                labelMin = "Min Warning (°C)",
                valueMin = draft.tempMinWarning.toString(),
                onMinChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(tempMinWarning = v) } },
                labelMax = "Max Warning (°C)",
                valueMax = draft.tempMaxWarning.toString(),
                onMaxChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(tempMaxWarning = v) } }
            )
            SingleNumberInput(
                label = "Hysteresis Recovery Margin (°C)",
                value = draft.tempHysteresis.toString(),
                onChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(tempHysteresis = v) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Relative Humidity Section
            SettingsSectionHeader(title = "Relative Humidity (%RH)")
            ThresholdInputPair(
                labelMin = "Min Critical (%)",
                valueMin = draft.humidityMinCritical.toString(),
                onMinChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(humidityMinCritical = v) } },
                labelMax = "Max Critical (%)",
                valueMax = draft.humidityMaxCritical.toString(),
                onMaxChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(humidityMaxCritical = v) } }
            )
            ThresholdInputPair(
                labelMin = "Min Warning (%)",
                valueMin = draft.humidityMinWarning.toString(),
                onMinChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(humidityMinWarning = v) } },
                labelMax = "Max Warning (%)",
                valueMax = draft.humidityMaxWarning.toString(),
                onMaxChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(humidityMaxWarning = v) } }
            )
            SingleNumberInput(
                label = "Hysteresis Margin (%)",
                value = draft.humidityHysteresis.toString(),
                onChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(humidityHysteresis = v) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Sonar Proximity Section
            SettingsSectionHeader(title = "Front Sonar Proximity (cm) — Read Only")
            ThresholdInputPair(
                labelMin = "Critical Distance (cm)",
                valueMin = draft.obstacleCriticalCm.toString(),
                onMinChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(obstacleCriticalCm = v) } },
                labelMax = "Warning Distance (cm)",
                valueMax = draft.obstacleWarningCm.toString(),
                onMaxChange = { it.toFloatOrNull()?.let { v -> draft = draft.copy(obstacleWarningCm = v) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Timeouts & Debounce Counts
            SettingsSectionHeader(title = "Telemetry Link Timeouts & Debounce")
            ThresholdInputPair(
                labelMin = "Link Latency Warning (s)",
                valueMin = draft.linkWarningTimeoutSeconds.toString(),
                onMinChange = { it.toLongOrNull()?.let { v -> draft = draft.copy(linkWarningTimeoutSeconds = v) } },
                labelMax = "Offline Critical Timeout (s)",
                valueMax = draft.offlineCriticalTimeoutSeconds.toString(),
                onMaxChange = { it.toLongOrNull()?.let { v -> draft = draft.copy(offlineCriticalTimeoutSeconds = v) } }
            )
            ThresholdInputPair(
                labelMin = "Breach Debounce Samples",
                valueMin = draft.confirmationCount.toString(),
                onMinChange = { it.toIntOrNull()?.let { v -> draft = draft.copy(confirmationCount = v) } },
                labelMax = "Recovery Samples",
                valueMax = draft.recoveryConfirmationCount.toString(),
                onMaxChange = { it.toIntOrNull()?.let { v -> draft = draft.copy(recoveryConfirmationCount = v) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { draft = persistedConfig },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Discard")
                }

                Button(
                    onClick = {
                        viewModel.updateThresholds(draft) { res ->
                            scope.launch {
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Thresholds updated successfully.")
                                } else {
                                    snackbarHostState.showSnackbar("Save failed: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    },
                    enabled = isValid && draft != persistedConfig,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ParrotGreen,
                        contentColor = DarkGreen
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation dialog for Restore Defaults
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Baseline Defaults?") },
            text = { Text("This will reset all threshold bounds, hysteresis margins, and debounce counts back to default development values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.restoreDefaultThresholds { res ->
                            scope.launch {
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Restored baseline defaults.")
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Defaults", color = StatusAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun ThresholdInputPair(
    labelMin: String,
    valueMin: String,
    onMinChange: (String) -> Unit,
    labelMax: String,
    valueMax: String,
    onMaxChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = valueMin,
            onValueChange = onMinChange,
            label = { Text(labelMin, fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = valueMax,
            onValueChange = onMaxChange,
            label = { Text(labelMax, fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
private fun SingleNumberInput(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true
    )
}

