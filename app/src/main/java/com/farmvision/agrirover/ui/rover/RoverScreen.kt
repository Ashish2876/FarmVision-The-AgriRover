package com.farmvision.agrirover.ui.rover

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.ui.rover.controller.VirtualJoystick
import com.farmvision.agrirover.ui.rover.controller.JoystickVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.LightLime
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors
import com.farmvision.agrirover.ui.theme.PaleGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.ParrotGreenDeep
import com.farmvision.agrirover.ui.theme.TelemetryValueLarge
import com.farmvision.agrirover.ui.theme.TextPrimary
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * Premium Robotics Rover Controller & Agricultural Cockpit.
 * Faithfully adapting the FarmVision AgriRover visual dashboard reference:
 * - A. Top Identity & Connection Bar
 * - B. Live Camera Preview Placeholder (with simulated crops canvas)
 * - C. Rover Status Card with custom vector tractor graphic & battery progress
 * - D. 4-Card Field Sensor Readings (Soil Moisture, Temperature, Humidity, Light Intensity)
 * - E. Rover Field Map & Task Info Summary (Crop-row path canvas & progress metrics)
 * - F. Main Robotics Controller Cockpit:
 *      1. Drive Mode Selector pills (Manual Control, Auto, Follow Path, Field Scan)
 *      2. Discrete Speed Control stepper & vertical gauge
 *      3. Circular Joystick D-Pad with central STOP
 *      4. Auxiliary Controls Grid (All Lights, Front Light, Side Light, Horn, Chute/Arm)
 *      5. Camera Tilt Stepper
 *      6. Industrial Red Emergency Stop Latch & deliberate re-arm
 *      7. Irrigation & Spraying toggles
 */
@Composable
fun RoverScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHardwareCenter: (() -> Unit)? = null
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onJoystickReleased()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onJoystickReleased()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        if (isLandscape) {
            RoverLandscapeLayout(
                telemetry = telemetry,
                settings = settings,
                viewModel = viewModel,
                onNavigateToHardwareCenter = onNavigateToHardwareCenter
            )
        } else {
            RoverPortraitLayout(
                telemetry = telemetry,
                settings = settings,
                viewModel = viewModel,
                onNavigateToHardwareCenter = onNavigateToHardwareCenter
            )
        }
    }
}

/**
 * Portrait Layout: Scrollable unified robotics dashboard stack.
 */
@Composable
private fun RoverPortraitLayout(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    viewModel: FarmViewModel,
    onNavigateToHardwareCenter: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small)
    ) {
        // A. Top Identity & Connection Status Bar
        RoverTopStatusBar(
            telemetry = telemetry,
            onBadgeClick = onNavigateToHardwareCenter
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Simulator Transparency Banner
        FarmVisionDemoBanner(
            message = "SIMULATOR ACTIVE: All telemetry, camera streams, and actuation commands operate locally without physical hardware."
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // B & C. Rover Overview Row: Camera Preview Placeholder & Rover Status Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            Box(modifier = Modifier.weight(1.1f)) {
                RoverCameraPreviewCard(telemetry = telemetry)
            }
            Box(modifier = Modifier.weight(0.9f)) {
                RoverStatusOverviewCard(telemetry = telemetry, settings = settings)
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // D. Field Sensor Readings (4-Card Grid)
        FieldSensorReadingsSection(telemetry = telemetry, settings = settings)

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // E. Rover Map & Task Info Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RoverMapCard()
            }
            Box(modifier = Modifier.weight(1f)) {
                RoverTaskInfoCard(telemetry = telemetry)
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // F. Main Controller Cockpit Section
        MainControllerCockpit(
            telemetry = telemetry,
            settings = settings,
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
    }
}

/**
 * Landscape Layout: Ergonomic 2-column cockpit separating telemetry from piloting.
 */
@Composable
private fun RoverLandscapeLayout(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    viewModel: FarmViewModel,
    onNavigateToHardwareCenter: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium)
    ) {
        // Left Column: Status, Camera, Sensors, Map
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            RoverTopStatusBar(
                telemetry = telemetry,
                onBadgeClick = onNavigateToHardwareCenter
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                Box(modifier = Modifier.weight(1.1f)) {
                    RoverCameraPreviewCard(telemetry = telemetry)
                }
                Box(modifier = Modifier.weight(0.9f)) {
                    RoverStatusOverviewCard(telemetry = telemetry, settings = settings)
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FieldSensorReadingsSection(telemetry = telemetry, settings = settings)

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RoverMapCard()
                }
                Box(modifier = Modifier.weight(1f)) {
                    RoverTaskInfoCard(telemetry = telemetry)
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
        }

        // Right Column: Piloting Cockpit
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainControllerCockpit(
                telemetry = telemetry,
                settings = settings,
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
        }
    }
}

// ============================================================================
// SECTION A — TOP STATUS BAR
// ============================================================================

@Composable
private fun RoverTopStatusBar(
    telemetry: RoverTelemetry,
    onBadgeClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Identity & Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = null,
                            tint = DarkGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FarmVision ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AgriRover",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = telemetry.roverName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ${telemetry.fieldName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ${telemetry.lastUpdated}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Connection & Battery Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Truthful Demo / Connection Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaleGreen,
                    border = BorderStroke(1.dp, DarkGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onBadgeClick?.invoke() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(DarkGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val isDemo = telemetry.isSimulation || telemetry.connectionStatus == FarmVisionStatusType.DemoData
                        val connectionText = when {
                            isDemo -> "Online (Demo)"
                            telemetry.connectionStatus == FarmVisionStatusType.Active -> "Online (Live)"
                            telemetry.connectionStatus == FarmVisionStatusType.Error -> "Offline"
                            else -> "Online"
                        }
                        Text(
                            text = connectionText,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

                // Battery Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = "Battery",
                            tint = DarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${telemetry.batteryPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// SECTION B — CAMERA PREVIEW PLACEHOLDER
// ============================================================================

@Composable
private fun RoverCameraPreviewCard(telemetry: RoverTelemetry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated Crop Rows Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Sky Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B382B), Color(0xFF0F2218)),
                        startY = 0f,
                        endY = h * 0.45f
                    ),
                    size = Size(w, h * 0.45f)
                )

                // Soil Horizon Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2E2218), Color(0xFF19110B)),
                        startY = h * 0.45f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.45f),
                    size = Size(w, h * 0.55f)
                )

                // Perspective Crop Rows
                val vanishingPoint = Offset(w * 0.5f, h * 0.45f)
                val rowStarts = listOf(0.05f, 0.25f, 0.45f, 0.55f, 0.75f, 0.95f)
                for (startRatio in rowStarts) {
                    val bottomX = w * startRatio
                    drawLine(
                        color = ParrotGreen.copy(alpha = 0.6f),
                        start = vanishingPoint,
                        end = Offset(bottomX, h),
                        strokeWidth = if (startRatio in 0.4f..0.6f) 5f else 3f
                    )
                }

                // Grid Horizon Line
                drawLine(
                    color = ParrotGreen.copy(alpha = 0.3f),
                    start = Offset(0f, h * 0.45f),
                    end = Offset(w, h * 0.45f),
                    strokeWidth = 2f
                )
            }

            // Top Camera Status Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FarmVisionSpacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE74C3C))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SIMULATED CAMERA STREAM • HARDWARE DISCONNECTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = "Expand Camera",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }

            // Bottom Camera Telemetry Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HDG: 184° S (Simulated) • TILT: ${telemetry.cameraTiltAngle}° (Simulated)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontSize = 9.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(DarkGreen)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "28ms (Simulated)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// SECTION C — ROVER STATUS OVERVIEW CARD
// ============================================================================

@Composable
private fun RoverStatusOverviewCard(
    telemetry: RoverTelemetry,
    settings: FarmSettings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FarmVisionSpacing.small),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rover Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = telemetry.activeCommand,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (telemetry.isEmergencyCutoff) MaterialTheme.colorScheme.error else DarkGreen,
                    fontSize = 10.sp
                )
            }

            // Stylized Tractor Rover Vector Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp, 44.dp)) {
                    val w = size.width
                    val h = size.height

                    // Main Chassis
                    drawRoundRect(
                        color = ParrotGreenDeep,
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = Size(w * 0.7f, h * 0.35f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )

                    // Cab / Cabin
                    drawRoundRect(
                        color = DarkGreen,
                        topLeft = Offset(w * 0.35f, h * 0.1f),
                        size = Size(w * 0.35f, h * 0.3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )

                    // Rear Wheel (Large Tractor Tire)
                    drawCircle(
                        color = Color.DarkGray,
                        radius = h * 0.32f,
                        center = Offset(w * 0.25f, h * 0.7f)
                    )
                    drawCircle(
                        color = LightLime,
                        radius = h * 0.16f,
                        center = Offset(w * 0.25f, h * 0.7f)
                    )

                    // Front Wheel (Smaller Tire)
                    drawCircle(
                        color = Color.DarkGray,
                        radius = h * 0.24f,
                        center = Offset(w * 0.75f, h * 0.75f)
                    )
                    drawCircle(
                        color = LightLime,
                        radius = h * 0.12f,
                        center = Offset(w * 0.75f, h * 0.75f)
                    )

                    // Antenna
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(w * 0.4f, h * 0.1f),
                        end = Offset(w * 0.4f, 0f),
                        strokeWidth = 2f
                    )
                    drawCircle(
                        color = DarkGreen,
                        radius = 2.5f,
                        center = Offset(w * 0.4f, 0f)
                    )
                }
            }

            // Metrics Rows: Battery, Connectivity, GPS
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Battery Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Battery",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${telemetry.batteryPercent}% (${telemetry.batteryVoltage}V)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { telemetry.batteryPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = DarkGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Connectivity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Connectivity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Simulated SoftAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Coordinates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = telemetry.simulatedGps,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================================
// SECTION D — FIELD SENSOR READINGS (4-CARD GRID)
// ============================================================================

@Composable
private fun FieldSensorReadingsSection(
    telemetry: RoverTelemetry,
    settings: FarmSettings
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Field Sensor Readings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // Semantic subtitle preserved for Drive & Monitor HUD test compatibility
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Environmental Telemetry HUD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • Live Rootzone",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC39BD3))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Demo Data",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC39BD3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // 2x2 Sensor Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            // 1. Soil Moisture
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Soil Moisture",
                value = String.format(java.util.Locale.US, "%.1f", telemetry.soilMoisture),
                unit = "%",
                statusLabel = "● Optimal",
                statusColor = DarkGreen,
                progress = (telemetry.soilMoisture / 100f).coerceIn(0f, 1f),
                icon = Icons.Rounded.Eco,
                iconTint = DarkGreen
            )

            // 2. Air Temperature
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Temperature",
                value = settings.temperatureUnit.formatValueOnly(telemetry.temperature),
                unit = settings.temperatureUnit.symbol,
                statusLabel = "● Nominal",
                statusColor = LocalAgriStatusColors.current.warning,
                progress = (telemetry.temperature / 50f).coerceIn(0f, 1f),
                icon = Icons.Rounded.Thermostat,
                iconTint = LocalAgriStatusColors.current.warning
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            // 3. Humidity
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Humidity",
                value = String.format(java.util.Locale.US, "%.1f", telemetry.humidity),
                unit = "%",
                statusLabel = "● Normal",
                statusColor = LocalAgriStatusColors.current.info,
                progress = (telemetry.humidity / 100f).coerceIn(0f, 1f),
                icon = Icons.Rounded.WaterDrop,
                iconTint = LocalAgriStatusColors.current.info
            )

            // 4. Light Intensity
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Light Intensity",
                value = "${telemetry.lightLux}",
                unit = "lux",
                statusLabel = "● Good",
                statusColor = LocalAgriStatusColors.current.warning,
                progress = (telemetry.lightLux / 100000f).coerceIn(0f, 1f),
                icon = Icons.Rounded.WbSunny,
                iconTint = LocalAgriStatusColors.current.warning
            )
        }
    }
}

@Composable
private fun SensorCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    statusLabel: String,
    statusColor: Color,
    progress: Float,
    icon: ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.small),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// SECTION E — ROVER FIELD MAP & TASK INFO SUMMARY
// ============================================================================

@Composable
private fun RoverMapCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FarmVisionSpacing.small),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        tint = DarkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rover Map",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Map",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkGreen,
                        fontSize = 10.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = DarkGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Canvas Illustrative Aerial Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Field Grid / Crop Rows (Diagonal or Horizontal)
                    for (i in 0..6) {
                        val y = h * (i / 6f)
                        drawLine(
                            color = outlineColor.copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 4f
                        )
                    }

                    // Dashed Green Path
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.6f)
                        quadraticTo(w * 0.4f, h * 0.3f, w * 0.75f, h * 0.5f)
                    }
                    drawPath(
                        path = path,
                        color = DarkGreen,
                        style = Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    )

                    // Rover Marker (Green Dot with Ripple)
                    drawCircle(
                        color = DarkGreen.copy(alpha = 0.3f),
                        radius = 12f,
                        center = Offset(w * 0.2f, h * 0.6f)
                    )
                    drawCircle(
                        color = DarkGreen,
                        radius = 6f,
                        center = Offset(w * 0.2f, h * 0.6f)
                    )

                    // Target Waypoint Pin (Red Pin)
                    drawCircle(
                        color = Color(0xFFBA2518),
                        radius = 6f,
                        center = Offset(w * 0.75f, h * 0.5f)
                    )
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "● Rover",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGreen,
                    fontSize = 10.sp
                )
                Text(
                    text = "--- Path",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "● Target",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBA2518),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RoverTaskInfoCard(telemetry: RoverTelemetry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FarmVisionSpacing.small),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = DarkGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Task Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Active Task & Progress
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto Scanning",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "20%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                }
                Text(
                    text = "Row 2 of 10 • Sector A",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                LinearProgressIndicator(
                    progress = { 0.20f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DarkGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Metrics: Area, Distance, ETA
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Target Area", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    Text(text = telemetry.fieldName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    Text(text = "12.4 m (Simulated)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Est. Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    Text(text = "48 min", style = MaterialTheme.typography.labelSmall, color = DarkGreen, fontSize = 9.sp)
                }
            }
        }
    }
}

// ============================================================================
// SECTION F — MAIN ROBOTICS CONTROLLER COCKPIT
// ============================================================================

@Composable
private fun MainControllerCockpit(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    viewModel: FarmViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.medium)
        ) {
            // Header for section
            FarmVisionSectionHeader(
                title = "Piloting Controls",
                description = "Tactile Vector & Directional Cockpit (${telemetry.driveModeEnum.label} • ${telemetry.speedPreset.label})"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 1. Drive Mode Selector Pill Bar
            ControlModePillBar(
                currentMode = telemetry.driveModeEnum,
                onModeSelect = { viewModel.setDriveMode(it) }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // 2. Speed Presets Bar with Mandatory Uncalibrated Disclaimer
            SpeedPresetPillBar(
                currentPreset = telemetry.speedPreset,
                onPresetSelect = { viewModel.setSpeedPreset(it) }
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 3. Primary Tactile Vector Piloting (Virtual Joystick + Differential Kinematics HUD)
            PrimaryJoystickCockpitRow(
                telemetry = telemetry,
                settings = settings,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 4. Secondary Directional Controls Row: Accessible Circular D-Pad + Auxiliary Controls Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular D-Pad with central STOP (Preserved for accessibility & existing tests)
                CircularDirectionalController(
                    activeCommand = telemetry.activeCommand,
                    isEnabled = !telemetry.isEmergencyCutoff && telemetry.driveModeEnum.allowsManualDrive,
                    onDirectionClick = { direction ->
                        viewModel.updateRoverMovement(direction, telemetry.speedPreset.maxSpeedKmh)
                    },
                    onStopClick = {
                        viewModel.updateRoverMovement("HOLD POSITION", 0.0f)
                    }
                )

                // Auxiliary Controls (2x2 grid + Tool Arm Stepper)
                AuxiliaryControlsGrid(
                    telemetry = telemetry,
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // 5. Bottom Row: Camera Tilt Stepper + Prominent STOP Button + Irrigation / Spray Toggles
            BottomCockpitRow(
                telemetry = telemetry,
                viewModel = viewModel
            )
        }
    }
}

/**
 * Drive Mode Selector Pills: Manual, Autonomous Waypoint (Simulated), Hold Position (Stop)
 */
@Composable
private fun ControlModePillBar(
    currentMode: RoverDriveMode,
    onModeSelect: (RoverDriveMode) -> Unit
) {
    val modes = listOf(
        RoverDriveMode.Manual to "Manual",
        RoverDriveMode.AutonomousWaypoint to "Auto Waypoint (Demo)",
        RoverDriveMode.HoldPosition to "Hold Position"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = currentMode == mode
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("rover_mode_${mode.name.lowercase()}")
                    .clickable { onModeSelect(mode) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Speed Preset Selector: Low (1.5 km/h), Medium (3.5 km/h), High (5.5 km/h)
 * with mandatory disclaimer: "Software Preset • Uncalibrated Duty Cycle"
 */
@Composable
private fun SpeedPresetPillBar(
    currentPreset: SpeedPreset,
    onPresetSelect: (SpeedPreset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            SpeedPreset.values().forEach { preset ->
                val isSelected = currentPreset == preset
                val label = "${preset.label} (${preset.maxSpeedKmh} km/h • ${(preset.dutyFraction * 100).toInt()}%)"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("rover_speed_${preset.name.lowercase()}")
                        .clickable { onPresetSelect(preset) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) LightLime else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Mandatory disclaimer
        Text(
            text = SpeedPreset.DISCLAIMER,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Primary Tactile Vector Piloting Row: Virtual Joystick + Differential Kinematics HUD
 */
@Composable
private fun PrimaryJoystickCockpitRow(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    viewModel: FarmViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Virtual Joystick
            VirtualJoystick(
                size = 140.dp,
                knobSize = 50.dp,
                isEnabled = !telemetry.isEmergencyCutoff,
                driveMode = telemetry.driveModeEnum,
                onVectorChanged = { viewModel.onJoystickMoved(it) },
                onRelease = { viewModel.onJoystickReleased() }
            )

            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

            // Right: Vector & Differential Output Telemetry Box
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tactile Vector Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val diff = com.farmvision.agrirover.ui.rover.controller.JoystickMath.computeDifferentialMix(telemetry.joystickVector)
                        val leftPct = (diff.left * 100).toInt()
                        val rightPct = (diff.right * 100).toInt()
                        val displaySpeed = settings.speedUnit.format(telemetry.speedKmh)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(text = displaySpeed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (telemetry.isEmergencyCutoff) MaterialTheme.colorScheme.error else DarkGreen, fontSize = 9.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Duty Cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(text = "${(telemetry.joystickVector.magnitude * telemetry.speedPreset.dutyFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Diff Mix", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(text = "L: ${leftPct}% | R: ${rightPct}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Vector", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(
                                text = "(${String.format(java.util.Locale.US, "%.2f", telemetry.joystickVector.x)}, ${String.format(java.util.Locale.US, "%.2f", telemetry.joystickVector.y)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.joystickVector.isNeutral) Color.Gray else DarkGreen,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Single-touch tracking • Secondary touch can engage E-Stop",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

/**
 * Vertical Speed Control Column: Stepper Up, vertical track, Stepper Down.
 */
@Composable
private fun SpeedControlColumn(
    currentSpeedKmh: Float,
    driveMode: String,
    speedUnit: com.farmvision.agrirover.data.model.SpeedUnit,
    isCutoff: Boolean,
    onSpeedStepUp: () -> Unit,
    onSpeedStepDown: () -> Unit
) {
    val speedLevel = when (driveMode) {
        "Precision" -> 1
        "Normal" -> 3
        "Turbo" -> 5
        else -> 3
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Text(
            text = "Speed",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = "$speedLevel/5",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCutoff) MaterialTheme.colorScheme.error else DarkGreen
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Step Up
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .size(36.dp)
                .clickable { onSpeedStepUp() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Increase Speed",
                    tint = DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Vertical Speed Track
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val fillRatio = speedLevel / 5f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillRatio)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ParrotGreen, DarkGreen)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Step Down
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .size(36.dp)
                .clickable { onSpeedStepDown() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Decrease Speed",
                    tint = DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Circular Joystick Directional Controller: Up, Down, Left, Right and Center STOP.
 */
@Composable
private fun CircularDirectionalController(
    activeCommand: String,
    isEnabled: Boolean,
    onDirectionClick: (String) -> Unit,
    onStopClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Forward (Up)
        Surface(
            onClick = { onDirectionClick("FORWARD") },
            enabled = isEnabled,
            shape = CircleShape,
            color = if (activeCommand == "FORWARD" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                if (activeCommand == "FORWARD" && isEnabled) 2.dp else 1.dp,
                if (activeCommand == "FORWARD" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .size(56.dp)
                .testTag("rover_ctrl_forward")
                .semantics { role = Role.Button }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Drive Forward",
                    tint = if (activeCommand == "FORWARD" && isEnabled) TextPrimary else DarkGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Middle Row: Left, Center STOP, Right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left
            Surface(
                onClick = { onDirectionClick("TURN LEFT") },
                enabled = isEnabled,
                shape = CircleShape,
                color = if (activeCommand == "TURN LEFT" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    if (activeCommand == "TURN LEFT" && isEnabled) 2.dp else 1.dp,
                    if (activeCommand == "TURN LEFT" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("rover_ctrl_left")
                    .semantics { role = Role.Button }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Turn Left",
                        tint = if (activeCommand == "TURN LEFT" && isEnabled) TextPrimary else DarkGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Center STOP Nub
            Surface(
                onClick = onStopClick,
                enabled = isEnabled,
                shape = CircleShape,
                color = if (isEnabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    2.dp,
                    if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("rover_ctrl_stop")
                    .semantics { role = Role.Button }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Hold Position / Stop",
                            tint = if (isEnabled) MaterialTheme.colorScheme.error else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "STOP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.error else Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Right
            Surface(
                onClick = { onDirectionClick("TURN RIGHT") },
                enabled = isEnabled,
                shape = CircleShape,
                color = if (activeCommand == "TURN RIGHT" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    if (activeCommand == "TURN RIGHT" && isEnabled) 2.dp else 1.dp,
                    if (activeCommand == "TURN RIGHT" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("rover_ctrl_right")
                    .semantics { role = Role.Button }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Turn Right",
                        tint = if (activeCommand == "TURN RIGHT" && isEnabled) TextPrimary else DarkGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Reverse (Down)
        Surface(
            onClick = { onDirectionClick("REVERSE") },
            enabled = isEnabled,
            shape = CircleShape,
            color = if (activeCommand == "REVERSE" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                if (activeCommand == "REVERSE" && isEnabled) 2.dp else 1.dp,
                if (activeCommand == "REVERSE" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .size(56.dp)
                .testTag("rover_ctrl_reverse")
                .semantics { role = Role.Button }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Drive Reverse",
                    tint = if (activeCommand == "REVERSE" && isEnabled) TextPrimary else DarkGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vector Readout for semantic assertion in tests
        Text(
            text = "Vector: ${activeCommand}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (activeCommand == "HOLD POSITION") Color.Gray else DarkGreen,
            fontSize = 11.sp
        )
    }
}

/**
 * Auxiliary Controls Grid: All Lights, Front Light, Side Light, Horn, Chute Arm.
 */
@Composable
private fun AuxiliaryControlsGrid(
    telemetry: RoverTelemetry,
    viewModel: FarmViewModel
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // 2x2 Toggle Grid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // All Lights Toggle
                AuxToggleButton(
                    icon = Icons.Rounded.Lightbulb,
                    label = "All Lights",
                    isActive = telemetry.isFrontLightOn && telemetry.isSideLightOn,
                    activeColor = Color(0xFFF39C12),
                    onClick = {
                        val turnOn = !(telemetry.isFrontLightOn && telemetry.isSideLightOn)
                        if (turnOn) {
                            if (!telemetry.isFrontLightOn) viewModel.toggleFrontLight()
                            if (!telemetry.isSideLightOn) viewModel.toggleSideLight()
                        } else {
                            if (telemetry.isFrontLightOn) viewModel.toggleFrontLight()
                            if (telemetry.isSideLightOn) viewModel.toggleSideLight()
                        }
                    }
                )

                // Front Light Toggle
                AuxToggleButton(
                    icon = Icons.Rounded.Highlight,
                    label = "Front Light",
                    isActive = telemetry.isFrontLightOn,
                    activeColor = DarkGreen,
                    onClick = { viewModel.toggleFrontLight() }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Side Light Toggle
                AuxToggleButton(
                    icon = Icons.Rounded.LightMode,
                    label = "Side Light",
                    isActive = telemetry.isSideLightOn,
                    activeColor = Color(0xFFE67E22),
                    onClick = { viewModel.toggleSideLight() }
                )

                // Horn Action
                AuxToggleButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = "Horn",
                    isActive = telemetry.isHornActive,
                    activeColor = DarkGreen,
                    onClick = { viewModel.triggerHorn() }
                )
            }
        }

        // Chute / Arm Stepper Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            Text(
                text = "Arm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { /* Stepper Up Demo */ }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Chute Up",
                        tint = DarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = "Tool Arm",
                        tint = DarkGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { /* Stepper Down Demo */ }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Chute Down",
                        tint = DarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuxToggleButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.size(52.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bottom Cockpit Row: Camera Tilt + Large Red STOP Button + Irrigation / Spray Toggles.
 */
@Composable
private fun BottomCockpitRow(
    telemetry: RoverTelemetry,
    viewModel: FarmViewModel
) {
    val statusColors = LocalAgriStatusColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Tilt
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .height(48.dp)
                    .weight(0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Camera Tilt",
                        tint = DarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tilt: ${telemetry.cameraTiltAngle}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                    Row {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Tilt Up",
                            tint = DarkGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.setCameraTilt(telemetry.cameraTiltAngle + 5) }
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Tilt Down",
                            tint = DarkGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.setCameraTilt(telemetry.cameraTiltAngle - 5) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

            // Prominent Red EMERGENCY STOP / SAFETY Button
            Surface(
                onClick = {
                    if (!telemetry.isEmergencyCutoff) {
                        viewModel.setEmergencyStop(true)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFC0392B),
                border = BorderStroke(2.dp, Color(0xFFE74C3C)),
                modifier = Modifier
                    .height(52.dp)
                    .weight(1.3f)
                    .testTag("rover_ctrl_estop")
                    .semantics { role = Role.Button },
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "EMERGENCY STOP",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EMERGENCY STOP",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

            // Tool Toggles: Irrigation & Spray
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Irrigation Toggle
                Surface(
                    onClick = { viewModel.toggleAuxIrrigation() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (telemetry.isAuxIrrigationActive) LocalAgriStatusColors.current.info.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (telemetry.isAuxIrrigationActive) LocalAgriStatusColors.current.info else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = "Irrigation",
                            tint = if (telemetry.isAuxIrrigationActive) LocalAgriStatusColors.current.info else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (telemetry.isAuxIrrigationActive) "IRR: ON" else "IRR: OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.isAuxIrrigationActive) LocalAgriStatusColors.current.info else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Spray Toggle
                Surface(
                    onClick = { viewModel.toggleSpray() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (telemetry.isSprayActive) LightLime else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (telemetry.isSprayActive) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Build,
                            contentDescription = "Spray",
                            tint = if (telemetry.isSprayActive) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (telemetry.isSprayActive) "SPY: ON" else "SPY: OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.isSprayActive) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Latching E-Stop Notification & Re-Arm Button
        if (telemetry.isEmergencyCutoff) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = statusColors.error.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, statusColors.error)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FarmVisionSpacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = statusColors.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "POWER CUTOFF ENGAGED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColors.error
                        )
                    }
                    Text(
                        text = "Zero velocity latched. Actuation locked.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FarmVisionPrimaryButton(
                        text = "Reset & Re-arm Motors",
                        onClick = { viewModel.setEmergencyStop(false) },
                        leadingIcon = Icons.Rounded.Refresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rover_ctrl_rearm")
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SAFETY INTERLOCK: ARMED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
