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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.rover.controller.RoverHudState
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen

/**
 * Individual Telemetry tile matching the reference station dark glass aesthetics.
 */
@Composable
fun TelemetryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    iconTint: Color = ParrotGreen
) {
    val colors = ControlStationTheme.colors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceTile)
            .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    color = colors.textMuted
                ),
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.5.sp,
                            color = colors.textSubtle
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 2-column x 4-row Telemetry grid plus 'More Telemetry' action card.
 */
@Composable
fun TelemetryTileGrid(
    hudState: RoverHudState,
    onNavigateToHardwareCenter: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Speed & Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TelemetryTile(
                icon = Icons.Rounded.Speed,
                label = "Speed",
                value = hudState.speedValue,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Rounded.LocationOn,
                label = "Distance",
                value = hudState.distanceValue,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Battery & Signal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TelemetryTile(
                icon = Icons.Rounded.BatteryChargingFull,
                label = "Battery",
                value = hudState.batteryValue,
                subtitle = if (hudState.isBatteryAvailable) hudState.batterySubtitle else null,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Rounded.SignalCellularAlt,
                label = "Signal",
                value = hudState.signalValue,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Temperature & Humidity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TelemetryTile(
                icon = Icons.Rounded.Thermostat,
                label = "Temperature",
                value = hudState.temperatureValue,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Rounded.WaterDrop,
                label = "Humidity",
                value = hudState.humidityValue,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 4: Soil Moisture & Soil pH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TelemetryTile(
                icon = Icons.Rounded.Eco,
                label = "Soil Moisture",
                value = hudState.soilMoistureValue,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                customIcon = {
                    RoverCustomIcons.Flask(
                        modifier = Modifier.size(16.dp),
                        tint = ParrotGreen
                    )
                },
                label = "Soil pH",
                value = hudState.soilPhValue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Row 5: More Telemetry Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceTile)
                .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(8.dp))
                .clickable { onNavigateToHardwareCenter?.invoke() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = ParrotGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "More Telemetry",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = colors.textPrimary
                    )
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Navigate to Telemetry",
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
