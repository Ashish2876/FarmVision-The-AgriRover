package com.farmvision.agrirover.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.SensorDataPoint
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * Field Analytics Screen.
 * Visualizes deterministic historical soil moisture and ambient temperature curves
 * corresponding to the user-selected time range (24 Hours, 7 Days, 30 Days) and unit preferences.
 * All historical measurements are strictly labelled as simulated DEMO DATA.
 */
@Composable
fun AnalyticsScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val currentRange by viewModel.analyticsRange.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val moisturePoints = viewModel.getSoilMoistureHistory(currentRange)
    val tempPoints = viewModel.getTemperatureHistory(currentRange, settings.temperatureUnit)
    val kpis = viewModel.getAnalyticsKpis(currentRange, settings.temperatureUnit)

    val tempMinVal = if (settings.temperatureUnit == TemperatureUnit.Fahrenheit) 55f else 15f
    val tempMaxVal = if (settings.temperatureUnit == TemperatureUnit.Fahrenheit) 95f else 35f

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Field Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Historical Sensor Trends & Agronomics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FarmVisionDemoBanner(
                message = "DEMO DATA: Time-series sensor points, averages, and historical telemetry are deterministic simulations."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Dynamic Time Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                AnalyticsTimeRange.entries.forEach { range ->
                    val isSelected = currentRange == range
                    Surface(
                        onClick = { viewModel.setAnalyticsRange(range) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // 1. Soil Moisture Trend Chart Card
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Soil Moisture History (%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Timeframe: ${currentRange.label} • Optimal rootzone band: 35% – 55%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

                SensorLineChart(
                    points = moisturePoints,
                    lineColor = FreshLeaf,
                    minValue = 30f,
                    maxValue = 55f
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    moisturePoints.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // 2. Ambient Temperature History Chart Card (respects unit preference)
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ambient Temperature (${settings.temperatureUnit.symbol})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Timeframe: ${currentRange.label} • Diurnal thermal swing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

                SensorLineChart(
                    points = tempPoints,
                    lineColor = Color(0xFFE5A93C),
                    minValue = tempMinVal,
                    maxValue = tempMaxVal
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tempPoints.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // Dynamic Summary KPI Cards corresponding to selected range
            FarmVisionSectionHeader(
                title = "Aggregated Agronomic KPIs",
                description = "${currentRange.label} summary metrics (${settings.temperatureUnit.symbol})"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                KpiCard(
                    title = "Avg Moisture",
                    value = kpis.avgMoisture,
                    subtitle = "Nominal",
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Thermal Range",
                    value = kpis.thermalRange,
                    subtitle = "Optimal",
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Irrigation",
                    value = kpis.irrigationVolume,
                    subtitle = "Logged",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

@Composable
private fun SensorLineChart(
    points: List<SensorDataPoint>,
    lineColor: Color,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val spacing = width / (points.size - 1)

            // Draw subtle horizontal grid lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height * 0.50f), Offset(width, height * 0.50f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1f)

            val path = Path()
            val valueRange = (maxValue - minValue).coerceAtLeast(1f)

            points.forEachIndexed { index, point ->
                val x = index * spacing
                val normalizedY = (point.value - minValue) / valueRange
                val y = height - (normalizedY * height).coerceIn(0f, height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw point node
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
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
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
