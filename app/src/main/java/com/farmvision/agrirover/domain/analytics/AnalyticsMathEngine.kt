package com.farmvision.agrirover.domain.analytics

import com.farmvision.agrirover.data.model.AbnormalReadingFlag
import com.farmvision.agrirover.data.model.AbnormalReadingSource
import com.farmvision.agrirover.data.model.AbnormalReadingsSummary
import com.farmvision.agrirover.data.model.AnalyticsOverviewMetrics
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.AnalyticsTimeWindow
import com.farmvision.agrirover.data.model.HistoricalDataBucket
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.HistoricalTrendDirection
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.data.model.TelemetryStatistics
import com.farmvision.agrirover.data.model.TrendSummary
import java.util.Locale
import kotlin.math.abs

/**
 * Pure Kotlin mathematics engine for historical telemetry analytics.
 * Strictly free of Android runtime imports for deterministic JVM testing.
 * Implements window filtering, honest statistics, least-squares trend,
 * time-bucketing with gap detection, and provisional constant governance.
 */
object AnalyticsMathEngine {

    // --- Provisional Constants (Section 5 & 6) ---

    /** Standard nominal write rate: 1 sample per metric per 60 seconds (Default D4). */
    const val DEFAULT_SAMPLE_INTERVAL_SECONDS = 60L

    /** Maximum future timestamp tolerance before a sample is rejected as erroneous clock drift (Default D6). */
    const val FUTURE_TIMESTAMP_TOLERANCE_MILLIS = 30_000L // 30 seconds

    /** Bounded chart point cap per series to preserve rendering performance on mobile (Default D9). */
    const val MAX_CHART_POINTS = 200

    /** Minimum number of valid samples required before computing a trend (Default D8). */
    const val MIN_SAMPLES_FOR_TREND = 3

    /** Minimum temporal time span required before computing a trend (Default D8). */
    const val MIN_SPAN_FOR_TREND_MILLIS = 10 * 60 * 1000L // 10 minutes

    /** Fractional deadband (+/- 5%) around zero within which a trend is classified as Steady (Default D8). */
    const val STEADY_DEADBAND_FRACTION = 0.05f

    /** Multiplier of expected sample interval beyond which a gap break is recognized in charts (Default D9). */
    const val GAP_THRESHOLD_MULTIPLIER = 2.5f

    // --- Provisional Constants for Abnormal Readings (Default D13) ---
    /** Configured thresholds (Phase 5 provisional rules) for covered metrics only. */
    const val AIR_TEMP_LOW_THRESHOLD = 10.0f
    const val AIR_TEMP_HIGH_THRESHOLD = 38.0f
    const val HUMIDITY_LOW_THRESHOLD = 30.0f
    const val HUMIDITY_HIGH_THRESHOLD = 85.0f
    const val SOIL_PH_LOW_THRESHOLD = 6.0f
    const val SOIL_PH_HIGH_THRESHOLD = 7.5f
    const val OBSTACLE_DISTANCE_WARNING_THRESHOLD = 30.0f

    /** Sudden change maximum expected delta per consecutive sample (Default D13). */
    const val SUDDEN_CHANGE_DELTA_AIR_TEMP = 5.0f
    const val SUDDEN_CHANGE_DELTA_HUMIDITY = 15.0f
    const val SUDDEN_CHANGE_DELTA_SOIL_PH = 0.8f
    const val SUDDEN_CHANGE_DELTA_OBSTACLE_DIST = 50.0f
    const val SUDDEN_CHANGE_DELTA_PROBE1_RAW = 250.0f
    const val SUDDEN_CHANGE_DELTA_PROBE2_RAW = 200.0f

    /** Maximum span for custom time range (Default D7 & D4: capped at retention period). */
    const val MAX_CUSTOM_RANGE_RETENTION_DAYS = 30L

    /**
     * Filters, deduplicates, and validates samples within a rolling time window [window.startEpochMillis, window.endEpochMillis].
     *
     * Rules:
     * - Samples with timestamp > (nowMillis + FUTURE_TIMESTAMP_TOLERANCE_MILLIS) are excluded as invalid.
     * - Samples outside [window.startEpochMillis, window.endEpochMillis] are ignored.
     * - Samples failing metric range or saturated ADC counts (<= 50 or >= 4050) are excluded.
     * - Samples with duplicate timestamps for the same metric & origin: later-received sample wins.
     * - Returns a pair of: (sorted valid samples, count of excluded samples that fell inside the window).
     */
    fun filterAndValidateWindow(
        samples: List<TelemetrySample>,
        window: AnalyticsTimeWindow,
        nowMillis: Long
    ): Pair<List<TelemetrySample>, Int> {
        var excludedCount = 0
        // Map to enforce duplicate timestamp resolution (later-received sample in the list overwrites earlier)
        val deduplicatedMap = LinkedHashMap<Long, TelemetrySample>()

        for (sample in samples) {
            // Reject samples beyond future tolerance
            if (sample.timestampEpochMillis > nowMillis + FUTURE_TIMESTAMP_TOLERANCE_MILLIS) {
                if (sample.timestampEpochMillis in window.startEpochMillis..window.endEpochMillis) {
                    excludedCount++
                }
                continue
            }

            // Check if within window
            if (sample.timestampEpochMillis < window.startEpochMillis || sample.timestampEpochMillis > window.endEpochMillis) {
                continue
            }

            // Check physical and electrical validity (NaN, Inf, Range, ADC Saturation)
            if (!sample.metric.isWithinValidRange(sample.canonicalValue)) {
                excludedCount++
                continue
            }

            // Duplicate timestamp: last sample received wins
            deduplicatedMap[sample.timestampEpochMillis] = sample
        }

        val sortedValid = deduplicatedMap.values.sortedBy { it.timestampEpochMillis }
        return Pair(sortedValid, excludedCount)
    }

    /**
     * Computes honest sample statistics over valid samples only (Default D7).
     * The mean is strictly an arithmetic sample mean and does not interpolate gaps.
     */
    fun computeStatistics(
        validSamples: List<TelemetrySample>,
        excludedCount: Int
    ): TelemetryStatistics {
        if (validSamples.isEmpty()) {
            return TelemetryStatistics.empty(excludedCount)
        }

        val count = validSamples.size
        val latest = validSamples.maxByOrNull { it.timestampEpochMillis }
        var sum = 0.0
        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE

        for (sample in validSamples) {
            val v = sample.canonicalValue
            sum += v
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val mean = (sum / count).toFloat()

        return TelemetryStatistics(
            count = count,
            latestValidSample = latest,
            sampleMean = mean,
            minValue = minVal,
            maxValue = maxVal,
            excludedCount = excludedCount
        )
    }

    /**
     * Computes a factual, neutral trend summary using ordinary least-squares (OLS) linear regression (Default D8).
     * Strictly avoids subjective words like "significant" and provides zero agronomic advice.
     */
    fun computeTrend(
        validSamples: List<TelemetrySample>,
        metric: HistoricalMetric
    ): TrendSummary {
        if (validSamples.size < MIN_SAMPLES_FOR_TREND) {
            return TrendSummary(
                direction = HistoricalTrendDirection.InsufficientData,
                displayText = "Not enough data to calculate trend (minimum $MIN_SAMPLES_FOR_TREND readings required)."
            )
        }

        val minTime = validSamples.first().timestampEpochMillis
        val maxTime = validSamples.last().timestampEpochMillis
        val timeSpanMillis = maxTime - minTime

        if (timeSpanMillis < MIN_SPAN_FOR_TREND_MILLIS) {
            return TrendSummary(
                direction = HistoricalTrendDirection.InsufficientData,
                displayText = "Not enough time span to calculate trend (minimum 10 minutes required)."
            )
        }

        // Convert timestamps to days from minTime for stable slope per day
        val spanDays = timeSpanMillis.toDouble() / (86400000.0)
        val n = validSamples.size
        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumXX = 0.0

        for (sample in validSamples) {
            val x = (sample.timestampEpochMillis - minTime).toDouble() / 86400000.0
            val y = sample.canonicalValue.toDouble()
            sumX += x
            sumY += y
            sumXY += x * y
            sumXX += x * x
        }

        val meanX = sumX / n
        val meanY = sumY / n
        val denominator = sumXX - n * meanX * meanX

        val slopePerDay = if (kotlin.math.abs(denominator) > 1e-9) {
            ((sumXY - n * meanX * meanY) / denominator).toFloat()
        } else {
            0.0f
        }

        val totalExpectedDelta = slopePerDay * spanDays.toFloat()
        val referenceMagnitude = kotlin.math.max(kotlin.math.abs(meanY.toFloat()), 1.0f)
        val relativeChange = kotlin.math.abs(totalExpectedDelta) / referenceMagnitude

        val direction = when {
            relativeChange <= STEADY_DEADBAND_FRACTION || kotlin.math.abs(slopePerDay) < 1e-4f -> HistoricalTrendDirection.Steady
            slopePerDay > 0f -> HistoricalTrendDirection.Rising
            else -> HistoricalTrendDirection.Falling
        }

        val text = when (direction) {
            HistoricalTrendDirection.Steady -> "${metric.displayName} remained steady over this period."
            HistoricalTrendDirection.Rising -> "${metric.displayName} rose over this period."
            HistoricalTrendDirection.Falling -> "${metric.displayName} fell over this period."
            HistoricalTrendDirection.InsufficientData -> "Not enough data to calculate trend."
        }

        return TrendSummary(
            direction = direction,
            displayText = text,
            slopePerDay = slopePerDay
        )
    }

    /**
     * Bounded time-bucketing generating at most [maxBuckets] (default 200) data points (Default D9).
     * Gaps in telemetry are preserved as empty buckets (isGap = true) and never linearly interpolated.
     */
    fun computeBuckets(
        validSamples: List<TelemetrySample>,
        window: AnalyticsTimeWindow,
        maxBuckets: Int = MAX_CHART_POINTS
    ): List<HistoricalDataBucket> {
        val windowDuration = window.endEpochMillis - window.startEpochMillis
        if (windowDuration <= 0L) return emptyList()

        val bucketCount = maxBuckets.coerceIn(1, MAX_CHART_POINTS)
        val bucketDuration = windowDuration.toDouble() / bucketCount

        // Group samples into buckets
        val bucketSamples = Array(bucketCount) { mutableListOf<TelemetrySample>() }

        for (sample in validSamples) {
            val offset = sample.timestampEpochMillis - window.startEpochMillis
            val index = (offset / bucketDuration).toInt().coerceIn(0, bucketCount - 1)
            bucketSamples[index].add(sample)
        }

        return (0 until bucketCount).map { i ->
            val bStart = (window.startEpochMillis + (i * bucketDuration)).toLong()
            val bEnd = (window.startEpochMillis + ((i + 1) * bucketDuration)).toLong()
            val bCenter = (bStart + bEnd) / 2
            val samplesInBucket = bucketSamples[i]

            if (samplesInBucket.isEmpty()) {
                HistoricalDataBucket(
                    bucketIndex = i,
                    startTimeMillis = bStart,
                    endTimeMillis = bEnd,
                    centerTimeMillis = bCenter,
                    min = null,
                    avg = null,
                    max = null,
                    sampleCount = 0,
                    isGap = true
                )
            } else {
                var min = Float.MAX_VALUE
                var max = -Float.MAX_VALUE
                var sum = 0.0

                for (s in samplesInBucket) {
                    val v = s.canonicalValue
                    if (v < min) min = v
                    if (v > max) max = v
                    sum += v
                }

                HistoricalDataBucket(
                    bucketIndex = i,
                    startTimeMillis = bStart,
                    endTimeMillis = bEnd,
                    centerTimeMillis = bCenter,
                    min = min,
                    avg = (sum / samplesInBucket.size).toFloat(),
                    max = max,
                    sampleCount = samplesInBucket.size,
                    isGap = false
                )
            }
        }
    }

    /**
     * Evaluates whether a temporal gap between two consecutive samples exceeds the gap threshold.
     */
    fun isGap(
        previousTimestampMillis: Long,
        currentTimestampMillis: Long,
        expectedIntervalMillis: Long = DEFAULT_SAMPLE_INTERVAL_SECONDS * 1000L
    ): Boolean {
        val delta = currentTimestampMillis - previousTimestampMillis
        return delta > (expectedIntervalMillis * GAP_THRESHOLD_MULTIPLIER)
    }

    /**
     * Validates and constructs a custom time window adhering to Default D7.
     * Rules:
     * - End time before start time is rejected with IllegalArgumentException.
     * - Future end time is clamped to [nowEpochMillis].
     * - Span exceeding [maxRetentionDays] is capped at the retention period (clamping start).
     */
    fun createCustomWindow(
        startEpochMillis: Long,
        endEpochMillis: Long,
        nowEpochMillis: Long,
        maxRetentionDays: Long = MAX_CUSTOM_RANGE_RETENTION_DAYS
    ): Result<AnalyticsTimeWindow> {
        if (endEpochMillis < startEpochMillis) {
            return Result.failure(IllegalArgumentException("End time cannot be earlier than start time"))
        }

        val clampedEnd = minOf(endEpochMillis, nowEpochMillis)
        if (clampedEnd < startEpochMillis) {
            return Result.failure(IllegalArgumentException("Selected range is in the future"))
        }

        val maxSpanMillis = maxRetentionDays * 86_400_000L
        val validStart = maxOf(startEpochMillis, clampedEnd - maxSpanMillis)

        return Result.success(
            AnalyticsTimeWindow(
                range = AnalyticsTimeRange.Custom,
                startEpochMillis = validStart,
                endEpochMillis = clampedEnd
            )
        )
    }

    /**
     * Evaluates rule-based abnormal readings adhering strictly to Default D13.
     * Three sources:
     * (1) Excluded by validation (counted and recorded).
     * (2) Readings outside configured thresholds (covered metrics only, never uncalibrated raw probes).
     * (3) Sudden change flag when adjacent valid samples differ by more than provisional constants.
     */
    fun computeAbnormalReadings(
        validSamples: List<TelemetrySample>,
        excludedCount: Int,
        metric: HistoricalMetric
    ): AbnormalReadingsSummary {
        val flags = mutableListOf<AbnormalReadingFlag>()
        var thresholdBreachCount = 0
        var suddenChangeCount = 0

        // 1. Configured Threshold Breaches (strictly never for uncalibrated raw probes)
        if (!metric.isRaw) {
            for (sample in validSamples) {
                val v = sample.canonicalValue
                val breachDesc = when (metric) {
                    HistoricalMetric.AirTemperature -> when {
                        v < AIR_TEMP_LOW_THRESHOLD -> "Air temperature %.1f°C below configured minimum threshold (%.1f°C)".format(Locale.US, v, AIR_TEMP_LOW_THRESHOLD)
                        v > AIR_TEMP_HIGH_THRESHOLD -> "Air temperature %.1f°C above configured maximum threshold (%.1f°C)".format(Locale.US, v, AIR_TEMP_HIGH_THRESHOLD)
                        else -> null
                    }
                    HistoricalMetric.Humidity -> when {
                        v < HUMIDITY_LOW_THRESHOLD -> "Relative humidity %.1f%% below configured minimum threshold (%.1f%%)".format(Locale.US, v, HUMIDITY_LOW_THRESHOLD)
                        v > HUMIDITY_HIGH_THRESHOLD -> "Relative humidity %.1f%% above configured maximum threshold (%.1f%%)".format(Locale.US, v, HUMIDITY_HIGH_THRESHOLD)
                        else -> null
                    }
                    HistoricalMetric.SoilPh -> when {
                        v < SOIL_PH_LOW_THRESHOLD -> "Soil pH %.2f below configured neutral band (%.1f)".format(Locale.US, v, SOIL_PH_LOW_THRESHOLD)
                        v > SOIL_PH_HIGH_THRESHOLD -> "Soil pH %.2f above configured neutral band (%.1f)".format(Locale.US, v, SOIL_PH_HIGH_THRESHOLD)
                        else -> null
                    }
                    HistoricalMetric.UltrasonicDistance -> when {
                        v < OBSTACLE_DISTANCE_WARNING_THRESHOLD -> "Obstacle proximity %.1f cm within warning threshold (%.1f cm)".format(Locale.US, v, OBSTACLE_DISTANCE_WARNING_THRESHOLD)
                        else -> null
                    }
                    else -> null
                }

                if (breachDesc != null) {
                    thresholdBreachCount++
                    flags.add(
                        AbnormalReadingFlag(
                            sampleId = sample.id,
                            metric = metric,
                            timestampEpochMillis = sample.timestampEpochMillis,
                            value = v,
                            source = AbnormalReadingSource.ConfiguredThresholdBreach,
                            description = breachDesc
                        )
                    )
                }
            }
        }

        // 2. Sudden Change Flag (consecutive valid samples within reasonable continuity window)
        val suddenDeltaThreshold = when (metric) {
            HistoricalMetric.SoilMoistureProbe1Raw -> SUDDEN_CHANGE_DELTA_PROBE1_RAW
            HistoricalMetric.SoilMoistureProbe2Raw -> SUDDEN_CHANGE_DELTA_PROBE2_RAW
            HistoricalMetric.AirTemperature -> SUDDEN_CHANGE_DELTA_AIR_TEMP
            HistoricalMetric.Humidity -> SUDDEN_CHANGE_DELTA_HUMIDITY
            HistoricalMetric.SoilPh -> SUDDEN_CHANGE_DELTA_SOIL_PH
            HistoricalMetric.UltrasonicDistance -> SUDDEN_CHANGE_DELTA_OBSTACLE_DIST
        }

        val sorted = validSamples.sortedBy { it.timestampEpochMillis }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            val timeDiff = curr.timestampEpochMillis - prev.timestampEpochMillis
            if (timeDiff <= 10 * DEFAULT_SAMPLE_INTERVAL_SECONDS * 1000L) {
                val delta = abs(curr.canonicalValue - prev.canonicalValue)
                if (delta > suddenDeltaThreshold) {
                    suddenChangeCount++
                    flags.add(
                        AbnormalReadingFlag(
                            sampleId = curr.id,
                            metric = metric,
                            timestampEpochMillis = curr.timestampEpochMillis,
                            value = curr.canonicalValue,
                            source = AbnormalReadingSource.SuddenChange,
                            description = "Sudden change of %.1f %s between consecutive readings (limit: %.1f %s)".format(
                                Locale.US,
                                delta,
                                metric.canonicalUnit,
                                suddenDeltaThreshold,
                                metric.canonicalUnit
                            )
                        )
                    )
                }
            }
        }

        return AbnormalReadingsSummary(
            totalExcludedCount = excludedCount,
            thresholdBreachCount = thresholdBreachCount,
            suddenChangeCount = suddenChangeCount,
            flags = flags
        )
    }

    /**
     * Computes multi-sensor overview averages for Section 7 Overview cards.
     * Raw probes 1 and 2 are strictly averaged independently, never together.
     */
    fun computeOverviewMetrics(
        allValidSamples: List<TelemetrySample>,
        sourceTag: String
    ): AnalyticsOverviewMetrics {
        if (allValidSamples.isEmpty()) {
            return AnalyticsOverviewMetrics(sourceTag = sourceTag)
        }

        fun averageFor(metric: HistoricalMetric): Float? {
            val matching = allValidSamples.filter { it.metric == metric }
            if (matching.isEmpty()) return null
            return (matching.sumOf { it.canonicalValue.toDouble() } / matching.size).toFloat()
        }

        val latestTime = allValidSamples.maxOfOrNull { it.timestampEpochMillis }

        return AnalyticsOverviewMetrics(
            probe1RawAvg = averageFor(HistoricalMetric.SoilMoistureProbe1Raw),
            probe2RawAvg = averageFor(HistoricalMetric.SoilMoistureProbe2Raw),
            airTempAvg = averageFor(HistoricalMetric.AirTemperature),
            humidityAvg = averageFor(HistoricalMetric.Humidity),
            soilPhAvg = averageFor(HistoricalMetric.SoilPh),
            totalSamplesCount = allValidSamples.size,
            latestTimestampEpochMillis = latestTime,
            sourceTag = sourceTag
        )
    }
}
