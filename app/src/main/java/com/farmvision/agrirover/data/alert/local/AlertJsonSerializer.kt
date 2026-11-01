package com.farmvision.agrirover.data.alert.local

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.AlertStatus

/**
 * Envelope for serialized alert records.
 */
data class AlertStorePayload(
    val schemaVersion: Int = 1,
    val lastSavedEpochMillis: Long = 0L,
    val activeAlerts: List<AlertItem> = emptyList(),
    val resolvedAlerts: List<AlertItem> = emptyList()
)

/**
 * Pure Kotlin JSON serializer and parser for alert items and persistence payload.
 * Fully framework-independent; runs identically on JVM test runners and Android runtime.
 */
object AlertJsonSerializer {

    const val CURRENT_SCHEMA_VERSION = 1

    private fun escape(string: String?): String {
        if (string == null) return "null"
        val sb = StringBuilder("\"")
        for (c in string) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0..0x1f) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun serializeAlert(a: AlertItem): String {
        val sb = StringBuilder("{")
        sb.append("\"id\": ").append(escape(a.id)).append(", ")
        sb.append("\"dedupKey\": ").append(escape(a.dedupKey)).append(", ")
        sb.append("\"category\": ").append(escape(a.category.name)).append(", ")
        sb.append("\"ruleId\": ").append(escape(a.ruleId)).append(", ")
        sb.append("\"severity\": ").append(escape(a.severity.name)).append(", ")
        sb.append("\"title\": ").append(escape(a.title)).append(", ")
        sb.append("\"message\": ").append(escape(a.message)).append(", ")
        sb.append("\"sourceDevice\": ").append(escape(a.sourceDevice)).append(", ")
        sb.append("\"sourceSensor\": ").append(escape(a.sourceSensor)).append(", ")
        sb.append("\"triggerValue\": ").append(a.triggerValue ?: "null").append(", ")
        sb.append("\"canonicalUnit\": ").append(escape(a.canonicalUnit)).append(", ")
        sb.append("\"configuredThreshold\": ").append(a.configuredThreshold ?: "null").append(", ")
        sb.append("\"origin\": ").append(escape(a.origin.name)).append(", ")
        sb.append("\"status\": ").append(escape(a.status.name)).append(", ")
        sb.append("\"occurrenceCount\": ").append(a.occurrenceCount).append(", ")
        sb.append("\"createdTimestampEpochMillis\": ").append(a.createdTimestampEpochMillis).append(", ")
        sb.append("\"lastUpdatedTimestampEpochMillis\": ").append(a.lastUpdatedTimestampEpochMillis).append(", ")
        sb.append("\"acknowledgedTimestampEpochMillis\": ").append(a.acknowledgedTimestampEpochMillis ?: "null").append(", ")
        sb.append("\"resolvedTimestampEpochMillis\": ").append(a.resolvedTimestampEpochMillis ?: "null").append(", ")
        sb.append("\"resolvingValue\": ").append(a.resolvingValue ?: "null").append(", ")
        sb.append("\"isRead\": ").append(a.isRead)
        sb.append("}")
        return sb.toString()
    }

    fun serialize(payload: AlertStorePayload): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": ").append(payload.schemaVersion).append(",\n")
        sb.append("  \"lastSavedEpochMillis\": ").append(payload.lastSavedEpochMillis).append(",\n")

        sb.append("  \"activeAlerts\": [\n")
        val activeSize = payload.activeAlerts.size
        for (i in 0 until activeSize) {
            sb.append("    ").append(serializeAlert(payload.activeAlerts[i]))
            if (i < activeSize - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        sb.append("  \"resolvedAlerts\": [\n")
        val resolvedSize = payload.resolvedAlerts.size
        for (i in 0 until resolvedSize) {
            sb.append("    ").append(serializeAlert(payload.resolvedAlerts[i]))
            if (i < resolvedSize - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun deserialize(json: String): AlertStorePayload {
        if (json.isBlank()) return AlertStorePayload()
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw IllegalArgumentException("Corrupted or truncated JSON payload in alert storage")
        }

        var schemaVersion = CURRENT_SCHEMA_VERSION
        var lastSaved = 0L
        val active = mutableListOf<AlertItem>()
        val resolved = mutableListOf<AlertItem>()

        val schemaMatch = Regex("\"schemaVersion\"\\s*:\\s*(\\d+)").find(trimmed)
        if (schemaMatch != null) {
            schemaVersion = schemaMatch.groupValues[1].toIntOrNull() ?: CURRENT_SCHEMA_VERSION
        }

        val lastSavedMatch = Regex("\"lastSavedEpochMillis\"\\s*:\\s*(\\d+)").find(trimmed)
        if (lastSavedMatch != null) {
            lastSaved = lastSavedMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        parseAlertArray(trimmed, "\"activeAlerts\"", active)
        parseAlertArray(trimmed, "\"resolvedAlerts\"", resolved)

        return AlertStorePayload(
            schemaVersion = schemaVersion,
            lastSavedEpochMillis = lastSaved,
            activeAlerts = active,
            resolvedAlerts = resolved
        )
    }

    private fun parseAlertArray(fullJson: String, arrayKey: String, outList: MutableList<AlertItem>) {
        val keyPos = fullJson.indexOf(arrayKey)
        if (keyPos == -1) return

        val bracketStart = fullJson.indexOf('[', keyPos)
        if (bracketStart == -1) return

        var depth = 0
        var bracketEnd = -1
        for (i in bracketStart until fullJson.length) {
            if (fullJson[i] == '[') depth++
            else if (fullJson[i] == ']') {
                depth--
                if (depth == 0) {
                    bracketEnd = i
                    break
                }
            }
        }
        if (bracketEnd <= bracketStart) return

        val arrayContent = fullJson.substring(bracketStart + 1, bracketEnd)
        var objDepth = 0
        var objStart = -1

        for (i in arrayContent.indices) {
            val c = arrayContent[i]
            if (c == '{') {
                if (objDepth == 0) objStart = i
                objDepth++
            } else if (c == '}') {
                objDepth--
                if (objDepth == 0 && objStart != -1) {
                    val objStr = arrayContent.substring(objStart, i + 1)
                    val parsed = parseSingleAlert(objStr)
                    if (parsed != null) {
                        outList.add(parsed)
                    }
                    objStart = -1
                }
            }
        }
    }

    private fun parseSingleAlert(objJson: String): AlertItem? {
        fun getString(key: String): String? {
            val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(objJson)
            return match?.groupValues?.get(1)
        }

        fun getLong(key: String): Long? {
            val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(objJson)
            return match?.groupValues?.get(1)?.toLongOrNull()
        }

        fun getFloat(key: String): Float? {
            val match = Regex("\"$key\"\\s*:\\s*(-?[0-9]*\\.?[0-9]+)").find(objJson)
            return match?.groupValues?.get(1)?.toFloatOrNull()
        }

        fun getBoolean(key: String): Boolean {
            val match = Regex("\"$key\"\\s*:\\s*(true|false)").find(objJson)
            return match?.groupValues?.get(1)?.toBoolean() ?: false
        }

        val id = getString("id") ?: return null
        val dedupKey = getString("dedupKey") ?: return null
        val catName = getString("category") ?: AlertCategory.System.name
        val ruleId = getString("ruleId") ?: "unknown"
        val sevName = getString("severity") ?: AlertSeverity.Warning.name
        val title = getString("title") ?: "Alert"
        val message = getString("message") ?: ""
        val sourceDev = getString("sourceDevice") ?: "AgriRover"
        val sourceSensor = getString("sourceSensor") ?: "sensor"
        val triggerVal = getFloat("triggerValue")
        val unit = getString("canonicalUnit") ?: ""
        val threshold = getFloat("configuredThreshold")
        val originName = getString("origin") ?: DataOrigin.Simulated.name
        val statusName = getString("status") ?: AlertStatus.Active.name
        val occCount = getLong("occurrenceCount")?.toInt() ?: 1
        val created = getLong("createdTimestampEpochMillis") ?: 0L
        val updated = getLong("lastUpdatedTimestampEpochMillis") ?: created
        val ack = getLong("acknowledgedTimestampEpochMillis")
        val resolved = getLong("resolvedTimestampEpochMillis")
        val resolvingVal = getFloat("resolvingValue")
        val isRead = getBoolean("isRead")

        val category = try { AlertCategory.valueOf(catName) } catch (_: Exception) { AlertCategory.System }
        val severity = try { AlertSeverity.valueOf(sevName) } catch (_: Exception) { AlertSeverity.Warning }
        val origin = try { DataOrigin.valueOf(originName) } catch (_: Exception) { DataOrigin.Simulated }
        val status = try { AlertStatus.valueOf(statusName) } catch (_: Exception) { AlertStatus.Active }

        return AlertItem(
            id = id,
            dedupKey = dedupKey,
            category = category,
            ruleId = ruleId,
            severity = severity,
            title = title,
            message = message,
            sourceDevice = sourceDev,
            sourceSensor = sourceSensor,
            triggerValue = triggerVal,
            canonicalUnit = unit,
            configuredThreshold = threshold,
            origin = origin,
            status = status,
            occurrenceCount = occCount,
            createdTimestampEpochMillis = created,
            lastUpdatedTimestampEpochMillis = updated,
            acknowledgedTimestampEpochMillis = ack,
            resolvedTimestampEpochMillis = resolved,
            resolvingValue = resolvingVal,
            isRead = isRead
        )
    }
}
