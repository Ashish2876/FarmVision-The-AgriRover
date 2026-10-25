package com.farmvision.agrirover

import com.farmvision.agrirover.domain.protocol.CommandAckStatus
import com.farmvision.agrirover.domain.protocol.FirmwareStage
import com.farmvision.agrirover.domain.protocol.ProtocolConstants
import com.farmvision.agrirover.domain.protocol.ProtocolErrorCode
import com.farmvision.agrirover.domain.protocol.ProtocolMessageParser
import com.farmvision.agrirover.domain.protocol.ProtocolValidator
import com.farmvision.agrirover.domain.protocol.RoverCommand
import com.farmvision.agrirover.domain.protocol.ValidationResult
import com.farmvision.agrirover.ui.hardware.model.HardwareUiDataMapper
import com.farmvision.agrirover.ui.hardware.model.PhysicalFeedbackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Milestone H6 Comprehensive Unit Test Suite.
 * Validates Stage F1 live actuation capabilities, actuator safety limits,
 * TB6612FNG differential drive kinematics, 10s pump auto-cutoff, E-Stop latching,
 * and physical hardware verification documentation.
 */
class HardwareActuationH6UnitTest {

    private fun resolveFile(relativePath: String): File {
        val f1 = File(relativePath)
        if (f1.exists()) return f1
        val f2 = File("../$relativePath")
        if (f2.exists()) return f2
        return f1
    }

    @Test
    fun verifyHardwareVerifiedDocumentation_ExistsWithSafetyInvariants() {
        val docFile = resolveFile("docs/hardware-verified.md")
        assertTrue("docs/hardware-verified.md must exist", docFile.exists())

        val content = docFile.readText()
        assertTrue("Must require wheels off ground during bench testing", content.contains("WHEELS OFF THE GROUND"))
        assertTrue("Must require physical mechanical emergency cutoff switch", content.contains("Mechanical Emergency Cutoff"))
        assertTrue("Must document HC-SR04 level shifting", content.contains("Logic Level Shifting"))
        assertTrue("Must document 250ms deadman failsafe", content.contains("250 ms Deadman Failsafe"))
        assertTrue("Must document 10s pump limit", content.contains("10,000 ms (10s)"))
        assertTrue("Must require WIRING CONFIRMED before F1 actuation", content.contains("WIRING CONFIRMED"))
    }

    @Test
    fun verifyActuatorManagerSourceFiles_Exist() {
        val header = resolveFile("firmware/esp32-main/include/actuator_manager.h")
        val source = resolveFile("firmware/esp32-main/src/actuator_manager.cpp")

        assertTrue("actuator_manager.h must exist", header.exists())
        assertTrue("actuator_manager.cpp must exist", source.exists())

        val sourceContent = source.readText()
        assertTrue("Must implement TB6612FNG differential drive", sourceContent.contains("setDrive"))
        assertTrue("Must implement 5% deadband", sourceContent.contains("0.05f"))
        assertTrue("Must implement slew rate limiter", sourceContent.contains("MOTOR_SLEW_RATE_LIMIT"))
        assertTrue("Must implement pump auto-cutoff timer", sourceContent.contains("updatePumpSafety"))
        assertTrue("Must implement emergencyCutoff", sourceContent.contains("emergencyCutoff"))
    }

    @Test
    fun verifyStageF1CapabilitiesManifest_EnablesActuatorsWithPhysicalHonesty() {
        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/capabilities_response_f1.json")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/capabilities_response_f1.json")
        val json = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

        val caps = ProtocolMessageParser.parseCapabilities(json)
        assertEquals(FirmwareStage.F1, caps.firmwareStage)
        assertEquals("Esp32DevKitV1", caps.controllerTarget)

        // Motors, pump, and servo enabled in F1
        assertTrue(caps.isActuatorEnabled("hw-motors-bo-geared"))
        assertTrue(caps.isActuatorEnabled("hw-irrigation-pump"))
        assertTrue(caps.isActuatorEnabled("hw-servo-camera-tilt"))

        // CRITICAL INVARIANT: Physical feedback honesty MUST hold even in F1
        caps.supportedActuators.forEach { actuator ->
            assertFalse(
                "Actuator ${actuator.id} cannot claim physical feedback on open-loop hardware",
                actuator.hasPhysicalFeedback
            )
        }

        assertTrue(caps.features.contains("DRIVE_CONTROL"))
        assertTrue(caps.features.contains("IRRIGATION_CONTROL"))
        assertTrue(caps.features.contains("SERVO_CONTROL"))
        assertTrue(caps.features.contains("EMERGENCY_STOP"))
    }

    @Test
    fun verifyStageF1Actuation_DifferentialDriveRangeValidation() {
        // Valid drive command in Stage F1
        val validDrive = RoverCommand(
            cmdId = "c-drive-1",
            sequence = 101L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            ttlMs = 250L,
            parameters = mapOf("throttle" to 0.65, "steering" to -0.30)
        )
        val validResult = ProtocolValidator.validateCommand(
            command = validDrive,
            currentClockMs = 1050L,
            firmwareStage = FirmwareStage.F1
        )
        assertTrue(validResult.isValid)

        // Invalid throttle (> 1.0)
        val invalidDrive = RoverCommand(
            cmdId = "c-drive-2",
            sequence = 102L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 1.5, "steering" to 0.0)
        )
        val invalidResult = ProtocolValidator.validateCommand(
            command = invalidDrive,
            currentClockMs = 1050L,
            firmwareStage = FirmwareStage.F1
        )
        assertFalse(invalidResult.isValid)
        assertEquals(ProtocolErrorCode.ERR_INVALID_PARAMETER, (invalidResult as ValidationResult.Invalid).errorCode)
    }

    @Test
    fun verifyStageF1Actuation_PumpSafetyCutoffEnforcement() {
        // Safe pump duration (5s)
        val safePump = RoverCommand(
            cmdId = "c-pump-1",
            sequence = 103L,
            commandType = "SET_PUMP",
            createdTimestampMs = 1000L,
            parameters = mapOf("durationMs" to 5000L)
        )
        val safeResult = ProtocolValidator.validateCommand(
            command = safePump,
            currentClockMs = 1050L,
            firmwareStage = FirmwareStage.F1
        )
        assertTrue(safeResult.isValid)

        // Unsafe pump duration (12s > 10s maximum limit)
        val unsafePump = RoverCommand(
            cmdId = "c-pump-2",
            sequence = 104L,
            commandType = "SET_PUMP",
            createdTimestampMs = 1000L,
            parameters = mapOf("durationMs" to 12000L)
        )
        val unsafeResult = ProtocolValidator.validateCommand(
            command = unsafePump,
            currentClockMs = 1050L,
            firmwareStage = FirmwareStage.F1
        )
        assertFalse(unsafeResult.isValid)
        assertEquals(ProtocolErrorCode.ERR_INVALID_PARAMETER, (unsafeResult as ValidationResult.Invalid).errorCode)
    }

    @Test
    fun verifyStageF1Actuation_EmergencyStopLatchedRejection() {
        val driveCmd = RoverCommand(
            cmdId = "c-drive-3",
            sequence = 105L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1000L,
            parameters = mapOf("throttle" to 0.5, "steering" to 0.0)
        )

        // Rejected when E-Stop is latched
        val result = ProtocolValidator.validateCommand(
            command = driveCmd,
            currentClockMs = 1050L,
            isEmergencyStopLatched = true,
            firmwareStage = FirmwareStage.F1
        )
        assertFalse(result.isValid)
        assertEquals(ProtocolErrorCode.ERR_EMERGENCY_STOP_ACTIVE, (result as ValidationResult.Invalid).errorCode)
    }

    @Test
    fun verifyCatalogItems_OpenLoopActuatorsNeverClaimPhysicalFeedback() {
        val items = HardwareUiDataMapper.createUiItems(isDemoMode = false)
        val openLoopActuators = listOf(
            "hw-motors-bo-geared",
            "hw-driver-tb6612fng",
            "hw-servo-camera",
            "hw-servo-soil-probe",
            "hw-servo-ultrasonic",
            "hw-irrigation-pump",
            "hw-irrigation-relay"
        )

        items.filter { it.component.id in openLoopActuators }.forEach { item ->
            assertEquals(
                "Open-loop actuator ${item.component.id} must be PhysicalFeedbackState.NotAvailable",
                PhysicalFeedbackState.NotAvailable,
                item.physicalFeedback
            )
            assertNotNull(
                "Open-loop actuator ${item.component.id} must display an honesty disclaimer",
                item.honestyDisclaimer
            )
        }
    }
}
