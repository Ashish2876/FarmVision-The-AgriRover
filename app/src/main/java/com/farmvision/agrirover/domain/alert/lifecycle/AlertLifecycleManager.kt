package com.farmvision.agrirover.domain.alert.lifecycle

import com.farmvision.agrirover.domain.alert.engine.AlertEvaluationOutput
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.AlertStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Events dispatched by [AlertLifecycleManager] to notification subscribers.
 */
sealed interface AlertLifecycleEvent {
    data class Raised(val alert: AlertItem) : AlertLifecycleEvent
    data class Escalated(val alert: AlertItem, val previousSeverity: AlertSeverity) : AlertLifecycleEvent
    data class Resolved(val alert: AlertItem) : AlertLifecycleEvent
}

/**
 * Manages the lifecycle state transitions, deduplication, and occurrence tracking for alerts.
 * Invariant: At most one active/unresolved alert per dedupKey.
 */
class AlertLifecycleManager(
    initialActiveAlerts: List<AlertItem> = emptyList(),
    initialResolvedAlerts: List<AlertItem> = emptyList()
) {
    private val _activeAlertsFlow = MutableStateFlow(initialActiveAlerts)
    val activeAlertsFlow: StateFlow<List<AlertItem>> = _activeAlertsFlow.asStateFlow()

    private val _resolvedAlertsFlow = MutableStateFlow(initialResolvedAlerts)
    val resolvedAlertsFlow: StateFlow<List<AlertItem>> = _resolvedAlertsFlow.asStateFlow()

    // Temporary event buffer consumed by Notification Manager
    private val pendingEvents = mutableListOf<AlertLifecycleEvent>()

    /**
     * Ingests single evaluation cycle outputs from [com.farmvision.agrirover.domain.alert.engine.AlertEngine].
     * Returns a list of lifecycle events suitable for notification dispatch.
     */
    fun processEvaluationOutput(
        output: AlertEvaluationOutput,
        nowEpochMillis: Long
    ): List<AlertLifecycleEvent> {
        val events = mutableListOf<AlertLifecycleEvent>()
        val currentActive = _activeAlertsFlow.value.toMutableList()
        val currentResolved = _resolvedAlertsFlow.value.toMutableList()

        // 1. Process New Alerts
        output.newAlerts.forEach { newAlert ->
            val existingIndex = currentActive.indexOfFirst { it.dedupKey == newAlert.dedupKey }
            if (existingIndex < 0) {
                currentActive.add(0, newAlert)
                events.add(AlertLifecycleEvent.Raised(newAlert))
            } else {
                // If already present, treat as repeat
                val existing = currentActive[existingIndex]
                val updated = existing.recordRepeat(nowEpochMillis, newAlert.triggerValue, newAlert.severity)
                currentActive[existingIndex] = updated
                if (updated.severity.isHigherThan(existing.severity)) {
                    events.add(AlertLifecycleEvent.Escalated(updated, existing.severity))
                }
            }
        }

        // 2. Process Repeat Breaches
        output.repeatKeys.forEach { (dedupKey, currentValue) ->
            val index = currentActive.indexOfFirst { it.dedupKey == dedupKey }
            if (index >= 0) {
                val existing = currentActive[index]
                val updated = existing.recordRepeat(nowEpochMillis, currentValue)
                currentActive[index] = updated
            }
        }

        // 3. Process Auto-Resolutions
        output.resolvedKeys.forEach { (dedupKey, resolvingVal) ->
            val index = currentActive.indexOfFirst { it.dedupKey == dedupKey }
            if (index >= 0) {
                val activeAlert = currentActive.removeAt(index)
                val resolvedAlert = activeAlert.resolve(
                    timestampEpochMillis = nowEpochMillis,
                    resolvingVal = resolvingVal,
                    recoveryNote = "Metric returned to normal operating range"
                )
                currentResolved.add(0, resolvedAlert)
                events.add(AlertLifecycleEvent.Resolved(resolvedAlert))
            }
        }

        _activeAlertsFlow.value = currentActive
        _resolvedAlertsFlow.value = currentResolved

        return events
    }

    /**
     * Operator acknowledges an alert. Acknowledging does NOT resolve the alert.
     */
    fun acknowledgeAlert(alertId: String, nowEpochMillis: Long): Boolean {
        val currentActive = _activeAlertsFlow.value.toMutableList()
        val index = currentActive.indexOfFirst { it.id == alertId }
        if (index >= 0) {
            val alert = currentActive[index]
            currentActive[index] = alert.acknowledge(nowEpochMillis)
            _activeAlertsFlow.value = currentActive
            return true
        }
        return false
    }

    /**
     * Restores in-memory state from disk storage.
     */
    fun restoreState(active: List<AlertItem>, resolved: List<AlertItem>) {
        _activeAlertsFlow.value = active
        _resolvedAlertsFlow.value = resolved
    }

    /**
     * Clears historical resolved alerts (User action).
     */
    fun clearResolvedHistory() {
        _resolvedAlertsFlow.value = emptyList()
    }
}

