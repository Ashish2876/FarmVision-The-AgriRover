package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen

/**
 * Speed preset selector tile positioned next to Emergency Stop in the bottom cockpit.
 * Displays speed icon, "Speed" label, current preset with dropdown caret,
 * and maintains `rover_speed_*` test tags for all presets.
 */
@Composable
fun SpeedTile(
    currentPreset: SpeedPreset,
    onSelectPreset: (SpeedPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceGlass)
                .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = "Speed Presets",
                tint = ParrotGreen,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        color = colors.textMuted
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentPreset.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Select Speed",
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.surfaceGlassDeep)
        ) {
            SpeedPreset.values().forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${preset.label} (${preset.maxSpeedKmh} km/h)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (preset == currentPreset) FontWeight.Bold else FontWeight.Normal,
                                    color = if (preset == currentPreset) ParrotGreen else colors.textPrimary
                                )
                            )
                            Text(
                                text = "Duty cycle ${(preset.dutyFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = colors.textMuted
                                )
                            )
                        }
                    },
                    onClick = {
                        onSelectPreset(preset)
                        expanded = false
                    },
                    modifier = Modifier.testTag("rover_speed_${preset.name.lowercase()}")
                )
            }
        }
    }
}

