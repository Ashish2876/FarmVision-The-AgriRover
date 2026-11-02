package com.farmvision.agrirover.data.local

import android.content.Context
import android.content.SharedPreferences
import com.farmvision.agrirover.data.model.AppLanguage
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent user preferences via SharedPreferences.
 * Emits reactive [FarmSettings] changes to all observing screens.
 */
class SettingsPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("farmvision_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<FarmSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): FarmSettings {
        val tempUnitName = prefs.getString(KEY_TEMP_UNIT, TemperatureUnit.Celsius.name)
        val speedUnitName = prefs.getString(KEY_SPEED_UNIT, SpeedUnit.Metric.name)
        val protocol = prefs.getString(KEY_PROTOCOL, "Wi-Fi SoftAP") ?: "Wi-Fi SoftAP"
        val mockActive = prefs.getBoolean(KEY_MOCK_ACTIVE, true)
        val themeModeName = prefs.getString(KEY_THEME_MODE, ThemeMode.System.name)
        val langCode = prefs.getString(KEY_LANGUAGE, AppLanguage.English.code)
        val roverName = prefs.getString(KEY_ROVER_NAME, "AgriRover Alpha") ?: "AgriRover Alpha"
        val backendUrl = prefs.getString(KEY_BACKEND_URL, "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        val notifSound = prefs.getBoolean(KEY_NOTIF_SOUND, true)
        val notifVibe = prefs.getBoolean(KEY_NOTIF_VIBE, true)

        val tempUnit = try {
            TemperatureUnit.valueOf(tempUnitName ?: TemperatureUnit.Celsius.name)
        } catch (_: Exception) {
            TemperatureUnit.Celsius
        }

        val speedUnit = try {
            SpeedUnit.valueOf(speedUnitName ?: SpeedUnit.Metric.name)
        } catch (_: Exception) {
            SpeedUnit.Metric
        }

        val themeMode = ThemeMode.fromString(themeModeName)
        val language = AppLanguage.fromCode(langCode)

        return FarmSettings(
            temperatureUnit = tempUnit,
            speedUnit = speedUnit,
            transportProtocol = protocol,
            isSimulatedEngineActive = mockActive,
            themeMode = themeMode,
            language = language,
            roverName = roverName,
            backendUrl = backendUrl,
            isNotificationSoundEnabled = notifSound,
            isNotificationVibrationEnabled = notifVibe
        )
    }

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        prefs.edit().putString(KEY_TEMP_UNIT, unit.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(temperatureUnit = unit)
    }

    fun updateSpeedUnit(unit: SpeedUnit) {
        prefs.edit().putString(KEY_SPEED_UNIT, unit.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(speedUnit = unit)
    }

    fun updateTransportProtocol(protocol: String) {
        prefs.edit().putString(KEY_PROTOCOL, protocol).apply()
        _settingsFlow.value = _settingsFlow.value.copy(transportProtocol = protocol)
    }

    fun updateSimulatedEngine(active: Boolean) {
        prefs.edit().putBoolean(KEY_MOCK_ACTIVE, active).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isSimulatedEngineActive = active)
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = mode)
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _settingsFlow.value = _settingsFlow.value.copy(language = language)
    }

    fun updateRoverName(name: String) {
        val sanitized = name.trim().ifBlank { "AgriRover Alpha" }
        prefs.edit().putString(KEY_ROVER_NAME, sanitized).apply()
        _settingsFlow.value = _settingsFlow.value.copy(roverName = sanitized)
    }

    fun updateBackendUrl(url: String) {
        val sanitized = url.trim().ifBlank { "http://10.0.2.2:8000" }
        prefs.edit().putString(KEY_BACKEND_URL, sanitized).apply()
        _settingsFlow.value = _settingsFlow.value.copy(backendUrl = sanitized)
    }

    fun updateNotificationSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_SOUND, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isNotificationSoundEnabled = enabled)
    }

    fun updateNotificationVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_VIBE, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isNotificationVibrationEnabled = enabled)
    }

    fun resetPreferences() {
        prefs.edit().clear().apply()
        _settingsFlow.value = FarmSettings()
    }

    companion object {
        private const val KEY_TEMP_UNIT = "key_temp_unit"
        private const val KEY_SPEED_UNIT = "key_speed_unit"
        private const val KEY_PROTOCOL = "key_protocol"
        private const val KEY_MOCK_ACTIVE = "key_mock_active"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_ROVER_NAME = "key_rover_name"
        private const val KEY_BACKEND_URL = "key_backend_url"
        private const val KEY_NOTIF_SOUND = "key_notif_sound"
        private const val KEY_NOTIF_VIBE = "key_notif_vibe"
    }
}
