package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen

/**
 * Vertical action toolbar on the left side of the Rover Control Station.
 * Matches reference station:
 * - Capture (photo snapshot)
 * - Record (video record)
 * - Lights (toggleable headlight)
 * - Map (mini-map toggle/highlight)
 * - Waypoint (waypoint marker)
 * - Pad (toggle D-Pad controller mode)
 */
@Composable
fun RoverToolbar(
    isLightsOn: Boolean,
    onToggleLights: () -> Unit,
    isMapExpanded: Boolean,
    onToggleMap: () -> Unit,
    isPadMode: Boolean,
    onTogglePadMode: () -> Unit,
    onCaptureClick: () -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors

    GlassPanel(
        modifier = modifier
            .width(52.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoverToolbarButton(
                icon = Icons.Rounded.CameraAlt,
                label = "Capture",
                isActive = false,
                onClick = onCaptureClick
            )

            RoverToolbarButton(
                icon = Icons.Rounded.Videocam,
                label = "Record",
                isActive = false,
                onClick = onRecordClick
            )

            RoverToolbarButton(
                icon = Icons.Rounded.Lightbulb,
                label = if (isLightsOn) "Lights\nON" else "Lights\nOFF",
                isActive = isLightsOn,
                onClick = onToggleLights
            )

            RoverToolbarButton(
                icon = Icons.Rounded.Map,
                label = "Map",
                isActive = isMapExpanded,
                onClick = onToggleMap
            )

            RoverToolbarButton(
                icon = Icons.Rounded.Directions,
                label = if (isPadMode) "D-Pad\nON" else "Sticks\nMode",
                isActive = isPadMode,
                onClick = onTogglePadMode
            )

            RoverToolbarButton(
                icon = Icons.Rounded.LocationOn,
                label = "Waypoint",
                isActive = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun RoverToolbarButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors
    val borderCol = if (isActive) ParrotGreen else colors.borderSubtle
    val bgCol = if (isActive) colors.surfaceTile else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgCol)
            .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) ParrotGreen else colors.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = if (isActive) ParrotGreen else colors.textMuted
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

