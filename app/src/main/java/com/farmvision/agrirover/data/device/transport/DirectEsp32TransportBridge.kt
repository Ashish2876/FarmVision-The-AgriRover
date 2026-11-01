package com.farmvision.agrirover.data.device.transport

import com.farmvision.agrirover.data.device.dto.FastApiJsonParser
import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.StandardRoverHttpClient
import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.TransportType
import com.farmvision.agrirover.domain.device.transport.RoverReadOnlyClient
import java.net.URL

/**
 * Read-only implementation of [RoverReadOnlyClient] dispatched over HTTP.
 *
 * SAFETY INVARIANT:
 * Restricted strictly to GET /api/v1/capabilities, GET /api/v1/telemetry, and GET /api/v1/status.
 * Absolutely NO capability to issue commands, steering, speed, pumps, or actuator actions.
 */
class HttpRoverReadOnlyClient(
    private val httpClient: RoverHttpClient = StandardRoverHttpClient(),
    private val baseUrlProvider: () -> String = { "http://192.168.4.1" }
) : RoverReadOnlyClient {

    override suspend fun getCapabilities(): Result<DeviceCapabilities> {
        return try {
            val url = "${baseUrlProvider().trimEnd('/')}/api/v1/capabilities"
            val response = httpClient.get(url, timeoutMs = 3000L)
            if (response.isSuccessful && response.body != null) {
                FastApiJsonParser.parseCapabilities(response.body)
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} fetching capabilities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRawTelemetryJson(): Result<String> {
        return try {
            val url = "${baseUrlProvider().trimEnd('/')}/api/v1/telemetry"
            val response = httpClient.get(url, timeoutMs = 3000L)
            if (response.isSuccessful && response.body != null) {
                Result.success(response.body)
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} fetching telemetry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatus(): Result<DeviceStatusSnapshot> {
        return try {
            val url = "${baseUrlProvider().trimEnd('/')}/api/v1/status"
            val response = httpClient.get(url, timeoutMs = 3000L)
            if (response.isSuccessful && response.body != null) {
                FastApiJsonParser.parseStatus(
                    rawJson = response.body,
                    transportType = TransportType.DirectEsp32SoftAp,
                    latencyMs = response.latencyMs
                )
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} fetching status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Direct ESP32 SoftAP Transport Bridge.
 * Talks directly to ESP32 DevKit V1 at http://192.168.4.1 over Wi-Fi SoftAP using [RoverReadOnlyClient].
 */
class DirectEsp32TransportBridge(
    private val readOnlyClient: RoverReadOnlyClient = HttpRoverReadOnlyClient(),
    private var baseEndpoint: String = "http://192.168.4.1"
) : DeviceTransportBridge {

    override val transportType: TransportType = TransportType.DirectEsp32SoftAp

    override suspend fun connect(endpoint: String): Result<DeviceIdentity> {
        this.baseEndpoint = endpoint
        // Test connectivity via read-only status query
        val statusResult = readOnlyClient.getStatus()
        return if (statusResult.isSuccess) {
            Result.success(
                DeviceIdentity(
                    deviceId = "agrirover-esp32-direct",
                    deviceName = "AgriRover Wi-Fi Direct",
                    macAddress = "24:6F:28:B2:1A:40",
                    hardwareRevision = "ESP32 DevKit V1",
                    firmwareVersion = "v1.2.0-ph11"
                )
            )
        } else {
            Result.failure(statusResult.exceptionOrNull() ?: Exception("Failed to connect to ESP32 SoftAP at $endpoint"))
        }
    }

    override suspend fun disconnect() {
        // Direct Wi-Fi SoftAP is stateless HTTP, no lingering socket connection to tear down
    }

    override suspend fun fetchCapabilities(): Result<DeviceCapabilities> {
        return readOnlyClient.getCapabilities()
    }

    override suspend fun fetchTelemetry(): Result<FastApiTelemetryPayload> {
        val rawJsonResult = readOnlyClient.getRawTelemetryJson()
        return if (rawJsonResult.isSuccess) {
            FastApiJsonParser.parseTelemetry(rawJsonResult.getOrThrow())
        } else {
            Result.failure(rawJsonResult.exceptionOrNull() ?: Exception("Telemetry fetch failed"))
        }
    }

    override suspend fun fetchStatus(): Result<DeviceStatusSnapshot> {
        return readOnlyClient.getStatus()
    }
}

