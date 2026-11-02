package com.farmvision.agrirover.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.AppLanguage
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.model.ThemeMode
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import com.farmvision.agrirover.ui.settings.components.SettingsConfirmationDialog
import com.farmvision.agrirover.ui.settings.components.SettingsRadioRow
import com.farmvision.agrirover.ui.settings.components.SettingsToggleRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 1 — General Settings Screen.
 * Controls visual theme, language, measurement units, and preference defaults.
 */
@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "General Settings",
                subtitle = "Appearance • Language • Preferences",
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
            // Appearance Theme Section
            FarmVisionSectionHeader(
                title = "Appearance",
                description = "Choose app color palette and surface contrast"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    SettingsRadioRow(
                        label = mode.label,
                        subtitle = when (mode) {
                            ThemeMode.System -> "Automatically match your Android device theme"
                            ThemeMode.Light -> "Vibrant Parrot Green and clean white surfaces"
                            ThemeMode.Dark -> "High contrast dark cockpit theme"
                        },
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Language Section
            FarmVisionSectionHeader(
                title = "Language",
                description = "Application localization"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEach { lang ->
                    SettingsRadioRow(
                        label = lang.label,
                        subtitle = "Default system language",
                        selected = settings.language == lang,
                        onClick = { viewModel.setLanguage(lang) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Measurement Units Section
            FarmVisionSectionHeader(
                title = "Measurement Units",
                description = "Telemetry reading formats and scales (Persisted)"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Temperature Scale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                TemperatureUnit.entries.forEach { unit ->
                    SettingsRadioRow(
                        label = unit.label,
                        selected = settings.temperatureUnit == unit,
                        onClick = { viewModel.setTemperatureUnit(unit) }
                    )
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
                Spacer(modifier = Modifier.height(4.dp))
                SpeedUnit.entries.forEach { unit ->
                    SettingsRadioRow(
                        label = unit.label,
                        selected = settings.speedUnit == unit,
                        onClick = { viewModel.setSpeedUnit(unit) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Simulation Engine
            FarmVisionSectionHeader(
                title = "Simulation Mode",
                description = "Telemetry generator for demonstration"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsToggleRow(
                    title = "Synthetic Telemetry Engine",
                    subtitle = "Generates demonstration sensor readings when offline",
                    checked = settings.isSimulatedEngineActive,
                    onCheckedChange = { viewModel.setSimulatedEngine(it) }
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Reset Preferences
            FarmVisionSectionHeader(
                title = "Reset Preferences",
                description = "Restore application settings to factory defaults"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Resetting preferences will restore theme, units, and protocols to default values. Crop profiles and telemetry logs will NOT be affected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset General Preferences")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }

    if (showResetDialog) {
        SettingsConfirmationDialog(
            title = "Reset Preferences?",
            message = "This will reset all unit, theme, and connection preferences to default values. Your crop profiles and recorded data will remain safe.",
            confirmText = "Reset",
            isDestructive = false,
            onConfirm = { viewModel.resetGeneralPreferences() },
            onDismiss = { showResetDialog = false }
        )
    }
}

