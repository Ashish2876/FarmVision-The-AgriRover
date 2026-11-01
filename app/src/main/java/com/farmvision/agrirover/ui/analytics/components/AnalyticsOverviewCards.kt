package com.farmvision.agrirover.ui.analytics.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.AnalyticsOverviewMetrics
import java.util.Locale

/**
 * Multi-sensor overview cards adhering to Section 7 & Defaults D5/D7.
 * Displays arithmetic sample averages over the selected time window.
 * Strictly separates Probe 1 and Probe 2 raw ADC counts without averaging them together.
 */
@Composable
fun AnalyticsOverviewCards(
    overviewMetrics: AnalyticsOverviewMetrics,
    modifier: Modifier = Modifier
) {
    val p1Str = overviewMetrics.probe1RawAvg?.let { String.format(Locale.US, "%.0f counts", it) } ?: "--"
    val p2Str = overviewMetrics.probe2RawAvg?.let { String.format(Locale.US, "%.0f counts", it) } ?: "--"
    val tempStr = overviewMetrics.airTempAvg?.let { String.format(Locale.US, "%.1f °C", it) } ?: "--"
    val humStr = overviewMetrics.humidityAvg?.let { String.format(Locale.US, "%.1f %%", it) } ?: "--"
    val phStr = overviewMetrics.soilPhAvg?.let { String.format(Locale.US, "%.2f pH", it) } ?: "--"
    val totalCount = overviewMetrics.totalSamplesCount

    FarmVisionCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_overview_cards")
            .semantics(mergeDescendants = true) {
                contentDescription = "Multi-sensor overview: Probe 1 raw average $p1Str, Probe 2 raw average $p2Str, Air temperature $tempStr, Humidity $humStr, Soil pH $phStr, Total samples $totalCount, Source ${overviewMetrics.sourceTag}"
            }
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sensor Overview Averages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Arithmetic sample means over active time window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val badgeStatus = if (overviewMetrics.sourceTag.contains("DEMO")) {
                FarmVisionStatusType.DemoData
            } else {
                FarmVisionStatusType.Active
            }
            FarmVisionStatusBadge(status = badgeStatus)
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Row 1: Dual Raw Probes (Independent, never combined)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            OverviewMetricTile(
                title = "Probe 1 Raw (Surface)",
                value = p1Str,
                subtext = "Uncalibrated ADC counts",
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_probe1_card")
            )
            OverviewMetricTile(
                title = "Probe 2 Raw (Deep)",
                value = p2Str,
                subtext = "Uncalibrated ADC counts",
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_probe2_card")
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Row 2: Atmospheric Readings (Air Temp & Relative Humidity)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            OverviewMetricTile(
                title = "Air Temperature",
                value = tempStr,
                subtext = "DHT22 sensor average",
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_air_temp_card")
            )
            OverviewMetricTile(
                title = "Relative Humidity",
                value = humStr,
                subtext = "DHT22 sensor average",
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_humidity_card")
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Row 3: Soil pH & Total Samples
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            OverviewMetricTile(
                title = "Soil pH (Est.)",
                value = phStr,
                subtext = "Standard 0-14 pH scale",
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_soil_ph_card")
            )
            OverviewMetricTile(
                title = "Window Samples",
                value = "$totalCount",
                subtext = "Total recorded records",
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_total_samples_card")
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Disclaimer footnote
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Dual capacitive probes are uncalibrated and reported separately; not averaged as a single percentage.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewMetricTile(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

