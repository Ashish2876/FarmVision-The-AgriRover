package com.farmvision.agrirover.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTrendDirection
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.FarmAlert
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.ui.components.AgriStatusChip
import com.farmvision.agrirover.ui.components.AgriStatusType
import com.farmvision.agrirover.ui.components.GlassmorphicCard
import com.farmvision.agrirover.ui.components.MetricTrend
import com.farmvision.agrirover.ui.components.TactileButton
import com.farmvision.agrirover.ui.components.TactileButtonVariant
import com.farmvision.agrirover.ui.components.TelemetryMetricCard
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.LightLime
import com.farmvision.agrirover.ui.theme.PaleGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusWarning
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import kotlinx.coroutines.launch

/**
 * Phase 5 Production Home Dashboard for FarmVision AgriRover.
 * Features:
 * 1. Mission Status Summary Card (Active Mission, Current Field, Rover Mode, Heartbeat).
 * 2. 4-Card Telemetry Quick-Glance Grid (Battery %, Speed km/h, Rootzone Soil Moisture %, GPS Fix).
 * 3. Quick Action Bar (Field Scan, Return to Dock, Disarm/Re-Arm Motors).
 * 4. Recent Alerts Ticker Bar.
 * 5. Environmental & Subsurface Sensor Panel (DHT22, Soil pH, Ultrasonic Distance, Solar Lux).
 * 6. Crop Health Summary & Notification ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FarmViewModel,
    onNavigateToRover: () -> Unit,
    onNavigateToFarm: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHardwareCenter: (() -> Unit)? = null,
    onNavigateToAlertCenter: (() -> Unit)? = null,
    onNavigateToDeviceManagement: (() -> Unit)? = null,
    onNavigateToAssistant: (() -> Unit)? = null
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
            // 1. Top App Bar with Branding & Actions
            HomeTopBar(
                alertCount = alerts.size,
                onRefresh = { viewModel.refreshTelemetry() },
                onNotificationClick = {
                    if (onNavigateToAlertCenter != null) {
                        onNavigateToAlertCenter()
                    } else {
                        viewModel.setNotificationSheetOpen(true)
                    }
                },
                onHardwareCenterClick = onNavigateToHardwareCenter,
                onDeviceManagementClick = onNavigateToDeviceManagement
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 2. Demo Mode & Simulation Boundary Banner
            FarmVisionDemoBanner(
                message = "DEMO MODE ACTIVE • Telemetry and multi-sensor acquisition simulated via Phase 4 domain models."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 3. Phase 5: Recent Alerts Ticker
            AlertsTickerBar(
                alerts = alerts,
                onOpenSheet = {
                    if (onNavigateToAlertCenter != null) {
                        onNavigateToAlertCenter()
                    } else {
                        viewModel.setNotificationSheetOpen(true)
                    }
                }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // 4. Phase 5: Mission Status Summary Card
            MissionStatusCard(
                telemetry = telemetry,
                settings = settings,
                onNavigateToRover = onNavigateToRover
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // 5. Phase 5: Quick Action Bar (Field Scan, Return to Dock, Disarm/Re-Arm Motors)
            QuickActionBar(
                isEmergencyCutoff = telemetry.isEmergencyCutoff,
                onStartScan = { viewModel.startFieldScan() },
                onReturnToDock = { viewModel.returnToDock() },
                onToggleDisarm = {
                    if (telemetry.isEmergencyCutoff) {
                        viewModel.setEmergencyStop(false)
                    } else {
                        viewModel.disarmMotors()
                    }
                }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // 6. Phase 5: 4-Card Telemetry Quick-Glance Grid
            TelemetryQuickGlanceGrid(
                telemetry = telemetry,
                settings = settings
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // 7. Secondary Environmental & Hardware Sensors (DHT22, Soil pH, HC-SR04 Sonar, Lux)
            SecondarySensorsSection(
                telemetry = telemetry,
                settings = settings
            )

            // Phase 12: Intelligent AI Agriculture Assistant Card
            if (onNavigateToAssistant != null) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))
                AssistantDashboardCard(
                    onOpenAssistant = onNavigateToAssistant
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // 8. Crop Health Summary Section
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

            // 9. Rover Controller Direct Access Overview Card
            RoverOverviewCard(
                telemetry = telemetry,
                settings = settings,
                onControlClick = onNavigateToRover
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }

        // Notification ModalBottomSheet
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

/**
 * Phase 5 Deliverable: Compact alerts ticker banner with instant tap-to-expand.
 */
@Composable
private fun AlertsTickerBar(
    alerts: List<FarmAlert>,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isNotEmpty()) {
        val latestAlert = alerts.first()
        val isWarningOrError = latestAlert.severity == FarmVisionStatusType.Warning || latestAlert.severity == FarmVisionStatusType.Error
        val borderColor = if (isWarningOrError) StatusWarning else DarkGreen
        val icon = if (isWarningOrError) Icons.Rounded.Warning else Icons.Rounded.CheckCircle

        GlassmorphicCard(
            modifier = modifier.fillMaxWidth(),
            borderColor = borderColor.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            onClick = onOpenSheet
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LATEST ALERT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = borderColor,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${latestAlert.timestamp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = latestAlert.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "View alerts",
                        tint = DarkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        GlassmorphicCard(
            modifier = modifier.fillMaxWidth(),
            borderColor = DarkGreen.copy(alpha = 0.35f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = DarkGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "All Systems Nominal • Zero Active Warnings",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = DarkGreen
                )
            }
        }
    }
}

/**
 * Phase 5 Deliverable: Primary Mission Status Summary Card.
 * Displays Active Mission, Current Field, Rover Mode, Heartbeat, and Data Origin.
 */
@Composable
private fun MissionStatusCard(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    onNavigateToRover: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusType = when {
        telemetry.isEmergencyCutoff -> AgriStatusType.Alert
        telemetry.roverMode == "STANDBY" -> AgriStatusType.Standby
        else -> AgriStatusType.Nominal
    }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (telemetry.isEmergencyCutoff) StatusAlert.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(20.dp),
        onClick = onNavigateToRover
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon, Title, AgriStatusChip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (telemetry.isEmergencyCutoff) StatusAlert.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = if (telemetry.isEmergencyCutoff) StatusAlert else DarkGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MISSION CONTROL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            fontSize = 11.sp
                        )
                        Text(
                            text = telemetry.roverIdentifier,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AgriStatusChip(
                    type = statusType,
                    customLabel = if (telemetry.isEmergencyCutoff) "E-STOPPED" else telemetry.roverMode
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Headline: Active Mission
            Text(
                text = telemetry.activeMission,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Meta Row (Current Field, Drive Mode, Command)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MissionMetaPill(
                    label = "FIELD",
                    value = telemetry.fieldName,
                    modifier = Modifier.weight(1f)
                )
                MissionMetaPill(
                    label = "PROFILE",
                    value = "${telemetry.driveMode} Mode",
                    modifier = Modifier.weight(1f)
                )
                MissionMetaPill(
                    label = "HEARTBEAT",
                    value = telemetry.lastUpdated,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer row: Data origin badge and protocol
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "DATA ORIGIN: ${telemetry.dataOrigin.name.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Tap to open controls →",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGreen
                )
            }
        }
    }
}

@Composable
private fun MissionMetaPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Phase 5 Deliverable: High-contrast Quick Action Bar for fast autonomous commands and E-Stop safety.
 */
@Composable
private fun QuickActionBar(
    isEmergencyCutoff: Boolean,
    onStartScan: () -> Unit,
    onReturnToDock: () -> Unit,
    onToggleDisarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FarmVisionSectionHeader(
            title = "Quick Actions",
            description = "Autonomous piloting & safety commands"
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Button 1: Start Field Scan
            Box(modifier = Modifier.weight(1f)) {
                TactileButton(
                    text = "Field Scan",
                    onClick = onStartScan,
                    variant = TactileButtonVariant.Primary,
                    leadingIcon = Icons.Rounded.Radar,
                    enabled = !isEmergencyCutoff,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Button 2: Return to Dock
            Box(modifier = Modifier.weight(1f)) {
                TactileButton(
                    text = "Dock Rover",
                    onClick = onReturnToDock,
                    variant = TactileButtonVariant.Secondary,
                    leadingIcon = Icons.Rounded.Home,
                    enabled = !isEmergencyCutoff,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Button 3: Disarm / Re-Arm Motors
            Box(modifier = Modifier.weight(1f)) {
                TactileButton(
                    text = if (isEmergencyCutoff) "Re-Arm" else "Disarm",
                    onClick = onToggleDisarm,
                    variant = TactileButtonVariant.Danger,
                    leadingIcon = Icons.Rounded.PowerSettingsNew,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Phase 5 Deliverable: 4-Card Telemetry Quick-Glance Grid.
 * Displays Battery %, Speed km/h, Rootzone Soil Moisture %, and GPS Fix.
 */
@Composable
private fun TelemetryQuickGlanceGrid(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FarmVisionSectionHeader(
            title = "Field Telemetry",
            description = "Real-time rover vitals & dual-probe soil acquisition",
            trailingAction = {
                Text(
                    text = "10s Poll",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Row 1: Battery % and Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricCard(
                title = "Battery",
                value = "${telemetry.batteryPercent}",
                unit = "%",
                subtitle = "${telemetry.batteryVoltage}V • Nominal",
                icon = Icons.Rounded.BatteryChargingFull,
                trend = MetricTrend.Stable,
                trendLabel = "Nominal",
                progressFraction = telemetry.batteryPercent / 100f,
                status = if (telemetry.batteryPercent < 20) AgriStatusType.Alert else AgriStatusType.Nominal,
                modifier = Modifier.weight(1f)
            )

            val speedUnit = settings.speedUnit
            val speedTrend = if (telemetry.speedKmh > 0f) MetricTrend.Rising else MetricTrend.Stable
            val speedStatusLabel = when {
                telemetry.isEmergencyCutoff -> "Cutoff"
                telemetry.speedKmh > 0f -> "Cruising"
                else -> "Stationary"
            }
            TelemetryMetricCard(
                title = "Speed",
                value = String.format(java.util.Locale.US, "%.1f", telemetry.speedKmh),
                unit = speedUnit.symbol,
                subtitle = telemetry.activeCommand,
                icon = Icons.Rounded.Speed,
                trend = speedTrend,
                trendLabel = speedStatusLabel,
                status = if (telemetry.isEmergencyCutoff) AgriStatusType.Alert else AgriStatusType.Nominal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Soil Moisture and GPS Fix
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val moistureTrend = when (telemetry.soilMoistureTrend) {
                FarmVisionTrendDirection.Rising -> MetricTrend.Rising
                FarmVisionTrendDirection.Falling -> MetricTrend.Falling
                FarmVisionTrendDirection.Stable -> MetricTrend.Stable
            }
            val moistureStatusLabel = if (telemetry.soilMoisture >= 35f) "Optimal" else "Low"
            TelemetryMetricCard(
                title = "Moisture",
                value = String.format(java.util.Locale.US, "%.1f", telemetry.soilMoisture),
                unit = "%",
                subtitle = "Dual Probe Rootzone",
                icon = Icons.Rounded.WaterDrop,
                trend = moistureTrend,
                trendLabel = moistureStatusLabel,
                progressFraction = (telemetry.soilMoisture / 100f).coerceIn(0f, 1f),
                status = if (telemetry.soilMoisture < 35f) AgriStatusType.Warning else AgriStatusType.Nominal,
                modifier = Modifier.weight(1f)
            )

            val hasGpsFix = telemetry.gpsFixStatus.contains("Fix", ignoreCase = true) && !telemetry.gpsFixStatus.contains("No Fix", ignoreCase = true)
            val gpsTrendLabel = if (hasGpsFix) "Locked" else "No Fix"
            val gpsStatus = if (hasGpsFix) AgriStatusType.Nominal else AgriStatusType.Warning

            TelemetryMetricCard(
                title = "GPS Fix",
                value = "3D Fix",
                unit = "(8 Sats)",
                subtitle = telemetry.simulatedGps,
                icon = Icons.Rounded.GpsFixed,
                trend = MetricTrend.Stable,
                trendLabel = gpsTrendLabel,
                status = gpsStatus,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Secondary multi-sensor panel representing Phase 4 hardware sensor integrations.
 */
@Composable
private fun SecondarySensorsSection(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FarmVisionSectionHeader(
            title = "Environmental & Hardware Sensors",
            description = "DHT22, Soil pH probe, HC-SR04 sonar & solar lux"
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            // Temperature (converts between °C and °F)
            FarmVisionMetricCard(
                label = FarmVisionMetricType.Temperature.defaultLabel,
                value = settings.temperatureUnit.formatValueOnly(telemetry.temperature),
                unit = settings.temperatureUnit.symbol,
                icon = FarmVisionMetricType.Temperature.icon,
                trend = telemetry.temperatureTrend,
                trendLabel = "DHT22 Sensor",
                isSampleData = true
            )

            // Air Humidity
            FarmVisionMetricCard(
                label = FarmVisionMetricType.AirHumidity.defaultLabel,
                value = "${telemetry.humidity}",
                unit = FarmVisionMetricType.AirHumidity.defaultUnit,
                icon = FarmVisionMetricType.AirHumidity.icon,
                trend = telemetry.humidityTrend,
                trendLabel = "DHT22 Sensor",
                isSampleData = true
            )

            // Soil pH Sensor (Phase 4 Inventory Item #6)
            FarmVisionMetricCard(
                label = "Soil pH Level",
                value = String.format(java.util.Locale.US, "%.1f", telemetry.soilPh),
                unit = "pH",
                icon = Icons.Rounded.Science,
                trend = FarmVisionTrendDirection.Stable,
                trendLabel = "Optimal (6.5–7.0)",
                isSampleData = true
            )

            // Ultrasonic Proximity (Phase 4 Inventory Item #7 HC-SR04)
            FarmVisionMetricCard(
                label = "Obstacle Clearance",
                value = String.format(java.util.Locale.US, "%.0f", telemetry.ultrasonicDistanceCm),
                unit = "cm",
                icon = Icons.Rounded.Radar,
                trend = FarmVisionTrendDirection.Stable,
                trendLabel = "HC-SR04 Sensor",
                isSampleData = true
            )

            // Solar Light (Simulated)
            FarmVisionMetricCard(
                label = FarmVisionMetricType.LightIntensity.defaultLabel,
                value = "${telemetry.lightLux}",
                unit = FarmVisionMetricType.LightIntensity.defaultUnit,
                icon = FarmVisionMetricType.LightIntensity.icon,
                trend = telemetry.lightTrend,
                trendLabel = "Simulated (No Physical Sensor)",
                isSampleData = true
            )
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
                    androidx.compose.runtime.key(alert.id) {
                        AlertItemCard(alert = alert)
                    }
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
    onNotificationClick: () -> Unit,
    onHardwareCenterClick: (() -> Unit)? = null,
    onDeviceManagementClick: (() -> Unit)? = null
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
                    color = DarkGreen
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDeviceManagementClick != null) {
                FarmVisionIconButton(
                    icon = Icons.Rounded.Radar,
                    contentDescription = "Device Management",
                    onClick = onDeviceManagementClick
                )
            }
            if (onHardwareCenterClick != null) {
                FarmVisionIconButton(
                    icon = Icons.Rounded.DeveloperBoard,
                    contentDescription = "Hardware Center",
                    onClick = onHardwareCenterClick
                )
            }

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
                    tint = DarkGreen,
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


/**
 * Phase 12: AI Agriculture Assistant Dashboard Card.
 */
@Composable
fun AssistantDashboardCard(
    onOpenAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpenAssistant,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, ParrotGreen.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ParrotGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌱", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Agriculture Assistant",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ParrotGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ask questions on crop health, soil moisture readings & alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open Assistant",
                tint = ParrotGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
