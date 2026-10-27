package com.farmvision.agrirover.data.local

import com.farmvision.agrirover.data.model.CalibrationState
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample

/**
 * Payload envelope for serialized telemetry history.
 */
data class TelemetryHistoryPayload(
    val schemaVersion: Int = 1,
    val lastSavedEpochMillis: Long = 0L,
    val samples: List<TelemetrySample> = emptyList()
)

/**
 * Pure Kotlin, zero-dependency JSON serializer and parser for telemetry history.
 * Avoids any external dependency, Room, or Android framework stubs.
 */
object TelemetryHistoryJsonSerializer {

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

    fun serialize(payload: TelemetryHistoryPayload): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": ").append(payload.schemaVersion).append(",\n")
        sb.append("  \"lastSavedEpochMillis\": ").append(payload.lastSavedEpochMillis).append(",\n")
        sb.append("  \"samples\": [\n")

        val total = payload.samples.size
        for (i in 0 until total) {
            val s = payload.samples[i]
            sb.append("    {")
            sb.append("\"id\": ").append(escape(s.id)).append(", ")
            sb.append("\"metric\": ").append(escape(s.metric.name)).append(", ")
            sb.append("\"canonicalValue\": ").append(s.canonicalValue).append(", ")
            sb.append("\"rawValue\": ").append(s.rawValue ?: "null").append(", ")
            sb.append("\"timestampEpochMillis\": ").append(s.timestampEpochMillis).append(", ")
            sb.append("\"origin\": ").append(escape(s.origin.name)).append(", ")
            sb.append("\"calibrationState\": ").append(escape(s.calibrationState.name))
            sb.append("}")
            if (i < total - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun deserialize(json: String): TelemetryHistoryPayload {
        if (json.isBlank()) return TelemetryHistoryPayload()

        var schemaVersion = CURRENT_SCHEMA_VERSION
        var lastSavedEpochMillis = 0L
        val samples = mutableListOf<TelemetrySample>()

        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return TelemetryHistoryPayload()
        }

        // Parse top-level keys
        val schemaMatch = Regex("\"schemaVersion\"\\s*:\\s*(\\d+)").find(trimmed)
        if (schemaMatch != null) {
            schemaVersion = schemaMatch.groupValues[1].toIntOrNull() ?: CURRENT_SCHEMA_VERSION
        }

        val lastSavedMatch = Regex("\"lastSavedEpochMillis\"\\s*:\\s*(\\d+)").find(trimmed)
        if (lastSavedMatch != null) {
            lastSavedEpochMillis = lastSavedMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        // Find samples array
        val samplesArrayStart = trimmed.indexOf("\"samples\"")
        if (samplesArrayStart != -1) {
            val bracketStart = trimmed.indexOf('[', samplesArrayStart)
            val bracketEnd = trimmed.lastIndexOf(']')
            if (bracketStart != -1 && bracketEnd > bracketStart) {
                val arrayContent = trimmed.substring(bracketStart + 1, bracketEnd)
                parseSamplesArray(arrayContent, samples)
            }
        }

        return TelemetryHistoryPayload(
            schemaVersion = schemaVersion,
            lastSavedEpochMillis = lastSavedEpochMillis,
            samples = samples
        )
    }

    private fun parseSamplesArray(content: String, outSamples: MutableList<TelemetrySample>) {
        var depth = 0
        var objStart = -1

        for (i in content.indices) {
            val c = content[i]
            if (c == '{') {
                if (depth == 0) objStart = i
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0 && objStart != -1) {
                    val objStr = content.substring(objStart, i + 1)
                    val sample = parseSampleObject(objStr)
                    if (sample != null) {
                        outSamples.add(sample)
                    }
                    objStart = -1
                }
            }
        }
    }

    private fun parseSampleObject(objStr: String): TelemetrySample? {
        val id = extractString(objStr, "id") ?: return null
        val metricStr = extractString(objStr, "metric") ?: return null
        val canonicalValue = extractFloat(objStr, "canonicalValue") ?: return null
        val rawValue = extractFloat(objStr, "rawValue")
        val timestamp = extractLong(objStr, "timestampEpochMillis") ?: return null
        val originStr = extractString(objStr, "origin") ?: return null
        val calibStr = extractString(objStr, "calibrationState") ?: "Uncalibrated"

        val metric = try {
            HistoricalMetric.valueOf(metricStr)
        } catch (_: Exception) { return null }

        val origin = try {
            DataOrigin.valueOf(originStr)
        } catch (_: Exception) { DataOrigin.Simulated }

        val calibState = try {
            CalibrationState.valueOf(calibStr)
        } catch (_: Exception) { CalibrationState.Uncalibrated }

        return TelemetrySample(
            id = id,
            metric = metric,
            canonicalValue = canonicalValue,
            rawValue = rawValue,
            timestampEpochMillis = timestamp,
            origin = origin,
            calibrationState = calibState
        )
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractFloat(json: String, key: String): Float? {
        val pattern = Regex("\"$key\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)")
        val match = pattern.find(json)?.groupValues?.get(1) ?: return null
        return match.toFloatOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }
}
