package com.farmvision.agrirover.ui.rover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.ui.rover.components.CircularDirectionalController
import com.farmvision.agrirover.ui.rover.components.EmergencyRearmBanner
import com.farmvision.agrirover.ui.rover.components.EmergencyStopButton
import com.farmvision.agrirover.ui.rover.components.MiniMapCard
import com.farmvision.agrirover.ui.rover.components.RoverCameraView
import com.farmvision.agrirover.ui.rover.components.RoverCustomIcons
import com.farmvision.agrirover.ui.rover.components.RoverModeSelector
import com.farmvision.agrirover.ui.rover.components.RoverStick
import com.farmvision.agrirover.ui.rover.components.RoverToolbar
import com.farmvision.agrirover.ui.rover.components.RoverTopBar
import com.farmvision.agrirover.ui.rover.components.SpeedTile
import com.farmvision.agrirover.ui.rover.components.StickAxisLock
import com.farmvision.agrirover.ui.rover.components.TelemetryTileGrid
import com.farmvision.agrirover.ui.rover.controller.RoverHudPresenter
import com.farmvision.agrirover.ui.rover.controller.TwinStickMapper
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * Premium Landscape Rover Control Station matching the reference station design.
 * Comprises:
 * 1. Top Bar: Exit button, branding, 5 Status Pills, action icons.
 * 2. Left Toolbar: Vertical actions (Capture, Record, Lights, Map, Pad mode, Waypoint).
 * 3. Main Center Viewport: Simulated camera view with perspective rows and reticle.
 * 4. Twin Tactile Sticks: Move (throttle, Y-only) & Steer (heading, X-only).
 * 5. Bottom Control Cluster: Drive Mode Pills, Speed preset dropdown, Emergency Stop, Camera selector.
 * 6. Right Side Panel: Tactical Mini-Map Card, 2x4 Telemetry grid, 'More Telemetry' navigation.
 */
@Composable
fun RoverLandscapeCockpit(
    telemetry: RoverTelemetry,
    settings: FarmSettings,
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHardwareCenter: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null
) {
    val colors = ControlStationTheme.colors
    val sessionSeconds by viewModel.sessionDurationSeconds.collectAsState()
    val hudState = remember(telemetry, settings, sessionSeconds) {
        RoverHudPresenter.present(telemetry, settings, sessionSeconds)
    }

    var isMapExpanded by remember { mutableStateOf(false) }
    var isPadMode by remember { mutableStateOf(false) }
    var rawThrottleY by remember { mutableFloatStateOf(0f) }
    var rawSteerX by remember { mutableFloatStateOf(0f) }

    val updateTwinStick = remember(viewModel, telemetry.isEmergencyCutoff) {
        { throttle: Float, steer: Float ->
            rawThrottleY = throttle
            rawSteerX = steer
            if (!telemetry.isEmergencyCutoff) {
                val combined = TwinStickMapper.combineTwinStick(
                    rawThrottleY = throttle,
                    rawSteerX = steer,
                    pivotTurnEnabled = false
                )
                if (combined.isNeutral) {
                    viewModel.onJoystickReleased()
                } else {
                    viewModel.onJoystickMoved(combined)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. TOP STATUS BAR
        RoverTopBar(
            hudState = hudState,
            onExit = onExit,
            onBadgeClick = onNavigateToHardwareCenter,
            onSettingsClick = onNavigateToHardwareCenter,
            onFullscreenToggle = {}
        )

        // 2. MAIN COCKPIT BODY
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // LEFT VERTICAL TOOLBAR (~52 dp)
            RoverToolbar(
                isLightsOn = telemetry.isFrontLightOn,
                onToggleLights = { viewModel.toggleFrontLight() },
                isMapExpanded = isMapExpanded,
                onToggleMap = { isMapExpanded = !isMapExpanded },
                isPadMode = isPadMode,
                onTogglePadMode = { isPadMode = !isPadMode },
                onCaptureClick = {},
                onRecordClick = {}
            )

            // CENTER FLANK & CAMERA VIEWPORT (~70% width)
            Box(
                modifier = Modifier
                    .weight(3.4f)
                    .fillMaxHeight()
            ) {
                // Main Camera Screen
                RoverCameraView(
                    isSimulation = telemetry.isSimulation,
                    isHardwareConnected = telemetry.connectionStatus == com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType.Online,
                    modifier = Modifier.fillMaxSize()
                )

                // OVERLAY: LEFT CONTROLLER (Move Stick or Directional D-Pad)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                ) {
                    if (isPadMode) {
                        CircularDirectionalController(
                            activeCommand = telemetry.activeCommand,
                            isEnabled = !telemetry.isEmergencyCutoff,
                            onDirectionClick = { dir ->
                                viewModel.updateRoverMovement(dir, telemetry.speedPreset.maxSpeedKmh)
                            },
                            onStopClick = {
                                viewModel.updateRoverMovement("HOLD POSITION", 0f)
                            }
                        )
                    } else {
                        RoverStick(
                            axisLock = StickAxisLock.Vertical,
                            label = "Move",
                            enabled = !telemetry.isEmergencyCutoff,
                            testTag = "rover_ctrl_joystick",
                            onValueChange = { valY ->
                                updateTwinStick(valY, rawSteerX)
                            }
                        ) {
                            RoverCustomIcons.Tractor(
                                modifier = Modifier.size(30.dp),
                                tint = DarkGreen
                            )
                        }
                    }
                }

                // OVERLAY: RIGHT CONTROLLER (Steer Stick)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                ) {
                    RoverStick(
                        axisLock = StickAxisLock.Horizontal,
                        label = "Steer",
                        enabled = !telemetry.isEmergencyCutoff,
                        onValueChange = { valX ->
                            updateTwinStick(rawThrottleY, valX)
                        }
                    ) {
                        RoverCustomIcons.SteeringWheel(
                            modifier = Modifier.size(30.dp),
                            tint = DarkGreen
                        )
                    }
                }

                // OVERLAY: CENTER BOTTOM CLUSTER (Modes, Speed, E-Stop, Camera)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Re-arm banner when latched
                    if (telemetry.isEmergencyCutoff) {
                        EmergencyRearmBanner(
                            onRearm = { viewModel.setEmergencyStop(false) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Mode Pills
                    RoverModeSelector(
                        currentMode = telemetry.driveModeEnum,
                        onSelectMode = { mode -> viewModel.setDriveMode(mode) }
                    )

                    // Bottom Row: Speed Tile, Large Emergency Stop, Camera Tile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Speed Preset Tile
                        SpeedTile(
                            currentPreset = telemetry.speedPreset,
                            onSelectPreset = { preset -> viewModel.setSpeedPreset(preset) }
                        )

                        // Central Emergency Stop
                        EmergencyStopButton(
                            isLatched = telemetry.isEmergencyCutoff,
                            onEmergencyStop = { viewModel.setEmergencyStop(true) },
                            onRearm = { viewModel.setEmergencyStop(false) },
                            size = 88.dp
                        )

                        // Camera Selector Tile
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceGlass)
                                .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Camera Stream",
                                tint = ParrotGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Camera",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        color = colors.textMuted
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Front",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT PANEL: MINI-MAP & TELEMETRY TILES (~20% width)
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tactical Mini-Map
                MiniMapCard()

                // Telemetry Tiles & More Telemetry Bar
                TelemetryTileGrid(
                    hudState = hudState,
                    onNavigateToHardwareCenter = onNavigateToHardwareCenter
                )
            }
        }
    }
}

