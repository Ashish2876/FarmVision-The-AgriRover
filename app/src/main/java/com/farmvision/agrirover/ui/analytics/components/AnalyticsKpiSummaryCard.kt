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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.HistoricalTrendDirection
import com.farmvision.agrirover.data.model.TelemetryStatistics
import com.farmvision.agrirover.data.model.TrendSummary
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Summary KPI and statistics card for Field Analytics (Default D7 & Section 7).
 * Reports:
 * - Latest valid reading with local timestamp.
 * - Arithmetic sample mean with honest disclaimer.
 * - Minimum, Maximum, and Valid reading count.
 * - Excluded readings count when non-zero.
 * - Factual trend observation with icon + text (never color alone).
 */
@Composable
fun AnalyticsKpiSummaryCard(
    metric: HistoricalMetric,
    statistics: TelemetryStatistics,
    trend: TrendSummary,
    provenanceLabel: String,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.getDefault())
    val latestTimeStr = statistics.latestValidSample?.let {
        Instant.ofEpochMilli(it.timestampEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    } ?: "No data"

    val unitStr = metric.canonicalUnit
    val meanStr = statistics.sampleMean?.let { String.format(Locale.US, "%.1f %s", it, unitStr) } ?: "--"
    val minStr = statistics.minValue?.let { String.format(Locale.US, "%.1f %s", it, unitStr) } ?: "--"
    val maxStr = statistics.maxValue?.let { String.format(Locale.US, "%.1f %s", it, unitStr) } ?: "--"
    val latestValStr = statistics.latestValidSample?.let { String.format(Locale.US, "%.1f %s", it.canonicalValue, unitStr) } ?: "--"

    FarmVisionCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_kpi_summary_card")
            .semantics(mergeDescendants = true) {
                contentDescription = "Summary for ${metric.displayName}: latest $latestValStr at $latestTimeStr, sample mean $meanStr, minimum $minStr, maximum $maxStr, count ${statistics.count}, trend ${trend.direction.label}, source $provenanceLabel"
            }
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Summary Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Arithmetic sample aggregates over selected window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val badgeStatus = if (provenanceLabel.contains("DEMO")) FarmVisionStatusType.DemoData else FarmVisionStatusType.Active
            FarmVisionStatusBadge(status = badgeStatus)
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Key Metric Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            // Latest Reading
            StatBox(
                label = "Latest Reading",
                value = latestValStr,
                subtext = latestTimeStr,
                modifier = Modifier.weight(1f)
            )

            // Sample Mean
            StatBox(
                label = "Sample Mean",
                value = meanStr,
                subtext = "Biased if uneven",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            // Min / Max Range
            StatBox(
                label = "Min / Max Range",
                value = "$minStr - $maxStr",
                subtext = "Observed extremes",
                modifier = Modifier.weight(1f)
            )

            // Sample Count
            StatBox(
                label = "Sample Count",
                value = "${statistics.count}",
                subtext = if (statistics.excludedCount > 0) "${statistics.excludedCount} excluded" else "All valid",
                modifier = Modifier.weight(1f)
            )
        }

        // Excluded Readings Notice (Section 6.3)
        if (statistics.excludedCount > 0) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Excluded readings warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${statistics.excludedCount} readings excluded (out-of-range, saturated ADC counts, or clock anomalies).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Factual Trend Observation (Section 7 & Default D8: never color alone)
        TrendObservationBox(trend = trend)
    }
}

@Composable
private fun StatBox(
    label: String,
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
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

@Composable
private fun TrendObservationBox(
    trend: TrendSummary,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAgriStatusColors.current
    val (icon, iconColor, bg) = when (trend.direction) {
        HistoricalTrendDirection.Rising -> Triple(Icons.Default.ArrowUpward, statusColors.online, statusColors.online.copy(alpha = 0.12f))
        HistoricalTrendDirection.Falling -> Triple(Icons.Default.ArrowDownward, statusColors.offline, statusColors.offline.copy(alpha = 0.12f))
        HistoricalTrendDirection.Steady -> Triple(Icons.AutoMirrored.Filled.ArrowForward, statusColors.connecting, statusColors.connecting.copy(alpha = 0.12f))
        HistoricalTrendDirection.InsufficientData -> Triple(Icons.Default.Info, statusColors.warning, statusColors.warning.copy(alpha = 0.12f))
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = trend.direction.label,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Trend: ${trend.direction.label}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = trend.displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
