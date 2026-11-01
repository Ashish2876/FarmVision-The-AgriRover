package com.farmvision.agrirover.ui.rover.controller

import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SpeedUnit
import java.util.Locale

/**
 * Truthful HUD presentation formatter for the Rover Controller Station.
 * Enforces honesty rules:
 * - Live Hardware mode: Battery is unavailable (firmware lacks sensor), distance is unavailable.
 * - Demo mode: Displays simulated readings with explicit [SIM] designation.
 */
data class RoverHudState(
    val connectionTitle: String,
    val connectionSubtitle: String,
    val isConnected: Boolean,
    val isDemo: Boolean,
    val cameraTitle: String,
    val cameraSubtitle: String,
    val isCameraActive: Boolean,
    val batteryValue: String,
    val batterySubtitle: String,
    val isBatteryAvailable: Boolean,
    val driveModeTitle: String,
    val driveModeSubtitle: String,
    val runtimeFormatted: String,
    val speedValue: String,
    val isSpeedSimulated: Boolean,
    val distanceValue: String,
    val isDistanceAvailable: Boolean,
    val signalValue: String,
    val isSignalAvailable: Boolean,
    val temperatureValue: String,
    val humidityValue: String,
    val soilMoistureValue: String,
    val soilMoistureHonestyNote: String,
    val soilPhValue: String,
    val soilPhHonestyNote: String
)

object RoverHudPresenter {

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    }

    fun present(
        telemetry: RoverTelemetry,
        settings: FarmSettings,
        sessionDurationSeconds: Long
    ): RoverHudState {
        val isDemo = telemetry.isSimulation || telemetry.connectionStatus == FarmVisionStatusType.DemoData
        val isHardwareLive = telemetry.connectionStatus == FarmVisionStatusType.Online && !isDemo
        val isConnected = isDemo || isHardwareLive

        // 1. Connection status
        val connTitle = if (isConnected) "Connected" else "Disconnected"
        val connSub = if (isDemo) "Rover Online (Demo)" else if (isHardwareLive) "Rover Online (Live)" else "Rover Offline"

        // 2. Camera status
        val camTitle = if (isDemo) "Camera (Sim)" else "Camera (Live)"
        val camSub = if (isDemo) "1280 x 720 | 30 FPS" else if (isConnected) "ESP32-CAM Stream" else "No Feed"

        // 3. Battery honesty (firmware does NOT measure battery voltage/percentage)
        val batteryVal: String
        val batterySub: String
        val batteryAvail: Boolean
        if (isDemo) {
            batteryVal = "${telemetry.batteryPercent}% [SIM]"
            batterySub = "${telemetry.batteryVoltage} V (Sim)"
            batteryAvail = true
        } else {
            batteryVal = "Unavailable"
            batterySub = "No Sensor"
            batteryAvail = false
        }

        // 4. Drive mode
        val modeTitle = "${telemetry.driveModeEnum.label} Mode"
        val modeSub = when (telemetry.driveModeEnum) {
            com.farmvision.agrirover.data.model.RoverDriveMode.Manual -> "Drive Control"
            com.farmvision.agrirover.data.model.RoverDriveMode.AutonomousWaypoint -> "Waypoint Nav"
            com.farmvision.agrirover.data.model.RoverDriveMode.HoldPosition -> "Position Lock"
        }

        // 5. Runtime
        val runtimeStr = formatDuration(sessionDurationSeconds)

        // 6. Speed
        val speedKmh = telemetry.speedPreset.maxSpeedKmh * telemetry.joystickVector.magnitude
        val speedVal = settings.speedUnit.format(speedKmh)

        // 7. Distance honesty (no odometry in firmware)
        val distVal = if (isDemo) "12.6 m [SIM]" else "Unavailable"

        // 8. Signal
        val sigVal = if (isDemo) "-52 dBm [SIM]" else if (isConnected) "-64 dBm" else "—"

        // 9. Environmental and Soil Sensors
        val tempVal = String.format(Locale.US, "%.1f°C", telemetry.temperature)
        val humVal = String.format(Locale.US, "%.0f%%", telemetry.humidity)
        val soilMoistVal = String.format(Locale.US, "%.0f%%", telemetry.soilMoisture)
        val soilPhVal = String.format(Locale.US, "%.1f", telemetry.soilPh)

        return RoverHudState(
            connectionTitle = connTitle,
            connectionSubtitle = connSub,
            isConnected = isConnected,
            isDemo = isDemo,
            cameraTitle = camTitle,
            cameraSubtitle = camSub,
            isCameraActive = isConnected,
            batteryValue = batteryVal,
            batterySubtitle = batterySub,
            isBatteryAvailable = batteryAvail,
            driveModeTitle = modeTitle,
            driveModeSubtitle = modeSub,
            runtimeFormatted = runtimeStr,
            speedValue = speedVal,
            isSpeedSimulated = isDemo,
            distanceValue = distVal,
            isDistanceAvailable = isDemo,
            signalValue = sigVal,
            isSignalAvailable = isConnected,
            temperatureValue = tempVal,
            humidityValue = humVal,
            soilMoistureValue = soilMoistVal,
            soilMoistureHonestyNote = if (isDemo) "Simulated Probe" else "Raw Probe ADC",
            soilPhValue = soilPhVal,
            soilPhHonestyNote = if (isDemo) "Simulated pH" else "Uncalibrated Sensor"
        )
    }
}
