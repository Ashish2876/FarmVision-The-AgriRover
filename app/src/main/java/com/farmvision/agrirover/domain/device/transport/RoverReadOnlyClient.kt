package com.farmvision.agrirover.domain.device.transport

import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot

/**
 * Strictly read-only communication client interface for rover queries.
 *
 * SAFETY INVARIANT:
 * This interface exposes ONLY read-only telemetry, capabilities, and status inspection methods.
 * It contains ZERO motor, actuator, steering, pump, relay, or command execution endpoints.
 * Command execution is isolated exclusively in the safety-interlocked RoverCommandPipeline.
 */
interface RoverReadOnlyClient {
    /**
     * Retrieve hardware capabilities and supported sensor declarations from /api/v1/capabilities.
     */
    suspend fun getCapabilities(): Result<DeviceCapabilities>

    /**
     * Retrieve live telemetry JSON payload from /api/v1/telemetry.
     */
    suspend fun getRawTelemetryJson(): Result<String>

    /**
     * Retrieve hardware health and link status from /api/v1/status.
     */
    suspend fun getStatus(): Result<DeviceStatusSnapshot>
}

