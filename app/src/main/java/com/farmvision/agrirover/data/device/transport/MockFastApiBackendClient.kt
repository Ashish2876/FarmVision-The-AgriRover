package com.farmvision.agrirover.data.device.transport

import com.farmvision.agrirover.data.device.dto.FastApiJsonParser
import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.TransportType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Offline high-fidelity mock client implementing the planned FastAPI Cloud Gateway contracts.
 *
 * DATA ISOLATION INVARIANT:
 * All telemetry produced by this mock is strictly tagged with [DataOrigin.Simulated].
 * It can NEVER be converted to or claimed as [DataOrigin.LiveHardware].
 */
class MockFastApiBackendClient(
    var simulatedLatencyMs: Long = 50L,
    var shouldFailNextRequest: Boolean = false,
    var failureReason: String = "Simulated FastAPI HTTP 503 Service Unavailable"
) {
    val origin: DataOrigin = DataOrigin.Simulated

    suspend fun authenticate(clientId: String, clientSecret: String): Result<String> {
        if (simulatedLatencyMs > 0) delay(simulatedLatencyMs)
        if (shouldFailNextRequest) {
            return Result.failure(Exception(failureReason))
        }
        return Result.success("mock_fastapi_jwt_token_${System.currentTimeMillis()}")
    }

    suspend fun getCapabilities(): Result<DeviceCapabilities> {
        if (simulatedLatencyMs > 0) delay(simulatedLatencyMs)
        if (shouldFailNextRequest) {
            return Result.failure(Exception(failureReason))
        }
        val mockJson = """
            {
                "supports_telemetry": true,
                "supports_capabilities": true,
                "supports_status": true,
                "max_polling_hz": 2.0,
                "available_sensors": ["soil_moisture", "dht22", "bh1750", "hc_sr04", "ina219"],
                "actuator_control_enabled": false
            }
        """.trimIndent()
        return FastApiJsonParser.parseCapabilities(mockJson)
    }

    suspend fun getTelemetry(): Result<FastApiTelemetryPayload> {
        if (simulatedLatencyMs > 0) delay(simulatedLatencyMs)
        if (shouldFailNextRequest) {
            return Result.failure(Exception(failureReason))
        }
        val now = System.currentTimeMillis()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val isoTimestamp = isoFormat.format(Date(now))

        val mockJson = """
            {
                "rover_id": "agrirover-mock-cloud",
                "timestamp": "$isoTimestamp",
                "battery_percent": 84,
                "battery_voltage": 12.35,
                "speed_kmh": 0.0,
                "motor_temperature": 32.4,
                "soil_moisture_raw": 1820,
                "soil_ph": 6.8,
                "temperature": 27.2,
                "humidity": 64.0,
                "light_lux": 45000,
                "ultrasonic_distance_cm": 120.0
            }
        """.trimIndent()
        return FastApiJsonParser.parseTelemetry(mockJson, now)
    }

    suspend fun getStatus(): Result<DeviceStatusSnapshot> {
        if (simulatedLatencyMs > 0) delay(simulatedLatencyMs)
        if (shouldFailNextRequest) {
            return Result.failure(Exception(failureReason))
        }
        val mockJson = """
            {
                "online": true,
                "uptime_seconds": 3600,
                "wifi_rssi_dbm": -55,
                "free_heap_bytes": 145000,
                "packets_received": 120,
                "packets_failed": 0
            }
        """.trimIndent()
        return FastApiJsonParser.parseStatus(
            rawJson = mockJson,
            transportType = TransportType.FastApiCloudGateway,
            latencyMs = simulatedLatencyMs
        )
    }
}

