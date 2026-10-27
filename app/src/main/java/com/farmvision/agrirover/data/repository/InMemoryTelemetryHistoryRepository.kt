package com.farmvision.agrirover.data.repository

import com.farmvision.agrirover.data.model.AnalyticsTimeWindow
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/**
 * In-memory, bounded ring-buffer implementation of [TelemetryHistoryRepository].
 * Ideal for high-speed JVM testing and lightweight runtime buffering.
 * Strictly non-durable across application process restarts (Default D2(c)).
 */
class InMemoryTelemetryHistoryRepository(
    val maxSamplesPerMetric: Int = 1000,
    val writeIntervalSeconds: Long = 60L
) : TelemetryHistoryRepository {

    private val lock = Any()

    private val buffers = mutableMapOf<HistoricalMetric, ArrayDeque<TelemetrySample>>()
    private val lastRecordedTimestamps = mutableMapOf<Pair<HistoricalMetric, DataOrigin>, Long>()

    private val _recentSamplesFlow = MutableStateFlow<List<TelemetrySample>>(emptyList())
    override val recentSamplesFlow: StateFlow<List<TelemetrySample>> = _recentSamplesFlow.asStateFlow()

    private fun recordSampleInternal(sample: TelemetrySample): Boolean {
        val key = Pair(sample.metric, sample.origin)
        val lastRecorded = lastRecordedTimestamps[key] ?: 0L
        val minIntervalMillis = writeIntervalSeconds * 1000L

        // Rate throttling: reject sample if less than writeIntervalSeconds from last stored sample
        if (sample.timestampEpochMillis - lastRecorded < minIntervalMillis && lastRecorded > 0L) {
            return false
        }

        val queue = buffers.getOrPut(sample.metric) { ArrayDeque(maxSamplesPerMetric) }
        if (queue.size >= maxSamplesPerMetric) {
            queue.pollFirst() // Discard oldest sample
        }
        queue.addLast(sample)
        lastRecordedTimestamps[key] = sample.timestampEpochMillis
        return true
    }

    override suspend fun recordSample(sample: TelemetrySample): Boolean = synchronized(lock) {
        val accepted = recordSampleInternal(sample)
        if (accepted) {
            updateRecentSamplesLocked()
        }
        accepted
    }

    override suspend fun recordSamples(samples: List<TelemetrySample>): Int = synchronized(lock) {
        var count = 0
        for (sample in samples) {
            if (recordSampleInternal(sample)) {
                count++
            }
        }
        if (count > 0) {
            updateRecentSamplesLocked()
        }
        count
    }

    override suspend fun getSamples(
        metric: HistoricalMetric,
        timeWindow: AnalyticsTimeWindow,
        origin: DataOrigin?
    ): List<TelemetrySample> = synchronized(lock) {
        val queue = buffers[metric] ?: return emptyList()
        return queue.filter { sample ->
            sample.timestampEpochMillis in timeWindow.startEpochMillis..timeWindow.endEpochMillis &&
                (origin == null || sample.origin == origin)
        }.sortedBy { it.timestampEpochMillis }
    }

    override suspend fun getRecentRecords(
        limit: Int,
        offset: Int,
        metric: HistoricalMetric?,
        origin: DataOrigin?
    ): List<TelemetrySample> = synchronized(lock) {
        val all = buffers.values.flatten()
            .filter { sample ->
                (metric == null || sample.metric == metric) &&
                    (origin == null || sample.origin == origin)
            }
            .sortedByDescending { it.timestampEpochMillis }

        if (offset >= all.size) return emptyList()
        return all.drop(offset).take(limit)
    }

    override suspend fun pruneOldRecords(retentionCutoffMillis: Long): Int = synchronized(lock) {
        var pruned = 0
        for ((_, queue) in buffers) {
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val s = iterator.next()
                if (s.timestampEpochMillis < retentionCutoffMillis) {
                    iterator.remove()
                    pruned++
                }
            }
        }
        if (pruned > 0) {
            updateRecentSamplesLocked()
        }
        return pruned
    }

    override suspend fun clearHistory(origin: DataOrigin?) = synchronized(lock) {
        if (origin == null) {
            buffers.clear()
            lastRecordedTimestamps.clear()
        } else {
            for ((_, queue) in buffers) {
                queue.removeAll { it.origin == origin }
            }
            lastRecordedTimestamps.keys.removeAll { it.second == origin }
        }
        updateRecentSamplesLocked()
    }

    override suspend fun getCount(origin: DataOrigin?): Int = synchronized(lock) {
        return buffers.values.flatten().count { origin == null || it.origin == origin }
    }

    private fun updateRecentSamplesLocked() {
        _recentSamplesFlow.value = buffers.values.flatten()
            .sortedByDescending { it.timestampEpochMillis }
            .take(100)
    }
}
