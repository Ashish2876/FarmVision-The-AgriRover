package com.farmvision.agrirover.domain.protocol

/**
 * Pure Kotlin, zero-dependency JSON parser and serializer for the AgriRover protocol.
 * Executes deterministically in JVM unit tests and on Android runtime without framework stubs.
 */
object ProtocolMessageParser {

    /**
     * Parses a JSON string into [RoverCapabilities].
     */
    fun parseCapabilities(rawJson: String): RoverCapabilities {
        assertPayloadSanity(rawJson)
        val root = SimpleJson.parseObject(rawJson)

        val version = root["protocolVersion"] as? String
            ?: throw IllegalArgumentException("Missing protocolVersion in capabilities payload")
        val deviceId = root["deviceId"] as? String ?: ""
        val firmwareVersion = root["firmwareVersion"] as? String ?: ""
        val stageStr = root["firmwareStage"] as? String ?: "F0"
        val firmwareStage = try {
            FirmwareStage.valueOf(stageStr)
        } catch (_: Exception) {
            FirmwareStage.F0
        }
        val controllerTarget = root["controllerTarget"] as? String ?: "Esp32DevKitV1"

        val sensorsRaw = root["supportedSensors"] as? List<*> ?: emptyList<Any>()
        val sensors = sensorsRaw.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val type = map["type"] as? String ?: "Unknown"
            val unit = map["unit"] as? String ?: ""
            val requiresCalibration = (map["requiresCalibration"] as? String)?.toBoolean()
                ?: (map["requiresCalibration"] as? Boolean) ?: false
            val isAvailable = (map["isAvailable"] as? String)?.toBoolean()
                ?: (map["isAvailable"] as? Boolean) ?: true
            SensorCapability(id, type, unit, requiresCalibration, isAvailable)
        }

        val actuatorsRaw = root["supportedActuators"] as? List<*> ?: emptyList<Any>()
        val actuators = actuatorsRaw.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val type = map["type"] as? String ?: "Unknown"
            val hasPhysicalFeedback = (map["hasPhysicalFeedback"] as? String)?.toBoolean()
                ?: (map["hasPhysicalFeedback"] as? Boolean) ?: false
            val isEnabled = (map["isEnabled"] as? String)?.toBoolean()
                ?: (map["isEnabled"] as? Boolean) ?: false
            val maxDurationMs = (map["maxDurationMs"] as? String)?.toLongOrNull()
                ?: (map["maxDurationMs"] as? Number)?.toLong()
            ActuatorCapability(id, type, hasPhysicalFeedback, isEnabled, maxDurationMs)
        }

        val featuresRaw = root["features"] as? List<*> ?: emptyList<Any>()
        val features = featuresRaw.mapNotNull { it as? String }
        val isEmergencyStopSupported = (root["isEmergencyStopSupported"] as? String)?.toBoolean()
            ?: (root["isEmergencyStopSupported"] as? Boolean) ?: false

        return RoverCapabilities(
            protocolVersion = version,
            deviceId = deviceId,
            firmwareVersion = firmwareVersion,
            firmwareStage = firmwareStage,
            controllerTarget = controllerTarget,
            supportedSensors = sensors,
            supportedActuators = actuators,
            features = features,
            isEmergencyStopSupported = isEmergencyStopSupported
        )
    }

    /**
     * Parses a JSON string into [RoverTelemetryPacket].
     */
    fun parseTelemetry(rawJson: String): RoverTelemetryPacket {
        assertPayloadSanity(rawJson)
        val root = SimpleJson.parseObject(rawJson)

        val version = root["protocolVersion"] as? String
            ?: throw IllegalArgumentException("Missing protocolVersion in telemetry packet")
        val deviceId = root["deviceId"] as? String ?: ""
        val firmwareVersion = root["firmwareVersion"] as? String ?: ""
        val sequence = (root["sequenceNumber"] as? String)?.toLongOrNull()
            ?: (root["sequenceNumber"] as? Number)?.toLong() ?: 0L
        val uptime = (root["uptimeMs"] as? String)?.toLongOrNull()
            ?: (root["uptimeMs"] as? Number)?.toLong() ?: 0L

        val readingsMap = mutableMapOf<String, SensorReadingPayload>()
        val readingsRaw = root["readings"] as? Map<*, *>
        readingsRaw?.forEach { (key, value) ->
            val readingKey = key as? String ?: return@forEach
            val readingObj = value as? Map<*, *> ?: return@forEach

            val valStr = readingObj["value"]
            val rawValStr = readingObj["rawValue"]

            val readingValue = (valStr as? String)?.toDoubleOrNull()
                ?: (valStr as? Number)?.toDouble() ?: 0.0
            val rawValue = (rawValStr as? String)?.toDoubleOrNull()
                ?: (rawValStr as? Number)?.toDouble() ?: readingValue
            val unit = readingObj["unit"] as? String ?: ""
            val isValid = (readingObj["isValid"] as? String)?.toBoolean()
                ?: (readingObj["isValid"] as? Boolean) ?: true
            val quality = readingObj["quality"] as? String ?: "OK"

            readingsMap[readingKey] = SensorReadingPayload(
                value = readingValue,
                rawValue = rawValue,
                unit = unit,
                isValid = isValid,
                quality = quality
            )
        }

        val diagnosticsMap = mutableMapOf<String, String>()
        val diagRaw = root["diagnostics"] as? Map<*, *>
        diagRaw?.forEach { (k, v) ->
            if (k is String && v != null) {
                diagnosticsMap[k] = v.toString()
            }
        }

        return RoverTelemetryPacket(
            protocolVersion = version,
            deviceId = deviceId,
            firmwareVersion = firmwareVersion,
            sequenceNumber = sequence,
            uptimeMs = uptime,
            readings = readingsMap,
            diagnostics = diagnosticsMap
        )
    }

    /**
     * Parses a JSON string into [RoverCommand].
     */
    fun parseCommand(rawJson: String): RoverCommand {
        assertPayloadSanity(rawJson)
        val root = SimpleJson.parseObject(rawJson)

        val version = root["protocolVersion"] as? String
            ?: throw IllegalArgumentException("Missing protocolVersion in command")
        val cmdId = root["cmdId"] as? String
            ?: throw IllegalArgumentException("Missing cmdId in command")
        val sequence = (root["sequence"] as? String)?.toLongOrNull()
            ?: (root["sequence"] as? Number)?.toLong() ?: 0L
        val commandType = root["commandType"] as? String
            ?: throw IllegalArgumentException("Missing commandType in command")
        val ttlMs = (root["ttlMs"] as? String)?.toLongOrNull()
            ?: (root["ttlMs"] as? Number)?.toLong() ?: ProtocolConstants.DEFAULT_DRIVE_TTL_MS
        val created = (root["createdTimestampMs"] as? String)?.toLongOrNull()
            ?: (root["createdTimestampMs"] as? Number)?.toLong() ?: 0L

        val priorityStr = root["priority"] as? String ?: "NORMAL"
        val priority = try {
            CommandPriority.valueOf(priorityStr.uppercase())
        } catch (_: Exception) {
            CommandPriority.NORMAL
        }

        val paramsRaw = root["parameters"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val params = mutableMapOf<String, Any?>()
        paramsRaw.forEach { (k, v) ->
            if (k is String) {
                // If it's a numeric string, parse to appropriate number
                if (v is String) {
                    val asLong = v.toLongOrNull()
                    val asDouble = v.toDoubleOrNull()
                    params[k] = when {
                        asLong != null && !v.contains(".") -> asLong
                        asDouble != null -> asDouble
                        v == "true" -> true
                        v == "false" -> false
                        else -> v
                    }
                } else {
                    params[k] = v
                }
            }
        }

        return RoverCommand(
            protocolVersion = version,
            cmdId = cmdId,
            sequence = sequence,
            commandType = commandType,
            ttlMs = ttlMs,
            createdTimestampMs = created,
            priority = priority,
            parameters = params
        )
    }

    /**
     * Parses a JSON string into [CommandAcknowledgement].
     */
    fun parseAcknowledgement(rawJson: String): CommandAcknowledgement {
        assertPayloadSanity(rawJson)
        val root = SimpleJson.parseObject(rawJson)

        val version = root["protocolVersion"] as? String
            ?: throw IllegalArgumentException("Missing protocolVersion in acknowledgement")
        val cmdId = root["cmdId"] as? String
            ?: throw IllegalArgumentException("Missing cmdId in acknowledgement")
        val statusStr = root["status"] as? String ?: "REJECTED"
        val status = try {
            CommandAckStatus.valueOf(statusStr.uppercase())
        } catch (_: Exception) {
            CommandAckStatus.REJECTED
        }
        val reasonCode = root["reasonCode"] as? String
        val latencyMs = (root["latencyMs"] as? String)?.toLongOrNull()
            ?: (root["latencyMs"] as? Number)?.toLong() ?: 0L
        val timestampMs = (root["timestampMs"] as? String)?.toLongOrNull()
            ?: (root["timestampMs"] as? Number)?.toLong() ?: 0L

        return CommandAcknowledgement(
            protocolVersion = version,
            cmdId = cmdId,
            status = status,
            reasonCode = reasonCode,
            latencyMs = latencyMs,
            timestampMs = timestampMs
        )
    }

    /**
     * Parses a JSON string into [ProtocolErrorResponse].
     */
    fun parseErrorResponse(rawJson: String): ProtocolErrorResponse {
        assertPayloadSanity(rawJson)
        val root = SimpleJson.parseObject(rawJson)

        val version = root["protocolVersion"] as? String
            ?: ProtocolConstants.PROTOCOL_VERSION
        val errCodeStr = root["errorCode"] as? String
        val errorCode = ProtocolErrorCode.fromCode(errCodeStr)
        val message = root["message"] as? String ?: errorCode.description
        val recoverable = (root["recoverable"] as? String)?.toBoolean()
            ?: (root["recoverable"] as? Boolean) ?: errorCode.isRecoverable
        val timestampMs = (root["timestampMs"] as? String)?.toLongOrNull()
            ?: (root["timestampMs"] as? Number)?.toLong() ?: 0L

        return ProtocolErrorResponse(
            protocolVersion = version,
            errorCode = errorCode,
            message = message,
            recoverable = recoverable,
            timestampMs = timestampMs
        )
    }

    /**
     * Serializes a [RoverCommand] to a JSON string.
     */
    fun serializeCommand(command: RoverCommand): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"protocolVersion\": ").append(escape(command.protocolVersion)).append(",\n")
        sb.append("  \"cmdId\": ").append(escape(command.cmdId)).append(",\n")
        sb.append("  \"sequence\": ").append(command.sequence).append(",\n")
        sb.append("  \"commandType\": ").append(escape(command.commandType)).append(",\n")
        sb.append("  \"ttlMs\": ").append(command.ttlMs).append(",\n")
        sb.append("  \"createdTimestampMs\": ").append(command.createdTimestampMs).append(",\n")
        sb.append("  \"priority\": ").append(escape(command.priority.name)).append(",\n")
        sb.append("  \"parameters\": {\n")
        val paramEntries = command.parameters.entries.toList()
        for (i in paramEntries.indices) {
            val (k, v) = paramEntries[i]
            val comma = if (i < paramEntries.size - 1) "," else ""
            sb.append("    ").append(escape(k)).append(": ").append(formatValue(v)).append(comma).append("\n")
        }
        sb.append("  }\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Serializes a [CommandAcknowledgement] to a JSON string.
     */
    fun serializeAcknowledgement(ack: CommandAcknowledgement): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"protocolVersion\": ").append(escape(ack.protocolVersion)).append(",\n")
        sb.append("  \"cmdId\": ").append(escape(ack.cmdId)).append(",\n")
        sb.append("  \"status\": ").append(escape(ack.status.name)).append(",\n")
        sb.append("  \"reasonCode\": ").append(escape(ack.reasonCode)).append(",\n")
        sb.append("  \"latencyMs\": ").append(ack.latencyMs).append(",\n")
        sb.append("  \"timestampMs\": ").append(ack.timestampMs).append("\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Serializes a [ProtocolErrorResponse] to a JSON string.
     */
    fun serializeErrorResponse(error: ProtocolErrorResponse): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"protocolVersion\": ").append(escape(error.protocolVersion)).append(",\n")
        sb.append("  \"errorCode\": ").append(escape(error.errorCode.code)).append(",\n")
        sb.append("  \"message\": ").append(escape(error.message)).append(",\n")
        sb.append("  \"recoverable\": ").append(error.recoverable).append(",\n")
        sb.append("  \"timestampMs\": ").append(error.timestampMs).append("\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Serializes [RoverCapabilities] to a JSON string.
     */
    fun serializeCapabilities(caps: RoverCapabilities): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"protocolVersion\": ").append(escape(caps.protocolVersion)).append(",\n")
        sb.append("  \"deviceId\": ").append(escape(caps.deviceId)).append(",\n")
        sb.append("  \"firmwareVersion\": ").append(escape(caps.firmwareVersion)).append(",\n")
        sb.append("  \"firmwareStage\": ").append(escape(caps.firmwareStage.name)).append(",\n")
        sb.append("  \"controllerTarget\": ").append(escape(caps.controllerTarget)).append(",\n")
        sb.append("  \"supportedSensors\": [\n")
        for (i in caps.supportedSensors.indices) {
            val s = caps.supportedSensors[i]
            val comma = if (i < caps.supportedSensors.size - 1) "," else ""
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escape(s.id)).append(",\n")
            sb.append("      \"type\": ").append(escape(s.type)).append(",\n")
            sb.append("      \"unit\": ").append(escape(s.unit)).append(",\n")
            sb.append("      \"requiresCalibration\": ").append(s.requiresCalibration).append(",\n")
            sb.append("      \"isAvailable\": ").append(s.isAvailable).append("\n")
            sb.append("    }").append(comma).append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"supportedActuators\": [\n")
        for (i in caps.supportedActuators.indices) {
            val a = caps.supportedActuators[i]
            val comma = if (i < caps.supportedActuators.size - 1) "," else ""
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escape(a.id)).append(",\n")
            sb.append("      \"type\": ").append(escape(a.type)).append(",\n")
            sb.append("      \"hasPhysicalFeedback\": ").append(a.hasPhysicalFeedback).append(",\n")
            sb.append("      \"isEnabled\": ").append(a.isEnabled)
            if (a.maxDurationMs != null) {
                sb.append(",\n      \"maxDurationMs\": ").append(a.maxDurationMs)
            }
            sb.append("\n    }").append(comma).append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"features\": [")
        sb.append(caps.features.joinToString(", ") { escape(it) })
        sb.append("],\n")
        sb.append("  \"isEmergencyStopSupported\": ").append(caps.isEmergencyStopSupported).append("\n")
        sb.append("}")
        return sb.toString()
    }

    private fun assertPayloadSanity(rawJson: String) {
        val sizeResult = ProtocolValidator.validatePayloadSize(rawJson)
        if (!sizeResult.isValid) {
            val invalid = sizeResult as ValidationResult.Invalid
            throw IllegalArgumentException("${invalid.errorCode.code}: ${invalid.reason}")
        }
        val sanityResult = ProtocolValidator.validateNumericSanity(rawJson)
        if (!sanityResult.isValid) {
            val invalid = sanityResult as ValidationResult.Invalid
            throw IllegalArgumentException("${invalid.errorCode.code}: ${invalid.reason}")
        }
    }

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

    private fun formatValue(v: Any?): String {
        return when (v) {
            null -> "null"
            is String -> escape(v)
            is Number -> v.toString()
            is Boolean -> v.toString()
            else -> escape(v.toString())
        }
    }

    /**
     * Pure Kotlin recursive-descent JSON parser.
     */
    internal object SimpleJson {
        fun parseObject(src: String): Map<String, Any?> {
            val parser = Parser(src)
            val result = parser.parseValue()
            if (result !is Map<*, *>) {
                throw IllegalArgumentException("Expected JSON object at root")
            }
            @Suppress("UNCHECKED_CAST")
            return result as Map<String, Any?>
        }

        private class Parser(private val src: String) {
            private var idx = 0

            fun parseValue(): Any? {
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

            private fun parseObject(): Map<String, Any?> {
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
}
