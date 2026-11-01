package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.CalibrationState
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.data.repository.InMemoryTelemetryHistoryRepository
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.data.time.FakeTimeProvider
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsViewModelTest {

    private val fixedNow = 1_700_000_000_000L
    private val timeProvider = FakeTimeProvider(fixedNow)

    @Test
    fun testInitialState_demoModeActive_loadsDualProbe() = runBlocking {
        val historyRepo = InMemoryTelemetryHistoryRepository()
        val mockRepo = MockFarmRepository(
            timeProvider = timeProvider,
            telemetryHistory = historyRepo
        )

        val viewModel = FarmViewModel(
            repository = mockRepo,
            defaultDispatcher = Dispatchers.Unconfined
        )

        val state = viewModel.analyticsUiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(HistoricalMetric.SoilMoistureProbe1Raw, state.selectedMetric)
        assertEquals(AnalyticsTimeRange.Hours24, state.selectedRange)
        assertTrue(state.isDemoModeActive)
        assertEquals("DEMO DATA", state.provenanceLabel)

        // Primary series: Probe 1
        assertNotNull(state.primarySeries)
        assertEquals(HistoricalMetric.SoilMoistureProbe1Raw, state.primarySeries?.metric)
        assertEquals(DataOrigin.Simulated, state.primarySeries?.origin)
        assertTrue("Primary series must have valid points", state.primarySeries!!.totalValidSamples.isNotEmpty())

        // Dual probe: Secondary series must be Probe 2
        assertNotNull(state.secondarySeries)
        assertEquals(HistoricalMetric.SoilMoistureProbe2Raw, state.secondarySeries?.metric)
        assertEquals(DataOrigin.Simulated, state.secondarySeries?.origin)

        // Same dataset invariant
        assertEquals(state.primarySeries!!.statistics, state.statistics)
        assertEquals(state.primarySeries!!.trend, state.trend)
    }

    @Test
    fun testMetricSelection_updatesPrimarySeries() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        viewModel.setAnalyticsMetric(HistoricalMetric.AirTemperature)

        val state = viewModel.analyticsUiState.value
        assertEquals(HistoricalMetric.AirTemperature, state.selectedMetric)
        assertEquals(HistoricalMetric.AirTemperature, state.primarySeries?.metric)
        assertNull("Non-probe metric must not have secondary dual-probe series", state.secondarySeries)
    }

    @Test
    fun testRangeSelection_recomputesWindow() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        viewModel.setAnalyticsRange(AnalyticsTimeRange.Days7)

        val state = viewModel.analyticsUiState.value
        assertEquals(AnalyticsTimeRange.Days7, state.selectedRange)
        assertEquals(AnalyticsTimeRange.Days7, viewModel.analyticsRange.value)
    }

    @Test
    fun testLiveMode_emptyHistory_honestEmptyState() = runBlocking {
        val historyRepo = InMemoryTelemetryHistoryRepository()
        val mockRepo = MockFarmRepository(
            timeProvider = timeProvider,
            telemetryHistory = historyRepo
        )

        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        // Switch to Live Mode (no live samples in repo)
        viewModel.toggleAnalyticsDemoMode(false)

        val state = viewModel.analyticsUiState.value
        assertFalse(state.isDemoModeActive)
        assertEquals("LIVE HARDWARE", state.provenanceLabel)
        assertEquals(0, state.statistics.count)
        assertTrue(state.isEmpty)
        assertTrue(state.primarySeries!!.totalValidSamples.isEmpty())
    }

    @Test
    fun testLiveMode_withRecordedLiveSamples() = runBlocking {
        val historyRepo = InMemoryTelemetryHistoryRepository(writeIntervalSeconds = 0L)
        val mockRepo = MockFarmRepository(
            timeProvider = timeProvider,
            telemetryHistory = historyRepo
        )

        // Seed live samples into history repository
        val liveSample1 = TelemetrySample(
            id = "live-1",
            metric = HistoricalMetric.AirTemperature,
            canonicalValue = 25.0f,
            rawValue = null,
            timestampEpochMillis = System.currentTimeMillis() - 60_000L,
            origin = DataOrigin.LiveHardware,
            calibrationState = CalibrationState.FactoryDefault
        )
        val liveSample2 = TelemetrySample(
            id = "live-2",
            metric = HistoricalMetric.AirTemperature,
            canonicalValue = 27.0f,
            rawValue = null,
            timestampEpochMillis = System.currentTimeMillis() - 10_000L,
            origin = DataOrigin.LiveHardware,
            calibrationState = CalibrationState.FactoryDefault
        )
        historyRepo.recordSample(liveSample1)
        historyRepo.recordSample(liveSample2)

        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)
        viewModel.setAnalyticsMetric(HistoricalMetric.AirTemperature)
        viewModel.toggleAnalyticsDemoMode(false)

        val state = viewModel.analyticsUiState.value
        assertFalse(state.isDemoModeActive)
        assertEquals(2, state.statistics.count)
        assertEquals(26.0f, state.statistics.sampleMean!!, 0.001f)
        assertEquals(DataOrigin.LiveHardware, state.primarySeries?.origin)
    }

    @Test
    fun testCancellation_rapidSelectorChanges() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        // Rapid changes
        viewModel.setAnalyticsMetric(HistoricalMetric.Humidity)
        viewModel.setAnalyticsMetric(HistoricalMetric.SoilPh)
        viewModel.setAnalyticsMetric(HistoricalMetric.UltrasonicDistance)

        val state = viewModel.analyticsUiState.value
        assertEquals(HistoricalMetric.UltrasonicDistance, state.selectedMetric)
        assertEquals(HistoricalMetric.UltrasonicDistance, state.primarySeries?.metric)
    }

    @Test
    fun testCustomDateRange_setsRangeAndComputesWindow() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        val start = fixedNow - 3_600_000L
        val end = fixedNow
        viewModel.setCustomDateRange(start, end)

        val state = viewModel.analyticsUiState.value
        assertEquals(AnalyticsTimeRange.Custom, state.selectedRange)
        assertEquals(start, state.customStartEpochMillis)
        assertEquals(end, state.customEndEpochMillis)
        assertFalse(state.isCustomDateRangePickerOpen)
    }

    @Test
    fun testCustomDatePickerOpen_togglesState() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        viewModel.setCustomDatePickerOpen(true)
        assertTrue(viewModel.analyticsUiState.value.isCustomDateRangePickerOpen)

        viewModel.setCustomDatePickerOpen(false)
        assertFalse(viewModel.analyticsUiState.value.isCustomDateRangePickerOpen)
    }

    @Test
    fun testAbnormalReadingsAndOverviewMetrics_presentInUiState() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        val state = viewModel.analyticsUiState.value
        assertEquals("DEMO DATA", state.overviewMetrics.sourceTag)
        assertNotNull(state.overviewMetrics.probe1RawAvg)
        assertNotNull(state.overviewMetrics.probe2RawAvg)
        assertNotNull(state.abnormalSummary)
    }

    @Test
    fun testTelemetryConnectionStatus_updatesUiState() = runBlocking {
        val mockRepo = MockFarmRepository(timeProvider = timeProvider)
        val viewModel = FarmViewModel(repository = mockRepo, defaultDispatcher = Dispatchers.Unconfined)

        val state = viewModel.analyticsUiState.value
        assertEquals(mockRepo.telemetryFlow.value.connectionStatus.defaultLabel, state.connectionStatus)
    }
}
