package com.farmvision.agrirover.domain.alert.engine

import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertSeverity

/**
 * Centralized, deterministic mapping governing rule classification and severity assignment.
 * UI components must never decide severity independently.
 */
object AlertSeverityPolicy {

    // Rule ID constants
    const val RULE_TEMP_HIGH_CRITICAL = "env-temp-high-critical"
    const val RULE_TEMP_HIGH_WARNING = "env-temp-high-warning"
    const val RULE_TEMP_LOW_CRITICAL = "env-temp-low-critical"
    const val RULE_TEMP_LOW_WARNING = "env-temp-low-warning"

    const val RULE_HUMIDITY_HIGH_CRITICAL = "env-humidity-high-critical"
    const val RULE_HUMIDITY_HIGH_WARNING = "env-humidity-high-warning"
    const val RULE_HUMIDITY_LOW_CRITICAL = "env-humidity-low-critical"
    const val RULE_HUMIDITY_LOW_WARNING = "env-humidity-low-warning"

    const val RULE_SOIL_MOISTURE_LOW_CRITICAL = "soil-moisture-low-critical"
    const val RULE_SOIL_MOISTURE_LOW_WARNING = "soil-moisture-low-warning"
    const val RULE_SOIL_MOISTURE_HIGH_CRITICAL = "soil-moisture-high-critical"
    const val RULE_SOIL_MOISTURE_HIGH_WARNING = "soil-moisture-high-warning"

    const val RULE_SOIL_ADC_SATURATION = "soil-adc-saturation"
    const val RULE_SOIL_PH_UNCALIBRATED = "soil-ph-uncalibrated-advisory"
    const val RULE_SOIL_PH_OUT_OF_RANGE = "soil-ph-out-of-range"

    const val RULE_ESTOP_ACTIVE = "rover-estop-active"
    const val RULE_ROVER_OFFLINE = "rover-link-offline"
    const val RULE_ROVER_LINK_UNSTABLE = "rover-link-unstable"

    const val RULE_OBSTACLE_CRITICAL = "sonar-obstacle-critical"
    const val RULE_OBSTACLE_WARNING = "sonar-obstacle-warning"

    const val RULE_SENSOR_STALE = "system-sensor-stale"
    const val RULE_SENSOR_INVALID = "system-sensor-invalid"

    const val RULE_RECOVERY_INFO = "system-condition-resolved"

    /**
     * Resolves canonical severity for an evaluation rule ID.
     */
    fun getSeverityForRule(ruleId: String): AlertSeverity = when (ruleId) {
        RULE_TEMP_HIGH_CRITICAL,
        RULE_TEMP_LOW_CRITICAL,
        RULE_HUMIDITY_HIGH_CRITICAL,
        RULE_HUMIDITY_LOW_CRITICAL,
        RULE_SOIL_MOISTURE_LOW_CRITICAL,
        RULE_SOIL_MOISTURE_HIGH_CRITICAL,
        RULE_ESTOP_ACTIVE,
        RULE_ROVER_OFFLINE,
        RULE_OBSTACLE_CRITICAL -> AlertSeverity.Critical

        RULE_TEMP_HIGH_WARNING,
        RULE_TEMP_LOW_WARNING,
        RULE_HUMIDITY_HIGH_WARNING,
        RULE_HUMIDITY_LOW_WARNING,
        RULE_SOIL_MOISTURE_LOW_WARNING,
        RULE_SOIL_MOISTURE_HIGH_WARNING,
        RULE_SOIL_ADC_SATURATION,
        RULE_SOIL_PH_UNCALIBRATED,
        RULE_SOIL_PH_OUT_OF_RANGE,
        RULE_ROVER_LINK_UNSTABLE,
        RULE_OBSTACLE_WARNING,
        RULE_SENSOR_STALE,
        RULE_SENSOR_INVALID -> AlertSeverity.Warning

        RULE_RECOVERY_INFO -> AlertSeverity.Information

        else -> AlertSeverity.Warning
    }

    /**
     * Maps an alert rule to its standard functional domain category.
     */
    fun getCategoryForRule(ruleId: String): AlertCategory = when {
        ruleId.startsWith("soil-moisture") || ruleId.startsWith("soil-adc") -> AlertCategory.Soil
        ruleId.startsWith("env-") -> AlertCategory.Environment
        ruleId.startsWith("soil-ph") -> AlertCategory.SoilPh
        ruleId.startsWith("rover-") || ruleId.startsWith("sonar-") -> AlertCategory.Rover
        else -> AlertCategory.System
    }
}

