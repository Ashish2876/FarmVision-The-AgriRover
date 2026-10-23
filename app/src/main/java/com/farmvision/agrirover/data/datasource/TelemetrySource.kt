package com.farmvision.agrirover.data.datasource

import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import kotlinx.coroutines.flow.StateFlow

/**
 * Replaceable domain boundary producing rover telemetry and multi-sensor reading streams.
 * In Phase 4, implemented by [SimulatedTelemetrySource] (software simulation only).
 * Future Phase 11 hardware implementations (Bluetooth, Wi-Fi, MQTT) implement this interface
 * without requiring any modifications to UI screens or ViewModels.
 */
interface TelemetrySource {
    /** Reactive stream of overall rover telemetry. */
    val telemetryFlow: StateFlow<RoverTelemetry>

    /** Reactive stream of individual canonical sensor readings. */
    val sensorReadingsFlow: StateFlow<Map<SensorType, SensorReading>>

    /** Synchronous snapshot of current rover telemetry. */
    fun getTelemetry(): RoverTelemetry

    /** Triggers a refresh of simulated telemetry data. */
    fun refreshTelemetry()
}
