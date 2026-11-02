package com.farmvision.agrirover.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.data.local.SettingsPreferencesManager
import com.farmvision.agrirover.data.model.AppLanguage
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.model.ThemeMode
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.StandardRoverHttpClient
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.domain.alert.repository.AlertSettingsRepository
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.repository.AssistantRepository
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Diagnostics execution state for testing backend reachability.
 */
sealed interface DiagnosticsState {
    data object Idle : DiagnosticsState
    data object Testing : DiagnosticsState
    data class Success(val latencyMs: Long, val message: String) : DiagnosticsState
    data class Failure(val message: String) : DiagnosticsState
}

/**
 * Application storage metrics for private local databases and cache.
 */
data class StorageStats(
    val conversationCount: Int = 0,
    val conversationFileSizeBytes: Long = 0L,
    val telemetryHistoryCount: Int = 0,
    val telemetryHistorySizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L
)

/**
 * Centralized Settings and Configuration Management ViewModel.
 * Unifies state management across General, Rover, AI, Privacy, Notifications, and Data Management.
 */
class SettingsViewModel(
    private val preferencesManager: SettingsPreferencesManager,
    private val assistantRepository: AssistantRepository? = null,
    private val alertSettingsRepository: AlertSettingsRepository? = null,
    private val telemetryHistoryRepository: TelemetryHistoryRepository? = null,
    private val httpClient: RoverHttpClient = StandardRoverHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    val settings: StateFlow<FarmSettings> = preferencesManager.settingsFlow

    val consentPreferences: StateFlow<AssistantConsentPreferences> =
        assistantRepository?.getConsentPreferencesFlow()?.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AssistantConsentPreferences()
        ) ?: MutableStateFlow(AssistantConsentPreferences()).asStateFlow()

    val alertThresholds: StateFlow<AlertThresholdConfig> =
        alertSettingsRepository?.settingsFlow ?: MutableStateFlow(AlertThresholdConfig()).asStateFlow()

    private val _diagnosticsState = MutableStateFlow<DiagnosticsState>(DiagnosticsState.Idle)
    val diagnosticsState: StateFlow<DiagnosticsState> = _diagnosticsState.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    // --- MODULE 1: GENERAL SETTINGS ---

    fun setThemeMode(mode: ThemeMode) {
        preferencesManager.updateThemeMode(mode)
    }

    fun setLanguage(language: AppLanguage) {
        preferencesManager.updateLanguage(language)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        preferencesManager.updateTemperatureUnit(unit)
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        preferencesManager.updateSpeedUnit(unit)
    }

    fun setSimulatedEngine(active: Boolean) {
        preferencesManager.updateSimulatedEngine(active)
    }

    fun resetGeneralPreferences() {
        preferencesManager.resetPreferences()
    }

    // --- MODULE 2: ROVER & DEVICE SETTINGS ---

    fun setRoverName(name: String) {
        preferencesManager.updateRoverName(name)
    }

    fun setBackendUrl(url: String) {
        preferencesManager.updateBackendUrl(url)
    }

    fun setTransportProtocol(protocol: String) {
        preferencesManager.updateTransportProtocol(protocol)
    }

    fun testBackendConnection(targetUrl: String? = null) {
        val baseUrl = targetUrl ?: settings.value.backendUrl
        _diagnosticsState.value = DiagnosticsState.Testing

        scope.launch {
            val startTime = System.currentTimeMillis()
            val healthUrl = "${baseUrl.trimEnd('/')}/health"
            try {
                val response = withContext(ioDispatcher) {
                    httpClient.get(
                        url = healthUrl,
                        headers = mapOf("Accept" to "application/json"),
                        timeoutMs = 5000L
                    )
                }
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    _diagnosticsState.value = DiagnosticsState.Success(
                        latencyMs = latency,
                        message = "FastAPI Cloud Gateway connected (HTTP ${response.statusCode}, ${latency}ms)"
                    )
                } else {
                    _diagnosticsState.value = DiagnosticsState.Failure(
                        "Backend returned HTTP ${response.statusCode}"
                    )
                }
            } catch (e: Exception) {
                _diagnosticsState.value = DiagnosticsState.Failure(
                    "Connection failed: ${e.message ?: "Unable to reach $healthUrl"}"
                )
            }
        }
    }

    fun clearDiagnostics() {
        _diagnosticsState.value = DiagnosticsState.Idle
    }

    // --- MODULE 4: PRIVACY & SECURITY ---

    fun updateConsent(preferences: AssistantConsentPreferences) {
        scope.launch {
            assistantRepository?.updateConsentPreferences(preferences)
        }
    }

    fun toggleConsentGranted(granted: Boolean) {
        val current = consentPreferences.value
        updateConsent(current.copy(consentGranted = granted))
    }

    fun toggleShareLiveTelemetry(share: Boolean) {
        val current = consentPreferences.value
        updateConsent(current.copy(shareLiveTelemetry = share))
    }

    fun toggleShareAlerts(share: Boolean) {
        val current = consentPreferences.value
        updateConsent(current.copy(shareAlerts = share))
    }

    fun toggleShareHistoricalAnalytics(share: Boolean) {
        val current = consentPreferences.value
        updateConsent(current.copy(shareHistoricalAnalytics = share))
    }

    fun toggleShareCropProfile(share: Boolean) {
        val current = consentPreferences.value
        updateConsent(current.copy(shareCropProfile = share))
    }

    // --- MODULE 5: NOTIFICATION SETTINGS ---

    fun setNotificationSound(enabled: Boolean) {
        preferencesManager.updateNotificationSound(enabled)
    }

    fun setNotificationVibration(enabled: Boolean) {
        preferencesManager.updateNotificationVibration(enabled)
    }

    // --- MODULE 6: DATA MANAGEMENT ---

    fun refreshStorageStats(context: Context) {
        scope.launch {
            val filesDir = context.filesDir
            val cacheDir = context.cacheDir

            val convFile = File(filesDir, "assistant_conversations.json")
            val convSize = if (convFile.exists()) convFile.length() else 0L
            val convCount = assistantRepository?.getConversations()?.size ?: 0

            val teleFile = File(filesDir, "telemetry_history.json")
            val teleSize = if (teleFile.exists()) teleFile.length() else 0L
            val teleCount = telemetryHistoryRepository?.getCount() ?: 0

            val cacheSize = withContext(ioDispatcher) {
                calculateDirectorySize(cacheDir)
            }

            _storageStats.value = StorageStats(
                conversationCount = convCount,
                conversationFileSizeBytes = convSize,
                telemetryHistoryCount = teleCount,
                telemetryHistorySizeBytes = teleSize,
                cacheSizeBytes = cacheSize
            )
        }
    }

    fun clearAiConversations(context: Context) {
        scope.launch {
            assistantRepository?.clearAllConversations()
            refreshStorageStats(context)
        }
    }

    fun clearTelemetryHistory(context: Context) {
        scope.launch {
            telemetryHistoryRepository?.clearHistory()
            refreshStorageStats(context)
        }
    }

    fun clearAppCache(context: Context) {
        scope.launch {
            withContext(ioDispatcher) {
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
            }
            refreshStorageStats(context)
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
}

