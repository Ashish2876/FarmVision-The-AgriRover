package com.farmvision.agrirover.ui.analytics

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.ui.analytics.components.AccessibleHistoryTable
import com.farmvision.agrirover.ui.analytics.components.AnalyticsKpiSummaryCard
import com.farmvision.agrirover.ui.analytics.components.AnalyticsOverviewCards
import com.farmvision.agrirover.ui.analytics.components.CustomDateRangeDialog
import com.farmvision.agrirover.ui.analytics.components.HistoricalBoundedCanvasChart
import com.farmvision.agrirover.ui.analytics.components.HistoricalMetricSelector
import com.farmvision.agrirover.ui.analytics.components.HistoricalRangeSelector
import com.farmvision.agrirover.ui.analytics.components.HistoricalRecordsList
import com.farmvision.agrirover.ui.analytics.model.AnalyticsUiState
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Field Analytics Screen (Phase 9 — Analytics & Telemetry History).
 * Visualizes historical telemetry, multi-sensor overview averages, honest statistics,
 * bounded Canvas charts with gap breaks, an accessible table alternative, and reverse-chronological log records.
 *
 * Invariants:
 * - Extension of the existing Analytics destination (no new destination).
 * - Soil Probe 1 and Probe 2 are plotted as raw ADC counts on separate series (Default D11).
 * - Gaps in telemetry are preserved as line breaks (Default D9).
 * - Demo and Live data are never merged into a single statistic (Default D5).
 * - TalkBack accessibility semantics, 48dp touch targets, responsive phone/tablet layouts.
 */
@Composable
fun AnalyticsScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier,
    onAnalyzeTrendsWithAi: ((List<com.farmvision.agrirover.domain.assistant.model.AssistantHistoricalMetric>) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.analyticsUiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val legacyKpis = viewModel.getAnalyticsKpis(uiState.selectedRange, settings.temperatureUnit)

    val lastUpdatedStr = uiState.lastUpdatedEpochMillis?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss", Locale.getDefault()))
    } ?: "Awaiting samples"

    // Custom Date Range Dialog
    CustomDateRangeDialog(
        isOpen = uiState.isCustomDateRangePickerOpen,
        initialStartMillis = uiState.customStartEpochMillis,
        initialEndMillis = uiState.customEndEpochMillis,
        onConfirm = { start, end ->
            viewModel.setCustomDateRange(start, end)
        },
        onDismiss = {
            viewModel.setCustomDatePickerOpen(false)
        }
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("analytics_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Screen Header with Connection Status & Last Updated
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
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Last updated: $lastUpdatedStr • Status: ${uiState.connectionStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                val badgeType = if (uiState.isDemoModeActive) FarmVisionStatusType.DemoData else FarmVisionStatusType.Active
                FarmVisionStatusBadge(status = badgeType)
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // Honest Provenance Banner (Default D5 & Section 6.4)
            if (uiState.isDemoModeActive) {
                FarmVisionDemoBanner(
                    message = "DEMO DATA: Historical points, diurnal curves, and statistics are deterministic simulations. Not measured from physical hardware."
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LIVE HARDWARE HISTORY: Displaying real measurements stored from ESP32 telemetry. Gaps reflect unpolled periods.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            if (onAnalyzeTrendsWithAi != null) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                FarmVisionCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val stats = uiState.statistics
                        val metric = com.farmvision.agrirover.domain.assistant.model.AssistantHistoricalMetric(
                            metricName = uiState.selectedMetric.displayName,
                            rangeLabel = uiState.selectedRange.label,
                            minValue = stats.minValue,
                            maxValue = stats.maxValue,
                            avgValue = stats.sampleMean,
                            dataPointsCount = stats.count,
                            insufficientData = stats.count == 0
                        )
                        onAnalyzeTrendsWithAi(listOf(metric))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "🌱 Analyze Trends with AI",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Interpret ${uiState.selectedMetric.displayName} dynamics over ${uiState.selectedRange.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Ask AI →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Mode Toggle: Demo Analytics vs Live Hardware History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                ModeToggleChip(
                    label = "Demo Simulation",
                    isSelected = uiState.isDemoModeActive,
                    onClick = { viewModel.toggleAnalyticsDemoMode(true) },
                    modifier = Modifier.weight(1f),
                    testTag = "analytics_mode_demo_chip"
                )
                ModeToggleChip(
                    label = "Live Hardware",
                    isSelected = !uiState.isDemoModeActive,
                    onClick = { viewModel.toggleAnalyticsDemoMode(false) },
                    modifier = Modifier.weight(1f),
                    testTag = "analytics_mode_live_chip"
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Multi-Sensor Overview Cards (Section 7 & Defaults D5/D7)
            AnalyticsOverviewCards(overviewMetrics = uiState.overviewMetrics)

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            if (isWideScreen) {
                // Two-column layout for tablets and landscape
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.large)
                ) {
                    // Left Column: Selectors, KPI Summary, Agronomic Indicators
                    Column(modifier = Modifier.weight(1f)) {
                        SelectorsSection(uiState = uiState, viewModel = viewModel)

                        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

                        if (!uiState.isLoading && uiState.errorMessage == null && !uiState.isEmpty) {
                            AnalyticsKpiSummaryCard(
                                metric = uiState.selectedMetric,
                                statistics = uiState.statistics,
                                trend = uiState.trend,
                                provenanceLabel = uiState.provenanceLabel,
                                abnormalSummary = uiState.abnormalSummary
                            )
                            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))
                        }

                        AgronomicIndicatorsSection(
                            timeRange = uiState.selectedRange,
                            temperatureSymbol = settings.temperatureUnit.symbol,
                            avgMoisture = legacyKpis.avgMoisture,
                            thermalRange = legacyKpis.thermalRange,
                            irrigationVolume = legacyKpis.irrigationVolume
                        )
                    }

                    // Right Column: Chart / Table View, Historical Records Log
                    Column(modifier = Modifier.weight(1.2f)) {
                        VisualizationModeToggle(
                            isTableViewSelected = uiState.isTableViewSelected,
                            onToggleTable = { viewModel.setAnalyticsTableView(it) }
                        )

                        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                        VisualizationContent(
                            uiState = uiState,
                            onRetry = { viewModel.refreshAnalytics() },
                            onSwitchToDemo = { viewModel.toggleAnalyticsDemoMode(true) }
                        )

                        Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

                        HistoricalRecordsList(
                            records = uiState.recentRecords,
                            totalCount = uiState.totalRecordCount,
                            hasMore = uiState.hasMoreRecords,
                            onLoadMore = { viewModel.loadMoreRecentRecords() }
                        )
                    }
                }
            } else {
                // Single-column portrait layout for phones
                SelectorsSection(uiState = uiState, viewModel = viewModel)

                Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

                VisualizationModeToggle(
                    isTableViewSelected = uiState.isTableViewSelected,
                    onToggleTable = { viewModel.setAnalyticsTableView(it) }
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                VisualizationContent(
                    uiState = uiState,
                    onRetry = { viewModel.refreshAnalytics() },
                    onSwitchToDemo = { viewModel.toggleAnalyticsDemoMode(true) }
                )

                if (!uiState.isLoading && uiState.errorMessage == null && !uiState.isEmpty) {
                    Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

                    AnalyticsKpiSummaryCard(
                        metric = uiState.selectedMetric,
                        statistics = uiState.statistics,
                        trend = uiState.trend,
                        provenanceLabel = uiState.provenanceLabel,
                        abnormalSummary = uiState.abnormalSummary
                    )
                }

                Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

                AgronomicIndicatorsSection(
                    timeRange = uiState.selectedRange,
                    temperatureSymbol = settings.temperatureUnit.symbol,
                    avgMoisture = legacyKpis.avgMoisture,
                    thermalRange = legacyKpis.thermalRange,
                    irrigationVolume = legacyKpis.irrigationVolume
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

                HistoricalRecordsList(
                    records = uiState.recentRecords,
                    totalCount = uiState.totalRecordCount,
                    hasMore = uiState.hasMoreRecords,
                    onLoadMore = { viewModel.loadMoreRecentRecords() }
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

@Composable
private fun SelectorsSection(
    uiState: AnalyticsUiState,
    viewModel: FarmViewModel
) {
    Column {
        Text(
            text = "Select Sensor Metric",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
        HistoricalMetricSelector(
            selectedMetric = uiState.selectedMetric,
            onMetricSelected = { viewModel.setAnalyticsMetric(it) }
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        Text(
            text = "Time Window (Rolling & Custom)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
        HistoricalRangeSelector(
            selectedRange = uiState.selectedRange,
            onRangeSelected = { viewModel.setAnalyticsRange(it) }
        )
    }
}

@Composable
private fun VisualizationModeToggle(
    isTableViewSelected: Boolean,
    onToggleTable: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Visualization View",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (!isTableViewSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .selectable(
                        selected = !isTableViewSelected,
                        role = Role.Tab,
                        onClick = { onToggleTable(false) }
                    )
                    .testTag("chart_view_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Chart View",
                        tint = if (!isTableViewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Chart",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (!isTableViewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isTableViewSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .selectable(
                        selected = isTableViewSelected,
                        role = Role.Tab,
                        onClick = { onToggleTable(true) }
                    )
                    .testTag("table_view_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Table View",
                        tint = if (isTableViewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Table",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTableViewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualizationContent(
    uiState: AnalyticsUiState,
    onRetry: () -> Unit,
    onSwitchToDemo: () -> Unit
) {
    when {
        uiState.isLoading -> {
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Processing historical telemetry...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        uiState.errorMessage != null -> {
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to load analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Retry")
                    }
                }
            }
        }

        uiState.isEmpty -> {
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Live Telemetry Stored",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No live hardware samples have been recorded yet for ${uiState.selectedMetric.displayName} within the selected ${uiState.selectedRange.label} window.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onSwitchToDemo,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("switch_to_demo_analytics_button")
                    ) {
                        Text("View Labelled Demo Analytics")
                    }
                }
            }
        }

        else -> {
            if (uiState.isTableViewSelected) {
                AccessibleHistoryTable(
                    series = uiState.primarySeries!!
                )
            } else {
                HistoricalBoundedCanvasChart(
                    primarySeries = uiState.primarySeries!!,
                    secondarySeries = uiState.secondarySeries,
                    timeRange = uiState.selectedRange
                )
            }
        }
    }
}

@Composable
private fun AgronomicIndicatorsSection(
    timeRange: AnalyticsTimeRange,
    temperatureSymbol: String,
    avgMoisture: String,
    thermalRange: String,
    irrigationVolume: String
) {
    Column {
        FarmVisionSectionHeader(
            title = "Aggregated Agronomic Indicators",
            description = "${timeRange.label} summary metrics ($temperatureSymbol)"
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            KpiBox(
                title = "Avg Moisture",
                value = avgMoisture,
                subtitle = "Nominal",
                modifier = Modifier.weight(1f)
            )

            KpiBox(
                title = "Thermal Range",
                value = thermalRange,
                subtitle = "Optimal",
                modifier = Modifier.weight(1f)
            )

            KpiBox(
                title = "Irrigation",
                value = irrigationVolume,
                subtitle = "Logged",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KpiBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant),
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
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Preview(name = "Analytics Phone Portrait", showBackground = true)
@Composable
private fun AnalyticsPhonePortraitPreview() {
    FarmVisionTheme {
        // Simple preview container
    }
}

@Preview(name = "Analytics Tablet Landscape", widthDp = 840, heightDp = 480, showBackground = true)
@Composable
private fun AnalyticsTabletLandscapePreview() {
    FarmVisionTheme {
        // Simple preview container
    }
}

@Preview(name = "Analytics 2.0x Font Scaling", fontScale = 2.0f, showBackground = true)
@Composable
private fun AnalyticsFontScalePreview() {
    FarmVisionTheme {
        // Simple preview container
    }
}
