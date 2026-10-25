package com.farmvision.agrirover

import com.farmvision.agrirover.domain.protocol.ActuatorCapability
import com.farmvision.agrirover.domain.protocol.CommandAckStatus
import com.farmvision.agrirover.domain.protocol.CommandAcknowledgement
import com.farmvision.agrirover.domain.protocol.CommandPriority
import com.farmvision.agrirover.domain.protocol.FirmwareStage
import com.farmvision.agrirover.domain.protocol.ProtocolConstants
import com.farmvision.agrirover.domain.protocol.ProtocolErrorCode
import com.farmvision.agrirover.domain.protocol.ProtocolErrorResponse
import com.farmvision.agrirover.domain.protocol.ProtocolMessageParser
import com.farmvision.agrirover.domain.protocol.ProtocolValidator
import com.farmvision.agrirover.domain.protocol.RoverCapabilities
import com.farmvision.agrirover.domain.protocol.RoverCommand
import com.farmvision.agrirover.domain.protocol.SensorCapability
import com.farmvision.agrirover.domain.protocol.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Milestone H3 Comprehensive Unit Test Suite.
 * Validates protocol specification adherence, contract fixture compatibility,
 * parser/serializer roundtrips, and strict safety validation invariants.
 */
class HardwareProtocolH3UnitTest {

    private fun loadFixture(fileName: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/$fileName")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/$fileName")
            ?: throw IllegalStateException("Fixture fixture not found: protocol_fixtures/$fileName")
        return stream.bufferedReader().use { it.readText() }
    }

    // =========================================================================
    // 1. Contract Fixtures Ingestion Tests
    // =========================================================================

    @Test
    fun parseCapabilities_F0_SensorsOnly() {
        val json = loadFixture("capabilities_response_f0.json")
        val caps = ProtocolMessageParser.parseCapabilities(json)

        assertEquals(ProtocolConstants.PROTOCOL_VERSION, caps.protocolVersion)
        assertEquals("agrirover-main-esp32", caps.deviceId)
        assertEquals(FirmwareStage.F0, caps.firmwareStage)
        assertEquals("Esp32DevKitV1", caps.controllerTarget)
        assertEquals(4, caps.supportedSensors.size)
        assertEquals(2, caps.supportedActuators.size)

        // Sensors
        assertTrue(caps.isSensorAvailable("hw-sensor-soil-moisture-probes"))
        assertTrue(caps.isSensorAvailable("hw-sensor-dht22"))
        assertTrue(caps.isSensorAvailable("hw-sensor-soil-ph"))
        assertTrue(caps.isSensorAvailable("hw-sensor-ultrasonic"))

        // Actuators disabled in F0
        assertFalse(caps.isActuatorEnabled("hw-motors-bo-geared"))
        assertFalse(caps.isActuatorEnabled("hw-irrigation-pump"))

        // Strict Honesty Invariant
        caps.supportedActuators.forEach { actuator ->
            assertFalse(
                "Actuator ${actuator.id} cannot claim physical closed-loop feedback",
                actuator.hasPhysicalFeedback
            )
        }

        assertTrue(caps.isEmergencyStopSupported)
        assertTrue(caps.features.contains("TELEMETRY_STREAM"))
        assertTrue(caps.features.contains("EMERGENCY_STOP"))
        assertFalse(caps.features.contains("DRIVE_CONTROL"))
    }

    @Test
    fun parseCapabilities_F1_ActuatorsEnabled() {
        val json = loadFixture("capabilities_response_f1.json")
        val caps = ProtocolMessageParser.parseCapabilities(json)

        assertEquals(FirmwareStage.F1, caps.firmwareStage)
        assertEquals(3, caps.supportedActuators.size)

        // In F1, verified actuators are enabled
        assertTrue(caps.isActuatorEnabled("hw-motors-bo-geared"))
        assertTrue(caps.isActuatorEnabled("hw-irrigation-pump"))
        assertTrue(caps.isActuatorEnabled("hw-servo-camera-tilt"))

        // Physical Honesty Rule still holds
        caps.supportedActuators.forEach { actuator ->
            assertFalse(
                "Actuator ${actuator.id} must not claim physical feedback",
                actuator.hasPhysicalFeedback
            )
        }

        assertTrue(caps.features.contains("DRIVE_CONTROL"))
        assertTrue(caps.features.contains("IRRIGATION_CONTROL"))
        assertTrue(caps.features.contains("SERVO_CONTROL"))
    }

    @Test
    fun parseTelemetry_ValidPacket() {
        val json = loadFixture("telemetry_packet_valid.json")
        val packet = ProtocolMessageParser.parseTelemetry(json)

        assertEquals(ProtocolConstants.PROTOCOL_VERSION, packet.protocolVersion)
        assertEquals("agrirover-main-esp32", packet.deviceId)
        assertEquals(1042L, packet.sequenceNumber)
        assertEquals(348500L, packet.uptimeMs)

        assertEquals(5, packet.readings.size)

        val moisture = packet.readings["hw-sensor-soil-moisture-probes"]
        assertNotNull(moisture)
        assertEquals(42.5, moisture!!.value, 0.001)
        assertEquals(2150.0, moisture.rawValue, 0.001)
        assertEquals("%", moisture.unit)
        assertTrue(moisture.isValid)
        assertEquals("OK", moisture.quality)

        val moisture2 = packet.readings["hw-sensor-soil-moisture-probe-2"]
        assertNotNull(moisture2)
        assertEquals(2200.0, moisture2!!.value, 0.001)
        assertEquals(2200.0, moisture2.rawValue, 0.001)
        assertEquals("counts", moisture2.unit)
        assertTrue(moisture2.isValid)
        assertEquals("RAW_UNCALIBRATED", moisture2.quality)

        val dht = packet.readings["hw-sensor-dht22"]
        assertNotNull(dht)
        assertEquals(26.8, dht!!.value, 0.001)
        assertEquals("°C", dht.unit)

        val ph = packet.readings["hw-sensor-soil-ph"]
        assertNotNull(ph)
        assertEquals(6.8, ph!!.value, 0.001)
        assertEquals("pH", ph.unit)

        val ultrasonic = packet.readings["hw-sensor-ultrasonic"]
        assertNotNull(ultrasonic)
        assertEquals(45.0, ultrasonic!!.value, 0.001)
        assertEquals("cm", ultrasonic.unit)

        assertEquals("142880", packet.diagnostics["freeHeapBytes"])
        assertEquals("-54", packet.diagnostics["wifiRssiDbm"])
    }

    @Test
    fun parseTelemetry_NaN_RejectedBySanitizer() {
        val json = loadFixture("telemetry_packet_nan_invalid.json")
        try {
            ProtocolMessageParser.parseTelemetry(json)
            fail("Expected parseTelemetry to reject NaN numeric value")
        } catch (ex: IllegalArgumentException) {
            assertTrue(ex.message?.contains(ProtocolErrorCode.ERR_INVALID_PARAMETER.code) == true)
        }
    }

    @Test
    fun parseCommand_DriveValid() {
        val json = loadFixture("command_drive_valid.json")
        val cmd = ProtocolMessageParser.parseCommand(json)

        assertEquals("cmd-170000-001", cmd.cmdId)
        assertEquals(501L, cmd.sequence)
        assertEquals("DRIVE_DIFFERENTIAL", cmd.commandType)
        assertEquals(250L, cmd.ttlMs)
        assertEquals(1700000000000L, cmd.createdTimestampMs)
        assertEquals(CommandPriority.NORMAL, cmd.priority)

        val throttle = (cmd.parameters["throttle"] as? Number)?.toDouble()
        val steering = (cmd.parameters["steering"] as? Number)?.toDouble()
        assertEquals(0.65, throttle ?: 0.0, 0.001)
        assertEquals(0.0, steering ?: 1.0, 0.001)
    }

    @Test
    fun parseCommand_EmergencyStop() {
        val json = loadFixture("command_emergency_stop.json")
        val cmd = ProtocolMessageParser.parseCommand(json)

        assertEquals("estop-170000-999", cmd.cmdId)
        assertEquals(502L, cmd.sequence)
        assertEquals("EMERGENCY_STOP", cmd.commandType)
        assertEquals(5000L, cmd.ttlMs)
        assertEquals(CommandPriority.CRITICAL, cmd.priority)
        assertEquals("OPERATOR_TRIGGERED", cmd.parameters["reason"])
    }

    @Test
    fun parseAcknowledgement_Accepted() {
        val json = loadFixture("command_ack_accepted.json")
        val ack = ProtocolMessageParser.parseAcknowledgement(json)

        assertEquals("cmd-170000-001", ack.cmdId)
        assertEquals(CommandAckStatus.ACCEPTED, ack.status)
        assertTrue(ack.isAccepted)
        assertEquals(8L, ack.latencyMs)
        assertEquals(1700000000008L, ack.timestampMs)
    }

    @Test
    fun parseAcknowledgement_Rejected() {
        val json = loadFixture("command_ack_rejected.json")
        val ack = ProtocolMessageParser.parseAcknowledgement(json)

        assertEquals("cmd-170000-002", ack.cmdId)
        assertEquals(CommandAckStatus.REJECTED, ack.status)
        assertFalse(ack.isAccepted)
        assertEquals("ERR_COMMAND_EXPIRED", ack.reasonCode)
        assertEquals(14L, ack.latencyMs)
    }

    @Test
    fun parseErrorResponse_Fixture() {
        val json = loadFixture("error_response.json")
        val err = ProtocolMessageParser.parseErrorResponse(json)

        assertEquals(ProtocolErrorCode.ERR_COMMAND_EXPIRED, err.errorCode)
        assertTrue(err.recoverable)
        assertTrue(err.message.contains("expired"))
    }

    // =========================================================================
    // 2. Protocol Safety & Invariant Validation Tests
    // =========================================================================

    @Test
    fun validatePayloadSize_UnderLimitPasses() {
        val small = """{"status":"OK"}"""
        val result = ProtocolValidator.validatePayloadSize(small)
        assertTrue(result.isValid)
    }

    @Test
    fun validatePayloadSize_ExceedingLimitFails() {
        val oversized = "{\"data\":\"" + "A".repeat(5000) + "\"}"
        val result = ProtocolValidator.validatePayloadSize(oversized)
        assertFalse(result.isValid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ProtocolErrorCode.ERR_PAYLOAD_TOO_LARGE, invalid.errorCode)
    }

    @Test
    fun validateNumericSanity_DetectsInfinitiesAndNaN() {
        val nanJson = """{"value": NaN}"""
        val infJson = """{"value": Infinity}"""
        val negInfJson = """{"value": -Infinity}"""
        val cleanJson = """{"value": 12.345, "negative": -42}"""

        assertFalse(ProtocolValidator.validateNumericSanity(nanJson).isValid)
        assertFalse(ProtocolValidator.validateNumericSanity(infJson).isValid)
        assertFalse(ProtocolValidator.validateNumericSanity(negInfJson).isValid)
        assertTrue(ProtocolValidator.validateNumericSanity(cleanJson).isValid)
    }

    @Test
    fun validateProtocolVersion_EnforcesExactMatch() {
        assertTrue(ProtocolValidator.validateProtocolVersion("1.0.0").isValid)
        assertFalse(ProtocolValidator.validateProtocolVersion("2.0.0").isValid)
        assertFalse(ProtocolValidator.validateProtocolVersion("0.9.0").isValid)
        assertFalse(ProtocolValidator.validateProtocolVersion(null).isValid)
    }

    @Test
    fun validateCommand_TtlExpiryDeadman() {
        val cmd = RoverCommand(
            cmdId = "c-1",
            sequence = 1L,
            commandType = "DRIVE_DIFFERENTIAL",
            ttlMs = 250L,
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 0.5, "steering" to 0.0)
        )

        // Within TTL: now = 1200ms (1000 + 250 = 1250ms deadline)
        val validResult = ProtocolValidator.validateCommand(
            command = cmd,
            currentClockMs = 1200L,
            isEmergencyStopLatched = false,
            firmwareStage = FirmwareStage.F1
        )
        assertTrue(validResult.isValid)

        // Expired: now = 1260ms (> 1250ms)
        val expiredResult = ProtocolValidator.validateCommand(
            command = cmd,
            currentClockMs = 1260L,
            isEmergencyStopLatched = false,
            firmwareStage = FirmwareStage.F1
        )
        assertFalse(expiredResult.isValid)
        assertEquals(ProtocolErrorCode.ERR_COMMAND_EXPIRED, (expiredResult as ValidationResult.Invalid).errorCode)
    }

    @Test
    fun validateCommand_EmergencyStopLatched_RejectsMotion() {
        val motionCmd = RoverCommand(
            cmdId = "c-2",
            sequence = 2L,
            commandType = "DRIVE_DIFFERENTIAL",
            ttlMs = 250L,
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 0.5, "steering" to 0.0)
        )

        val rejected = ProtocolValidator.validateCommand(
            command = motionCmd,
            currentClockMs = 1050L,
            isEmergencyStopLatched = true, // E-Stop Active!
            firmwareStage = FirmwareStage.F1
        )
        assertFalse(rejected.isValid)
        assertEquals(ProtocolErrorCode.ERR_EMERGENCY_STOP_ACTIVE, (rejected as ValidationResult.Invalid).errorCode)

        // But E-STOP itself and RESET_ESTOP are allowed through
        val estopCmd = RoverCommand(
            cmdId = "c-3",
            sequence = 3L,
            commandType = "EMERGENCY_STOP",
            createdTimestampMs = 1000L,
            ttlMs = 5000L
        )
        val estopAllowed = ProtocolValidator.validateCommand(
            command = estopCmd,
            currentClockMs = 1050L,
            isEmergencyStopLatched = true,
            firmwareStage = FirmwareStage.F1
        )
        assertTrue(estopAllowed.isValid)
    }

    @Test
    fun validateCommand_F0SensorsOnly_RejectsActuation() {
        val driveCmd = RoverCommand(
            cmdId = "c-4",
            sequence = 4L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 0.5, "steering" to 0.0)
        )

        val rejected = ProtocolValidator.validateCommand(
            command = driveCmd,
            currentClockMs = 1050L,
            isEmergencyStopLatched = false,
            firmwareStage = FirmwareStage.F0 // F0 Sensors only!
        )
        assertFalse(rejected.isValid)
        assertEquals(ProtocolErrorCode.ERR_ACTUATOR_DISABLED, (rejected as ValidationResult.Invalid).errorCode)
    }

    @Test
    fun validateCommand_DifferentialDriveParameterRanges() {
        val validDrive = RoverCommand(
            cmdId = "c-5",
            sequence = 5L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 1.0, "steering" to -1.0)
        )
        assertTrue(ProtocolValidator.validateCommand(validDrive, 1050L, firmwareStage = FirmwareStage.F1).isValid)

        val invalidThrottle = RoverCommand(
            cmdId = "c-6",
            sequence = 6L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 1.05, "steering" to 0.0)
        )
        assertFalse(ProtocolValidator.validateCommand(invalidThrottle, 1050L, firmwareStage = FirmwareStage.F1).isValid)

        val invalidSteering = RoverCommand(
            cmdId = "c-7",
            sequence = 7L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 0.5, "steering" to -1.2)
        )
        assertFalse(ProtocolValidator.validateCommand(invalidSteering, 1050L, firmwareStage = FirmwareStage.F1).isValid)
    }

    @Test
    fun validateCommand_PumpSafetyCutoffDuration() {
        val validPump = RoverCommand(
            cmdId = "c-8",
            sequence = 8L,
            commandType = "SET_PUMP",
            createdTimestampMs = 1000L,
            parameters = mapOf("durationMs" to 5000L)
        )
        assertTrue(ProtocolValidator.validateCommand(validPump, 1050L, firmwareStage = FirmwareStage.F1).isValid)

        val excessivePump = RoverCommand(
            cmdId = "c-9",
            sequence = 9L,
            commandType = "SET_PUMP",
            createdTimestampMs = 1000L,
            parameters = mapOf("durationMs" to 15000L) // Exceeds 10,000ms limit!
        )
        assertFalse(ProtocolValidator.validateCommand(excessivePump, 1050L, firmwareStage = FirmwareStage.F1).isValid)
    }

    @Test
    fun validateMonotonicSequence_StrictlyIncreasing() {
        assertTrue(ProtocolValidator.isMonotonicSequence(1043L, 1042L))
        assertFalse(ProtocolValidator.isMonotonicSequence(1042L, 1042L)) // Duplicate packet dropped
        assertFalse(ProtocolValidator.isMonotonicSequence(1040L, 1042L)) // Out-of-order packet dropped
    }

    // =========================================================================
    // 3. Roundtrip Serialization & Deserialization
    // =========================================================================

    @Test
    fun roundtrip_Command() {
        val original = RoverCommand(
            cmdId = "cmd-test-101",
            sequence = 42L,
            commandType = "DRIVE_DIFFERENTIAL",
            ttlMs = 250L,
            createdTimestampMs = 1700000010000L,
            priority = CommandPriority.HIGH,
            parameters = mapOf(
                "throttle" to 0.75,
                "steering" to -0.25,
                "driveMode" to "Sport"
            )
        )
        val serialized = ProtocolMessageParser.serializeCommand(original)
        val deserialized = ProtocolMessageParser.parseCommand(serialized)

        assertEquals(original.cmdId, deserialized.cmdId)
        assertEquals(original.sequence, deserialized.sequence)
        assertEquals(original.commandType, deserialized.commandType)
        assertEquals(original.ttlMs, deserialized.ttlMs)
        assertEquals(original.createdTimestampMs, deserialized.createdTimestampMs)
        assertEquals(original.priority, deserialized.priority)
        assertEquals(0.75, (deserialized.parameters["throttle"] as Number).toDouble(), 0.001)
        assertEquals(-0.25, (deserialized.parameters["steering"] as Number).toDouble(), 0.001)
        assertEquals("Sport", deserialized.parameters["driveMode"])
    }

    @Test
    fun roundtrip_Acknowledgement() {
        val original = CommandAcknowledgement(
            cmdId = "cmd-ack-202",
            status = CommandAckStatus.ACCEPTED,
            reasonCode = null,
            latencyMs = 12L,
            timestampMs = 1700000010012L
        )
        val serialized = ProtocolMessageParser.serializeAcknowledgement(original)
        val deserialized = ProtocolMessageParser.parseAcknowledgement(serialized)

        assertEquals(original.cmdId, deserialized.cmdId)
        assertEquals(original.status, deserialized.status)
        assertEquals(original.latencyMs, deserialized.latencyMs)
        assertEquals(original.timestampMs, deserialized.timestampMs)
    }

    @Test
    fun roundtrip_ErrorResponse() {
        val original = ProtocolErrorResponse(
            errorCode = ProtocolErrorCode.ERR_EMERGENCY_STOP_ACTIVE,
            message = "E-Stop engaged by operator",
            recoverable = true,
            timestampMs = 1700000010050L
        )
        val serialized = ProtocolMessageParser.serializeErrorResponse(original)
        val deserialized = ProtocolMessageParser.parseErrorResponse(serialized)

        assertEquals(original.errorCode, deserialized.errorCode)
        assertEquals(original.message, deserialized.message)
        assertEquals(original.recoverable, deserialized.recoverable)
        assertEquals(original.timestampMs, deserialized.timestampMs)
    }

    @Test
    fun roundtrip_Capabilities() {
        val original = RoverCapabilities(
            protocolVersion = "1.0.0",
            deviceId = "test-rover",
            firmwareVersion = "v1.0.0-f1",
            firmwareStage = FirmwareStage.F1,
            controllerTarget = "Esp32DevKitV1",
            supportedSensors = listOf(
                SensorCapability("s1", "SoilMoisture", "%", true, true)
            ),
            supportedActuators = listOf(
                ActuatorCapability("a1", "Motor", false, true, null)
            ),
            features = listOf("TELEMETRY", "DRIVE"),
            isEmergencyStopSupported = true
        )
        val serialized = ProtocolMessageParser.serializeCapabilities(original)
        val deserialized = ProtocolMessageParser.parseCapabilities(serialized)

        assertEquals(original.protocolVersion, deserialized.protocolVersion)
        assertEquals(original.deviceId, deserialized.deviceId)
        assertEquals(original.firmwareStage, deserialized.firmwareStage)
        assertEquals(1, deserialized.supportedSensors.size)
        assertEquals(1, deserialized.supportedActuators.size)
        assertFalse(deserialized.supportedActuators[0].hasPhysicalFeedback)
    }
}
