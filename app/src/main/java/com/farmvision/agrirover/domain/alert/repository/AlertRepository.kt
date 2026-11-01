package com.farmvision.agrirover.domain.alert.repository

import com.farmvision.agrirover.domain.alert.model.AlertItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface governing durable storage, retrieval, and state mutations for alerts.
 */
interface AlertRepository {

    /** Reactive stream of unresolved (Active or Acknowledged) alerts. */
    val activeAlertsFlow: StateFlow<List<AlertItem>>

    /** Reactive stream of resolved historical alerts. */
    val resolvedAlertsFlow: StateFlow<List<AlertItem>>

    /**
     * Persists updated lists of active and resolved alerts to durable storage.
     */
    suspend fun saveAlerts(active: List<AlertItem>, resolved: List<AlertItem>): Result<Unit>

    /**
     * Acknowledges an active alert by ID, marking it read and recording timestamp.
     */
    suspend fun acknowledgeAlert(id: String): Result<Boolean>

    /**
     * Clears all resolved historical alerts from durable storage.
     */
    suspend fun clearHistory(): Result<Unit>

    /**
     * Looks up an individual alert by its unique ID.
     */
    suspend fun getAlertById(id: String): AlertItem?
}

