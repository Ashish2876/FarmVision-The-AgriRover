package com.farmvision.agrirover.ui.analytics.model

import com.farmvision.agrirover.data.model.AbnormalReadingsSummary
import com.farmvision.agrirover.data.model.AnalyticsOverviewMetrics
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.HistoricalSeries
import com.farmvision.agrirover.data.model.HistoricalTrendDirection
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.data.model.TelemetryStatistics
import com.farmvision.agrirover.data.model.TrendSummary

/**
 * High-fidelity, reactive UI state for Field Analytics destination (Milestone N4 & Section 7).
 */
data class AnalyticsUiState(
    val selectedMetric: HistoricalMetric = HistoricalMetric.SoilMoistureProbe1Raw,
    val selectedRange: AnalyticsTimeRange = AnalyticsTimeRange.Hours24,
    val isDemoModeActive: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val primarySeries: HistoricalSeries? = null,
    val secondarySeries: HistoricalSeries? = null, // Used for dual-probe comparison (Probe 2 raw ADC) or demo vs live
    val statistics: TelemetryStatistics = TelemetryStatistics.empty(),
    val trend: TrendSummary = TrendSummary(HistoricalTrendDirection.InsufficientData, "Loading historical telemetry..."),
    val recentRecords: List<TelemetrySample> = emptyList(),
    val totalRecordCount: Int = 0,
    val recordsLimit: Int = 100,
    val hasMoreRecords: Boolean = false,
    val provenanceLabel: String = "DEMO DATA",
    val isTableViewSelected: Boolean = false,
    val abnormalSummary: AbnormalReadingsSummary = AbnormalReadingsSummary(),
    val overviewMetrics: AnalyticsOverviewMetrics = AnalyticsOverviewMetrics(),
    val connectionStatus: String = "OFFLINE",
    val isRoverConnected: Boolean = false,
    val lastUpdatedEpochMillis: Long? = null,
    val customStartEpochMillis: Long? = null,
    val customEndEpochMillis: Long? = null,
    val isCustomDateRangePickerOpen: Boolean = false
) {
    val isEmpty: Boolean
        get() = (primarySeries?.totalValidSamples?.isEmpty() ?: true) && !isLoading

    val isOnlyDemoData: Boolean
        get() = isDemoModeActive && (primarySeries?.origin?.name == "Simulated" || provenanceLabel.contains("DEMO"))
}
