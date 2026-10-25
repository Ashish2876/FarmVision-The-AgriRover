package com.farmvision.agrirover.domain.protocol

/**
 * Protocol Constants for Microcontroller Communication (v1.0.0).
 */
object ProtocolConstants {
    const val PROTOCOL_VERSION = "1.0.0"
    const val MAX_PAYLOAD_BYTES = 4096
    const val DEFAULT_DRIVE_TTL_MS = 250L
    const val MAX_PUMP_DURATION_MS = 10000L // 10 seconds auto-cutoff
    const val HEADER_AUTH_TOKEN = "X-AgriRover-Token"
    const val DEFAULT_MAIN_ENDPOINT = "http://192.168.4.1:80"
    const val DEFAULT_CAMERA_ENDPOINT = "http://192.168.4.2:81"
}

/**
 * Structured protocol error codes defined in docs/hardware-protocol.md.
 */
enum class ProtocolErrorCode(
    val code: String,
    val httpStatus: Int,
    val isRecoverable: Boolean,
    val description: String
) {
    ERR_AUTH_MISSING(
        "ERR_AUTH_MISSING",
        401,
        true,
        "X-AgriRover-Token header was omitted."
    ),
    ERR_AUTH_INVALID(
        "ERR_AUTH_INVALID",
        401,
        true,
        "Provided bearer token does not match firmware secret."
    ),
    ERR_COMMAND_EXPIRED(
        "ERR_COMMAND_EXPIRED",
        408,
        true,
        "Command arrived after createdTimestampMs + ttlMs."
    ),
    ERR_COMMAND_REJECTED(
        "ERR_COMMAND_REJECTED",
        400,
        true,
        "Command syntax, parameter range, or type unrecognized."
    ),
    ERR_PAYLOAD_TOO_LARGE(
        "ERR_PAYLOAD_TOO_LARGE",
        413,
        true,
        "Payload exceeded MAX_PAYLOAD_BYTES (4096 bytes)."
    ),
    ERR_INVALID_PARAMETER(
        "ERR_INVALID_PARAMETER",
        422,
        true,
        "NaN, infinity, or out-of-range sensor/actuator value."
    ),
    ERR_ACTUATOR_DISABLED(
        "ERR_ACTUATOR_DISABLED",
        403,
        true,
        "Actuator commanded while in F0 stage or unconfirmed wiring."
    ),
    ERR_EMERGENCY_STOP_ACTIVE(
        "ERR_EMERGENCY_STOP_ACTIVE",
        423,
        true,
        "Motion command rejected because Emergency Stop is latched."
    ),
    ERR_DEVICE_BUSY(
        "ERR_DEVICE_BUSY",
        503,
        true,
        "ESP32 RTOS task queue full; throttle request rate."
    ),
    ERR_VERSION_MISMATCH(
        "ERR_VERSION_MISMATCH",
        426,
        false,
        "Incompatible protocol version; requires client update."
    );

    companion object {
        fun fromCode(code: String?): ProtocolErrorCode {
            if (code == null) return ERR_COMMAND_REJECTED
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: ERR_COMMAND_REJECTED
        }
    }
}

/**
 * Structured error response returned by firmware on HTTP 4xx / 5xx.
 */
data class ProtocolErrorResponse(
    val protocolVersion: String = ProtocolConstants.PROTOCOL_VERSION,
    val errorCode: ProtocolErrorCode,
    val message: String,
    val recoverable: Boolean,
    val timestampMs: Long
)

/**
 * Firmware enablement lifecycle stage.
 */
enum class FirmwareStage {
    F0, // Sensors only: Actuation disabled
    F1  // Sensors + Actuators enabled after physical wiring confirmation
}

/**
 * Sensor capability descriptor reported by GET /api/v1/capabilities.
 */
data class SensorCapability(
    val id: String,
    val type: String,
    val unit: String,
    val requiresCalibration: Boolean,
    val isAvailable: Boolean
)

/**
 * Actuator capability descriptor reported by GET /api/v1/capabilities.
 * Strict Honesty Invariant: Actuators without encoder or flow sensor hardware
 * MUST have [hasPhysicalFeedback] set to false.
 */
data class ActuatorCapability(
    val id: String,
    val type: String,
    val hasPhysicalFeedback: Boolean,
    val isEnabled: Boolean,
    val maxDurationMs: Long? = null
)

/**
 * Capabilities manifest returned by GET /api/v1/capabilities.
 */
data class RoverCapabilities(
    val protocolVersion: String,
    val deviceId: String,
    val firmwareVersion: String,
    val firmwareStage: FirmwareStage,
    val controllerTarget: String,
    val supportedSensors: List<SensorCapability>,
    val supportedActuators: List<ActuatorCapability>,
    val features: List<String>,
    val isEmergencyStopSupported: Boolean
) {
    /**
     * Checks if a sensor by ID is reported as supported and available.
     */
    fun isSensorAvailable(sensorId: String): Boolean =
        supportedSensors.any { it.id == sensorId && it.isAvailable }

    /**
     * Checks if an actuator by ID is supported and currently enabled.
     */
    fun isActuatorEnabled(actuatorId: String): Boolean =
        supportedActuators.any { it.id == actuatorId && it.isEnabled }
}

/**
 * Single sensor measurement in a telemetry packet.
 */
data class SensorReadingPayload(
    val value: Double,
    val rawValue: Double,
    val unit: String,
    val isValid: Boolean,
    val quality: String
)

/**
 * Full telemetry packet ingested via GET /api/v1/telemetry.
 */
data class RoverTelemetryPacket(
    val protocolVersion: String,
    val deviceId: String,
    val firmwareVersion: String,
    val sequenceNumber: Long,
    val uptimeMs: Long,
    val readings: Map<String, SensorReadingPayload>,
    val diagnostics: Map<String, String>
)

/**
 * Priority classification for dispatched commands.
 */
enum class CommandPriority {
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Command dispatch model sent via POST /api/v1/command or POST /api/v1/emergency-stop.
 */
data class RoverCommand(
    val protocolVersion: String = ProtocolConstants.PROTOCOL_VERSION,
    val cmdId: String,
    val sequence: Long,
    val commandType: String,
    val ttlMs: Long = ProtocolConstants.DEFAULT_DRIVE_TTL_MS,
    val createdTimestampMs: Long,
    val priority: CommandPriority = CommandPriority.NORMAL,
    val parameters: Map<String, Any?> = emptyMap()
)

/**
 * Status of command execution reported in acknowledgement.
 */
enum class CommandAckStatus {
    ACCEPTED,
    REJECTED,
    EXPIRED
}

/**
 * Acknowledgement response returned by firmware for a command dispatch.
 */
data class CommandAcknowledgement(
    val protocolVersion: String = ProtocolConstants.PROTOCOL_VERSION,
    val cmdId: String,
    val status: CommandAckStatus,
    val reasonCode: String? = null,
    val latencyMs: Long,
    val timestampMs: Long
) {
    val isAccepted: Boolean = status == CommandAckStatus.ACCEPTED
}
