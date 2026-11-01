package com.farmvision.agrirover.data.alert.repository

import android.content.Context
import com.farmvision.agrirover.data.alert.local.AlertJsonSerializer
import com.farmvision.agrirover.data.alert.local.AlertStorePayload
import com.farmvision.agrirover.data.time.SystemTimeProvider
import com.farmvision.agrirover.data.time.TimeProvider
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.repository.AlertRepository
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
 * Production implementation of [AlertRepository] backed by a lightweight,
 * versioned, atomic JSON file in internal app storage.
 *
 * Durability: Persists active and resolved alerts across application restarts.
 * Concurrency: Thread-safe serialized updates via JVM monitor lock.
 * Bounded Retention: Prunes resolved records older than [retentionDays] (default 30d)
 * and caps total resolved records at [maxResolvedRecords] (default 500).
 */
class JsonFileAlertRepository(
    val storageFile: File,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    val retentionDays: Long = 30L,
    val maxResolvedRecords: Int = 500
) : AlertRepository {

    constructor(
        context: Context,
        timeProvider: TimeProvider = SystemTimeProvider(),
        retentionDays: Long = 30L,
        maxResolvedRecords: Int = 500
    ) : this(
        storageFile = File(context.filesDir, DEFAULT_FILENAME),
        timeProvider = timeProvider,
        retentionDays = retentionDays,
        maxResolvedRecords = maxResolvedRecords
    )

    companion object {
        const val DEFAULT_FILENAME = "alerts_store.json"
    }

    private val lock = Any()

    private val activeList = mutableListOf<AlertItem>()
    private val resolvedList = mutableListOf<AlertItem>()

    private val _activeAlertsFlow = MutableStateFlow<List<AlertItem>>(emptyList())
    override val activeAlertsFlow: StateFlow<List<AlertItem>> = _activeAlertsFlow.asStateFlow()

    private val _resolvedAlertsFlow = MutableStateFlow<List<AlertItem>>(emptyList())
    override val resolvedAlertsFlow: StateFlow<List<AlertItem>> = _resolvedAlertsFlow.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() = synchronized(lock) {
        if (!storageFile.exists() || storageFile.length() == 0L) {
            return
        }

        try {
            val json = storageFile.readText(StandardCharsets.UTF_8)
            val payload = AlertJsonSerializer.deserialize(json)

            activeList.clear()
            activeList.addAll(payload.activeAlerts)

            resolvedList.clear()
            resolvedList.addAll(payload.resolvedAlerts)

            pruneExpiredResolvedLocked()
            syncFlowsLocked()
        } catch (e: Exception) {
            // Graceful corruption fallback: back up corrupt file and reinitialize clean
            try {
                val backupFile = File(storageFile.parentFile, "${storageFile.name}.bak")
                storageFile.renameTo(backupFile)
            } catch (_: Exception) {}

            activeList.clear()
            resolvedList.clear()
            syncFlowsLocked()
        }
    }

    private fun pruneExpiredResolvedLocked() {
        val now = timeProvider.nowEpochMillis()
        val cutoff = now - (retentionDays * 24L * 60 * 60 * 1000)

        // Filter out expired resolved alerts
        resolvedList.removeAll { alert ->
            val resolvedTime = alert.resolvedTimestampEpochMillis ?: alert.createdTimestampEpochMillis
            resolvedTime < cutoff
        }

        // Enforce max count
        if (resolvedList.size > maxResolvedRecords) {
            val trimmed = resolvedList.take(maxResolvedRecords)
            resolvedList.clear()
            resolvedList.addAll(trimmed)
        }
    }

    private fun syncFlowsLocked() {
        _activeAlertsFlow.value = activeList.toList()
        _resolvedAlertsFlow.value = resolvedList.toList()
    }

    private fun persistToDiskLocked() {
        val parent = storageFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val payload = AlertStorePayload(
            schemaVersion = AlertJsonSerializer.CURRENT_SCHEMA_VERSION,
            lastSavedEpochMillis = timeProvider.nowEpochMillis(),
            activeAlerts = activeList.toList(),
            resolvedAlerts = resolvedList.toList()
        )
        val json = AlertJsonSerializer.serialize(payload)

        val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        FileOutputStream(tempFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(json)
                writer.flush()
            }
        }

        Files.move(
            tempFile.toPath(),
            storageFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    override suspend fun saveAlerts(active: List<AlertItem>, resolved: List<AlertItem>): Result<Unit> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                try {
                    activeList.clear()
                    activeList.addAll(active)

                    resolvedList.clear()
                    resolvedList.addAll(resolved)

                    pruneExpiredResolvedLocked()
                    persistToDiskLocked()
                    syncFlowsLocked()
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    override suspend fun acknowledgeAlert(id: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                try {
                    val index = activeList.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        val alert = activeList[index]
                        activeList[index] = alert.acknowledge(timeProvider.nowEpochMillis())
                        persistToDiskLocked()
                        syncFlowsLocked()
                        Result.success(true)
                    } else {
                        Result.success(false)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    override suspend fun clearHistory(): Result<Unit> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                try {
                    resolvedList.clear()
                    persistToDiskLocked()
                    syncFlowsLocked()
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    override suspend fun getAlertById(id: String): AlertItem? =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                activeList.find { it.id == id } ?: resolvedList.find { it.id == id }
            }
        }
}

