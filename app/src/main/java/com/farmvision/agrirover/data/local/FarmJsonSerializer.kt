package com.farmvision.agrirover.data.local

import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import java.time.LocalDate

/**
 * Data payload stored in the versioned JSON persistence file.
 */
data class FarmDataPayload(
    val schemaVersion: Int = 1,
    val lastSavedEpochMillis: Long = 0L,
    val isSampleDataSeeded: Boolean = false,
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val plantings: List<Planting> = emptyList()
)

/**
 * Pure Kotlin, zero-dependency JSON serializer and parser for agricultural entities.
 * Avoids Android framework stubs so it executes identically in JVM unit tests and Android runtime.
 */
object FarmJsonSerializer {

    const val CURRENT_SCHEMA_VERSION = 1

    fun escape(string: String?): String {
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

    fun serialize(payload: FarmDataPayload): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": ").append(payload.schemaVersion).append(",\n")
        sb.append("  \"lastSavedEpochMillis\": ").append(payload.lastSavedEpochMillis).append(",\n")
        sb.append("  \"isSampleDataSeeded\": ").append(payload.isSampleDataSeeded).append(",\n")

        // Farms
        sb.append("  \"farms\": [\n")
        payload.farms.forEachIndexed { i, f ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escape(f.id)).append(",\n")
            sb.append("      \"name\": ").append(escape(f.name)).append(",\n")
            sb.append("      \"description\": ").append(escape(f.description)).append(",\n")
            sb.append("      \"locationDescription\": ").append(escape(f.locationDescription)).append(",\n")
            sb.append("      \"areaSquareMeters\": ").append(f.areaSquareMeters ?: "null").append(",\n")
            sb.append("      \"notes\": ").append(escape(f.notes)).append(",\n")
            sb.append("      \"createdAtEpochMillis\": ").append(f.createdAtEpochMillis).append(",\n")
            sb.append("      \"updatedAtEpochMillis\": ").append(f.updatedAtEpochMillis).append(",\n")
            sb.append("      \"origin\": ").append(escape(f.origin.name)).append("\n")
            sb.append("    }").append(if (i < payload.farms.size - 1) ",\n" else "\n")
        }
        sb.append("  ],\n")

        // Plots
        sb.append("  \"plots\": [\n")
        payload.plots.forEachIndexed { i, p ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escape(p.id)).append(",\n")
            sb.append("      \"farmId\": ").append(escape(p.farmId)).append(",\n")
            sb.append("      \"name\": ").append(escape(p.name)).append(",\n")
            sb.append("      \"description\": ").append(escape(p.description)).append(",\n")
            sb.append("      \"areaSquareMeters\": ").append(p.areaSquareMeters ?: "null").append(",\n")
            sb.append("      \"notes\": ").append(escape(p.notes)).append(",\n")
            sb.append("      \"createdAtEpochMillis\": ").append(p.createdAtEpochMillis).append(",\n")
            sb.append("      \"updatedAtEpochMillis\": ").append(p.updatedAtEpochMillis).append(",\n")
            sb.append("      \"origin\": ").append(escape(p.origin.name)).append("\n")
            sb.append("    }").append(if (i < payload.plots.size - 1) ",\n" else "\n")
        }
        sb.append("  ],\n")

        // Plantings
        sb.append("  \"plantings\": [\n")
        payload.plantings.forEachIndexed { i, pl ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escape(pl.id)).append(",\n")
            sb.append("      \"plotId\": ").append(escape(pl.plotId)).append(",\n")
            sb.append("      \"cropName\": ").append(escape(pl.cropName)).append(",\n")
            sb.append("      \"variety\": ").append(escape(pl.variety)).append(",\n")
            sb.append("      \"plantingDate\": ").append(escape(pl.plantingDate?.toString())).append(",\n")
            sb.append("      \"growthStage\": ").append(escape(pl.growthStage.name)).append(",\n")
            sb.append("      \"userNotes\": ").append(escape(pl.userNotes)).append(",\n")
            sb.append("      \"userHealthStatus\": ").append(escape(pl.userHealthStatus.name)).append(",\n")
            sb.append("      \"daysToHarvest\": ").append(pl.daysToHarvest ?: "null").append(",\n")
            sb.append("      \"createdAtEpochMillis\": ").append(pl.createdAtEpochMillis).append(",\n")
            sb.append("      \"updatedAtEpochMillis\": ").append(pl.updatedAtEpochMillis).append(",\n")
            sb.append("      \"origin\": ").append(escape(pl.origin.name)).append("\n")
            sb.append("    }").append(if (i < payload.plantings.size - 1) ",\n" else "\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }

    fun deserialize(json: String): FarmDataPayload {
        val parser = SimpleJsonParser(json)
        val root = parser.parseObject()

        val version = (root["schemaVersion"] as? String)?.toIntOrNull() ?: CURRENT_SCHEMA_VERSION
        val lastSaved = (root["lastSavedEpochMillis"] as? String)?.toLongOrNull() ?: 0L
        val seeded = (root["isSampleDataSeeded"] as? String)?.toBooleanStrictOrNull() ?: false

        val farmsList = mutableListOf<Farm>()
        val farmsRaw = root["farms"]
        if (farmsRaw is List<*>) {
            for (item in farmsRaw) {
                if (item is Map<*, *>) {
                    val id = item["id"] as? String ?: continue
                    val name = item["name"] as? String ?: continue
                    val desc = item["description"] as? String
                    val loc = item["locationDescription"] as? String
                    val area = (item["areaSquareMeters"] as? String)?.toDoubleOrNull()
                    val notes = item["notes"] as? String
                    val created = (item["createdAtEpochMillis"] as? String)?.toLongOrNull() ?: 0L
                    val updated = (item["updatedAtEpochMillis"] as? String)?.toLongOrNull() ?: created
                    val originStr = item["origin"] as? String ?: RecordOrigin.USER.name
                    val origin = try { RecordOrigin.valueOf(originStr) } catch (_: Exception) { RecordOrigin.USER }

                    farmsList.add(
                        Farm(
                            id = id,
                            name = name,
                            description = desc,
                            locationDescription = loc,
                            areaSquareMeters = area,
                            notes = notes,
                            createdAtEpochMillis = created,
                            updatedAtEpochMillis = updated,
                            origin = origin
                        )
                    )
                }
            }
        }

        val plotsList = mutableListOf<Plot>()
        val plotsRaw = root["plots"]
        if (plotsRaw is List<*>) {
            for (item in plotsRaw) {
                if (item is Map<*, *>) {
                    val id = item["id"] as? String ?: continue
                    val farmId = item["farmId"] as? String ?: continue
                    val name = item["name"] as? String ?: continue
                    val desc = item["description"] as? String
                    val area = (item["areaSquareMeters"] as? String)?.toDoubleOrNull()
                    val notes = item["notes"] as? String
                    val created = (item["createdAtEpochMillis"] as? String)?.toLongOrNull() ?: 0L
                    val updated = (item["updatedAtEpochMillis"] as? String)?.toLongOrNull() ?: created
                    val originStr = item["origin"] as? String ?: RecordOrigin.USER.name
                    val origin = try { RecordOrigin.valueOf(originStr) } catch (_: Exception) { RecordOrigin.USER }

                    plotsList.add(
                        Plot(
                            id = id,
                            farmId = farmId,
                            name = name,
                            description = desc,
                            areaSquareMeters = area,
                            notes = notes,
                            createdAtEpochMillis = created,
                            updatedAtEpochMillis = updated,
                            origin = origin
                        )
                    )
                }
            }
        }

        val plantingsList = mutableListOf<Planting>()
        val plantingsRaw = root["plantings"]
        if (plantingsRaw is List<*>) {
            for (item in plantingsRaw) {
                if (item is Map<*, *>) {
                    val id = item["id"] as? String ?: continue
                    val plotId = item["plotId"] as? String ?: continue
                    val cropName = item["cropName"] as? String ?: continue
                    val variety = item["variety"] as? String
                    val dateStr = item["plantingDate"] as? String
                    val date = if (!dateStr.isNullOrBlank() && dateStr != "null") {
                        try { LocalDate.parse(dateStr) } catch (_: Exception) { null }
                    } else null

                    val stageStr = item["growthStage"] as? String ?: GrowthStage.Unspecified.name
                    val stage = try { GrowthStage.valueOf(stageStr) } catch (_: Exception) { GrowthStage.fromString(stageStr) }

                    val userNotes = item["userNotes"] as? String
                    val healthStr = item["userHealthStatus"] as? String ?: PlantHealthStatus.InsufficientData.name
                    val health = try { PlantHealthStatus.valueOf(healthStr) } catch (_: Exception) { PlantHealthStatus.fromString(healthStr) }

                    val days = (item["daysToHarvest"] as? String)?.toIntOrNull()
                    val created = (item["createdAtEpochMillis"] as? String)?.toLongOrNull() ?: 0L
                    val updated = (item["updatedAtEpochMillis"] as? String)?.toLongOrNull() ?: created
                    val originStr = item["origin"] as? String ?: RecordOrigin.USER.name
                    val origin = try { RecordOrigin.valueOf(originStr) } catch (_: Exception) { RecordOrigin.USER }

                    plantingsList.add(
                        Planting(
                            id = id,
                            plotId = plotId,
                            cropName = cropName,
                            variety = variety,
                            plantingDate = date,
                            growthStage = stage,
                            userNotes = userNotes,
                            userHealthStatus = health,
                            daysToHarvest = days,
                            createdAtEpochMillis = created,
                            updatedAtEpochMillis = updated,
                            origin = origin
                        )
                    )
                }
            }
        }

        return FarmDataPayload(
            schemaVersion = version,
            lastSavedEpochMillis = lastSaved,
            isSampleDataSeeded = seeded,
            farms = farmsList,
            plots = plotsList,
            plantings = plantingsList
        )
    }

    /**
     * Lightweight recursive-descent JSON parser supporting objects, arrays, strings, numbers, booleans, and nulls.
     */
    private class SimpleJsonParser(private val src: String) {
        private var idx = 0

        fun parseObject(): Map<String, Any?> {
            skipWhitespace()
            expect('{')
            val map = mutableMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                idx++
                return map
            }

            while (idx < src.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                val next = peek()
                if (next == ',') {
                    idx++
                } else if (next == '}') {
                    idx++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or '}' at index $idx")
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                idx++
                return list
            }

            while (idx < src.length) {
                skipWhitespace()
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                val next = peek()
                if (next == ',') {
                    idx++
                } else if (next == ']') {
                    idx++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or ']' at index $idx")
                }
            }
            return list
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            return when (val c = peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> {
                    if (c == '-' || c in '0'..'9') {
                        parseNumber()
                    } else {
                        throw IllegalArgumentException("Unexpected character '$c' at index $idx")
                    }
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (idx < src.length) {
                val c = src[idx++]
                if (c == '"') {
                    return sb.toString()
                } else if (c == '\\') {
                    if (idx >= src.length) break
                    when (val esc = src[idx++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (idx + 4 <= src.length) {
                                val hex = src.substring(idx, idx + 4)
                                idx += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string at index $idx")
        }

        private fun parseNumber(): String {
            val start = idx
            if (peek() == '-') idx++
            while (idx < src.length && (src[idx] in '0'..'9' || src[idx] == '.' || src[idx] == 'e' || src[idx] == 'E' || src[idx] == '+' || src[idx] == '-')) {
                idx++
            }
            return src.substring(start, idx)
        }

        private fun parseBoolean(): String {
            return if (src.startsWith("true", idx)) {
                idx += 4
                "true"
            } else if (src.startsWith("false", idx)) {
                idx += 5
                "false"
            } else {
                throw IllegalArgumentException("Expected boolean at index $idx")
            }
        }

        private fun parseNull(): String? {
            return if (src.startsWith("null", idx)) {
                idx += 4
                null
            } else {
                throw IllegalArgumentException("Expected null at index $idx")
            }
        }

        private fun skipWhitespace() {
            while (idx < src.length && src[idx].isWhitespace()) {
                idx++
            }
        }

        private fun peek(): Char = if (idx < src.length) src[idx] else '\u0000'

        private fun expect(expected: Char) {
            if (idx >= src.length || src[idx] != expected) {
                throw IllegalArgumentException("Expected '$expected' at index $idx but found '${peek()}'")
            }
            idx++
        }
    }
}
