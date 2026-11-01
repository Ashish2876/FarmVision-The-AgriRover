package com.farmvision.agrirover.domain.alert.model

import com.farmvision.agrirover.data.model.DataOrigin

/**
 * Functional category classifying the agricultural domain source of an alert.
 */
enum class AlertCategory(val displayName: String) {
    Soil("Soil & Rootzone"),
    Environment("Ambient Environment"),
    SoilPh("Soil pH Chemistry"),
    Rover("Rover & Hardware Link"),
    System("System & Sensor Pipeline")
}

/**
 * Three-tier severity hierarchy conforming to industrial agricultural telemetry standards.
 * Ordered from lowest to highest operational urgency.
 */
enum class AlertSeverity(val level: Int, val label: String) {
    Information(1, "INFO"),
    Warning(2, "WARNING"),
    Critical(3, "CRITICAL");

    fun isHigherThan(other: AlertSeverity): Boolean = this.level > other.level
}

/**
 * Operational lifecycle states for an evaluated alert instance.
 */
enum class AlertStatus {
    Active,
    Acknowledged,
    Resolved
}

/**
 * Telemetry sample data-quality classification evaluated prior to numerical threshold checks.
 */
enum class DataQualityState {
    /** Reading is present, freshly polled, and within physically possible bounds. */
    Valid,

    /** Expected reading is absent or null in the ingested telemetry packet. */
    Missing,

    /** Reading is out of physical sensor bounds (e.g., NaN, Inf, impossible temperature). */
    Invalid,

    /** Reading exceeds the maximum allowed age since last telemetry arrival. */
    Stale,

    /** Reading comes from a sensor without a verified multi-point calibration profile. */
    Uncalibrated
}

/**
 * Record of severity escalation or de-escalation over an alert's active lifespan.
 */
data class AlertSeverityTransition(
    val severity: AlertSeverity,
    val timestampEpochMillis: Long,
    val triggeringValue: Float?,
    val note: String
)

/**
 * Canonical domain entity representing an alert in FarmVision AgriRover.
 * Fully immutable; lifecycle state transitions yield new copies.
 */
data class AlertItem(
    val id: String,
    val dedupKey: String,
    val category: AlertCategory,
    val ruleId: String,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val sourceDevice: String = "AgriRover-Alpha",
    val sourceSensor: String,
    val triggerValue: Float? = null,
    val canonicalUnit: String = "",
    val configuredThreshold: Float? = null,
    val origin: DataOrigin = DataOrigin.Simulated,
    val status: AlertStatus = AlertStatus.Active,
    val occurrenceCount: Int = 1,
    val createdTimestampEpochMillis: Long,
    val lastUpdatedTimestampEpochMillis: Long = createdTimestampEpochMillis,
    val acknowledgedTimestampEpochMillis: Long? = null,
    val resolvedTimestampEpochMillis: Long? = null,
    val resolvingValue: Float? = null,
    val isRead: Boolean = false,
    val severityHistory: List<AlertSeverityTransition> = emptyList()
) {
    val isActive: Boolean
        get() = status == AlertStatus.Active || status == AlertStatus.Acknowledged

    val isResolved: Boolean
        get() = status == AlertStatus.Resolved

    val isSimulated: Boolean
        get() = origin == DataOrigin.Simulated

    fun acknowledge(timestampEpochMillis: Long): AlertItem {
        if (status != AlertStatus.Active) return this
        return copy(
            status = AlertStatus.Acknowledged,
            acknowledgedTimestampEpochMillis = timestampEpochMillis,
            isRead = true
        )
    }

    fun resolve(timestampEpochMillis: Long, resolvingVal: Float?, recoveryNote: String): AlertItem {
        if (status == AlertStatus.Resolved) return this
        val transition = AlertSeverityTransition(
            severity = AlertSeverity.Information,
            timestampEpochMillis = timestampEpochMillis,
            triggeringValue = resolvingVal,
            note = recoveryNote
        )
        return copy(
            status = AlertStatus.Resolved,
            resolvedTimestampEpochMillis = timestampEpochMillis,
            resolvingValue = resolvingVal,
            severityHistory = severityHistory + transition
        )
    }

    fun recordRepeat(
        timestampEpochMillis: Long,
        currentValue: Float?,
        newSeverity: AlertSeverity? = null
    ): AlertItem {
        val escalated = newSeverity != null && newSeverity.isHigherThan(severity)
        val activeSeverity = if (escalated) newSeverity!! else severity
        val newHistory = if (escalated) {
            severityHistory + AlertSeverityTransition(
                severity = activeSeverity,
                timestampEpochMillis = timestampEpochMillis,
                triggeringValue = currentValue,
                note = "Severity escalated from ${severity.name} to ${activeSeverity.name}"
            )
        } else {
            severityHistory
        }

        return copy(
            severity = activeSeverity,
            occurrenceCount = occurrenceCount + 1,
            lastUpdatedTimestampEpochMillis = timestampEpochMillis,
            triggerValue = currentValue ?: triggerValue,
            severityHistory = newHistory
        )
    }
}

