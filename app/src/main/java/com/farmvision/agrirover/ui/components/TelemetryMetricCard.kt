package com.farmvision.agrirover.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.BorderSubtleDark
import com.farmvision.agrirover.ui.theme.DarkSurfaceHigh
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.MintAccent
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusOnline
import com.farmvision.agrirover.ui.theme.StatusWarning
import com.farmvision.agrirover.ui.theme.TextMutedDark

enum class MetricTrend {
    Rising,
    Falling,
    Stable
}

/**
 * Reusable telemetry card displaying live telemetry metrics, safe threshold progress,
 * and trend dynamics for agriculture sensors and rover telemetry.
 */
@Composable
fun TelemetryMetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trend: MetricTrend = MetricTrend.Stable,
    trendLabel: String = "Stable",
    status: AgriStatusType = AgriStatusType.Nominal,
    progressFraction: Float? = null,
    onClick: (() -> Unit)? = null
) {
    val statusColor = when (status) {
        AgriStatusType.Nominal -> FreshLeaf
        AgriStatusType.Warning -> StatusWarning
        AgriStatusType.Alert -> StatusAlert
        AgriStatusType.Info -> MintAccent
        AgriStatusType.Standby -> TextMutedDark
    }

    GlassmorphicCard(
        modifier = modifier,
        borderColor = statusColor.copy(alpha = 0.35f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Icon, Title, and Trend Chip
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
                            .background(statusColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = TextMutedDark
                    )
                }

                TrendIndicator(trend = trend, label = trendLabel)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Value & Unit Display
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Optional Safe Threshold Gauge
            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = DarkSurfaceHigh
                )
            }
        }
    }
}

@Composable
private fun TrendIndicator(
    trend: MetricTrend,
    label: String
) {
    val (icon, color) = when (trend) {
        MetricTrend.Rising -> Icons.Rounded.ArrowUpward to FreshLeaf
        MetricTrend.Falling -> Icons.Rounded.ArrowDownward to StatusWarning
        MetricTrend.Stable -> Icons.Rounded.Remove to TextMutedDark
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = color
        )
    }
}

@Preview(name = "Telemetry Card Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun TelemetryMetricCardPreview() {
    FarmVisionTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TelemetryMetricCard(
                title = "Soil Moisture",
                value = "42.8",
                unit = "%",
                icon = Icons.Rounded.WaterDrop,
                trend = MetricTrend.Rising,
                trendLabel = "+2.1%",
                progressFraction = 0.65f,
                status = AgriStatusType.Nominal
            )
        }
    }
}
