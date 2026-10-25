package com.farmvision.agrirover.ui.hardware.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.hardware.ConfigurationStatus
import com.farmvision.agrirover.ui.hardware.components.HardwareHonestyCard
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareUiItem
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Full detail screen for an agricultural rover component.
 * Displays:
 * 1. Component Identity & Status
 * 2. Current Telemetry or Commanded Value
 * 3. Diagnostics & Error Log
 * 4. Configuration & Candidate Pinout (marked DRAFT / UNVERIFIED)
 * 5. "What This Component Cannot Report" honesty boundaries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentDetailScreen(
    item: HardwareUiItem,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val statusColors = LocalAgriStatusColors.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.component.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Hardware Center"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(FarmVisionSpacing.large)
        ) {
            // Identity Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FarmVisionSpacing.medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(FarmVisionSpacing.medium)) {
                    Text(
                        text = item.component.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Model: ${item.component.modelDesignation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Category: ${item.component.category.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                    ) {
                        // Connection / Presence Chip
                        val connectionText = if (!item.isElectronic) "Cataloged (Passive)" else item.connectionStatus.label
                        val connectionColor = if (item.isElectronic && item.connectionStatus == EndpointConnectionStatus.Connected) {
                            statusColors.online
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(FarmVisionSpacing.extraSmall),
                            color = connectionColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, connectionColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = connectionText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = connectionColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Maturity Status Chip
                        Surface(
                            shape = RoundedCornerShape(FarmVisionSpacing.extraSmall),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Maturity: ${item.component.status.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 1. Current Telemetry / Commanded Value
            SectionTitle(title = "Current Operational State")
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FarmVisionSpacing.medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(FarmVisionSpacing.medium)) {
                    DetailRow(label = "Latest Reading", value = item.latestValueDisplay, isHighlight = true)
                    DetailRow(label = "Telemetry Freshness", value = item.freshnessText)
                    DetailRow(label = "Actuator State", value = item.actuatorState.label)
                    DetailRow(label = "Physical Feedback", value = item.physicalFeedback.label)
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 2. Hardware Boundaries & Honesty Notice
            if (item.honestyDisclaimer != null) {
                SectionTitle(title = "Hardware Reality & Honesty Notice")
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                HardwareHonestyCard(disclaimerText = item.honestyDisclaimer)
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
            }

            // 3. Draft Wiring & Pin Configuration (UNVERIFIED)
            SectionTitle(title = "Configuration & Pinout (DRAFT / UNVERIFIED)")
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FarmVisionSpacing.medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(FarmVisionSpacing.medium)) {
                    DetailRow(
                        label = "Target Controller",
                        value = item.endpointTarget?.name ?: "None / Mechanical"
                    )

                    if (item.draftPins.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                        Text(
                            text = "Draft Candidate Pins (Requires Physical Verification):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColors.warning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        item.draftPins.forEach { (pinRole, gpio) ->
                            val gpioText = if (gpio != null) "GPIO $gpio (Draft)" else "Unassigned"
                            DetailRow(label = pinRole, value = gpioText, isMonospace = true)
                        }
                    } else {
                        DetailRow(label = "Pin Assignments", value = "No GPIO pins (Mechanical / Passive)")
                    }

                    if (item.component.provisionalNotes != null) {
                        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                        DetailRow(label = "Engineering Notes", value = item.component.provisionalNotes)
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 4. Diagnostics & Traceability
            SectionTitle(title = "Diagnostics & Specification")
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FarmVisionSpacing.medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(FarmVisionSpacing.medium)) {
                    DetailRow(label = "Component ID", value = item.component.id, isMonospace = true)
                    DetailRow(label = "Quantity in Inventory", value = item.component.quantity?.toString() ?: "Unspecified")
                    DetailRow(label = "Last Diagnostic Error", value = "None (No link active)")
                    DetailRow(label = "Documentation", value = item.component.documentationRef ?: "docs/hardware-data-model.md")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlight) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = FontWeight.Medium
                )
            },
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
