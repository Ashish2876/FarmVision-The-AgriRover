package com.farmvision.agrirover.data.local

import android.content.Context
import android.content.SharedPreferences
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
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

        return FarmSettings(
            temperatureUnit = tempUnit,
            speedUnit = speedUnit,
            transportProtocol = protocol,
            isSimulatedEngineActive = mockActive
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

    companion object {
        private const val KEY_TEMP_UNIT = "key_temp_unit"
        private const val KEY_SPEED_UNIT = "key_speed_unit"
        private const val KEY_PROTOCOL = "key_protocol"
        private const val KEY_MOCK_ACTIVE = "key_mock_active"
    }
}
