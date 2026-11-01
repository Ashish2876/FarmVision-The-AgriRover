package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.TextPrimary

/**
 * Drive Mode selector pill bar positioned above the bottom control cluster.
 * Matches reference station:
 * - Manual (Gamepad icon)
 * - Auto (Compass/Explore icon)
 * - Follow (Pin icon / Hold Position)
 * Preserves `rover_mode_manual`, `rover_mode_autonomouswaypoint`, `rover_mode_holdposition` test tags.
 */
@Composable
fun RoverModeSelector(
    currentMode: RoverDriveMode,
    onSelectMode: (RoverDriveMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceGlass)
            .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(20.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Manual Mode
        RoverModePillItem(
            label = "Manual",
            isSelected = currentMode == RoverDriveMode.Manual,
            testTag = "rover_mode_manual",
            customIcon = { tint ->
                RoverCustomIcons.Gamepad(
                    modifier = Modifier.size(14.dp),
                    tint = tint
                )
            },
            onClick = { onSelectMode(RoverDriveMode.Manual) }
        )

        // Autonomous Waypoint Mode
        RoverModePillItem(
            label = "Auto",
            icon = Icons.Rounded.Explore,
            isSelected = currentMode == RoverDriveMode.AutonomousWaypoint,
            testTag = "rover_mode_autonomouswaypoint",
            onClick = { onSelectMode(RoverDriveMode.AutonomousWaypoint) }
        )

        // Hold Position / Follow Mode
        RoverModePillItem(
            label = "Follow",
            icon = Icons.Rounded.LocationOn,
            isSelected = currentMode == RoverDriveMode.HoldPosition,
            testTag = "rover_mode_holdposition",
            onClick = { onSelectMode(RoverDriveMode.HoldPosition) }
        )
    }
}

@Composable
private fun RoverModePillItem(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    customIcon: (@Composable (Color) -> Unit)? = null
) {
    val colors = ControlStationTheme.colors
    val bg = if (isSelected) ParrotGreen else Color.Transparent
    val contentColor = if (isSelected) TextPrimary else colors.textMuted

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon(contentColor)
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                color = contentColor
            )
        )
    }
}

