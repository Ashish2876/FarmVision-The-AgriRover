package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors
import com.farmvision.agrirover.ui.theme.TelemetryValueLarge

/**
 * Standard agricultural metric classification providing clean defaults
 * without coupling to future hardware or domain layers.
 */
enum class FarmVisionMetricType(
    val defaultLabel: String,
    val defaultUnit: String,
    val icon: ImageVector
) {
    SoilMoisture("Soil Moisture", "%", Icons.Rounded.WaterDrop),
    Temperature("Temperature", "°C", Icons.Rounded.DeviceThermostat),
    AirHumidity("Air Humidity", "%", Icons.Rounded.Air),
    LightIntensity("Solar Radiation", "lx", Icons.Rounded.LightMode),
    BatteryPercentage("Battery Level", "%", Icons.Rounded.BatteryChargingFull)
}

/**
 * Directional trend indicator for dynamic telemetry values.
 */
enum class FarmVisionTrendDirection {
    Rising,
    Falling,
    Stable
}

/**
 * Reusable sensor metric card for agricultural telemetry displays.
 * Separates numeric values and measurement units cleanly, supports tabular figures
 * to avoid number jitter, and mandates explicit sample tagging for demo data.
 */
@Composable
fun FarmVisionMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    status: FarmVisionStatusType? = null,
    trend: FarmVisionTrendDirection? = null,
    trendLabel: String? = null,
    isSampleData: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    FarmVisionCard(
        modifier = modifier,
        onClick = onClick
    ) {
        // Header: Label, Icon, and optional Status / Sample indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
                        )
                    }
                    Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSampleData) {
                FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
            } else if (status != null) {
                FarmVisionStatusBadge(status = status)
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Body: Big Numeric Value (Tabular figures) + Unit + Trend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = TelemetryValueLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(FarmVisionSpacing.extraSmall))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (trend != null) {
                MetricTrendBadge(trend = trend, label = trendLabel)
            }
        }
    }
}

@Composable
private fun MetricTrendBadge(
    trend: FarmVisionTrendDirection,
    label: String?
) {
    val statusColors = LocalAgriStatusColors.current
    val (icon, color) = when (trend) {
        FarmVisionTrendDirection.Rising -> Icons.Rounded.ArrowUpward to statusColors.success
        FarmVisionTrendDirection.Falling -> Icons.Rounded.ArrowDownward to statusColors.warning
        FarmVisionTrendDirection.Stable -> Icons.Rounded.Remove to statusColors.idle
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(FarmVisionSpacing.iconSizeSmall)
        )
        if (label != null) {
            Spacer(modifier = Modifier.width(FarmVisionSpacing.xxSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
