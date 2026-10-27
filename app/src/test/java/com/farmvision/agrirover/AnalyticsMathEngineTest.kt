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
}
