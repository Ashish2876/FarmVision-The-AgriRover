package com.farmvision.agrirover.domain.repository

import com.farmvision.agrirover.data.model.AnalyticsTimeWindow
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import kotlinx.coroutines.flow.StateFlow

/**
 * Historical Telemetry Repository boundary interface.
 * Provides durable/in-memory historical sample persistence, rolling-window range queries,
 * reverse-chronological record lists, retention pruning, and rate throttling.
 */
interface TelemetryHistoryRepository {

    /** Reactive stream of recent telemetry samples for live observation. */
    val recentSamplesFlow: StateFlow<List<TelemetrySample>>

    /**
     * Stores a validated telemetry sample.
     * Enforces rate throttling (provisional default: at most 1 sample per metric per 60s).
     * Returns true if accepted and persisted, or false if throttled.
     */
    suspend fun recordSample(sample: TelemetrySample): Boolean

    /**
     * Batch stores telemetry samples.
     * Returns the number of samples actually persisted after validation and rate limits.
     */
    suspend fun recordSamples(samples: List<TelemetrySample>): Int

    /**
     * Retrieves valid historical samples for a specific metric within a time window [start, end].
     * Optionally filtered by [origin] (Live vs Demo).
     * Guaranteed sorted chronologically ascending by [TelemetrySample.timestampEpochMillis].
     */
    suspend fun getSamples(
        metric: HistoricalMetric,
        timeWindow: AnalyticsTimeWindow,
        origin: DataOrigin? = null
    ): List<TelemetrySample>

    /**
     * Retrieves recent records reverse-chronological (newest first).
     * Bounded by [limit] (default 100) and [offset] for pagination.
     */
    suspend fun getRecentRecords(
        limit: Int = 100,
        offset: Int = 0,
        metric: HistoricalMetric? = null,
        origin: DataOrigin? = null
    ): List<TelemetrySample>

    /**
     * Enforces retention policy by purging records with timestamp older than [retentionCutoffMillis].
     * Returns the count of pruned records.
     */
    suspend fun pruneOldRecords(retentionCutoffMillis: Long): Int

    /**
     * Clears history, optionally filtered by origin.
     */
    suspend fun clearHistory(origin: DataOrigin? = null)

    /**
     * Returns the total count of stored records.
     */
    suspend fun getCount(origin: DataOrigin? = null): Int
}
