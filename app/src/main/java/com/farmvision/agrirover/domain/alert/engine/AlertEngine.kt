package com.farmvision.agrirover.domain.alert.engine

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.DataQualityState
import java.util.Locale

/**
 * Configurable thresholds, debounce counts, and hysteresis margins.
 * Development baselines for testing, NOT universal agronomic recommendations.
 */
data class AlertThresholdConfig(
    val tempMinWarning: Float = 10.0f,
    val tempMinCritical: Float = 4.0f,
    val tempMaxWarning: Float = 35.0f,
    val tempMaxCritical: Float = 42.0f,
    val tempHysteresis: Float = 1.5f,

    val humidityMinWarning: Float = 30.0f,
    val humidityMinCritical: Float = 20.0f,
    val humidityMaxWarning: Float = 85.0f,
    val humidityMaxCritical: Float = 95.0f,
    val humidityHysteresis: Float = 4.0f,

    val soilMoistureMinWarning: Float = 30.0f,
    val soilMoistureMinCritical: Float = 20.0f,
    val soilMoistureMaxWarning: Float = 70.0f,
    val soilMoistureMaxCritical: Float = 85.0f,
    val soilMoistureHysteresis: Float = 3.0f,

    val soilPhMinWarning: Float = 5.5f,
    val soilPhMaxWarning: Float = 7.8f,
    val soilPhHysteresis: Float = 0.3f,

    val obstacleWarningCm: Float = 20.0f,
    val obstacleCriticalCm: Float = 10.0f,
    val obstacleHysteresisCm: Float = 5.0f,

    val linkWarningTimeoutSeconds: Long = 5L,
    val offlineCriticalTimeoutSeconds: Long = 15L,

    val confirmationCount: Int = 3,
    val recoveryConfirmationCount: Int = 3,
    val isSoilCategoryEnabled: Boolean = true,
    val isEnvCategoryEnabled: Boolean = true,
    val isSoilPhCategoryEnabled: Boolean = true,
    val isRoverCategoryEnabled: Boolean = true
)

/**
 * Result of single-cycle telemetry evaluation.
 */
data class AlertEvaluationOutput(
    val newAlerts: List<AlertItem>,
    val repeatKeys: List<Pair<String, Float?>>,
    val resolvedKeys: List<Pair<String, Float?>>
)

/**
 * Pure Kotlin evaluation engine for agricultural telemetry.
 * Framework-independent: strictly zero Android or Compose dependencies in domain logic.
 */
class AlertEngine(
    private var config: AlertThresholdConfig = AlertThresholdConfig(),
    private val confirmationCount: Int = config.confirmationCount,
    private val recoveryCount: Int = config.recoveryConfirmationCount
) {
    // Consecutive sample counter tracking threshold breaches per dedupKey
    private val breachConsecutiveCounts = mutableMapOf<String, Int>()

    // Consecutive sample counter tracking normal/recovery samples per dedupKey
    private val recoveryConsecutiveCounts = mutableMapOf<String, Int>()

    // Active condition cache to track what is currently in breach
    private val activeConditionKeys = mutableSetOf<String>()

    fun updateConfig(newConfig: AlertThresholdConfig) {
        this.config = newConfig
    }

    fun getConfig(): AlertThresholdConfig = config

    /**
     * Helper to construct unique deduplication key: category:ruleId:sourceSensor:origin
     */
    fun buildDedupKey(category: AlertCategory, ruleId: String, sensorId: String, origin: DataOrigin): String {
        return "${category.name}:${ruleId}:${sensorId}:${origin.name}"
    }

    /**
     * Evaluates a complete [RoverTelemetry] snapshot and [SensorReading] map.
     */
    fun evaluate(
        telemetry: RoverTelemetry,
        sensorReadings: Map<SensorType, SensorReading>,
        nowEpochMillis: Long,
        telemetryAgeSeconds: Long = 0L
    ): AlertEvaluationOutput {
        val newAlerts = mutableListOf<AlertItem>()
        val repeatKeys = mutableListOf<Pair<String, Float?>>()
        val resolvedKeys = mutableListOf<Pair<String, Float?>>()

        val origin = if (telemetry.isSimulation) DataOrigin.Simulated else DataOrigin.LiveHardware

        // 1. Rover Safety & Connection Rules (Rover Category)
        if (config.isRoverCategoryEnabled) {
            evaluateRoverCategory(telemetry, nowEpochMillis, telemetryAgeSeconds, origin, newAlerts, repeatKeys, resolvedKeys)
        }

        // 2. Ultrasonic Sonar Proximity (Rover Category - Read Only)
        if (config.isRoverCategoryEnabled) {
            val sonarReading = sensorReadings[SensorType.UltrasonicDistance]
            evaluateSonarProximity(sonarReading, nowEpochMillis, origin, newAlerts, repeatKeys, resolvedKeys)
        }

        // 3. Ambient Environment (DHT22: Temperature & Humidity)
        if (config.isEnvCategoryEnabled) {
            val tempReading = sensorReadings[SensorType.AirTemperature]
            evaluateTemperature(tempReading, nowEpochMillis, origin, newAlerts, repeatKeys, resolvedKeys)

            val humReading = sensorReadings[SensorType.Humidity]
            evaluateHumidity(humReading, nowEpochMillis, origin, newAlerts, repeatKeys, resolvedKeys)
        }

        // 4. Soil Moisture
        if (config.isSoilCategoryEnabled) {
            val soilReading = sensorReadings[SensorType.SoilMoisture]
            evaluateSoilMoisture(soilReading, nowEpochMillis, origin, newAlerts, repeatKeys, resolvedKeys)
        }

        // 5. Soil pH
        if (config.isSoilPhCategoryEnabled) {
            val phReading = sensorReadings[SensorType.SoilPh]
            evaluateSoilPh(phReading, nowEpochMillis, origin, newAlerts, repeatKeys, resolvedKeys)
        }

        return AlertEvaluationOutput(
            newAlerts = newAlerts,
            repeatKeys = repeatKeys,
            resolvedKeys = resolvedKeys
        )
    }

    // --- Private Evaluators ---

    private fun evaluateRoverCategory(
        telemetry: RoverTelemetry,
        nowEpochMillis: Long,
        ageSeconds: Long,
        origin: DataOrigin,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>,
        resolvedKeys: MutableList<Pair<String, Float?>>
    ) {
        // Emergency Stop Latched (Critical)
        val estopKey = buildDedupKey(AlertCategory.Rover, AlertSeverityPolicy.RULE_ESTOP_ACTIVE, "rover-safety-interlock", origin)
        if (telemetry.isEmergencyCutoff) {
            handleBreach(
                dedupKey = estopKey,
                category = AlertCategory.Rover,
                ruleId = AlertSeverityPolicy.RULE_ESTOP_ACTIVE,
                severity = AlertSeverity.Critical,
                title = "Emergency Stop Latched",
                message = "Rover safety cutoff is engaged. Motor drive outputs are disabled.",
                sensorId = "rover-safety-interlock",
                currentVal = 1.0f,
                unit = "",
                threshold = 1.0f,
                origin = origin,
                nowEpochMillis = nowEpochMillis,
                immediate = true,
                newAlerts = newAlerts,
                repeatKeys = repeatKeys
            )
        } else {
            handleRecovery(estopKey, 0.0f, "Emergency stop released", resolvedKeys, immediate = true)
        }

        // Rover Link Timeouts
        val offlineKey = buildDedupKey(AlertCategory.Rover, AlertSeverityPolicy.RULE_ROVER_OFFLINE, "esp32-telemetry-link", origin)
        val unstableKey = buildDedupKey(AlertCategory.Rover, AlertSeverityPolicy.RULE_ROVER_LINK_UNSTABLE, "esp32-telemetry-link", origin)

        if (ageSeconds >= config.offlineCriticalTimeoutSeconds) {
            handleBreach(
                dedupKey = offlineKey,
                category = AlertCategory.Rover,
                ruleId = AlertSeverityPolicy.RULE_ROVER_OFFLINE,
                severity = AlertSeverity.Critical,
                title = "Rover Connection Lost",
                message = "No telemetry received for ${ageSeconds}s (Threshold: ${config.offlineCriticalTimeoutSeconds}s). Check Wi-Fi SoftAP link.",
                sensorId = "esp32-telemetry-link",
                currentVal = ageSeconds.toFloat(),
                unit = "s",
                threshold = config.offlineCriticalTimeoutSeconds.toFloat(),
                origin = origin,
                nowEpochMillis = nowEpochMillis,
                immediate = true,
                newAlerts = newAlerts,
                repeatKeys = repeatKeys
            )
        } else {
            handleRecovery(offlineKey, ageSeconds.toFloat(), "Rover connection restored", resolvedKeys, immediate = true)

            if (ageSeconds >= config.linkWarningTimeoutSeconds) {
                handleBreach(
                    dedupKey = unstableKey,
                    category = AlertCategory.Rover,
                    ruleId = AlertSeverityPolicy.RULE_ROVER_LINK_UNSTABLE,
                    severity = AlertSeverity.Warning,
                    title = "Telemetry Link Interrupted",
                    message = "Telemetry packet latency is elevated (${ageSeconds}s). Link may be unstable.",
                    sensorId = "esp32-telemetry-link",
                    currentVal = ageSeconds.toFloat(),
                    unit = "s",
                    threshold = config.linkWarningTimeoutSeconds.toFloat(),
                    origin = origin,
                    nowEpochMillis = nowEpochMillis,
                    immediate = false,
                    newAlerts = newAlerts,
                    repeatKeys = repeatKeys
                )
            } else {
                handleRecovery(unstableKey, ageSeconds.toFloat(), "Telemetry latency normalized", resolvedKeys, immediate = false)
            }
        }
    }

    private fun evaluateSonarProximity(
        reading: SensorReading?,
        nowEpochMillis: Long,
        origin: DataOrigin,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>,
        resolvedKeys: MutableList<Pair<String, Float?>>
    ) {
        val quality = DataQualityValidator.validateReading(reading, nowEpochMillis)
        if (quality != DataQualityState.Valid || reading == null) return

        val dist = reading.value
        val critKey = buildDedupKey(AlertCategory.Rover, AlertSeverityPolicy.RULE_OBSTACLE_CRITICAL, reading.sensorId, origin)
        val warnKey = buildDedupKey(AlertCategory.Rover, AlertSeverityPolicy.RULE_OBSTACLE_WARNING, reading.sensorId, origin)

        // Critical Proximity (< 10 cm)
        if (dist <= config.obstacleCriticalCm) {
            handleBreach(
                dedupKey = critKey,
                category = AlertCategory.Rover,
                ruleId = AlertSeverityPolicy.RULE_OBSTACLE_CRITICAL,
                severity = AlertSeverity.Critical,
                title = "Critical Obstacle Proximity",
                message = String.format(Locale.US, "Front obstacle detected at %.1f cm (Critical limit: %.1f cm).", dist, config.obstacleCriticalCm),
                sensorId = reading.sensorId,
                currentVal = dist,
                unit = "cm",
                threshold = config.obstacleCriticalCm,
                origin = origin,
                nowEpochMillis = nowEpochMillis,
                immediate = false,
                newAlerts = newAlerts,
                repeatKeys = repeatKeys
            )
        } else if (dist >= config.obstacleCriticalCm + config.obstacleHysteresisCm) {
            handleRecovery(critKey, dist, "Obstacle cleared critical zone", resolvedKeys, immediate = false)
        }

        // Warning Proximity (< 20 cm and > 10 cm)
        if (dist <= config.obstacleWarningCm && dist > config.obstacleCriticalCm) {
            handleBreach(
                dedupKey = warnKey,
                category = AlertCategory.Rover,
                ruleId = AlertSeverityPolicy.RULE_OBSTACLE_WARNING,
                severity = AlertSeverity.Warning,
                title = "Obstacle Proximity Warning",
                message = String.format(Locale.US, "Obstacle detected at %.1f cm (Warning limit: %.1f cm).", dist, config.obstacleWarningCm),
                sensorId = reading.sensorId,
                currentVal = dist,
                unit = "cm",
                threshold = config.obstacleWarningCm,
                origin = origin,
                nowEpochMillis = nowEpochMillis,
                immediate = false,
                newAlerts = newAlerts,
                repeatKeys = repeatKeys
            )
        } else if (dist >= config.obstacleWarningCm + config.obstacleHysteresisCm) {
            handleRecovery(warnKey, dist, "Obstacle proximity clear", resolvedKeys, immediate = false)
        }
    }

    private fun evaluateTemperature(
        reading: SensorReading?,
        nowEpochMillis: Long,
        origin: DataOrigin,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>,
        resolvedKeys: MutableList<Pair<String, Float?>>
    ) {
        val quality = DataQualityValidator.validateReading(reading, nowEpochMillis)
        if (quality != DataQualityState.Valid || reading == null) return

        val temp = reading.value
        val highCritKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_HIGH_CRITICAL, reading.sensorId, origin)
        val highWarnKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_HIGH_WARNING, reading.sensorId, origin)
        val lowCritKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_LOW_CRITICAL, reading.sensorId, origin)
        val lowWarnKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_LOW_WARNING, reading.sensorId, origin)

        // High Temp Critical
        if (temp >= config.tempMaxCritical) {
            handleBreach(
                highCritKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_HIGH_CRITICAL, AlertSeverity.Critical,
                "Extreme Thermal Hazard", String.format(Locale.US, "Air temperature reached %.1f°C (Critical: %.1f°C).", temp, config.tempMaxCritical),
                reading.sensorId, temp, "°C", config.tempMaxCritical, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (temp <= config.tempMaxCritical - config.tempHysteresis) {
            handleRecovery(highCritKey, temp, "Temperature recovered below critical limit", resolvedKeys, false)
        }

        // High Temp Warning
        if (temp >= config.tempMaxWarning && temp < config.tempMaxCritical) {
            handleBreach(
                highWarnKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_HIGH_WARNING, AlertSeverity.Warning,
                "High Air Temperature Warning", String.format(Locale.US, "Air temperature is %.1f°C (Warning: %.1f°C).", temp, config.tempMaxWarning),
                reading.sensorId, temp, "°C", config.tempMaxWarning, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (temp <= config.tempMaxWarning - config.tempHysteresis) {
            handleRecovery(highWarnKey, temp, "Temperature returned to normal range", resolvedKeys, false)
        }

        // Low Temp Critical
        if (temp <= config.tempMinCritical) {
            handleBreach(
                lowCritKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_LOW_CRITICAL, AlertSeverity.Critical,
                "Frost Hazard Alert", String.format(Locale.US, "Air temperature dropped to %.1f°C (Frost Critical: %.1f°C).", temp, config.tempMinCritical),
                reading.sensorId, temp, "°C", config.tempMinCritical, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (temp >= config.tempMinCritical + config.tempHysteresis) {
            handleRecovery(lowCritKey, temp, "Temperature rose above frost critical limit", resolvedKeys, false)
        }

        // Low Temp Warning
        if (temp <= config.tempMinWarning && temp > config.tempMinCritical) {
            handleBreach(
                lowWarnKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_TEMP_LOW_WARNING, AlertSeverity.Warning,
                "Low Temperature Warning", String.format(Locale.US, "Air temperature is %.1f°C (Chilling warning: %.1f°C).", temp, config.tempMinWarning),
                reading.sensorId, temp, "°C", config.tempMinWarning, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (temp >= config.tempMinWarning + config.tempHysteresis) {
            handleRecovery(lowWarnKey, temp, "Temperature returned to normal range", resolvedKeys, false)
        }
    }

    private fun evaluateHumidity(
        reading: SensorReading?,
        nowEpochMillis: Long,
        origin: DataOrigin,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>,
        resolvedKeys: MutableList<Pair<String, Float?>>
    ) {
        val quality = DataQualityValidator.validateReading(reading, nowEpochMillis)
        if (quality != DataQualityState.Valid || reading == null) return

        val hum = reading.value
        val highCritKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_HUMIDITY_HIGH_CRITICAL, reading.sensorId, origin)
        val highWarnKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_HUMIDITY_HIGH_WARNING, reading.sensorId, origin)
        val lowWarnKey = buildDedupKey(AlertCategory.Environment, AlertSeverityPolicy.RULE_HUMIDITY_LOW_WARNING, reading.sensorId, origin)

        // High Humidity Critical (Condensation / Saturated Atmosphere)
        if (hum >= config.humidityMaxCritical) {
            handleBreach(
                highCritKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_HUMIDITY_HIGH_CRITICAL, AlertSeverity.Critical,
                "Atmospheric Saturation Hazard", String.format(Locale.US, "Humidity reached %.1f%% (Critical: %.1f%%).", hum, config.humidityMaxCritical),
                reading.sensorId, hum, "%", config.humidityMaxCritical, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (hum <= config.humidityMaxCritical - config.humidityHysteresis) {
            handleRecovery(highCritKey, hum, "Humidity recovered below saturation limit", resolvedKeys, false)
        }

        // High Humidity Warning (Fungal Spore Risk)
        if (hum >= config.humidityMaxWarning && hum < config.humidityMaxCritical) {
            handleBreach(
                highWarnKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_HUMIDITY_HIGH_WARNING, AlertSeverity.Warning,
                "Elevated Humidity Advisory", String.format(Locale.US, "Relative humidity is %.1f%% (Warning: %.1f%%).", hum, config.humidityMaxWarning),
                reading.sensorId, hum, "%", config.humidityMaxWarning, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (hum <= config.humidityMaxWarning - config.humidityHysteresis) {
            handleRecovery(highWarnKey, hum, "Humidity returned to normal range", resolvedKeys, false)
        }

        // Low Humidity Warning (Vapor Pressure Deficit / Desiccation)
        if (hum <= config.humidityMinWarning) {
            handleBreach(
                lowWarnKey, AlertCategory.Environment, AlertSeverityPolicy.RULE_HUMIDITY_LOW_WARNING, AlertSeverity.Warning,
                "Low Humidity Advisory", String.format(Locale.US, "Relative humidity dropped to %.1f%% (Warning: %.1f%%).", hum, config.humidityMinWarning),
                reading.sensorId, hum, "%", config.humidityMinWarning, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (hum >= config.humidityMinWarning + config.humidityHysteresis) {
            handleRecovery(lowWarnKey, hum, "Humidity returned to normal range", resolvedKeys, false)
        }
    }

    private fun evaluateSoilMoisture(
        reading: SensorReading?,
        nowEpochMillis: Long,
        origin: DataOrigin,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>,
        resolvedKeys: MutableList<Pair<String, Float?>>
    ) {
        if (reading == null) return

        // In Live mode: never treat raw ADC as calibrated %. Only check electrical saturation boundary.
        if (origin == DataOrigin.LiveHardware) {
            val adcKey = buildDedupKey(AlertCategory.Soil, AlertSeverityPolicy.RULE_SOIL_ADC_SATURATION, reading.sensorId, origin)
            if (DataQualityValidator.isAdcElectricallySaturated(reading.value)) {
                handleBreach(
                    adcKey, AlertCategory.Soil, AlertSeverityPolicy.RULE_SOIL_ADC_SATURATION, AlertSeverity.Warning,
                    "Soil Probe Electrical Saturation", String.format(Locale.US, "Soil probe ADC reading is %.0f counts (Out of normal bounds 50-4050). Probe may be unseated or faulted.", reading.value),
                    reading.sensorId, reading.value, "ADC counts", 4050.0f, origin, nowEpochMillis, false, newAlerts, repeatKeys
                )
            } else {
                handleRecovery(adcKey, reading.value, "Soil probe ADC reading normalized within electrical bounds", resolvedKeys, false)
            }
            return
        }

        // In Demo mode: evaluate calibrated simulated %
        val quality = DataQualityValidator.validateReading(reading, nowEpochMillis)
        if (quality != DataQualityState.Valid) return

        val moisture = reading.value
        val lowCritKey = buildDedupKey(AlertCategory.Soil, AlertSeverityPolicy.RULE_SOIL_MOISTURE_LOW_CRITICAL, reading.sensorId, origin)
        val lowWarnKey = buildDedupKey(AlertCategory.Soil, AlertSeverityPolicy.RULE_SOIL_MOISTURE_LOW_WARNING, reading.sensorId, origin)

        if (moisture <= config.soilMoistureMinCritical) {
            handleBreach(
                lowCritKey, AlertCategory.Soil, AlertSeverityPolicy.RULE_SOIL_MOISTURE_LOW_CRITICAL, AlertSeverity.Critical,
                "Severe Rootzone Drought", String.format(Locale.US, "Soil moisture dropped to %.1f%% (Critical: %.1f%%). Irrigation urgent.", moisture, config.soilMoistureMinCritical),
                reading.sensorId, moisture, "%", config.soilMoistureMinCritical, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (moisture >= config.soilMoistureMinCritical + config.soilMoistureHysteresis) {
            handleRecovery(lowCritKey, moisture, "Soil moisture recovered above critical threshold", resolvedKeys, false)
        }

        if (moisture <= config.soilMoistureMinWarning && moisture > config.soilMoistureMinCritical) {
            handleBreach(
                lowWarnKey, AlertCategory.Soil, AlertSeverityPolicy.RULE_SOIL_MOISTURE_LOW_WARNING, AlertSeverity.Warning,
                "Soil Moisture Stress", String.format(Locale.US, "Soil moisture is %.1f%% (Warning: %.1f%%). Irrigation recommended.", moisture, config.soilMoistureMinWarning),
                reading.sensorId, moisture, "%", config.soilMoistureMinWarning, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (moisture >= config.soilMoistureMinWarning + config.soilMoistureHysteresis) {
            handleRecovery(lowWarnKey, moisture, "Soil moisture returned to optimal band", resolvedKeys, false)
        }
    }

    private fun evaluateSoilPh(
        reading: SensorReading?,
        nowEpochMillis: Long,
        origin: DataOrigin,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>,
        resolvedKeys: MutableList<Pair<String, Float?>>
    ) {
        if (reading == null) return

        // In Live mode: suppress numeric agronomic alerts; display UNCALIBRATED SENSOR advisory
        if (origin == DataOrigin.LiveHardware) {
            val advKey = buildDedupKey(AlertCategory.SoilPh, AlertSeverityPolicy.RULE_SOIL_PH_UNCALIBRATED, reading.sensorId, origin)
            handleBreach(
                advKey, AlertCategory.SoilPh, AlertSeverityPolicy.RULE_SOIL_PH_UNCALIBRATED, AlertSeverity.Warning,
                "Uncalibrated Soil pH Probe", "Live soil pH probe requires 2-point buffer calibration. Displayed measurements are raw transducer readings.",
                reading.sensorId, reading.value, "pH", 7.0f, origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
            return
        }

        // In Demo mode: evaluate simulated pH thresholds
        val quality = DataQualityValidator.validateReading(reading, nowEpochMillis)
        if (quality != DataQualityState.Valid) return

        val ph = reading.value
        val outOfRangeKey = buildDedupKey(AlertCategory.SoilPh, AlertSeverityPolicy.RULE_SOIL_PH_OUT_OF_RANGE, reading.sensorId, origin)

        if (ph < config.soilPhMinWarning || ph > config.soilPhMaxWarning) {
            val isAcidic = ph < config.soilPhMinWarning
            val desc = if (isAcidic) {
                String.format(Locale.US, "Soil acidity elevated at %.1f pH (Min: %.1f pH).", ph, config.soilPhMinWarning)
            } else {
                String.format(Locale.US, "Soil alkalinity elevated at %.1f pH (Max: %.1f pH).", ph, config.soilPhMaxWarning)
            }
            handleBreach(
                outOfRangeKey, AlertCategory.SoilPh, AlertSeverityPolicy.RULE_SOIL_PH_OUT_OF_RANGE, AlertSeverity.Warning,
                "Soil pH Out of Optimal Range", desc, reading.sensorId, ph, "pH",
                if (isAcidic) config.soilPhMinWarning else config.soilPhMaxWarning,
                origin, nowEpochMillis, false, newAlerts, repeatKeys
            )
        } else if (ph >= config.soilPhMinWarning + config.soilPhHysteresis && ph <= config.soilPhMaxWarning - config.soilPhHysteresis) {
            handleRecovery(outOfRangeKey, ph, "Soil pH returned to optimal band", resolvedKeys, false)
        }
    }

    private fun handleBreach(
        dedupKey: String,
        category: AlertCategory,
        ruleId: String,
        severity: AlertSeverity,
        title: String,
        message: String,
        sensorId: String,
        currentVal: Float?,
        unit: String,
        threshold: Float?,
        origin: DataOrigin,
        nowEpochMillis: Long,
        immediate: Boolean,
        newAlerts: MutableList<AlertItem>,
        repeatKeys: MutableList<Pair<String, Float?>>
    ) {
        recoveryConsecutiveCounts[dedupKey] = 0

        val currentCount = (breachConsecutiveCounts[dedupKey] ?: 0) + 1
        breachConsecutiveCounts[dedupKey] = currentCount

        val thresholdMet = immediate || currentCount >= confirmationCount

        if (thresholdMet) {
            if (!activeConditionKeys.contains(dedupKey)) {
                activeConditionKeys.add(dedupKey)
                val alert = AlertItem(
                    id = "alert-${System.nanoTime()}",
                    dedupKey = dedupKey,
                    category = category,
                    ruleId = ruleId,
                    severity = severity,
                    title = title,
                    message = message,
                    sourceSensor = sensorId,
                    triggerValue = currentVal,
                    canonicalUnit = unit,
                    configuredThreshold = threshold,
                    origin = origin,
                    createdTimestampEpochMillis = nowEpochMillis
                )
                newAlerts.add(alert)
            } else {
                repeatKeys.add(dedupKey to currentVal)
            }
        }
    }

    private fun handleRecovery(
        dedupKey: String,
        resolvingVal: Float?,
        note: String,
        resolvedKeys: MutableList<Pair<String, Float?>>,
        immediate: Boolean
    ) {
        breachConsecutiveCounts[dedupKey] = 0

        if (activeConditionKeys.contains(dedupKey)) {
            val recoveryCountActual = (recoveryConsecutiveCounts[dedupKey] ?: 0) + 1
            recoveryConsecutiveCounts[dedupKey] = recoveryCountActual

            if (immediate || recoveryCountActual >= recoveryCount) {
                activeConditionKeys.remove(dedupKey)
                recoveryConsecutiveCounts[dedupKey] = 0
                resolvedKeys.add(dedupKey to resolvingVal)
            }
        }
    }
}
