package com.farmvision.agrirover.ui.home

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
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCardFooter
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCardHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionEmptyState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionIconButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionMetricCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionMetricType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTextButton
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.FarmAlert
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import kotlinx.coroutines.launch

/**
 * Production-style Home Dashboard screen for FarmVision AgriRover.
 * Displays real-time rover state, environmental and soil telemetry, plant health summary, and alerts.
 * Backed by shared [FarmViewModel] StateFlows and reactive user unit preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FarmViewModel,
    onNavigateToRover: () -> Unit,
    onNavigateToFarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val telemetry by viewModel.telemetry.collectAsState()
    val plants by viewModel.plants.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isNotificationSheetOpen by viewModel.isNotificationSheetOpen.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Top App Bar
            HomeTopBar(
                alertCount = alerts.size,
                onRefresh = { viewModel.refreshTelemetry() },
                onNotificationClick = {
                    viewModel.setNotificationSheetOpen(true)
                }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // Honest Demo / Simulated Banner
            FarmVisionDemoBanner(
                message = "DEMO MODE ACTIVE • Telemetry and sensor readings are simulated. Hardware integration scheduled for future milestones."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Rover Quick Status Card (reflects live movement and E-Stop state)
            RoverOverviewCard(
                telemetry = telemetry,
                settings = settings,
                onControlClick = onNavigateToRover
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // Live Telemetry Section (reflects moisture and unit preferences)
            FarmVisionSectionHeader(
                title = "Field Telemetry",
                description = "Live soil & environmental conditions (Simulated)",
                trailingAction = {
                    Text(
                        text = "10s Poll",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 4 Telemetry Metrics with unit conversions applied
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                // Soil Moisture (updates when crops are irrigated)
                FarmVisionMetricCard(
                    label = FarmVisionMetricType.SoilMoisture.defaultLabel,
                    value = "${telemetry.soilMoisture}",
                    unit = FarmVisionMetricType.SoilMoisture.defaultUnit,
                    icon = FarmVisionMetricType.SoilMoisture.icon,
                    trend = telemetry.soilMoistureTrend,
                    trendLabel = "Optimal range (35–55%)",
                    isSampleData = true
                )

                // Temperature (converts between °C and °F)
                FarmVisionMetricCard(
                    label = FarmVisionMetricType.Temperature.defaultLabel,
                    value = settings.temperatureUnit.formatValueOnly(telemetry.temperature),
                    unit = settings.temperatureUnit.symbol,
                    icon = FarmVisionMetricType.Temperature.icon,
                    trend = telemetry.temperatureTrend,
                    trendLabel = "Nominal",
                    isSampleData = true
                )

                // Air Humidity
                FarmVisionMetricCard(
                    label = FarmVisionMetricType.AirHumidity.defaultLabel,
                    value = "${telemetry.humidity}",
                    unit = FarmVisionMetricType.AirHumidity.defaultUnit,
                    icon = FarmVisionMetricType.AirHumidity.icon,
                    trend = telemetry.humidityTrend,
                    trendLabel = "-2% / 1h",
                    isSampleData = true
                )

                // Light Intensity
                FarmVisionMetricCard(
                    label = FarmVisionMetricType.LightIntensity.defaultLabel,
                    value = "${telemetry.lightLux}",
                    unit = FarmVisionMetricType.LightIntensity.defaultUnit,
                    icon = FarmVisionMetricType.LightIntensity.icon,
                    trend = telemetry.lightTrend,
                    trendLabel = "Peak Sunlight",
                    isSampleData = true
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // Plant Health Summary Section
            FarmVisionSectionHeader(
                title = "Crop Health Summary",
                description = "${plants.size} crops actively monitored",
                trailingAction = {
                    FarmVisionTextButton(
                        text = "View All",
                        onClick = onNavigateToFarm
                    )
                }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                plants.take(3).forEach { plant ->
                    PlantSummaryItem(plant = plant, onClick = onNavigateToFarm)
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // Recent Alerts Section
            FarmVisionSectionHeader(
                title = "Recent Field Alerts",
                description = "System notifications and environmental thresholds",
                trailingAction = {
                    if (alerts.isNotEmpty()) {
                        FarmVisionTextButton(
                            text = "View Sheet",
                            onClick = { viewModel.setNotificationSheetOpen(true) }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            if (alerts.isEmpty()) {
                FarmVisionEmptyState(
                    title = "All Systems Clear",
                    message = "Zero active environmental warnings or rover telemetry faults.",
                    actionText = "Refresh Telemetry",
                    onActionClick = { viewModel.refreshTelemetry() }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    alerts.forEach { alert ->
                        AlertItemCard(alert = alert)
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }

        // P4: Notification ModalBottomSheet
        if (isNotificationSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setNotificationSheetOpen(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                NotificationSheetContent(
                    alerts = alerts,
                    onClearAll = { viewModel.clearAllAlerts() },
                    onDismiss = {
                        coroutineScope.launch {
                            sheetState.hide()
                            viewModel.setNotificationSheetOpen(false)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationSheetContent(
    alerts: List<FarmAlert>,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "System Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (alerts.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(text = "${alerts.size}")
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alerts.isNotEmpty()) {
                    FarmVisionTextButton(
                        text = "Clear All",
                        onClick = onClearAll
                    )
                }
                FarmVisionIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close notifications",
                    onClick = onDismiss
                )
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        if (alerts.isEmpty()) {
            FarmVisionEmptyState(
                title = "No New Notifications",
                message = "You have caught up with all rover system alerts and environmental advisories.",
                actionText = "Close",
                onActionClick = onDismiss
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                alerts.forEach { alert ->
                    AlertItemCard(alert = alert)
                }
                Spacer(modifier = Modifier.height(FarmVisionSpacing.large))
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    alertCount: Int,
    onRefresh: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Agriculture,
                        contentDescription = "FarmVision Logo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(FarmVisionSpacing.medium))

            Column {
                Text(
                    text = "FarmVision",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Smart AgriRover System",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            FarmVisionIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh Telemetry",
                onClick = onRefresh
            )

            BadgedBox(
                badge = {
                    if (alertCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(text = "$alertCount")
                        }
                    }
                }
            ) {
                FarmVisionIconButton(
                    icon = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    onClick = onNotificationClick
                )
            }
        }
    }
}

@Composable
private fun RoverOverviewCard(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    onControlClick: () -> Unit
) {
    FarmVisionCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        FarmVisionCardHeader(
            title = telemetry.roverName,
            supportingText = if (telemetry.isEmergencyCutoff) {
                "SAFETY CUTOFF ENGAGED • Motors Inactive"
            } else {
                "Mode: ${telemetry.roverMode} • ${settings.transportProtocol}"
            },
            leadingIcon = Icons.Rounded.Agriculture,
            trailingAction = {
                if (telemetry.isEmergencyCutoff) {
                    FarmVisionStatusBadge(
                        status = FarmVisionStatusType.Error,
                        customLabel = "E-STOPPED"
                    )
                } else {
                    FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
                }
            }
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Status Indicators Row with converted units
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(FarmVisionSpacing.extraSmall))
                Text(
                    text = "${telemetry.batteryPercent}% (${telemetry.batteryVoltage}V)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Speed: ${settings.speedUnit.format(telemetry.speedKmh)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Motor: ${settings.temperatureUnit.format(telemetry.motorTemperature)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        FarmVisionCardFooter(
            statusContent = {
                Text(
                    text = "Command: ${telemetry.activeCommand}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (telemetry.isEmergencyCutoff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            actionContent = {
                FarmVisionPrimaryButton(
                    text = "Open Rover Controls",
                    onClick = onControlClick,
                    leadingIcon = Icons.Rounded.PlayArrow
                )
            }
        )
    }
}

@Composable
private fun PlantSummaryItem(
    plant: PlantItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${plant.plotLocation} • ${plant.growthStage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FarmVisionStatusBadge(
                    status = plant.healthStatus,
                    customLabel = if (plant.isWatered) "Irrigated (${plant.healthPercent}%)" else "${plant.healthPercent}% Healthy"
                )
                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AlertItemCard(alert: FarmAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.medium),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    FarmVisionStatusBadge(status = alert.severity)
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.extraSmall))

                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

            Text(
                text = alert.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
