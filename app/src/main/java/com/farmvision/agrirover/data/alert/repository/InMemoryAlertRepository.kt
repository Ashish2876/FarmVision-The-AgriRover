package com.farmvision.agrirover.data.alert.repository

import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double and preview implementation of [AlertRepository].
 */
class InMemoryAlertRepository(
    initialActive: List<AlertItem> = emptyList(),
    initialResolved: List<AlertItem> = emptyList()
) : AlertRepository {

    private val _activeAlertsFlow = MutableStateFlow(initialActive)
    override val activeAlertsFlow: StateFlow<List<AlertItem>> = _activeAlertsFlow.asStateFlow()

    private val _resolvedAlertsFlow = MutableStateFlow(initialResolved)
    override val resolvedAlertsFlow: StateFlow<List<AlertItem>> = _resolvedAlertsFlow.asStateFlow()

    override suspend fun saveAlerts(active: List<AlertItem>, resolved: List<AlertItem>): Result<Unit> {
        _activeAlertsFlow.value = active
        _resolvedAlertsFlow.value = resolved
        return Result.success(Unit)
    }

    override suspend fun acknowledgeAlert(id: String): Result<Boolean> {
        val current = _activeAlertsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val alert = current[index]
            current[index] = alert.acknowledge(System.currentTimeMillis())
            _activeAlertsFlow.value = current
            return Result.success(true)
        }
        return Result.success(false)
    }

    override suspend fun clearHistory(): Result<Unit> {
        _resolvedAlertsFlow.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun getAlertById(id: String): AlertItem? {
        return _activeAlertsFlow.value.find { it.id == id } ?: _resolvedAlertsFlow.value.find { it.id == id }
    }
}

