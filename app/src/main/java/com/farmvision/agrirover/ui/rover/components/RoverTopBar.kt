package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.rover.controller.RoverHudState
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen

/**
 * Top Status Bar for the Rover Control Station matching the reference station design.
 * Features:
 * - Back button with circular glass outline
 * - FarmVision AgriRover logo branding
 * - 5 Status Pills: Connection, Camera, Battery (with honesty), Drive Mode, Runtime
 * - Settings and Fullscreen action buttons
 */
@Composable
fun RoverTopBar(
    hudState: RoverHudState,
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    onBadgeClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onFullscreenToggle: (() -> Unit)? = null
) {
    val colors = ControlStationTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Back button & Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.3f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceGlass)
                    .border(BorderStroke(1.dp, colors.borderGlow), CircleShape)
                    .clickable { onExit?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Exit Controller",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Branding Logo
            RoverCustomIcons.FarmLeaf(
                modifier = Modifier.size(24.dp),
                tint = ParrotGreen
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = "FarmVision",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = "AgriRover",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        color = ParrotGreen
                    )
                )
            }
        }

        // 2. Middle: Status Pills Strip
        Row(
            modifier = Modifier.weight(3.5f),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill 1: Connection
            RoverTopStatusPill(
                icon = Icons.Rounded.Wifi,
                title = hudState.connectionTitle,
                subtitle = hudState.connectionSubtitle,
                statusColor = if (hudState.isConnected) ParrotGreen else colors.textMuted,
                onClick = onBadgeClick
            )

            // Pill 2: Camera
            RoverTopStatusPill(
                icon = Icons.Rounded.CameraAlt,
                title = hudState.cameraTitle,
                subtitle = hudState.cameraSubtitle,
                statusColor = if (hudState.isCameraActive) ParrotGreen else colors.textMuted,
                onClick = onBadgeClick
            )

            // Pill 3: Battery (with honesty rules)
            RoverTopStatusPill(
                icon = Icons.Rounded.BatteryChargingFull,
                title = hudState.batteryValue,
                subtitle = hudState.batterySubtitle,
                statusColor = if (hudState.isBatteryAvailable) ParrotGreen else colors.textSubtle,
                onClick = onBadgeClick
            )

            // Pill 4: Drive Mode
            RoverTopStatusPill(
                customIcon = {
                    RoverCustomIcons.SteeringWheel(
                        modifier = Modifier.size(14.dp),
                        tint = ParrotGreen
                    )
                },
                title = hudState.driveModeTitle,
                subtitle = hudState.driveModeSubtitle,
                statusColor = ParrotGreen,
                onClick = null
            )

            // Pill 5: Runtime
            RoverTopStatusPill(
                icon = Icons.Rounded.Schedule,
                title = hudState.runtimeFormatted,
                subtitle = "Runtime",
                statusColor = colors.textPrimary,
                onClick = null
            )
        }

        // 3. Right: Settings & Fullscreen buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            modifier = Modifier.weight(0.7f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceGlass)
                    .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(8.dp))
                    .clickable { onSettingsClick?.invoke() ?: onBadgeClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceGlass)
                    .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(8.dp))
                    .clickable { onFullscreenToggle?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RoverTopStatusPill(
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String,
    statusColor: Color,
    onClick: (() -> Unit)? = null
) {
    val colors = ControlStationTheme.colors

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfacePill)
            .border(BorderStroke(1.dp, colors.borderGlow.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon()
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = colors.textPrimary
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    color = colors.textMuted
                ),
                maxLines = 1
            )
        }
    }
}

