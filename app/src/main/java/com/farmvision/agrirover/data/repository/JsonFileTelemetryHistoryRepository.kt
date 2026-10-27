package com.farmvision.agrirover.data.repository

import android.content.Context
import com.farmvision.agrirover.data.local.TelemetryHistoryJsonSerializer
import com.farmvision.agrirover.data.local.TelemetryHistoryPayload
import com.farmvision.agrirover.data.model.AnalyticsTimeWindow
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.data.time.SystemTimeProvider
import com.farmvision.agrirover.data.time.TimeProvider
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Production implementation of [TelemetryHistoryRepository] backed by a lightweight,
 * versioned atomic JSON file in internal app storage (Default D2(a)).
 *
 * Durability: Persists telemetry samples across application restarts.
 * Concurrency: Thread-safe serialized updates via JVM monitor lock.
 * Write Throttling: Enforces 1 sample per metric per [writeIntervalSeconds] (default 60s).
 * Bounded Retention: Prunes records older than [retentionDays] (default 30d) and caps total rows at [maxTotalRecords] (default 5000).
 */
class JsonFileTelemetryHistoryRepository(
    val storageFile: File,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    val writeIntervalSeconds: Long = 60L,
    val retentionDays: Long = 30L,
    val maxTotalRecords: Int = 5000
) : TelemetryHistoryRepository {

    constructor(
        context: Context,
        timeProvider: TimeProvider = SystemTimeProvider(),
        writeIntervalSeconds: Long = 60L,
        retentionDays: Long = 30L,
        maxTotalRecords: Int = 5000
    ) : this(
        storageFile = File(context.filesDir, DEFAULT_FILENAME),
        timeProvider = timeProvider,
        writeIntervalSeconds = writeIntervalSeconds,
        retentionDays = retentionDays,
        maxTotalRecords = maxTotalRecords
    )

    companion object {
        const val DEFAULT_FILENAME = "telemetry_history.json"
    }

    private val lock = Any()

    private val samplesList = mutableListOf<TelemetrySample>()
    private val lastRecordedTimestamps = mutableMapOf<Pair<HistoricalMetric, DataOrigin>, Long>()

    private val _recentSamplesFlow = MutableStateFlow<List<TelemetrySample>>(emptyList())
    override val recentSamplesFlow: StateFlow<List<TelemetrySample>> = _recentSamplesFlow.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() = synchronized(lock) {
        if (!storageFile.exists() || storageFile.length() == 0L) {
            return
        }

        try {
            val json = storageFile.readText(StandardCharsets.UTF_8)
            val payload = TelemetryHistoryJsonSerializer.deserialize(json)
            samplesList.clear()
            samplesList.addAll(payload.samples)

            // Reconstruct last recorded timestamps
            lastRecordedTimestamps.clear()
            for (s in samplesList) {
                val key = Pair(s.metric, s.origin)
                val existing = lastRecordedTimestamps[key] ?: 0L
                if (s.timestampEpochMillis > existing) {
                    lastRecordedTimestamps[key] = s.timestampEpochMillis
                }
            }

            updateRecentFlowLocked()
        } catch (e: Exception) {
            // Quarantine corrupted file to avoid crashes
            val backup = File(storageFile.parentFile, "${storageFile.name}.corrupt_${System.currentTimeMillis()}")
            try {
                if (storageFile.exists()) {
                    Files.move(storageFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (_: Exception) { }
            samplesList.clear()
            updateRecentFlowLocked()
        }
    }

    private fun persistToDiskLocked() {
        val payload = TelemetryHistoryPayload(
            schemaVersion = TelemetryHistoryJsonSerializer.CURRENT_SCHEMA_VERSION,
            lastSavedEpochMillis = timeProvider.nowEpochMillis(),
            samples = samplesList
        )
        val json = TelemetryHistoryJsonSerializer.serialize(payload)

        val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }
            Files.move(tempFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            // Fallback non-atomic rename if ATOMIC_MOVE not supported by filesystem
            try {
                Files.move(tempFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) { }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    override suspend fun recordSample(sample: TelemetrySample): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val key = Pair(sample.metric, sample.origin)
            val lastRecorded = lastRecordedTimestamps[key] ?: 0L
            val minIntervalMillis = writeIntervalSeconds * 1000L

            if (sample.timestampEpochMillis - lastRecorded < minIntervalMillis && lastRecorded > 0L) {
                return@synchronized false
            }

            // Enforce automatic retention pruning
            val now = timeProvider.nowEpochMillis()
            val cutoff = now - (retentionDays * 86400000L)
            samplesList.removeAll { it.timestampEpochMillis < cutoff }

            // Add new sample
            samplesList.add(sample)
            lastRecordedTimestamps[key] = sample.timestampEpochMillis

            // Enforce total row cap
            if (samplesList.size > maxTotalRecords) {
                val excess = samplesList.size - maxTotalRecords
                repeat(excess) {
                    if (samplesList.isNotEmpty()) {
                        samplesList.removeAt(0)
                    }
                }
            }

            persistToDiskLocked()
            updateRecentFlowLocked()
            true
        }
    }

    override suspend fun recordSamples(samples: List<TelemetrySample>): Int = withContext(Dispatchers.IO) {
        synchronized(lock) {
            var count = 0
            val minIntervalMillis = writeIntervalSeconds * 1000L

            for (sample in samples) {
                val key = Pair(sample.metric, sample.origin)
                val lastRecorded = lastRecordedTimestamps[key] ?: 0L
                if (sample.timestampEpochMillis - lastRecorded >= minIntervalMillis || lastRecorded == 0L) {
                    samplesList.add(sample)
                    lastRecordedTimestamps[key] = sample.timestampEpochMillis
                    count++
                }
            }

            if (count > 0) {
                val now = timeProvider.nowEpochMillis()
                val cutoff = now - (retentionDays * 86400000L)
                samplesList.removeAll { it.timestampEpochMillis < cutoff }

                if (samplesList.size > maxTotalRecords) {
                    val excess = samplesList.size - maxTotalRecords
                    repeat(excess) {
                        if (samplesList.isNotEmpty()) {
                            samplesList.removeAt(0)
                        }
                    }
                }

                persistToDiskLocked()
                updateRecentFlowLocked()
            }
            count
        }
    }

    override suspend fun getSamples(
        metric: HistoricalMetric,
        timeWindow: AnalyticsTimeWindow,
        origin: DataOrigin?
    ): List<TelemetrySample> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            samplesList.filter { sample ->
                sample.metric == metric &&
                    sample.timestampEpochMillis in timeWindow.startEpochMillis..timeWindow.endEpochMillis &&
                    (origin == null || sample.origin == origin)
            }.sortedBy { it.timestampEpochMillis }
        }
    }

    override suspend fun getRecentRecords(
        limit: Int,
        offset: Int,
        metric: HistoricalMetric?,
        origin: DataOrigin?
    ): List<TelemetrySample> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val filtered = samplesList.filter { sample ->
                (metric == null || sample.metric == metric) &&
                    (origin == null || sample.origin == origin)
            }.sortedByDescending { it.timestampEpochMillis }

            if (offset >= filtered.size) return@synchronized emptyList()
            filtered.drop(offset).take(limit)
        }
    }

    override suspend fun pruneOldRecords(retentionCutoffMillis: Long): Int = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val initialSize = samplesList.size
            samplesList.removeAll { it.timestampEpochMillis < retentionCutoffMillis }
            val pruned = initialSize - samplesList.size
            if (pruned > 0) {
                persistToDiskLocked()
                updateRecentFlowLocked()
            }
            pruned
        }
    }

    override suspend fun clearHistory(origin: DataOrigin?) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (origin == null) {
                samplesList.clear()
                lastRecordedTimestamps.clear()
            } else {
                samplesList.removeAll { it.origin == origin }
                lastRecordedTimestamps.keys.removeAll { it.second == origin }
            }
            persistToDiskLocked()
            updateRecentFlowLocked()
        }
    }

    override suspend fun getCount(origin: DataOrigin?): Int = withContext(Dispatchers.IO) {
        synchronized(lock) {
            samplesList.count { origin == null || it.origin == origin }
        }
    }

    private fun updateRecentFlowLocked() {
        _recentSamplesFlow.value = samplesList
            .sortedByDescending { it.timestampEpochMillis }
            .take(100)
    }
}
