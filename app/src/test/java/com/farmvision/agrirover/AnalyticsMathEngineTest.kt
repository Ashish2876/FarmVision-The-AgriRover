package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.AnalyticsTimeWindow
import com.farmvision.agrirover.data.model.CalibrationState
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.HistoricalTrendDirection
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.domain.analytics.AnalyticsMathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AnalyticsMathEngineTest {

    private val fixedNow = 1_700_000_000_000L // Constant test epoch
    private val window24h = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)

    @Test
    fun testWindowBoundaries_exactInclusion() {
        val samples = listOf(
            TelemetrySample(
                id = "s-start",
                metric = HistoricalMetric.SoilMoistureProbe1Raw,
                canonicalValue = 1500f,
                rawValue = 1500f,
                timestampEpochMillis = window24h.startEpochMillis,
                origin = DataOrigin.LiveHardware
            ),
            TelemetrySample(
                id = "s-end",
                metric = HistoricalMetric.SoilMoistureProbe1Raw,
                canonicalValue = 1800f,
                rawValue = 1800f,
                timestampEpochMillis = window24h.endEpochMillis,
                origin = DataOrigin.LiveHardware
            ),
            TelemetrySample(
                id = "s-outside-before",
                metric = HistoricalMetric.SoilMoistureProbe1Raw,
                canonicalValue = 1200f,
                rawValue = 1200f,
                timestampEpochMillis = window24h.startEpochMillis - 1L,
                origin = DataOrigin.LiveHardware
            ),
            TelemetrySample(
                id = "s-outside-after",
                metric = HistoricalMetric.SoilMoistureProbe1Raw,
                canonicalValue = 1900f,
                rawValue = 1900f,
                timestampEpochMillis = window24h.endEpochMillis + 1L,
                origin = DataOrigin.LiveHardware
            )
        )

        val (valid, excluded) = AnalyticsMathEngine.filterAndValidateWindow(samples, window24h, fixedNow)
        assertEquals(2, valid.size)
        assertEquals("s-start", valid[0].id)
        assertEquals("s-end", valid[1].id)
        assertEquals(0, excluded) // Outside window samples are not excluded anomalies, simply out-of-scope
    }

    @Test
    fun testDuplicateTimestamps_laterReceivedWins() {
        val timestamp = fixedNow - 3600_000L
        val sample1 = TelemetrySample(
            id = "s-early",
            metric = HistoricalMetric.AirTemperature,
            canonicalValue = 24.0f,
            timestampEpochMillis = timestamp,
            origin = DataOrigin.LiveHardware
        )
        val sample2 = TelemetrySample(
            id = "s-late",
            metric = HistoricalMetric.AirTemperature,
            canonicalValue = 25.5f,
            timestampEpochMillis = timestamp,
            origin = DataOrigin.LiveHardware
        )

        val (valid, _) = AnalyticsMathEngine.filterAndValidateWindow(listOf(sample1, sample2), window24h, fixedNow)
        assertEquals(1, valid.size)
        assertEquals("s-late", valid[0].id)
        assertEquals(25.5f, valid[0].canonicalValue, 0.001f)
    }

    @Test
    fun testFutureTimestamp_beyondToleranceRejected() {
        val futureBeyondTolerance = fixedNow + AnalyticsMathEngine.FUTURE_TIMESTAMP_TOLERANCE_MILLIS + 5000L
        val futureWithinTolerance = fixedNow + 10_000L

        val windowWithFuture = AnalyticsTimeWindow(
            range = AnalyticsTimeRange.Hours24,
            startEpochMillis = fixedNow - 86400_000L,
            endEpochMillis = fixedNow + 60_000L
        )

        val samples = listOf(
            TelemetrySample(
                id = "s-within-tolerance",
                metric = HistoricalMetric.Humidity,
                canonicalValue = 55f,
                timestampEpochMillis = futureWithinTolerance,
                origin = DataOrigin.LiveHardware
            ),
            TelemetrySample(
                id = "s-beyond-tolerance",
                metric = HistoricalMetric.Humidity,
                canonicalValue = 60f,
                timestampEpochMillis = futureBeyondTolerance,
                origin = DataOrigin.LiveHardware
            )
        )

        val (valid, excluded) = AnalyticsMathEngine.filterAndValidateWindow(samples, windowWithFuture, fixedNow)
        assertEquals(1, valid.size)
        assertEquals("s-within-tolerance", valid[0].id)
        assertEquals(1, excluded)
    }

    @Test
    fun testInvalidAndSaturatedExclusions() {
        val baseTime = fixedNow - 10000L
        val samples = listOf(
            // NaN
            TelemetrySample("s-nan", HistoricalMetric.AirTemperature, Float.NaN, null, baseTime, DataOrigin.LiveHardware),
            // Out of range (air temp > 70 C)
            TelemetrySample("s-high-temp", HistoricalMetric.AirTemperature, 95.0f, null, baseTime + 1000, DataOrigin.LiveHardware),
            // Raw ADC saturated low (<= 50)
            TelemetrySample("s-adc-low", HistoricalMetric.SoilMoistureProbe1Raw, 30f, 30f, baseTime + 2000, DataOrigin.LiveHardware),
            // Raw ADC saturated high (>= 4050)
            TelemetrySample("s-adc-high", HistoricalMetric.SoilMoistureProbe2Raw, 4080f, 4080f, baseTime + 3000, DataOrigin.LiveHardware),
            // Valid probe
            TelemetrySample("s-valid", HistoricalMetric.SoilMoistureProbe1Raw, 2100f, 2100f, baseTime + 4000, DataOrigin.LiveHardware)
        )

        val (valid, excluded) = AnalyticsMathEngine.filterAndValidateWindow(samples, window24h, fixedNow)
        assertEquals(1, valid.size)
        assertEquals("s-valid", valid[0].id)
        assertEquals(4, excluded)
    }

    @Test
    fun testStatistics_honestComputation() {
        val samples = listOf(
            TelemetrySample("1", HistoricalMetric.SoilPh, 6.0f, null, fixedNow - 30000, DataOrigin.LiveHardware),
            TelemetrySample("2", HistoricalMetric.SoilPh, 7.0f, null, fixedNow - 20000, DataOrigin.LiveHardware),
            TelemetrySample("3", HistoricalMetric.SoilPh, 8.0f, null, fixedNow - 10000, DataOrigin.LiveHardware)
        )

        val stats = AnalyticsMathEngine.computeStatistics(samples, excludedCount = 2)
        assertEquals(3, stats.count)
        assertEquals(7.0f, stats.sampleMean!!, 0.001f)
        assertEquals(6.0f, stats.minValue!!, 0.001f)
        assertEquals(8.0f, stats.maxValue!!, 0.001f)
        assertEquals(2, stats.excludedCount)
        assertEquals("3", stats.latestValidSample?.id)
    }

    @Test
    fun testStatistics_emptyDataset() {
        val stats = AnalyticsMathEngine.computeStatistics(emptyList(), excludedCount = 5)
        assertEquals(0, stats.count)
        assertNull(stats.sampleMean)
        assertNull(stats.minValue)
        assertNull(stats.maxValue)
        assertNull(stats.latestValidSample)
        assertEquals(5, stats.excludedCount)
    }

    @Test
    fun testTrend_risingFallingSteadyAndInsufficient() {
        val base = fixedNow - (60 * 60 * 1000L) // 1 hour ago
        val metric = HistoricalMetric.AirTemperature

        // Insufficient sample count (< 3)
        val fewSamples = listOf(
            TelemetrySample("1", metric, 20f, null, base, DataOrigin.LiveHardware),
            TelemetrySample("2", metric, 25f, null, base + 30 * 60 * 1000L, DataOrigin.LiveHardware)
        )
        val trendFew = AnalyticsMathEngine.computeTrend(fewSamples, metric)
        assertEquals(HistoricalTrendDirection.InsufficientData, trendFew.direction)

        // Insufficient time span (< 10 min)
        val shortSpan = listOf(
            TelemetrySample("1", metric, 20f, null, base, DataOrigin.LiveHardware),
            TelemetrySample("2", metric, 22f, null, base + 60 * 1000L, DataOrigin.LiveHardware),
            TelemetrySample("3", metric, 24f, null, base + 120 * 1000L, DataOrigin.LiveHardware)
        )
        val trendShort = AnalyticsMathEngine.computeTrend(shortSpan, metric)
        assertEquals(HistoricalTrendDirection.InsufficientData, trendShort.direction)

        // Rising trend over 1 hour
        val risingSamples = listOf(
            TelemetrySample("1", metric, 20f, null, base, DataOrigin.LiveHardware),
            TelemetrySample("2", metric, 25f, null, base + 30 * 60 * 1000L, DataOrigin.LiveHardware),
            TelemetrySample("3", metric, 30f, null, base + 60 * 60 * 1000L, DataOrigin.LiveHardware)
        )
        val trendRising = AnalyticsMathEngine.computeTrend(risingSamples, metric)
        assertEquals(HistoricalTrendDirection.Rising, trendRising.direction)
        assertTrue(trendRising.displayText.contains("rose over this period"))

        // Falling trend
        val fallingSamples = listOf(
            TelemetrySample("1", metric, 30f, null, base, DataOrigin.LiveHardware),
            TelemetrySample("2", metric, 25f, null, base + 30 * 60 * 1000L, DataOrigin.LiveHardware),
            TelemetrySample("3", metric, 20f, null, base + 60 * 60 * 1000L, DataOrigin.LiveHardware)
        )
        val trendFalling = AnalyticsMathEngine.computeTrend(fallingSamples, metric)
        assertEquals(HistoricalTrendDirection.Falling, trendFalling.direction)
        assertTrue(trendFalling.displayText.contains("fell over this period"))

        // Steady trend within +/- 5% deadband
        val steadySamples = listOf(
            TelemetrySample("1", metric, 25.0f, null, base, DataOrigin.LiveHardware),
            TelemetrySample("2", metric, 25.1f, null, base + 30 * 60 * 1000L, DataOrigin.LiveHardware),
            TelemetrySample("3", metric, 25.05f, null, base + 60 * 60 * 1000L, DataOrigin.LiveHardware)
        )
        val trendSteady = AnalyticsMathEngine.computeTrend(steadySamples, metric)
        assertEquals(HistoricalTrendDirection.Steady, trendSteady.direction)
        assertTrue(trendSteady.displayText.contains("remained steady"))
    }

    @Test
    fun testBucketing_capAndGapDetection() {
        val window = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)
        val sample1 = TelemetrySample("1", HistoricalMetric.AirTemperature, 20f, null, window.startEpochMillis + 1000L, DataOrigin.LiveHardware)
        val sample2 = TelemetrySample("2", HistoricalMetric.AirTemperature, 30f, null, window.startEpochMillis + 2000L, DataOrigin.LiveHardware)
        // Huge gap until end
        val sample3 = TelemetrySample("3", HistoricalMetric.AirTemperature, 25f, null, window.endEpochMillis - 1000L, DataOrigin.LiveHardware)

        val buckets = AnalyticsMathEngine.computeBuckets(listOf(sample1, sample2, sample3), window, maxBuckets = 50)
        assertEquals(50, buckets.size)
        // First bucket has sample1 and sample2
        val b0 = buckets[0]
        assertFalse(b0.isGap)
        assertEquals(2, b0.sampleCount)
        assertEquals(20f, b0.min!!, 0.001f)
        assertEquals(30f, b0.max!!, 0.001f)
        assertEquals(25f, b0.avg!!, 0.001f)

        // Middle bucket is gap
        val bMid = buckets[25]
        assertTrue(bMid.isGap)
        assertEquals(0, bMid.sampleCount)
        assertNull(bMid.avg)

        // Last bucket has sample3
        val bLast = buckets[49]
        assertFalse(bLast.isGap)
        assertEquals(1, bLast.sampleCount)
    }

    @Test
    fun testTimeZones_utcVsLocalFormatting_includingKolkataAndDst() {
        val utcInstant = Instant.ofEpochMilli(fixedNow)

        // Asia/Kolkata (UTC+05:30 non-whole-hour offset)
        val kolkataZone = ZoneId.of("Asia/Kolkata")
        val kolkataTime = ZonedDateTime.ofInstant(utcInstant, kolkataZone)
        val kolkataFormatted = kolkataTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.US))
        assertTrue(kolkataFormatted.contains("+05:30") || kolkataFormatted.contains("IST"))

        // Europe/London DST transition check (March / October)
        val londonZone = ZoneId.of("Europe/London")
        val summerInstant = Instant.parse("2026-07-01T12:00:00Z")
        val winterInstant = Instant.parse("2026-01-01T12:00:00Z")
        val summerTime = ZonedDateTime.ofInstant(summerInstant, londonZone)
        val winterTime = ZonedDateTime.ofInstant(winterInstant, londonZone)

        assertEquals(1, summerTime.offset.totalSeconds / 3600) // BST (UTC+1)
        assertEquals(0, winterTime.offset.totalSeconds / 3600) // GMT (UTC+0)
    }

    @Test
    fun testRollingWindows_allSupportedRangesIncluding1hAnd6h() {
        val window1h = AnalyticsTimeRange.Hours1.calculateWindow(fixedNow)
        assertEquals(3600_000L, window1h.endEpochMillis - window1h.startEpochMillis)
        assertEquals(fixedNow, window1h.endEpochMillis)

        val window6h = AnalyticsTimeRange.Hours6.calculateWindow(fixedNow)
        assertEquals(6 * 3600_000L, window6h.endEpochMillis - window6h.startEpochMillis)
        assertEquals(fixedNow, window6h.endEpochMillis)

        val window24h = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)
        assertEquals(24 * 3600_000L, window24h.endEpochMillis - window24h.startEpochMillis)

        val window7d = AnalyticsTimeRange.Days7.calculateWindow(fixedNow)
        assertEquals(7 * 86400_000L, window7d.endEpochMillis - window7d.startEpochMillis)

        val window30d = AnalyticsTimeRange.Days30.calculateWindow(fixedNow)
        assertEquals(30 * 86400_000L, window30d.endEpochMillis - window30d.startEpochMillis)
    }

    @Test
    fun testCustomWindow_validationAndClamping() {
        val now = fixedNow

        // 1. End before start is rejected
        val invalidResult = AnalyticsMathEngine.createCustomWindow(
            startEpochMillis = now - 1000L,
            endEpochMillis = now - 2000L,
            nowEpochMillis = now
        )
        assertTrue(invalidResult.isFailure)

        // 2. Future end time is clamped to now
        val futureEndResult = AnalyticsMathEngine.createCustomWindow(
            startEpochMillis = now - 10_000L,
            endEpochMillis = now + 50_000L,
            nowEpochMillis = now
        )
        assertTrue(futureEndResult.isSuccess)
        val clampedWindow = futureEndResult.getOrThrow()
        assertEquals(now, clampedWindow.endEpochMillis)
        assertEquals(now - 10_000L, clampedWindow.startEpochMillis)

        // 3. Span exceeding 30 days is capped at 30 days
        val hugeSpanResult = AnalyticsMathEngine.createCustomWindow(
            startEpochMillis = now - 50L * 86400_000L,
            endEpochMillis = now,
            nowEpochMillis = now
        )
        assertTrue(hugeSpanResult.isSuccess)
        val cappedWindow = hugeSpanResult.getOrThrow()
        assertEquals(now, cappedWindow.endEpochMillis)
        assertEquals(now - 30L * 86400_000L, cappedWindow.startEpochMillis)
    }

    @Test
    fun testAbnormalReadings_thresholdBreachesAndRawProbeImmunity() {
        // Covered metric: Air temperature (breaches below 10°C or above 38°C)
        val tempSamples = listOf(
            TelemetrySample("t1", HistoricalMetric.AirTemperature, 8.5f, null, fixedNow - 3600_000L, DataOrigin.LiveHardware),
            TelemetrySample("t2", HistoricalMetric.AirTemperature, 24.0f, null, fixedNow - 1800_000L, DataOrigin.LiveHardware),
            TelemetrySample("t3", HistoricalMetric.AirTemperature, 41.2f, null, fixedNow, DataOrigin.LiveHardware)
        )
        val tempAbnormal = AnalyticsMathEngine.computeAbnormalReadings(tempSamples, excludedCount = 1, metric = HistoricalMetric.AirTemperature)
        assertEquals(1, tempAbnormal.totalExcludedCount)
        assertEquals(2, tempAbnormal.thresholdBreachCount)
        assertEquals(0, tempAbnormal.suddenChangeCount)
        assertEquals(2, tempAbnormal.flags.size)
        assertTrue(tempAbnormal.flags.any { it.description.contains("below configured minimum") })
        assertTrue(tempAbnormal.flags.any { it.description.contains("above configured maximum") })

        // Raw probes invariant: Probe 1 and Probe 2 are UNCALIBRATED and must NEVER trigger threshold breaches!
        val probeSamples = listOf(
            TelemetrySample("p1", HistoricalMetric.SoilMoistureProbe1Raw, 1200f, 1200f, fixedNow - 2000, DataOrigin.LiveHardware),
            TelemetrySample("p2", HistoricalMetric.SoilMoistureProbe1Raw, 3500f, 3500f, fixedNow - 1000, DataOrigin.LiveHardware)
        )
        val probeAbnormal = AnalyticsMathEngine.computeAbnormalReadings(probeSamples, excludedCount = 0, metric = HistoricalMetric.SoilMoistureProbe1Raw)
        assertEquals(0, probeAbnormal.thresholdBreachCount)
    }

    @Test
    fun testAbnormalReadings_suddenChangeDetection() {
        val metric = HistoricalMetric.AirTemperature // limit is 5.0°C
        val samples = listOf(
            TelemetrySample("s1", metric, 20.0f, null, fixedNow - 120_000L, DataOrigin.LiveHardware),
            // Sudden jump by 8.0°C in next sample
            TelemetrySample("s2", metric, 28.0f, null, fixedNow - 60_000L, DataOrigin.LiveHardware),
            // Normal gradual change by 0.5°C
            TelemetrySample("s3", metric, 28.5f, null, fixedNow, DataOrigin.LiveHardware)
        )

        val abnormal = AnalyticsMathEngine.computeAbnormalReadings(samples, excludedCount = 0, metric = metric)
        assertEquals(1, abnormal.suddenChangeCount)
        assertEquals(1, abnormal.flags.size)
        assertEquals("s2", abnormal.flags[0].sampleId)
        assertEquals(com.farmvision.agrirover.data.model.AbnormalReadingSource.SuddenChange, abnormal.flags[0].source)
        assertTrue(abnormal.flags[0].description.contains("Sudden change of 8.0 °C"))
    }

    @Test
    fun testOverviewMetrics_probe1AndProbe2SeparateAverages() {
        val samples = listOf(
            TelemetrySample("p1-a", HistoricalMetric.SoilMoistureProbe1Raw, 2000f, 2000f, fixedNow - 2000L, DataOrigin.LiveHardware),
            TelemetrySample("p1-b", HistoricalMetric.SoilMoistureProbe1Raw, 2100f, 2100f, fixedNow - 1000L, DataOrigin.LiveHardware),
            TelemetrySample("p2-a", HistoricalMetric.SoilMoistureProbe2Raw, 1800f, 1800f, fixedNow - 2000L, DataOrigin.LiveHardware),
            TelemetrySample("p2-b", HistoricalMetric.SoilMoistureProbe2Raw, 1900f, 1900f, fixedNow - 1000L, DataOrigin.LiveHardware),
            TelemetrySample("t-a", HistoricalMetric.AirTemperature, 25.0f, null, fixedNow - 1000L, DataOrigin.LiveHardware),
            TelemetrySample("h-a", HistoricalMetric.Humidity, 60.0f, null, fixedNow - 1000L, DataOrigin.LiveHardware),
            TelemetrySample("ph-a", HistoricalMetric.SoilPh, 6.8f, null, fixedNow - 1000L, DataOrigin.LiveHardware)
        )

        val overview = AnalyticsMathEngine.computeOverviewMetrics(samples, sourceTag = "LIVE HARDWARE")
        assertEquals(2050f, overview.probe1RawAvg!!, 0.01f)
        assertEquals(1850f, overview.probe2RawAvg!!, 0.01f) // Separated from Probe 1!
        assertEquals(25.0f, overview.airTempAvg!!, 0.01f)
        assertEquals(60.0f, overview.humidityAvg!!, 0.01f)
        assertEquals(6.8f, overview.soilPhAvg!!, 0.01f)
        assertEquals(7, overview.totalSamplesCount)
        assertEquals(fixedNow - 1000L, overview.latestTimestampEpochMillis)
        assertEquals("LIVE HARDWARE", overview.sourceTag)
    }
}
