package com.farmvision.agrirover.ui.alert.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertStatus
import com.farmvision.agrirover.ui.alert.AlertViewModel
import com.farmvision.agrirover.ui.alert.components.AlertProvenanceBadge
import com.farmvision.agrirover.ui.alert.components.AlertSeverityBadge
import com.farmvision.agrirover.ui.alert.components.EmptyAlertsPlaceholder
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed provenance, telemetry values, and lifecycle audit screen for an alert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailsScreen(
    viewModel: AlertViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    modifier: Modifier = Modifier,
    onExplainAlertWithAi: ((AlertItem) -> Unit)? = null
) {
    val alert by viewModel.selectedAlert.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alert Details",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (alert == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(FarmVisionSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyAlertsPlaceholder(
                    title = "Alert Not Found",
                    subtitle = "This alert record could not be loaded or was pruned from history."
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = ParrotGreen, contentColor = DarkGreen)
                ) {
                    Text("Return to Alert Centre")
                }
            }
        } else {
            val item = alert!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small)
            ) {
                // Header & Provenance Card
                FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlertSeverityBadge(severity = item.severity)
                            AlertProvenanceBadge(isSimulated = item.isSimulated)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry & Threshold Card
                FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium)) {
                        Text(
                            text = "Telemetry & Thresholds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(
                            label = "Triggering Value",
                            value = if (item.triggerValue != null) "${item.triggerValue} ${item.canonicalUnit}" else "Unavailable"
                        )
                        DetailRow(
                            label = "Configured Threshold",
                            value = if (item.configuredThreshold != null) "${item.configuredThreshold} ${item.canonicalUnit}" else "N/A"
                        )
                        DetailRow(
                            label = "Sensor Source",
                            value = "${item.sourceSensor} (${item.category.displayName})"
                        )
                        DetailRow(
                            label = "Reporting Device",
                            value = item.sourceDevice
                        )
                        DetailRow(
                            label = "Evaluation Rule ID",
                            value = item.ruleId
                        )
                        DetailRow(
                            label = "Occurrence Count",
                            value = "${item.occurrenceCount} event(s)"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lifecycle & Timeline Card
                FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium)) {
                        Text(
                            text = "Lifecycle & Timestamps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(
                            label = "Status",
                            value = item.status.name.uppercase()
                        )
                        DetailRow(
                            label = "First Triggered",
                            value = formatFullTimestamp(item.createdTimestampEpochMillis)
                        )
                        DetailRow(
                            label = "Last Updated",
                            value = formatFullTimestamp(item.lastUpdatedTimestampEpochMillis)
                        )
                        DetailRow(
                            label = "Acknowledged At",
                            value = if (item.acknowledgedTimestampEpochMillis != null) {
                                formatFullTimestamp(item.acknowledgedTimestampEpochMillis!!)
                            } else "Not acknowledged"
                        )
                        DetailRow(
                            label = "Resolved At",
                            value = if (item.resolvedTimestampEpochMillis != null) {
                                formatFullTimestamp(item.resolvedTimestampEpochMillis!!)
                            } else "Unresolved / Active"
                        )
                        if (item.resolvingValue != null) {
                            DetailRow(
                                label = "Resolving Value",
                                value = "${item.resolvingValue} ${item.canonicalUnit}"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                if (item.status == AlertStatus.Active) {
                    Button(
                        onClick = { viewModel.acknowledgeAlert(item.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParrotGreen,
                            contentColor = DarkGreen
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Acknowledge Alert", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Telemetry History", color = DarkGreen)
                }

                if (onExplainAlertWithAi != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onExplainAlertWithAi(item) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParrotGreen,
                            contentColor = DarkGreen
                        )
                    ) {
                        Text("🌱", modifier = Modifier.padding(end = 6.dp))
                        Text("Explain this Alert with AI", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatFullTimestamp(epochMillis: Long): String {
    val date = Date(epochMillis)
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return formatter.format(date)
}

