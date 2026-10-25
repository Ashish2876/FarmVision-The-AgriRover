package com.farmvision.agrirover.domain.protocol

/**
 * Result of validating a protocol payload or command packet.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errorCode: ProtocolErrorCode, val reason: String) : ValidationResult() {
        val isRecoverable: Boolean get() = errorCode.isRecoverable
    }

    val isValid: Boolean get() = this is Valid
}

/**
 * Validates protocol constraints, numerical limits, timing deadman, and safety states.
 * Adheres strictly to Section 5 of docs/hardware-protocol.md.
 */
object ProtocolValidator {

    private val ACTUATOR_COMMANDS = setOf(
        "DRIVE_DIFFERENTIAL",
        "SET_PUMP",
        "SERVO_POSITION",
        "ACTUATE_RELAY"
    )

    /**
     * Rejects payloads exceeding [ProtocolConstants.MAX_PAYLOAD_BYTES] (4096 bytes).
     */
    fun validatePayloadSize(rawJson: String): ValidationResult {
        val byteCount = rawJson.toByteArray(Charsets.UTF_8).size
        return if (byteCount > ProtocolConstants.MAX_PAYLOAD_BYTES) {
            ValidationResult.Invalid(
                errorCode = ProtocolErrorCode.ERR_PAYLOAD_TOO_LARGE,
                reason = "Payload size ($byteCount bytes) exceeds maximum allowable limit (${ProtocolConstants.MAX_PAYLOAD_BYTES} bytes)"
            )
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Rejects payloads containing unquoted or literal NaN, Infinity, or -Infinity.
     */
    fun validateNumericSanity(rawJson: String): ValidationResult {
        val regex = Regex("""(?i):\s*(-?infinity|nan)\b""")
        if (regex.containsMatchIn(rawJson)) {
            return ValidationResult.Invalid(
                errorCode = ProtocolErrorCode.ERR_INVALID_PARAMETER,
                reason = "Payload contains non-finite numerical token (NaN or Infinity)"
            )
        }
        return ValidationResult.Valid
    }

    /**
     * Validates protocol version matching [ProtocolConstants.PROTOCOL_VERSION].
     */
    fun validateProtocolVersion(version: String?): ValidationResult {
        return if (version != ProtocolConstants.PROTOCOL_VERSION) {
            ValidationResult.Invalid(
                errorCode = ProtocolErrorCode.ERR_VERSION_MISMATCH,
                reason = "Protocol version '$version' does not match required version '${ProtocolConstants.PROTOCOL_VERSION}'"
            )
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Validates an outgoing or incoming command against TTL expiry, E-stop, firmware stage, and parameter limits.
     */
    fun validateCommand(
        command: RoverCommand,
        currentClockMs: Long,
        isEmergencyStopLatched: Boolean = false,
        firmwareStage: FirmwareStage = FirmwareStage.F0
    ): ValidationResult {
        // 1. Version check
        val versionCheck = validateProtocolVersion(command.protocolVersion)
        if (!versionCheck.isValid) return versionCheck

        // 2. TTL Expiry check
        val expirationTime = command.createdTimestampMs + command.ttlMs
        if (currentClockMs > expirationTime) {
            val delta = currentClockMs - expirationTime
            return ValidationResult.Invalid(
                errorCode = ProtocolErrorCode.ERR_COMMAND_EXPIRED,
                reason = "Command TTL exceeded by ${delta}ms (Created: ${command.createdTimestampMs}, TTL: ${command.ttlMs}ms, Now: $currentClockMs)"
            )
        }

        // 3. Emergency Stop Guard
        if (isEmergencyStopLatched &&
            command.commandType != "EMERGENCY_STOP" &&
            command.commandType != "RESET_ESTOP"
        ) {
            return ValidationResult.Invalid(
                errorCode = ProtocolErrorCode.ERR_EMERGENCY_STOP_ACTIVE,
                reason = "Command rejected: Emergency Stop is currently latched active."
            )
        }

        // 4. Firmware Stage Actuator Guard (F0 sensors-only firmware cannot execute actuation)
        if (firmwareStage == FirmwareStage.F0 && ACTUATOR_COMMANDS.contains(command.commandType)) {
            return ValidationResult.Invalid(
                errorCode = ProtocolErrorCode.ERR_ACTUATOR_DISABLED,
                reason = "Command rejected: Actuators are disabled in Stage F0 firmware until wiring is confirmed."
            )
        }

        // 5. Parameter Validation by Command Type
        when (command.commandType) {
            "DRIVE_DIFFERENTIAL" -> {
                val throttle = (command.parameters["throttle"] as? Number)?.toDouble()
                val steering = (command.parameters["steering"] as? Number)?.toDouble()

                if (throttle == null || throttle.isNaN() || throttle < -1.0 || throttle > 1.0) {
                    return ValidationResult.Invalid(
                        errorCode = ProtocolErrorCode.ERR_INVALID_PARAMETER,
                        reason = "Throttle parameter must be a valid float in range [-1.0, 1.0], found: $throttle"
                    )
                }
                if (steering == null || steering.isNaN() || steering < -1.0 || steering > 1.0) {
                    return ValidationResult.Invalid(
                        errorCode = ProtocolErrorCode.ERR_INVALID_PARAMETER,
                        reason = "Steering parameter must be a valid float in range [-1.0, 1.0], found: $steering"
                    )
                }
            }

            "SET_PUMP" -> {
                val durationMs = (command.parameters["durationMs"] as? Number)?.toLong()
                if (durationMs != null && (durationMs <= 0 || durationMs > ProtocolConstants.MAX_PUMP_DURATION_MS)) {
                    return ValidationResult.Invalid(
                        errorCode = ProtocolErrorCode.ERR_INVALID_PARAMETER,
                        reason = "Pump duration must be between 1ms and ${ProtocolConstants.MAX_PUMP_DURATION_MS}ms, found: $durationMs"
                    )
                }
            }

            "SERVO_POSITION" -> {
                val angle = (command.parameters["angle"] as? Number)?.toDouble()
                if (angle == null || angle.isNaN() || angle < 0.0 || angle > 180.0) {
                    return ValidationResult.Invalid(
                        errorCode = ProtocolErrorCode.ERR_INVALID_PARAMETER,
                        reason = "Servo angle parameter must be between 0.0 and 180.0 degrees, found: $angle"
                    )
                }
            }
        }

        return ValidationResult.Valid
    }

    /**
     * Checks if a new sequence number strictly exceeds the last seen sequence number.
     */
    fun isMonotonicSequence(newSeq: Long, lastSeq: Long): Boolean = newSeq > lastSeq
}
