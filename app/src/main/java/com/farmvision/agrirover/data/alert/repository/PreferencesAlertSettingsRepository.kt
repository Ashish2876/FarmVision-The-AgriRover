package com.farmvision.agrirover.data.alert.repository

import android.content.Context
import android.content.SharedPreferences
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.domain.alert.repository.AlertSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences implementation of [AlertSettingsRepository].
 * Enforces strict input validation and runtime re-evaluation without application restart.
 */
class PreferencesAlertSettingsRepository(
    private val prefs: SharedPreferences
) : AlertSettingsRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        const val PREFS_NAME = "farmvision_alert_settings"

        // Keys
        private const val KEY_TEMP_MIN_WARN = "temp_min_warn"
        private const val KEY_TEMP_MIN_CRIT = "temp_min_crit"
        private const val KEY_TEMP_MAX_WARN = "temp_max_warn"
        private const val KEY_TEMP_MAX_CRIT = "temp_max_crit"
        private const val KEY_TEMP_HYST = "temp_hyst"

        private const val KEY_HUM_MIN_WARN = "hum_min_warn"
        private const val KEY_HUM_MIN_CRIT = "hum_min_crit"
        private const val KEY_HUM_MAX_WARN = "hum_max_warn"
        private const val KEY_HUM_MAX_CRIT = "hum_max_crit"
        private const val KEY_HUM_HYST = "hum_hyst"

        private const val KEY_SOIL_MIN_WARN = "soil_min_warn"
        private const val KEY_SOIL_MIN_CRIT = "soil_min_crit"
        private const val KEY_SOIL_MAX_WARN = "soil_max_warn"
        private const val KEY_SOIL_MAX_CRIT = "soil_max_crit"
        private const val KEY_SOIL_HYST = "soil_hyst"

        private const val KEY_PH_MIN_WARN = "ph_min_warn"
        private const val KEY_PH_MAX_WARN = "ph_max_warn"
        private const val KEY_PH_HYST = "ph_hyst"

        private const val KEY_OBSTACLE_WARN = "obstacle_warn"
        private const val KEY_OBSTACLE_CRIT = "obstacle_crit"
        private const val KEY_OBSTACLE_HYST = "obstacle_hyst"

        private const val KEY_LINK_WARN_TIMEOUT = "link_warn_timeout"
        private const val KEY_OFFLINE_CRIT_TIMEOUT = "offline_crit_timeout"

        private const val KEY_CONFIRM_COUNT = "confirm_count"
        private const val KEY_RECOVERY_COUNT = "recovery_count"

        private const val KEY_EN_SOIL = "enable_soil"
        private const val KEY_EN_ENV = "enable_env"
        private const val KEY_EN_PH = "enable_ph"
        private const val KEY_EN_ROVER = "enable_rover"

        /**
         * Validates a threshold configuration against physical and logical constraints.
         * Returns a list of human-readable error descriptions if invalid.
         */
        fun validate(config: AlertThresholdConfig): List<String> {
            val errors = mutableListOf<String>()

            if (config.tempMinWarning >= config.tempMaxWarning) {
                errors.add("Temperature min warning (${config.tempMinWarning}°C) must be less than max warning (${config.tempMaxWarning}°C).")
            }
            if (config.tempMinCritical >= config.tempMaxCritical) {
                errors.add("Temperature min critical (${config.tempMinCritical}°C) must be less than max critical (${config.tempMaxCritical}°C).")
            }
            if (config.tempHysteresis <= 0.0f) {
                errors.add("Temperature hysteresis must be greater than zero.")
            }

            if (config.humidityMinWarning >= config.humidityMaxWarning) {
                errors.add("Humidity min warning (${config.humidityMinWarning}%) must be less than max warning (${config.humidityMaxWarning}%).")
            }
            if (config.humidityMinCritical >= config.humidityMaxCritical) {
                errors.add("Humidity min critical (${config.humidityMinCritical}%) must be less than max critical (${config.humidityMaxCritical}%).")
            }
            if (config.humidityHysteresis <= 0.0f) {
                errors.add("Humidity hysteresis must be greater than zero.")
            }

            if (config.soilMoistureMinWarning >= config.soilMoistureMaxWarning) {
                errors.add("Soil moisture min warning (${config.soilMoistureMinWarning}%) must be less than max warning (${config.soilMoistureMaxWarning}%).")
            }
            if (config.soilMoistureHysteresis <= 0.0f) {
                errors.add("Soil moisture hysteresis must be greater than zero.")
            }

            if (config.soilPhMinWarning >= config.soilPhMaxWarning) {
                errors.add("Soil pH min warning (${config.soilPhMinWarning}) must be less than max warning (${config.soilPhMaxWarning}).")
            }
            if (config.soilPhHysteresis <= 0.0f) {
                errors.add("Soil pH hysteresis must be greater than zero.")
            }

            if (config.obstacleCriticalCm >= config.obstacleWarningCm) {
                errors.add("Obstacle critical distance (${config.obstacleCriticalCm} cm) must be less than warning distance (${config.obstacleWarningCm} cm).")
            }
            if (config.obstacleHysteresisCm <= 0.0f) {
                errors.add("Obstacle hysteresis must be greater than zero.")
            }

            if (config.linkWarningTimeoutSeconds <= 0L) {
                errors.add("Link warning timeout must be greater than zero.")
            }
            if (config.offlineCriticalTimeoutSeconds <= config.linkWarningTimeoutSeconds) {
                errors.add("Offline critical timeout (${config.offlineCriticalTimeoutSeconds}s) must exceed link warning timeout (${config.linkWarningTimeoutSeconds}s).")
            }

            if (config.confirmationCount < 1) {
                errors.add("Debounce confirmation count must be at least 1.")
            }
            if (config.recoveryConfirmationCount < 1) {
                errors.add("Recovery confirmation count must be at least 1.")
            }

            return errors
        }
    }

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    override val settingsFlow: StateFlow<AlertThresholdConfig> = _settingsFlow.asStateFlow()

    private fun loadFromPrefs(): AlertThresholdConfig {
        return try {
            val defaults = AlertThresholdConfig()
            AlertThresholdConfig(
                tempMinWarning = prefs.getFloat(KEY_TEMP_MIN_WARN, defaults.tempMinWarning),
                tempMinCritical = prefs.getFloat(KEY_TEMP_MIN_CRIT, defaults.tempMinCritical),
                tempMaxWarning = prefs.getFloat(KEY_TEMP_MAX_WARN, defaults.tempMaxWarning),
                tempMaxCritical = prefs.getFloat(KEY_TEMP_MAX_CRIT, defaults.tempMaxCritical),
                tempHysteresis = prefs.getFloat(KEY_TEMP_HYST, defaults.tempHysteresis),

                humidityMinWarning = prefs.getFloat(KEY_HUM_MIN_WARN, defaults.humidityMinWarning),
                humidityMinCritical = prefs.getFloat(KEY_HUM_MIN_CRIT, defaults.humidityMinCritical),
                humidityMaxWarning = prefs.getFloat(KEY_HUM_MAX_WARN, defaults.humidityMaxWarning),
                humidityMaxCritical = prefs.getFloat(KEY_HUM_MAX_CRIT, defaults.humidityMaxCritical),
                humidityHysteresis = prefs.getFloat(KEY_HUM_HYST, defaults.humidityHysteresis),

                soilMoistureMinWarning = prefs.getFloat(KEY_SOIL_MIN_WARN, defaults.soilMoistureMinWarning),
                soilMoistureMinCritical = prefs.getFloat(KEY_SOIL_MIN_CRIT, defaults.soilMoistureMinCritical),
                soilMoistureMaxWarning = prefs.getFloat(KEY_SOIL_MAX_WARN, defaults.soilMoistureMaxWarning),
                soilMoistureMaxCritical = prefs.getFloat(KEY_SOIL_MAX_CRIT, defaults.soilMoistureMaxCritical),
                soilMoistureHysteresis = prefs.getFloat(KEY_SOIL_HYST, defaults.soilMoistureHysteresis),

                soilPhMinWarning = prefs.getFloat(KEY_PH_MIN_WARN, defaults.soilPhMinWarning),
                soilPhMaxWarning = prefs.getFloat(KEY_PH_MAX_WARN, defaults.soilPhMaxWarning),
                soilPhHysteresis = prefs.getFloat(KEY_PH_HYST, defaults.soilPhHysteresis),

                obstacleWarningCm = prefs.getFloat(KEY_OBSTACLE_WARN, defaults.obstacleWarningCm),
                obstacleCriticalCm = prefs.getFloat(KEY_OBSTACLE_CRIT, defaults.obstacleCriticalCm),
                obstacleHysteresisCm = prefs.getFloat(KEY_OBSTACLE_HYST, defaults.obstacleHysteresisCm),

                linkWarningTimeoutSeconds = prefs.getLong(KEY_LINK_WARN_TIMEOUT, defaults.linkWarningTimeoutSeconds),
                offlineCriticalTimeoutSeconds = prefs.getLong(KEY_OFFLINE_CRIT_TIMEOUT, defaults.offlineCriticalTimeoutSeconds),

                confirmationCount = prefs.getInt(KEY_CONFIRM_COUNT, defaults.confirmationCount),
                recoveryConfirmationCount = prefs.getInt(KEY_RECOVERY_COUNT, defaults.recoveryConfirmationCount),

                isSoilCategoryEnabled = prefs.getBoolean(KEY_EN_SOIL, defaults.isSoilCategoryEnabled),
                isEnvCategoryEnabled = prefs.getBoolean(KEY_EN_ENV, defaults.isEnvCategoryEnabled),
                isSoilPhCategoryEnabled = prefs.getBoolean(KEY_EN_PH, defaults.isSoilPhCategoryEnabled),
                isRoverCategoryEnabled = prefs.getBoolean(KEY_EN_ROVER, defaults.isRoverCategoryEnabled)
            )
        } catch (_: Exception) {
            AlertThresholdConfig()
        }
    }

    override suspend fun updateSettings(config: AlertThresholdConfig): Result<Unit> {
        val validationErrors = validate(config)
        if (validationErrors.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(validationErrors.joinToString("; ")))
        }

        return try {
            prefs.edit()
                .putFloat(KEY_TEMP_MIN_WARN, config.tempMinWarning)
                .putFloat(KEY_TEMP_MIN_CRIT, config.tempMinCritical)
                .putFloat(KEY_TEMP_MAX_WARN, config.tempMaxWarning)
                .putFloat(KEY_TEMP_MAX_CRIT, config.tempMaxCritical)
                .putFloat(KEY_TEMP_HYST, config.tempHysteresis)

                .putFloat(KEY_HUM_MIN_WARN, config.humidityMinWarning)
                .putFloat(KEY_HUM_MIN_CRIT, config.humidityMinCritical)
                .putFloat(KEY_HUM_MAX_WARN, config.humidityMaxWarning)
                .putFloat(KEY_HUM_MAX_CRIT, config.humidityMaxCritical)
                .putFloat(KEY_HUM_HYST, config.humidityHysteresis)

                .putFloat(KEY_SOIL_MIN_WARN, config.soilMoistureMinWarning)
                .putFloat(KEY_SOIL_MIN_CRIT, config.soilMoistureMinCritical)
                .putFloat(KEY_SOIL_MAX_WARN, config.soilMoistureMaxWarning)
                .putFloat(KEY_SOIL_MAX_CRIT, config.soilMoistureMaxCritical)
                .putFloat(KEY_SOIL_HYST, config.soilMoistureHysteresis)

                .putFloat(KEY_PH_MIN_WARN, config.soilPhMinWarning)
                .putFloat(KEY_PH_MAX_WARN, config.soilPhMaxWarning)
                .putFloat(KEY_PH_HYST, config.soilPhHysteresis)

                .putFloat(KEY_OBSTACLE_WARN, config.obstacleWarningCm)
                .putFloat(KEY_OBSTACLE_CRIT, config.obstacleCriticalCm)
                .putFloat(KEY_OBSTACLE_HYST, config.obstacleHysteresisCm)

                .putLong(KEY_LINK_WARN_TIMEOUT, config.linkWarningTimeoutSeconds)
                .putLong(KEY_OFFLINE_CRIT_TIMEOUT, config.offlineCriticalTimeoutSeconds)

                .putInt(KEY_CONFIRM_COUNT, config.confirmationCount)
                .putInt(KEY_RECOVERY_COUNT, config.recoveryConfirmationCount)

                .putBoolean(KEY_EN_SOIL, config.isSoilCategoryEnabled)
                .putBoolean(KEY_EN_ENV, config.isEnvCategoryEnabled)
                .putBoolean(KEY_EN_PH, config.isSoilPhCategoryEnabled)
                .putBoolean(KEY_EN_ROVER, config.isRoverCategoryEnabled)
                .apply()

            _settingsFlow.value = config
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreDefaults(): Result<Unit> {
        val defaults = AlertThresholdConfig()
        return updateSettings(defaults)
    }
}

