package com.farmvision.agrirover.domain.alert.repository

import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing and persisting user-configurable alert thresholds.
 */
interface AlertSettingsRepository {

    /** Reactive stream of the active alert threshold and evaluation configuration. */
    val settingsFlow: StateFlow<AlertThresholdConfig>

    /**
     * Updates active thresholds after strict domain validation.
     * Throws [IllegalArgumentException] with user-facing validation errors if invalid.
     */
    suspend fun updateSettings(config: AlertThresholdConfig): Result<Unit>

    /**
     * Restores factory development baseline defaults.
     */
    suspend fun restoreDefaults(): Result<Unit>
}

