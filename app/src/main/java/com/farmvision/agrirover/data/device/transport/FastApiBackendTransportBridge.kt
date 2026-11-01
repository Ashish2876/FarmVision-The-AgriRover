package com.farmvision.agrirover.data.device.transport

import com.farmvision.agrirover.data.device.dto.FastApiJsonParser
import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.data.device.security.KeystoreSecretStorage
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.StandardRoverHttpClient
import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.TransportType

/**
 * FastAPI Cloud Gateway Transport Bridge.
 * Connects to planned FastAPI cloud REST service with hardware-backed Bearer token authentication.
 */
class FastApiBackendTransportBridge(
    private val keystoreSecretStorage: KeystoreSecretStorage? = null,
    private val httpClient: RoverHttpClient = StandardRoverHttpClient(),
    private val mockClient: MockFastApiBackendClient = MockFastApiBackendClient(),
    var isOfflineMockMode: Boolean = true
) : DeviceTransportBridge {

    override val transportType: TransportType = TransportType.FastApiCloudGateway
    private var baseEndpoint: String = "https://api.farmvision.com"

    override suspend fun connect(endpoint: String): Result<DeviceIdentity> {
        this.baseEndpoint = endpoint
        if (isOfflineMockMode || endpoint.contains("mock", ignoreCase = true)) {
            val statusResult = mockClient.getStatus()
            return if (statusResult.isSuccess) {
                Result.success(
                    DeviceIdentity(
                        deviceId = "agrirover-cloud-01",
                        deviceName = "AgriRover Cloud Gateway",
                        macAddress = "CC:50:E3:8A:2F:10",
                        hardwareRevision = "Cloud FastApi Gateway v1",
                        firmwareVersion = "v1.2.0-ph11"
                    )
                )
            } else {
                Result.failure(statusResult.exceptionOrNull() ?: Exception("Failed to connect to Mock FastAPI Gateway"))
            }
        }

        return try {
            val token = keystoreSecretStorage?.getBearerToken()
            val headers = if (token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
            val url = "${baseEndpoint.trimEnd('/')}/api/v1/status"
            val response = httpClient.get(url, headers = headers, timeoutMs = 4000L)
            if (response.isSuccessful) {
                Result.success(
                    DeviceIdentity(
                        deviceId = "agrirover-cloud-01",
                        deviceName = "AgriRover Cloud Gateway",
                        macAddress = "CC:50:E3:8A:2F:10",
                        hardwareRevision = "Cloud FastApi Gateway v1",
                        firmwareVersion = "v1.2.0-ph11"
                    )
                )
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} connecting to FastAPI Gateway"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        // Stateless HTTPS connection
    }

    override suspend fun fetchCapabilities(): Result<DeviceCapabilities> {
        if (isOfflineMockMode || baseEndpoint.contains("mock", ignoreCase = true)) {
            return mockClient.getCapabilities()
        }
        return try {
            val token = keystoreSecretStorage?.getBearerToken()
            val headers = if (token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
            val url = "${baseEndpoint.trimEnd('/')}/api/v1/capabilities"
            val response = httpClient.get(url, headers = headers, timeoutMs = 4000L)
            if (response.isSuccessful && response.body != null) {
                FastApiJsonParser.parseCapabilities(response.body)
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} fetching capabilities from FastAPI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchTelemetry(): Result<FastApiTelemetryPayload> {
        if (isOfflineMockMode || baseEndpoint.contains("mock", ignoreCase = true)) {
            return mockClient.getTelemetry()
        }
        return try {
            val token = keystoreSecretStorage?.getBearerToken()
            val headers = if (token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
            val url = "${baseEndpoint.trimEnd('/')}/api/v1/telemetry"
            val response = httpClient.get(url, headers = headers, timeoutMs = 4000L)
            if (response.isSuccessful && response.body != null) {
                FastApiJsonParser.parseTelemetry(response.body)
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} fetching telemetry from FastAPI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchStatus(): Result<DeviceStatusSnapshot> {
        if (isOfflineMockMode || baseEndpoint.contains("mock", ignoreCase = true)) {
            return mockClient.getStatus()
        }
        return try {
            val token = keystoreSecretStorage?.getBearerToken()
            val headers = if (token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
            val url = "${baseEndpoint.trimEnd('/')}/api/v1/status"
            val response = httpClient.get(url, headers = headers, timeoutMs = 4000L)
            if (response.isSuccessful && response.body != null) {
                FastApiJsonParser.parseStatus(
                    rawJson = response.body,
                    transportType = TransportType.FastApiCloudGateway,
                    latencyMs = response.latencyMs
                )
            } else {
                Result.failure(Exception("HTTP ${response.statusCode} fetching status from FastAPI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

