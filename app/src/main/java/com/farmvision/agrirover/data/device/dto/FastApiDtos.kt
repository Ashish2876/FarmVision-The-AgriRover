package com.farmvision.agrirover.data.device.dto

import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.JsonRawField
import com.farmvision.agrirover.domain.device.model.JsonTokenType
import com.farmvision.agrirover.domain.device.model.TelemetryTimestamp
import com.farmvision.agrirover.domain.device.model.TimestampQuality
import com.farmvision.agrirover.domain.device.model.TransportType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Lossless DTO representation of telemetry payload from FastAPI or ESP32 GET /api/v1/telemetry.
 */
data class FastApiTelemetryPayload(
    val roverId: String?,
    val timestamp: TelemetryTimestamp,
    val batteryPercent: Int?,
    val batteryVoltage: Float?,
    val speedKmh: Float?,
    val motorTemperature: Float?,
    val soilMoistureRawAdc: Int?,
    val soilPhRaw: Float?,
    val temperatureCelsius: Float?,
    val humidityPercent: Float?,
    val lightLux: Int?,
    val ultrasonicDistanceCm: Float?,
    val rawJson: String
)

/**
 * Pure Kotlin lossless parser for telemetry, capabilities, and status JSON.
 * Fully framework-independent; runs identically on JVM test runners and Android runtime.
 */
object FastApiJsonParser {

    /**
     * Extracts a lossless [JsonRawField] from raw JSON string.
     */
    fun extractRawField(rawJson: String, key: String): JsonRawField {
        val keyPattern = Regex("\"$key\"\\s*:\\s*(null|true|false|\"[^\"]*\"|\\{[^}]*\\}|\\[[^\\]]*\\]|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
        val match = keyPattern.find(rawJson) ?: return JsonRawField.Absent

        val token = match.groupValues[1]
        return when {
            token == "null" -> JsonRawField.ExplicitNull
            token == "true" || token == "false" -> JsonRawField.Present(JsonTokenType.BOOLEAN, token)
            token.startsWith("\"") && token.endsWith("\"") -> {
                val content = token.substring(1, token.length - 1)
                JsonRawField.Present(JsonTokenType.STRING, content)
            }
            token.startsWith("{") -> JsonRawField.Present(JsonTokenType.OBJECT, token)
            token.startsWith("[") -> JsonRawField.Present(JsonTokenType.ARRAY, token)
            else -> JsonRawField.Present(JsonTokenType.NUMBER, token)
        }
    }

    /**
     * Parses a [JsonRawField] into a typed [TelemetryTimestamp].
     * Never synthesizes device timestamps.
     */
    fun parseTimestamp(field: JsonRawField, receivedAtEpochMillis: Long): TelemetryTimestamp {
        return when (field) {
            is JsonRawField.Absent -> TelemetryTimestamp(
                sourceField = field,
                sourceTimestampEpochMillis = null,
                receivedAtEpochMillis = receivedAtEpochMillis,
                timestampQuality = TimestampQuality.Absent
            )
            is JsonRawField.ExplicitNull -> TelemetryTimestamp(
                sourceField = field,
                sourceTimestampEpochMillis = null,
                receivedAtEpochMillis = receivedAtEpochMillis,
                timestampQuality = TimestampQuality.ExplicitNull
            )
            is JsonRawField.Present -> {
                when (field.tokenType) {
                    JsonTokenType.BOOLEAN,
                    JsonTokenType.OBJECT,
                    JsonTokenType.ARRAY -> TelemetryTimestamp(
                        sourceField = field,
                        sourceTimestampEpochMillis = null,
                        receivedAtEpochMillis = receivedAtEpochMillis,
                        timestampQuality = TimestampQuality.TypeMismatch
                    )
                    JsonTokenType.NUMBER -> {
                        val rawNum = field.rawValue.toDoubleOrNull()
                        val longVal = field.rawValue.toLongOrNull()
                        when {
                            rawNum == null || rawNum < 0.0 -> TelemetryTimestamp(
                                sourceField = field,
                                sourceTimestampEpochMillis = null,
                                receivedAtEpochMillis = receivedAtEpochMillis,
                                timestampQuality = TimestampQuality.Malformed
                            )
                            longVal != null && longVal in 0L..86_400_000L -> TelemetryTimestamp(
                                sourceField = field,
                                sourceTimestampEpochMillis = null,
                                receivedAtEpochMillis = receivedAtEpochMillis,
                                timestampQuality = TimestampQuality.ZeroUptimeSkew
                            )
                            // Epoch milliseconds roughly between 2020 and 2050
                            longVal != null && longVal in 1_577_836_800_000L..2_524_608_000_000L -> {
                                val quality = if (longVal > receivedAtEpochMillis + 30_000L) {
                                    TimestampQuality.FutureDrift
                                } else {
                                    TimestampQuality.ValidUtc
                                }
                                TelemetryTimestamp(
                                    sourceField = field,
                                    sourceTimestampEpochMillis = longVal,
                                    receivedAtEpochMillis = receivedAtEpochMillis,
                                    timestampQuality = quality
                                )
                            }
                            else -> TelemetryTimestamp(
                                sourceField = field,
                                sourceTimestampEpochMillis = null,
                                receivedAtEpochMillis = receivedAtEpochMillis,
                                timestampQuality = TimestampQuality.Malformed
                            )
                        }
                    }
                    JsonTokenType.STRING -> {
                        val trimmed = field.rawValue.trim()
                        when {
                            trimmed.isEmpty() -> TelemetryTimestamp(
                                sourceField = field,
                                sourceTimestampEpochMillis = null,
                                receivedAtEpochMillis = receivedAtEpochMillis,
                                timestampQuality = TimestampQuality.EmptyString
                            )
                            trimmed == "0" -> TelemetryTimestamp(
                                sourceField = field,
                                sourceTimestampEpochMillis = null,
                                receivedAtEpochMillis = receivedAtEpochMillis,
                                timestampQuality = TimestampQuality.ZeroUptimeSkew
                            )
                            else -> {
                                val parsedUtc = parseIso8601UtcOrNull(trimmed)
                                if (parsedUtc != null) {
                                    val quality = if (parsedUtc > receivedAtEpochMillis + 30_000L) {
                                        TimestampQuality.FutureDrift
                                    } else {
                                        TimestampQuality.ValidUtc
                                    }
                                    TelemetryTimestamp(
                                        sourceField = field,
                                        sourceTimestampEpochMillis = parsedUtc,
                                        receivedAtEpochMillis = receivedAtEpochMillis,
                                        timestampQuality = quality
                                    )
                                } else {
                                    val numericEpoch = trimmed.toLongOrNull()
                                    if (numericEpoch != null && numericEpoch in 1_577_836_800_000L..2_524_608_000_000L) {
                                        val quality = if (numericEpoch > receivedAtEpochMillis + 30_000L) {
                                            TimestampQuality.FutureDrift
                                        } else {
                                            TimestampQuality.ValidUtc
                                        }
                                        TelemetryTimestamp(
                                            sourceField = field,
                                            sourceTimestampEpochMillis = numericEpoch,
                                            receivedAtEpochMillis = receivedAtEpochMillis,
                                            timestampQuality = quality
                                        )
                                    } else {
                                        TelemetryTimestamp(
                                            sourceField = field,
                                            sourceTimestampEpochMillis = null,
                                            receivedAtEpochMillis = receivedAtEpochMillis,
                                            timestampQuality = TimestampQuality.Malformed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun optString(rawJson: String, key: String): String? {
        val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(rawJson)
        return match?.groupValues?.get(1)
    }

    private fun optLong(rawJson: String, key: String): Long? {
        val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(rawJson)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun optFloat(rawJson: String, key: String): Float? {
        val match = Regex("\"$key\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)").find(rawJson)
        return match?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun optInt(rawJson: String, key: String): Int? {
        return optLong(rawJson, key)?.toInt()
    }

    private fun optBoolean(rawJson: String, key: String, default: Boolean): Boolean {
        val match = Regex("\"$key\"\\s*:\\s*(true|false)").find(rawJson)
        return match?.groupValues?.get(1)?.toBoolean() ?: default
    }

    fun parseTelemetry(rawJson: String, receivedAtEpochMillis: Long = System.currentTimeMillis()): Result<FastApiTelemetryPayload> {
        return try {
            val tsField = extractRawField(rawJson, "timestamp")
            val telemetryTimestamp = parseTimestamp(tsField, receivedAtEpochMillis)

            val roverId = optString(rawJson, "rover_id") ?: optString(rawJson, "rover_name") ?: "agrirover-01"
            val batteryPercent = optInt(rawJson, "battery_percent") ?: optInt(rawJson, "battery") ?: 80
            val batteryVoltage = optFloat(rawJson, "battery_voltage")
            val speedKmh = optFloat(rawJson, "speed_kmh")
            val motorTemperature = optFloat(rawJson, "motor_temperature")
            val soilMoistureRaw = optInt(rawJson, "soil_moisture_raw") ?: optInt(rawJson, "soil_moisture_counts")
            val soilPh = optFloat(rawJson, "soil_ph")
            val temperature = optFloat(rawJson, "temperature")
            val humidity = optFloat(rawJson, "humidity")
            val lightLux = optInt(rawJson, "light_lux") ?: optInt(rawJson, "lux")
            val ultrasonicDistance = optFloat(rawJson, "ultrasonic_distance_cm") ?: optFloat(rawJson, "obstacle_distance_cm")

            val payload = FastApiTelemetryPayload(
                roverId = roverId,
                timestamp = telemetryTimestamp,
                batteryPercent = batteryPercent,
                batteryVoltage = batteryVoltage,
                speedKmh = speedKmh,
                motorTemperature = motorTemperature,
                soilMoistureRawAdc = soilMoistureRaw,
                soilPhRaw = soilPh,
                temperatureCelsius = temperature,
                humidityPercent = humidity,
                lightLux = lightLux,
                ultrasonicDistanceCm = ultrasonicDistance,
                rawJson = rawJson
            )
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseCapabilities(rawJson: String): Result<DeviceCapabilities> {
        return try {
            val sensorsMatch = Regex("\"available_sensors\"\\s*:\\s*\\[([^\\]]*)\\]").find(rawJson)
            val sensorsList = mutableListOf<String>()
            if (sensorsMatch != null) {
                val elements = sensorsMatch.groupValues[1].split(",")
                for (elem in elements) {
                    val clean = elem.trim().removeSurrounding("\"").trim()
                    if (clean.isNotEmpty()) sensorsList.add(clean)
                }
            }

            val caps = DeviceCapabilities(
                supportsTelemetry = optBoolean(rawJson, "supports_telemetry", true),
                supportsCapabilities = optBoolean(rawJson, "supports_capabilities", true),
                supportsStatus = optBoolean(rawJson, "supports_status", true),
                maxPollingHz = optFloat(rawJson, "max_polling_hz") ?: 2.0f,
                availableSensors = if (sensorsList.isNotEmpty()) sensorsList else listOf("soil_moisture", "dht22", "bh1750", "hc_sr04", "ina219"),
                actuatorControlEnabled = optBoolean(rawJson, "actuator_control_enabled", false)
            )
            Result.success(caps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseStatus(rawJson: String, transportType: TransportType = TransportType.DirectEsp32SoftAp, latencyMs: Long = 0L): Result<DeviceStatusSnapshot> {
        return try {
            val status = DeviceStatusSnapshot(
                isOnline = optBoolean(rawJson, "online", optBoolean(rawJson, "status", true)),
                uptimeSeconds = optLong(rawJson, "uptime_seconds") ?: optLong(rawJson, "uptime") ?: 0L,
                wifiRssiDbm = optInt(rawJson, "wifi_rssi_dbm") ?: optInt(rawJson, "rssi") ?: -65,
                freeHeapBytes = optLong(rawJson, "free_heap") ?: optLong(rawJson, "free_heap_bytes") ?: 120_000L,
                transportType = transportType,
                lastPollLatencyMs = latencyMs,
                totalPacketsReceived = optLong(rawJson, "packets_received") ?: 1L,
                failedPacketsCount = optLong(rawJson, "packets_failed") ?: 0L
            )
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIso8601UtcOrNull(str: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(str)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }
}
