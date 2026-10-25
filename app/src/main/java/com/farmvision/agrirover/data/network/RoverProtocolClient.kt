package com.farmvision.agrirover.data.network

import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.domain.protocol.CommandAckStatus
import com.farmvision.agrirover.domain.protocol.CommandAcknowledgement
import com.farmvision.agrirover.domain.protocol.CommandPriority
import com.farmvision.agrirover.domain.protocol.ProtocolConstants
import com.farmvision.agrirover.domain.protocol.ProtocolMessageParser
import com.farmvision.agrirover.domain.protocol.RoverCapabilities
import com.farmvision.agrirover.domain.protocol.RoverCommand
import com.farmvision.agrirover.domain.protocol.RoverTelemetryPacket
import com.farmvision.agrirover.ui.hardware.model.ConnectionDiagnosticResult

/**
 * High-level network client orchestrating structured REST communication with the rover microcontrollers.
 */
class RoverProtocolClient(
    private val httpClient: RoverHttpClient = StandardRoverHttpClient()
) {

    /**
     * Probes an endpoint and returns a detailed [ConnectionDiagnosticResult] without throwing exceptions.
     */
    suspend fun probeEndpoint(
        target: ControllerTarget,
        host: String,
        port: Int,
        authToken: String? = null,
        timeoutMs: Long = 2500L
    ): ConnectionDiagnosticResult {
        val now = System.currentTimeMillis()
        val normalizedHost = host.trim()
        val baseUrl = "http://$normalizedHost:$port"
        val endpointUrl = if (target == ControllerTarget.Esp32Cam) {
            "$baseUrl/status"
        } else {
            "$baseUrl/api/v1/capabilities"
        }

        val headers = mutableMapOf<String, String>()
        if (!authToken.isNullOrBlank()) {
            headers[ProtocolConstants.HEADER_AUTH_TOKEN] = authToken.trim()
        }

        return try {
            val response = httpClient.get(
                url = endpointUrl,
                headers = headers,
                timeoutMs = timeoutMs
            )

            if (response.isSuccessful) {
                val details = if (target == ControllerTarget.Esp32Cam) {
                    "ESP32-CAM reachable at $baseUrl (${response.latencyMs}ms)"
                } else {
                    val caps = try {
                        response.body?.let { ProtocolMessageParser.parseCapabilities(it) }
                    } catch (_: Exception) { null }

                    if (caps != null) {
                        "Main ESP32 Connected: Stage ${caps.firmwareStage.name}, Firmware: ${caps.firmwareVersion} (${response.latencyMs}ms)"
                    } else {
                        "Main ESP32 reachable at $baseUrl (${response.latencyMs}ms)"
                    }
                }

                ConnectionDiagnosticResult(
                    target = target,
                    success = true,
                    statusCode = response.statusCode,
                    latencyMs = response.latencyMs,
                    message = details,
                    timestampMs = System.currentTimeMillis()
                )
            } else if (response.statusCode == 401) {
                ConnectionDiagnosticResult(
                    target = target,
                    success = false,
                    statusCode = 401,
                    latencyMs = response.latencyMs,
                    message = "HTTP 401 Unauthorized: Invalid or missing X-AgriRover-Token.",
                    timestampMs = System.currentTimeMillis()
                )
            } else {
                ConnectionDiagnosticResult(
                    target = target,
                    success = false,
                    statusCode = response.statusCode,
                    latencyMs = response.latencyMs,
                    message = "HTTP ${response.statusCode} error received from $baseUrl.",
                    timestampMs = System.currentTimeMillis()
                )
            }
        } catch (e: RoverNetworkException) {
            ConnectionDiagnosticResult(
                target = target,
                success = false,
                statusCode = null,
                latencyMs = null,
                message = e.message ?: "Failed to connect to $baseUrl",
                timestampMs = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            ConnectionDiagnosticResult(
                target = target,
                success = false,
                statusCode = null,
                latencyMs = null,
                message = "Unexpected connection error on $baseUrl: ${e.message}",
                timestampMs = System.currentTimeMillis()
            )
        }
    }

    /**
     * Probes an ESP32-CAM stream or snapshot endpoint to validate video content-type and first frame (Default D6).
     * Returns success only if the response content-type is an image or MJPEG stream with non-zero frame bytes.
     */
    suspend fun validateCameraStream(
        streamUrl: String,
        timeoutMs: Long = 3000L
    ): Result<String> {
        return try {
            val response = httpClient.get(
                url = streamUrl,
                headers = emptyMap(),
                timeoutMs = timeoutMs
            )
            val contentType = response.headers["Content-Type"]?.firstOrNull()
                ?: response.headers["content-type"]?.firstOrNull() ?: ""
            val isValidVideoType = contentType.contains("multipart/x-mixed-replace", ignoreCase = true) ||
                contentType.contains("image/jpeg", ignoreCase = true) ||
                contentType.contains("image/jpg", ignoreCase = true)

            if (response.isSuccessful && isValidVideoType && (response.body?.length ?: 0) > 0) {
                Result.success("Live Video Validated: $contentType (${response.latencyMs}ms)")
            } else if (response.isSuccessful) {
                Result.failure(
                    RoverNetworkException("HTTP 200 OK received but response is not a valid video stream: content-type='$contentType'")
                )
            } else {
                Result.failure(
                    RoverNetworkException("Camera endpoint returned HTTP ${response.statusCode}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the capabilities manifest via GET /api/v1/capabilities.
     */
    suspend fun fetchCapabilities(
        baseUrl: String,
        authToken: String? = null,
        timeoutMs: Long = 3000L
    ): Result<RoverCapabilities> {
        return try {
            val headers = makeHeaders(authToken)
            val response = httpClient.get(
                url = "$baseUrl/api/v1/capabilities",
                headers = headers,
                timeoutMs = timeoutMs
            )
            if (!response.isSuccessful) {
                return Result.failure(
                    RoverNetworkException("HTTP ${response.statusCode} fetching capabilities: ${response.body}")
                )
            }
            val body = response.body ?: throw RoverNetworkException("Empty capabilities response body")
            val caps = ProtocolMessageParser.parseCapabilities(body)
            Result.success(caps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ingests a telemetry packet via GET /api/v1/telemetry.
     */
    suspend fun fetchTelemetry(
        baseUrl: String,
        authToken: String? = null,
        timeoutMs: Long = 2000L
    ): Result<RoverTelemetryPacket> {
        return try {
            val headers = makeHeaders(authToken)
            val response = httpClient.get(
                url = "$baseUrl/api/v1/telemetry",
                headers = headers,
                timeoutMs = timeoutMs
            )
            if (!response.isSuccessful) {
                return Result.failure(
                    RoverNetworkException("HTTP ${response.statusCode} fetching telemetry: ${response.body}")
                )
            }
            val body = response.body ?: throw RoverNetworkException("Empty telemetry response body")
            val packet = ProtocolMessageParser.parseTelemetry(body)
            Result.success(packet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dispatches a command to the rover controller via POST /api/v1/command.
     * Enforces fail-closed behavior if no bearer token is provisioned in Live mode (Default D9).
     */
    suspend fun dispatchCommand(
        baseUrl: String,
        command: RoverCommand,
        authToken: String? = null,
        timeoutMs: Long = 2000L
    ): Result<CommandAcknowledgement> {
        if (authToken.isNullOrBlank()) {
            return Result.failure(
                RoverAuthenticationException("Actionable Configuration Error: Bearer token is not provisioned. Configure X-AgriRover-Token in Settings > Rover Connection.")
            )
        }

        return try {
            val headers = makeHeaders(authToken)
            val body = ProtocolMessageParser.serializeCommand(command)
            val response = httpClient.post(
                url = "$baseUrl/api/v1/command",
                body = body,
                headers = headers,
                timeoutMs = timeoutMs
            )
            val responseBody = response.body
                ?: throw RoverNetworkException("Empty command acknowledgement response body")
            val ack = ProtocolMessageParser.parseAcknowledgement(responseBody)
            Result.success(ack)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dispatches an Emergency Stop command via POST /api/v1/emergency-stop.
     * Unconditional safety transition: stop executes regardless of whether authToken is provided.
     */
    suspend fun dispatchEmergencyStop(
        baseUrl: String,
        authToken: String? = null,
        timeoutMs: Long = 2000L
    ): Result<CommandAcknowledgement> {
        val estopCommand = RoverCommand(
            cmdId = "estop-${System.currentTimeMillis()}",
            sequence = System.currentTimeMillis(),
            commandType = "EMERGENCY_STOP",
            ttlMs = 5000L,
            createdTimestampMs = System.currentTimeMillis(),
            priority = CommandPriority.CRITICAL,
            parameters = mapOf("reason" to "APP_EMERGENCY_STOP")
        )

        return try {
            val headers = makeHeaders(authToken)
            val body = ProtocolMessageParser.serializeCommand(estopCommand)
            val response = httpClient.post(
                url = "$baseUrl/api/v1/emergency-stop",
                body = body,
                headers = headers,
                timeoutMs = timeoutMs
            )
            val responseBody = response.body
                ?: throw RoverNetworkException("Empty emergency stop response body")
            val ack = ProtocolMessageParser.parseAcknowledgement(responseBody)
            Result.success(ack)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dispatches an Emergency Stop Reset request via POST /api/v1/emergency-stop/reset.
     * Strictly requires authentication (fails closed if token is blank).
     */
    suspend fun dispatchEmergencyStopReset(
        baseUrl: String,
        authToken: String,
        timeoutMs: Long = 2000L
    ): Result<CommandAcknowledgement> {
        if (authToken.isBlank()) {
            return Result.failure(
                RoverAuthenticationException("X-AgriRover-Token is required to reset Emergency Stop.")
            )
        }

        return try {
            val headers = makeHeaders(authToken)
            val response = httpClient.post(
                url = "$baseUrl/api/v1/emergency-stop/reset",
                body = "{}",
                headers = headers,
                timeoutMs = timeoutMs
            )
            val responseBody = response.body
                ?: throw RoverNetworkException("Empty emergency stop reset response body")
            val ack = ProtocolMessageParser.parseAcknowledgement(responseBody)
            Result.success(ack)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun makeHeaders(token: String?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            map[ProtocolConstants.HEADER_AUTH_TOKEN] = token.trim()
        }
        return map
    }
}
