package com.farmvision.agrirover.data.device.transport

import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.TransportType

/**
 * Common bridge abstraction for rover communication transports.
 * Implemented by Direct ESP32 SoftAP and FastAPI Cloud Gateway transports.
 */
interface DeviceTransportBridge {
    val transportType: TransportType

    /**
     * Perform initial connection verification and retrieve device identity.
     */
    suspend fun connect(endpoint: String): Result<DeviceIdentity>

    /**
     * Terminate the connection.
     */
    suspend fun disconnect()

    /**
     * Fetch declared hardware capabilities.
     */
    suspend fun fetchCapabilities(): Result<DeviceCapabilities>

    /**
     * Fetch lossless telemetry payload.
     */
    suspend fun fetchTelemetry(): Result<FastApiTelemetryPayload>

    /**
     * Fetch link health and hardware status.
     */
    suspend fun fetchStatus(): Result<DeviceStatusSnapshot>
}

