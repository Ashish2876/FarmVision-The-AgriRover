package com.farmvision.agrirover.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.farmvision.agrirover.data.model.hardware.HardwareConnectionSettings
import com.farmvision.agrirover.ui.hardware.model.HardwareOperationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface governing rover hardware connection and operation mode settings.
 */
interface HardwareSettingsRepository {
    val settingsFlow: StateFlow<HardwareConnectionSettings>
    fun getSettings(): HardwareConnectionSettings
    fun updateSettings(settings: HardwareConnectionSettings)
    fun setOperationMode(mode: HardwareOperationMode)
    fun resetToDefaults()
}

/**
 * SharedPreferences-backed implementation of [HardwareSettingsRepository].
 *
 * Persistence & Safety Guarantees:
 * - Persists all endpoint addresses, ports, and configuration.
 * - DEMO MODE never silently transitions to LIVE mode.
 * - Live mode never silently falls back to Demo mode.
 * - Auth tokens are stored safely in private preferences.
 */
class SharedPreferencesHardwareSettingsRepository(
    private val prefs: SharedPreferences
) : HardwareSettingsRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _settingsFlow = MutableStateFlow(loadSettings())
    override val settingsFlow: StateFlow<HardwareConnectionSettings> = _settingsFlow.asStateFlow()

    override fun getSettings(): HardwareConnectionSettings = _settingsFlow.value

    override fun updateSettings(settings: HardwareConnectionSettings) {
        prefs.edit()
            .putString(KEY_OP_MODE, settings.operationMode.name)
            .putString(KEY_MAIN_HOST, settings.mainEsp32Host.trim())
            .putInt(KEY_MAIN_PORT, settings.mainEsp32Port)
            .putString(KEY_CAM_HOST, settings.esp32CamHost.trim())
            .putInt(KEY_CAM_PORT, settings.esp32CamPort)
            .putString(KEY_AUTH_TOKEN, settings.authToken)
            .putLong(KEY_TIMEOUT_MS, settings.connectionTimeoutMs)
            .putLong(KEY_POLLING_INTERVAL_MS, settings.telemetryPollingIntervalMs)
            .apply()

        _settingsFlow.value = settings
    }

    override fun setOperationMode(mode: HardwareOperationMode) {
        val current = _settingsFlow.value
        if (current.operationMode != mode) {
            val updated = current.copy(operationMode = mode)
            prefs.edit().putString(KEY_OP_MODE, mode.name).apply()
            _settingsFlow.value = updated
        }
    }

    override fun resetToDefaults() {
        val defaults = HardwareConnectionSettings()
        updateSettings(defaults)
    }

    private fun loadSettings(): HardwareConnectionSettings {
        val opModeName = prefs.getString(KEY_OP_MODE, HardwareOperationMode.Demo.name)
        val opMode = try {
            HardwareOperationMode.valueOf(opModeName ?: HardwareOperationMode.Demo.name)
        } catch (_: Exception) {
            HardwareOperationMode.Demo
        }

        val mainHost = prefs.getString(KEY_MAIN_HOST, HardwareConnectionSettings.DEFAULT_MAIN_ESP32_HOST)
            ?: HardwareConnectionSettings.DEFAULT_MAIN_ESP32_HOST
        val mainPort = prefs.getInt(KEY_MAIN_PORT, HardwareConnectionSettings.DEFAULT_MAIN_ESP32_PORT)
        val camHost = prefs.getString(KEY_CAM_HOST, HardwareConnectionSettings.DEFAULT_ESP32_CAM_HOST)
            ?: HardwareConnectionSettings.DEFAULT_ESP32_CAM_HOST
        val camPort = prefs.getInt(KEY_CAM_PORT, HardwareConnectionSettings.DEFAULT_ESP32_CAM_PORT)
        val authToken = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        val timeoutMs = prefs.getLong(KEY_TIMEOUT_MS, HardwareConnectionSettings.DEFAULT_TIMEOUT_MS)
        val pollingMs = prefs.getLong(KEY_POLLING_INTERVAL_MS, HardwareConnectionSettings.DEFAULT_POLLING_INTERVAL_MS)

        return HardwareConnectionSettings(
            operationMode = opMode,
            mainEsp32Host = mainHost,
            mainEsp32Port = if (mainPort in 1..65535) mainPort else HardwareConnectionSettings.DEFAULT_MAIN_ESP32_PORT,
            esp32CamHost = camHost,
            esp32CamPort = if (camPort in 1..65535) camPort else HardwareConnectionSettings.DEFAULT_ESP32_CAM_PORT,
            authToken = authToken,
            connectionTimeoutMs = if (timeoutMs > 0L) timeoutMs else HardwareConnectionSettings.DEFAULT_TIMEOUT_MS,
            telemetryPollingIntervalMs = if (pollingMs > 0L) pollingMs else HardwareConnectionSettings.DEFAULT_POLLING_INTERVAL_MS
        )
    }

    companion object {
        const val PREFS_NAME = "farmvision_hardware_settings"
        private const val KEY_OP_MODE = "key_op_mode"
        private const val KEY_MAIN_HOST = "key_main_host"
        private const val KEY_MAIN_PORT = "key_main_port"
        private const val KEY_CAM_HOST = "key_cam_host"
        private const val KEY_CAM_PORT = "key_cam_port"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_TIMEOUT_MS = "key_timeout_ms"
        private const val KEY_POLLING_INTERVAL_MS = "key_polling_interval_ms"
    }
}

/**
 * Pure in-memory implementation of [HardwareSettingsRepository] for JVM unit tests and Compose previews.
 */
class InMemoryHardwareSettingsRepository(
    initialSettings: HardwareConnectionSettings = HardwareConnectionSettings()
) : HardwareSettingsRepository {

    private val _settingsFlow = MutableStateFlow(initialSettings)
    override val settingsFlow: StateFlow<HardwareConnectionSettings> = _settingsFlow.asStateFlow()

    override fun getSettings(): HardwareConnectionSettings = _settingsFlow.value

    override fun updateSettings(settings: HardwareConnectionSettings) {
        _settingsFlow.value = settings
    }

    override fun setOperationMode(mode: HardwareOperationMode) {
        _settingsFlow.value = _settingsFlow.value.copy(operationMode = mode)
    }

    override fun resetToDefaults() {
        _settingsFlow.value = HardwareConnectionSettings()
    }
}
