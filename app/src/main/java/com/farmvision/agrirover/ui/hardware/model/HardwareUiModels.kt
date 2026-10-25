package com.farmvision.agrirover.ui.hardware.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ConfigurationStatus
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.HardwareCatalogSeed
import com.farmvision.agrirover.data.model.hardware.HardwareComponent
import com.farmvision.agrirover.domain.protocol.RoverTelemetryPacket

/**
 * Axis 1: Endpoint Connection Status (Per controller endpoint).
 * In H1/H2, live connection requires authenticated handshake; endpoints default to [Unknown], [Disconnected], or [NotConfigured].
 */
enum class EndpointConnectionStatus(
    val label: String,
    val icon: ImageVector,
    val isReachable: Boolean
) {
    Connecting("Connecting", Icons.Rounded.Sync, false),
    Connected("Connected", Icons.Rounded.CheckCircle, true),
    Disconnected("Disconnected", Icons.Rounded.Cancel, false),
    Stale("Connection Stale", Icons.Rounded.Warning, false),
    Timeout("Timeout", Icons.Rounded.CloudOff, false),
    Error("Error", Icons.Rounded.Error, false),
    Unknown("Unknown / Not Polled", Icons.AutoMirrored.Rounded.HelpOutline, false),
    NotConfigured("Not Configured", Icons.Rounded.Info, false)
}

/**
 * Axis 2: Telemetry Freshness (Per sensor reading).
 * Freshness determined using injected clock and configurable thresholds.
 */
enum class TelemetryFreshness(val label: String) {
    Fresh("Fresh (< 3s)"),
    Stale("Stale (3–10s)"),
    Unavailable("Unavailable (> 10s)"),
    Unknown("Unknown")
}

/**
 * Axis 3: Sensor / Diagnostic Health.
 */
enum class SensorHealthStatus(
    val label: String,
    val icon: ImageVector
) {
    Healthy("Healthy", Icons.Rounded.CheckCircle),
    Warning("Warning", Icons.Rounded.Warning),
    Fault("Fault / Error", Icons.Rounded.Error),
    Unavailable("Unavailable", Icons.Rounded.CloudOff),
    Uncalibrated("Uncalibrated", Icons.AutoMirrored.Rounded.HelpOutline),
    Unknown("Unknown", Icons.AutoMirrored.Rounded.HelpOutline)
}

/**
 * Axis 4: Actuator Command State.
 */
enum class ActuatorCommandState(val label: String) {
    Idle("Idle / Standby"),
    Commanded("Command Dispatched"),
    Acknowledged("Acknowledged by Firmware"),
    Rejected("Rejected by Firmware"),
    TimedOut("Command Timed Out"),
    Expired("Command Expired (TTL Exceeded)"),
    HaltedByEStop("Halted by Emergency Stop"),
    Unknown("Unknown")
}

/**
 * Axis 5: Physical Feedback State.
 * Mandatory honesty rule: Actuators without encoder, tachometer, or flow sensor
 * hardware MUST be marked [NotAvailable].
 */
enum class PhysicalFeedbackState(val label: String, val isAvailable: Boolean) {
    Available("Feedback Available", true),
    NotAvailable("No Physical Feedback Hardware", false)
}

/**
 * Axis 6: Configuration maturity lifecycle status.
 * Re-exported from domain: [ConfigurationStatus.NotConfigured], [ConfigurationStatus.Draft], [ConfigurationStatus.Verified].
 * RULE: Agent may never set [ConfigurationStatus.Verified] without physical user evidence.
 */

/**
 * Axis 7: Inventory Presence.
 * Components that are mechanical or passive appear with [Cataloged] presence and no connectivity implied.
 */
enum class InventoryPresence(val label: String) {
    Cataloged("Cataloged in Inventory")
}

/**
 * Operational mode of the FarmVision AgriRover app.
 */
enum class HardwareOperationMode(val label: String) {
    Demo("DEMO MODE — SIMULATED TELEMETRY"),
    Live("LIVE HARDWARE MODE")
}

/**
 * Diagnostic test result for a controller endpoint probe.
 */
data class ConnectionDiagnosticResult(
    val target: ControllerTarget,
    val success: Boolean,
    val statusCode: Int? = null,
    val latencyMs: Long? = null,
    val message: String,
    val timestampMs: Long
)

/**
 * Evaluates telemetry freshness based on an injected clock timestamp.
 */
fun evaluateTelemetryFreshness(
    readingTimestampMs: Long?,
    currentClockMs: Long,
    freshThresholdMs: Long = 3000L,
    staleThresholdMs: Long = 10000L
): TelemetryFreshness {
    if (readingTimestampMs == null || readingTimestampMs <= 0L) {
        return TelemetryFreshness.Unknown
    }
    val elapsed = currentClockMs - readingTimestampMs
    return when {
        elapsed < 0L -> TelemetryFreshness.Unknown
        elapsed <= freshThresholdMs -> TelemetryFreshness.Fresh
        elapsed <= staleThresholdMs -> TelemetryFreshness.Stale
        else -> TelemetryFreshness.Unavailable
    }
}

/**
 * Filter categories for the Hardware Center UI.
 */
enum class HardwareFilterCategory(val label: String) {
    All("All (23)"),
    Controllers("Controllers (2)"),
    Sensors("Sensors (4)"),
    Motion("Motion (5)"),
    Irrigation("Irrigation (2)"),
    Power("Power & System (3)"),
    PassiveInventory("Passive Inventory (7)")
}

/**
 * Summary counter model adhering strictly to Section 8 counting rules:
 * - Summary counts ONLY electronic devices that can report connectivity (13 total).
 * - Confirmed Connected requires an authenticated live heartbeat/telemetry.
 * - Disconnected requires endpoint known unreachable.
 * - Warning requires device-reported diagnostic warning or fault.
 * - Unknown encompasses all unpolled/unconfigured electronic items.
 * - Passive and mechanical items are excluded from electronic connection counts.
 * - Overall health score is omitted entirely.
 */
data class HardwareSummaryCounts(
    val confirmedConnected: Int,
    val disconnected: Int,
    val warningOrFault: Int,
    val unknownOrDraft: Int,
    val passiveInventory: Int
)

/**
 * Enriched UI presentation item wrapping a catalog [HardwareComponent].
 */
data class HardwareUiItem(
    val component: HardwareComponent,
    val isElectronic: Boolean,
    val endpointTarget: ControllerTarget?,
    val connectionStatus: EndpointConnectionStatus,
    val healthStatus: SensorHealthStatus,
    val actuatorState: ActuatorCommandState = ActuatorCommandState.Idle,
    val physicalFeedback: PhysicalFeedbackState = PhysicalFeedbackState.NotAvailable,
    val latestValueDisplay: String,
    val freshnessText: String,
    val draftPins: Map<String, Int?> = emptyMap(),
    val honestyDisclaimer: String? = null
)

/**
 * Factory and mapper object generating truthful UI items from the Section 4 inventory.
 */
object HardwareUiDataMapper {

    /**
     * Map catalog items to UI items based on operation mode.
     * In H1, live communication is absent; in Live mode all items show Unknown/NotConfigured.
     * In Demo mode, readings are explicitly marked as simulated demo values.
     */
    fun createUiItems(isDemoMode: Boolean): List<HardwareUiItem> {
        return HardwareCatalogSeed.INVENTORY_ITEMS.map { item ->
            val isElectronic = isElectronicComponent(item.id)
            val target = getControllerTarget(item.id)
            val draftPins = getDraftCandidatePins(item.id)
            val disclaimer = getHonestyDisclaimer(item.id)

            val connectionStatus = if (!isElectronic) {
                EndpointConnectionStatus.Unknown
            } else if (isDemoMode) {
                EndpointConnectionStatus.Unknown // Never claim Connected without real hardware evidence
            } else {
                EndpointConnectionStatus.Unknown
            }

            val healthStatus = if (!isElectronic) {
                SensorHealthStatus.Unavailable
            } else if (item.category == ComponentCategory.Sensor) {
                if (isDemoMode) SensorHealthStatus.Uncalibrated else SensorHealthStatus.Unavailable
            } else {
                SensorHealthStatus.Unavailable
            }

            val latestValue = if (isDemoMode) {
                getDemoValue(item.id)
            } else {
                "--"
            }

            val freshness = if (isDemoMode && isElectronic) {
                "Simulated (Demo)"
            } else {
                "No live telemetry"
            }

            HardwareUiItem(
                component = item,
                isElectronic = isElectronic,
                endpointTarget = target,
                connectionStatus = connectionStatus,
                healthStatus = healthStatus,
                actuatorState = ActuatorCommandState.Idle,
                physicalFeedback = PhysicalFeedbackState.NotAvailable,
                latestValueDisplay = latestValue,
                freshnessText = freshness,
                draftPins = draftPins,
                honestyDisclaimer = disclaimer
            )
        }
    }

    /**
     * Updates electronic sensor and controller UI items with live telemetry values.
     */
    fun updateWithLiveTelemetry(
        items: List<HardwareUiItem>,
        telemetry: RoverTelemetryPacket,
        currentClockMs: Long
    ): List<HardwareUiItem> {
        return items.map { item ->
            when (item.component.id) {
                "hw-controller-esp32" -> {
                    item.copy(
                        connectionStatus = EndpointConnectionStatus.Connected,
                        healthStatus = SensorHealthStatus.Healthy,
                        latestValueDisplay = "Uptime: ${telemetry.uptimeMs / 1000}s",
                        freshnessText = "Live (seq #${telemetry.sequenceNumber})"
                    )
                }
                "hw-sensor-soil-moisture-probes" -> {
                    val reading = telemetry.readings[item.component.id]
                    val reading2 = telemetry.readings["hw-sensor-soil-moisture-probe-2"]
                    if (reading != null) {
                        val p2Raw = reading2?.rawValue?.toInt() ?: 0
                        val p2Saturated = reading2 != null && (p2Raw <= 50 || p2Raw >= 4050)
                        val p2Text = if (p2Saturated) "$p2Raw counts (Saturated/Suspect)" else "$p2Raw counts (Raw)"
                        val display = if (reading2 != null) {
                            "P1: ${reading.value}% | P2: $p2Text"
                        } else {
                            "${reading.value}% (Raw: ${reading.rawValue.toInt()})"
                        }
                        val health = when {
                            !reading.isValid || (reading2 != null && !reading2.isValid) -> SensorHealthStatus.Fault
                            p2Saturated -> SensorHealthStatus.Warning
                            else -> SensorHealthStatus.Healthy
                        }
                        item.copy(
                            connectionStatus = EndpointConnectionStatus.Connected,
                            healthStatus = health,
                            latestValueDisplay = display,
                            freshnessText = "Live (Fresh)"
                        )
                    } else item
                }
                "hw-sensor-dht22" -> {
                    val reading = telemetry.readings[item.component.id]
                    if (reading != null) {
                        item.copy(
                            connectionStatus = EndpointConnectionStatus.Connected,
                            healthStatus = if (reading.isValid) SensorHealthStatus.Healthy else SensorHealthStatus.Fault,
                            latestValueDisplay = "${reading.value}°C",
                            freshnessText = "Live (Fresh)"
                        )
                    } else item
                }
                "hw-sensor-soil-ph" -> {
                    val reading = telemetry.readings[item.component.id]
                    if (reading != null) {
                        item.copy(
                            connectionStatus = EndpointConnectionStatus.Connected,
                            healthStatus = if (reading.isValid) SensorHealthStatus.Healthy else SensorHealthStatus.Fault,
                            latestValueDisplay = "${reading.value} pH",
                            freshnessText = "Live (Fresh)"
                        )
                    } else item
                }
                "hw-sensor-ultrasonic" -> {
                    val reading = telemetry.readings[item.component.id]
                    if (reading != null) {
                        item.copy(
                            connectionStatus = EndpointConnectionStatus.Connected,
                            healthStatus = if (reading.isValid) SensorHealthStatus.Healthy else SensorHealthStatus.Fault,
                            latestValueDisplay = "${reading.value} cm",
                            freshnessText = "Live (Fresh)"
                        )
                    } else item
                }
                else -> item
            }
        }
    }

    /**
     * Marks electronic items as disconnected when live endpoint connection fails.
     * Enforces typed failure states (Timeout, Error with specific reason, Disconnected).
     */
    fun markLiveTelemetryDisconnected(
        items: List<HardwareUiItem>,
        reason: String
    ): List<HardwareUiItem> {
        val status = when {
            reason.contains("timed out", ignoreCase = true) || reason.contains("timeout", ignoreCase = true) ->
                EndpointConnectionStatus.Timeout
            reason.contains("401", ignoreCase = true) || reason.contains("unauthorized", ignoreCase = true) ||
            reason.contains("auth", ignoreCase = true) || reason.contains("version", ignoreCase = true) ->
                EndpointConnectionStatus.Error
            else ->
                EndpointConnectionStatus.Disconnected
        }

        val freshness = when {
            reason.contains("timed out", ignoreCase = true) || reason.contains("timeout", ignoreCase = true) ->
                "Timeout"
            reason.contains("401", ignoreCase = true) || reason.contains("unauthorized", ignoreCase = true) || reason.contains("auth", ignoreCase = true) ->
                "Auth Error"
            reason.contains("version", ignoreCase = true) ->
                "Version Mismatch"
            else ->
                "Unreachable"
        }

        return items.map { item ->
            if (item.isElectronic) {
                item.copy(
                    connectionStatus = status,
                    healthStatus = SensorHealthStatus.Unavailable,
                    latestValueDisplay = "--",
                    freshnessText = freshness
                )
            } else {
                item
            }
        }
    }

    /**
     * Compute summary counts per Section 8 rules.
     */
    fun computeSummaryCounts(items: List<HardwareUiItem>): HardwareSummaryCounts {
        val electronicItems = items.filter { it.isElectronic }
        val passiveItems = items.filter { !it.isElectronic }

        val connected = electronicItems.count { it.connectionStatus == EndpointConnectionStatus.Connected }
        val disconnected = electronicItems.count {
            it.connectionStatus == EndpointConnectionStatus.Disconnected ||
            it.connectionStatus == EndpointConnectionStatus.Timeout ||
            it.connectionStatus == EndpointConnectionStatus.Error
        }
        val warning = electronicItems.count {
            it.healthStatus == SensorHealthStatus.Warning || it.healthStatus == SensorHealthStatus.Fault
        }
        val unknown = electronicItems.count {
            it.connectionStatus == EndpointConnectionStatus.Unknown ||
                it.connectionStatus == EndpointConnectionStatus.Connecting ||
                it.connectionStatus == EndpointConnectionStatus.NotConfigured ||
                it.connectionStatus == EndpointConnectionStatus.Stale
        }

        return HardwareSummaryCounts(
            confirmedConnected = connected,
            disconnected = disconnected,
            warningOrFault = warning,
            unknownOrDraft = unknown,
            passiveInventory = passiveItems.size
        )
    }

    /**
     * Categorize items as electronic vs passive.
     * Exactly 13 items are electronic/active; 10 items are passive/mechanical/inspection-only.
     */
    fun isElectronicComponent(componentId: String): Boolean {
        return when (componentId) {
            "hw-controller-esp32",
            "hw-camera-esp32cam",
            "hw-sensor-soil-moisture-probes",
            "hw-sensor-dht22",
            "hw-sensor-soil-ph",
            "hw-sensor-ultrasonic",
            "hw-driver-tb6612fng",
            "hw-motors-bo-geared",
            "hw-servo-camera",
            "hw-servo-soil-probe",
            "hw-servo-ultrasonic",
            "hw-irrigation-pump",
            "hw-irrigation-relay" -> true
            // Non-communicating / passive / programming accessory
            else -> false
        }
    }

    private fun getControllerTarget(componentId: String): ControllerTarget? {
        return when (componentId) {
            "hw-controller-esp32" -> ControllerTarget.Esp32DevKitV1
            "hw-camera-esp32cam" -> ControllerTarget.Esp32Cam
            "hw-tool-esp32cam-mb" -> null
            "hw-servo-camera" -> ControllerTarget.Esp32DevKitV1
            else -> ControllerTarget.Esp32DevKitV1
        }
    }

    /**
     * Candidate pin assignments (DRAFT / UNVERIFIED).
     * Marked strictly as draft for engineering review.
     */
    private fun getDraftCandidatePins(componentId: String): Map<String, Int?> {
        return when (componentId) {
            "hw-sensor-soil-moisture-probes" -> mapOf(
                "Probe 1 ADC (ADC1)" to 32,
                "Probe 2 ADC (ADC1)" to 34
            )
            "hw-sensor-dht22" -> mapOf(
                "Data Line" to 4
            )
            "hw-sensor-soil-ph" -> mapOf(
                "Analog In (ADC1)" to 35
            )
            "hw-sensor-ultrasonic" -> mapOf(
                "Trigger Pin" to 13,
                "Echo Pin (Needs 5V Divider!)" to 14
            )
            "hw-driver-tb6612fng" -> mapOf(
                "PWMA (Speed Left)" to 25,
                "AIN1 (Dir Left 1)" to 26,
                "AIN2 (Dir Left 2)" to 27,
                "PWMB (Speed Right)" to 16,
                "BIN1 (Dir Right 1)" to 17,
                "BIN2 (Dir Right 2)" to 18,
                "STBY (Standby)" to 19
            )
            "hw-servo-camera" -> mapOf("PWM Control" to 21)
            "hw-servo-soil-probe" -> mapOf("PWM Control" to 22)
            "hw-servo-ultrasonic" -> mapOf("PWM Control" to 23)
            "hw-irrigation-relay" -> mapOf(
                "Ch 1 (Pump Trigger)" to 5,
                "Ch 2 (Aux / Spare)" to null
            )
            "hw-irrigation-pump" -> mapOf("Switched Rail" to null)
            else -> emptyMap()
        }
    }

    /**
     * Explicit "What this device cannot report" honesty disclaimers.
     */
    private fun getHonestyDisclaimer(componentId: String): String? {
        return when (componentId) {
            "hw-irrigation-pump", "hw-irrigation-relay", "hw-irrigation-tubing", "hw-irrigation-reservoir" ->
                "NO FLOW SENSOR IN INVENTORY: Actuation of the pump relay confirms electrical command only. The system CANNOT detect whether water is physically flowing, whether lines are clogged, or reservoir fluid level."
            "hw-motors-bo-geared", "hw-driver-tb6612fng" ->
                "NO ENCODERS OR CURRENT SENSORS: Motor firmware acknowledgement confirms signal transmission only. Measured wheel rotation, speed, wheel slip, and distance traveled cannot be verified."
            "hw-servo-camera", "hw-servo-soil-probe", "hw-servo-ultrasonic" ->
                "NO SERVO POSITION FEEDBACK: Standard 3-pin hobby servos accept commanded angle via PWM but have no feedback line. Commanded angle is shown; actual physical orientation cannot be confirmed."
            "hw-power-battery", "hw-power-buck-converter" ->
                "NO BATTERY VOLTAGE SENSING: The inventory includes no voltage divider or current shunt. Battery voltage and capacity are unavailable in live mode and will show as Unavailable."
            "hw-sensor-dht22" ->
                "ATMOSPHERIC SENSOR ONLY: The DHT22 measures ambient air temperature and humidity. It does NOT measure soil temperature or solar radiation."
            "hw-sensor-soil-ph" ->
                "BUFFER CALIBRATION REQUIRED: Raw analog millivolts cannot indicate agronomic pH without two-point calibration using standard buffer solutions (pH 4.01 / 6.86)."
            "hw-sensor-soil-moisture-probes" ->
                "TWO-POINT CALIBRATION REQUIRED: Capacitive probes produce uncalibrated raw analog counts until dry-air and wet-submersion calibration is performed per probe."
            "hw-sensor-ultrasonic" ->
                "UNVERIFIED LEVEL SHIFTING: Standard HC-SR04 Echo line outputs 5.0V. Operating without a resistive voltage divider risks permanent damage to ESP32 GPIO."
            "hw-tool-esp32cam-mb" ->
                "FLASHING TOOL ONLY: Micro-USB daughterboard used exclusively for flashing the ESP32-CAM. Not mounted on the rover during field runtime."
            else -> null
        }
    }

    private fun getDemoValue(componentId: String): String {
        return when (componentId) {
            "hw-sensor-soil-moisture-probes" -> "42.5 % (P1: 44%, P2: 40%)"
            "hw-sensor-dht22" -> "26.8 °C • 62.3 % RH"
            "hw-sensor-soil-ph" -> "6.8 pH (Simulated)"
            "hw-sensor-ultrasonic" -> "45.0 cm (Simulated)"
            "hw-motors-bo-geared" -> "0.0 km/h (Stopped)"
            "hw-servo-camera" -> "0° (Neutral Tilt)"
            "hw-servo-soil-probe" -> "0° (Retracted)"
            "hw-servo-ultrasonic" -> "90° (Centered)"
            "hw-irrigation-pump" -> "OFF (Idle)"
            "hw-irrigation-relay" -> "Ch1: OFF • Ch2: OFF"
            "hw-controller-esp32" -> "v1.0.0-draft (Simulated)"
            "hw-camera-esp32cam" -> "OV2640 Standby"
            else -> "Cataloged"
        }
    }
}
