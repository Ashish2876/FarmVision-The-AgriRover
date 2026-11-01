package com.farmvision.agrirover.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.HistoricalDataBucket
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.HistoricalSeries
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.ChartSeriesSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * High-performance, bounded Canvas chart with gap breaks and dual-series plotting (Default D9, D10, D11).
 *
 * Invariants:
 * - Line stroke breaks wherever a gap exists (isGap = true). Never draws across gaps.
 * - Soil Probe 1 and Probe 2 are rendered on separate series with distinct colors and legend.
 * - Full TalkBack accessibility via merged semantics contentDescription.
 */
@Composable
fun HistoricalBoundedCanvasChart(
    primarySeries: HistoricalSeries,
    secondarySeries: HistoricalSeries?,
    timeRange: AnalyticsTimeRange,
    modifier: Modifier = Modifier
) {
    val primaryBuckets = primarySeries.buckets
    val secondaryBuckets = secondarySeries?.buckets

    // Determine value domain extremes across both series
    val pMin = primaryBuckets.mapNotNull { it.min }.minOrNull()
    val pMax = primaryBuckets.mapNotNull { it.max }.maxOrNull()
    val sMin = secondaryBuckets?.mapNotNull { it.min }?.minOrNull()
    val sMax = secondaryBuckets?.mapNotNull { it.max }?.maxOrNull()

    val combinedMin = listOfNotNull(pMin, sMin).minOrNull() ?: 0f
    val combinedMax = listOfNotNull(pMax, sMax).maxOrNull() ?: 100f
    // Pad bounds slightly to avoid clipping nodes at edges
    val rangeSpan = (combinedMax - combinedMin).coerceAtLeast(1f)
    val chartMin = combinedMin - (rangeSpan * 0.05f)
    val chartMax = combinedMax + (rangeSpan * 0.05f)

    val formatter = when (timeRange) {
        AnalyticsTimeRange.Hours1,
        AnalyticsTimeRange.Hours6,
        AnalyticsTimeRange.Hours24 -> DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        AnalyticsTimeRange.Days7 -> DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        AnalyticsTimeRange.Days30,
        AnalyticsTimeRange.Custom -> DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
    }

    val primaryColor = DarkGreen
    val secondaryColor = ChartSeriesSecondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    val accessibilityDesc = buildString {
        append("${primarySeries.metric.displayName} chart over ${timeRange.label}. ")
        append("Axis: ${primarySeries.metric.axisLabel}. ")
        append("Minimum: ${String.format(Locale.US, "%.1f", combinedMin)}, Maximum: ${String.format(Locale.US, "%.1f", combinedMax)}. ")
        if (secondarySeries != null) {
            append("Dual series comparing Probe 1 and Probe 2. ")
        }
        append("Trend: ${primarySeries.trend.direction.label}. ")
        append("Data source: ${primarySeries.origin.label}.")
    }

    FarmVisionCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("historical_canvas_chart_card")
    ) {
        // Chart Header & Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = primarySeries.metric.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("chart_metric_title")
                )
                Text(
                    text = "Axis: ${primarySeries.metric.axisLabel} • ${timeRange.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Legend if dual series
        if (secondarySeries != null) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.large)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(primaryColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Probe 1 (Raw ADC)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(secondaryColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Probe 2 (Raw ADC)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Main Chart Canvas with merged accessibility semantics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("historical_canvas_chart")
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDesc
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw subtle horizontal grid lines
                val gridStroke = 1f
                drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = gridStroke)
                drawLine(gridColor, Offset(0f, height * 0.50f), Offset(width, height * 0.50f), strokeWidth = gridStroke)
                drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = gridStroke)

                val effectiveSpan = (chartMax - chartMin).coerceAtLeast(1f)

                // Draw secondary series first (if available) with dashed stroke
                if (secondaryBuckets != null && secondaryBuckets.isNotEmpty()) {
                    drawSeries(
                        buckets = secondaryBuckets,
                        color = secondaryColor,
                        chartMin = chartMin,
                        effectiveSpan = effectiveSpan,
                        width = width,
                        height = height,
                        isDashed = true
                    )
                }

                // Draw primary series with solid stroke
                if (primaryBuckets.isNotEmpty()) {
                    drawSeries(
                        buckets = primaryBuckets,
                        color = primaryColor,
                        chartMin = chartMin,
                        effectiveSpan = effectiveSpan,
                        width = width,
                        height = height,
                        isDashed = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // X-Axis Timestamp Tick Labels
        if (primaryBuckets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = (primaryBuckets.size / 5).coerceAtLeast(1)
                for (i in primaryBuckets.indices step step) {
                    val b = primaryBuckets[i]
                    val timeStr = Instant.ofEpochMilli(b.centerTimeMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
    buckets: List<HistoricalDataBucket>,
    color: Color,
    chartMin: Float,
    effectiveSpan: Float,
    width: Float,
    height: Float,
    isDashed: Boolean
) {
    val totalBuckets = buckets.size
    if (totalBuckets < 2) return

    val bucketWidth = width / (totalBuckets - 1)
    val path = Path()
    var isDrawing = false

    buckets.forEachIndexed { index, b ->
        if (b.isGap || b.avg == null) {
            // Gap detected: break path so line does NOT bridge across missing data
            isDrawing = false
        } else {
            val x = index * bucketWidth
            val normY = (b.avg - chartMin) / effectiveSpan
            val y = height - (normY * height).coerceIn(0f, height)

            if (!isDrawing) {
                path.moveTo(x, y)
                isDrawing = true
            } else {
                path.lineTo(x, y)
            }

            // Draw individual bucket node dot
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }

    val pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
    )
}
