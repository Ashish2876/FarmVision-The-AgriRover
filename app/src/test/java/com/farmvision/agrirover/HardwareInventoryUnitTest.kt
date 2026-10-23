package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.data.model.hardware.CommunicationProfile
import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ConfigurationStatus
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.Dht22SensorConfig
import com.farmvision.agrirover.data.model.hardware.DriveMotorId
import com.farmvision.agrirover.data.model.hardware.DriveSystemConfig
import com.farmvision.agrirover.data.model.hardware.HardwareCatalogSeed
import com.farmvision.agrirover.data.model.hardware.HardwareComponent
import com.farmvision.agrirover.data.model.hardware.HardwarePinValidator
import com.farmvision.agrirover.data.model.hardware.IrrigationSystemConfig
import com.farmvision.agrirover.data.model.hardware.PowerSystemConfig
import com.farmvision.agrirover.data.model.hardware.ServoActuatorConfig
import com.farmvision.agrirover.data.model.hardware.SoilMoistureSensorConfig
import com.farmvision.agrirover.data.repository.MockFarmRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Section 4 Hardware Inventory compliance, controller profiles,
 * motor grouping assumptions, unspecified quantities, structural pin validation,
 * and safety boundary rules.
 */
class HardwareInventoryUnitTest {

    @Test
    fun hardwareCatalog_containsExact23ItemsFromInventory() {
        val items = HardwareCatalogSeed.INVENTORY_ITEMS
        assertEquals("Master inventory must contain exactly 23 components", 23, items.size)

        // Unique stable IDs across all components
        val uniqueIds = items.map { it.id }.toSet()
        assertEquals("All 23 components must possess unique stable identifiers", 23, uniqueIds.size)
    }

    @Test
    fun hardwareCatalog_categoriesAndRoles_matchSpecificationExactly() {
        // Controller category (1 item: ESP32 DevKit V1)
        val controllers = HardwareCatalogSeed.getByCategory(ComponentCategory.Controller)
        assertEquals(1, controllers.size)
        assertEquals("ESP32 DevKit V1", controllers.first().displayName)

        // Camera category (1 item: ESP32-CAM)
        val cameras = HardwareCatalogSeed.getByCategory(ComponentCategory.Camera)
        assertEquals(1, cameras.size)
        assertEquals("ESP32-CAM (OV2640)", cameras.first().displayName)

        // Programming Tool category (1 item: ESP32-CAM-MB; NOT a controller)
        val tools = HardwareCatalogSeed.getByCategory(ComponentCategory.ProgrammingTool)
        assertEquals(1, tools.size)
        assertEquals("hw-tool-esp32cam-mb", tools.first().id)
        assertEquals("ESP32-CAM-MB USB Programmer", tools.first().displayName)

        // Sensors (4 items: Soil Moisture, DHT22, Soil pH, Ultrasonic)
        val sensors = HardwareCatalogSeed.getByCategory(ComponentCategory.Sensor)
        assertEquals(4, sensors.size)

        // Motor Driver (1 item: TB6612FNG)
        val drivers = HardwareCatalogSeed.getByCategory(ComponentCategory.MotorDriver)
        assertEquals(1, drivers.size)

        // Motors (1 item representing the 4 BO Geared DC Motors)
        val motors = HardwareCatalogSeed.getByCategory(ComponentCategory.Motor)
        assertEquals(1, motors.size)
        assertEquals(4, motors.first().quantity)

        // Servos (3 distinct servo roles)
        val servos = HardwareCatalogSeed.getByCategory(ComponentCategory.Servo)
        assertEquals(3, servos.size)

        // Servo Hardware (1 item: brackets and horns)
        val servoHardware = HardwareCatalogSeed.getByCategory(ComponentCategory.ServoHardware)
        assertEquals(1, servoHardware.size)

        // Irrigation (4 items: pump, relay, tubing, reservoir)
        val irrigation = HardwareCatalogSeed.getByCategory(ComponentCategory.Irrigation)
        assertEquals(4, irrigation.size)

        // Power (4 items: battery, buck converter, switch, fuse)
        val power = HardwareCatalogSeed.getByCategory(ComponentCategory.Power)
        assertEquals(4, power.size)

        // Assembly (2 items: chassis, fasteners)
        val assembly = HardwareCatalogSeed.getByCategory(ComponentCategory.Assembly)
        assertEquals(2, assembly.size)
    }

    @Test
    fun hardwareCatalog_unspecifiedQuantities_areRecordedHonestlyAsNull() {
        // Section 4 inventory specifies quantity 'unspecified' for:
        // 1. Servo mounting brackets and horns
        val servoBrackets = HardwareCatalogSeed.findById("hw-servo-hardware-mounts")
        assertNotNull(servoBrackets)
        assertNull("Servo brackets quantity must be unspecified (null)", servoBrackets?.quantity)
        assertFalse(servoBrackets!!.isQuantitySpecified)

        // 2. Water tubing
        val tubing = HardwareCatalogSeed.findById("hw-irrigation-tubing")
        assertNotNull(tubing)
        assertNull("Water tubing quantity must be unspecified (null)", tubing?.quantity)
        assertFalse(tubing!!.isQuantitySpecified)

        // 3. Inline fuse and holder
        val fuse = HardwareCatalogSeed.findById("hw-power-fuse")
        assertNotNull(fuse)
        assertNull("Inline fuse quantity must be unspecified (null)", fuse?.quantity)
        assertFalse(fuse!!.isQuantitySpecified)

        // 4. Jumper wires, brackets, hardware
        val fasteners = HardwareCatalogSeed.findById("hw-assembly-fasteners")
        assertNotNull(fasteners)
        assertNull("Fasteners/jumper wires quantity must be unspecified (null)", fasteners?.quantity)
        assertFalse(fasteners!!.isQuantitySpecified)
    }

    @Test
    fun hardwareCatalog_noItemMayBeMarkedVerifiedInPhase4() {
        val items = HardwareCatalogSeed.INVENTORY_ITEMS
        items.forEach { item ->
            assertTrue(
                "Item ${item.id} cannot be marked Verified in Phase 4",
                item.status != ConfigurationStatus.Verified
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun hardwareComponent_rejectsVerifiedStatusInPhase4() {
        // Enforcing safety rule: attempt to construct verified component fails fast
        HardwareComponent(
            id = "illegal-verified-item",
            displayName = "Illegal Item",
            category = ComponentCategory.Sensor,
            intendedRole = "Testing guard",
            modelDesignation = "Test",
            quantity = 1,
            status = ConfigurationStatus.Verified
        )
    }

    @Test
    fun sensorConfiguration_distinctProbesAndDht22MultiReading() {
        val probe1 = SoilMoistureSensorConfig(id = "sensor-moisture-1", probeName = "Probe 1 (Surface)")
        val probe2 = SoilMoistureSensorConfig(id = "sensor-moisture-2", probeName = "Probe 2 (Deep)")
        assertEquals(ControllerTarget.Esp32DevKitV1, probe1.targetController)
        assertEquals(ControllerTarget.Esp32DevKitV1, probe2.targetController)
        assertNull(probe1.analogPin) // Pins unconfigured by default

        val dht22 = Dht22SensorConfig()
        assertEquals(2, dht22.supportedTypes.size)
        assertTrue(dht22.supportedTypes.contains(SensorType.AirTemperature))
        assertTrue(dht22.supportedTypes.contains(SensorType.Humidity))
        assertNull(dht22.digitalPin)
    }

    @Test
    fun driveSystemConfig_encapsulatesProvisionalMotorChannelGrouping() {
        val config = DriveSystemConfig()
        assertEquals("driver-tb6612fng", config.driverId)

        // Verify four drive motor IDs
        assertEquals(2, config.leftMotors.size)
        assertEquals(2, config.rightMotors.size)
        assertTrue(config.leftMotors.contains(DriveMotorId.FrontLeft))
        assertTrue(config.leftMotors.contains(DriveMotorId.RearLeft))
        assertTrue(config.rightMotors.contains(DriveMotorId.FrontRight))
        assertTrue(config.rightMotors.contains(DriveMotorId.RearRight))

        // Pins unassigned by default
        assertNull(config.pwmaPin)
        assertNull(config.ain1Pin)
        assertNull(config.standbyPin)
    }

    @Test
    fun servoActuators_distinctRolesAndSingleAxisCameraLimitation() {
        val cameraServo = ServoActuatorConfig(
            id = "servo-cam",
            name = "SG90 Camera Servo",
            model = "SG90",
            intendedRole = "Camera tilt",
            targetController = ControllerTarget.Esp32DevKitV1,
            isDualAxis = false // Not pan-and-tilt
        )
        assertFalse("Camera servo must not claim dual-axis/pan-and-tilt capability", cameraServo.isDualAxis)

        val soilServo = ServoActuatorConfig(
            id = "servo-soil",
            name = "MG90S Probe Servo",
            model = "MG90S",
            intendedRole = "Soil probe insertion",
            targetController = ControllerTarget.Esp32DevKitV1
        )
        assertEquals("MG90S", soilServo.model)
    }

    @Test
    fun powerSystemConfig_explicitlyFlagsAbsenceOfVoltageSensing() {
        val powerConfig = PowerSystemConfig()
        assertFalse(
            "Phase 4 power config must reflect that no hardware voltage divider exists in inventory",
            powerConfig.hasHardwareVoltageDivider
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun communicationProfile_rejectsClaimOfRealHardwareConnection() {
        CommunicationProfile(
            controllerTarget = ControllerTarget.Esp32DevKitV1,
            isRealConnectionEstablished = true // Must fail in Phase 4
        )
    }

    @Test
    fun hardwarePinValidator_detectsDuplicateGpioAssignments() {
        val validPins = listOf(
            HardwarePinValidator.PinAssignment(ControllerTarget.Esp32DevKitV1, "dht22", "DATA", 4),
            HardwarePinValidator.PinAssignment(ControllerTarget.Esp32DevKitV1, "pump-relay", "IN1", 5),
            HardwarePinValidator.PinAssignment(ControllerTarget.Esp32DevKitV1, "probe-1", "AOUT", 32)
        )
        val validResult = HardwarePinValidator.validateAssignments(validPins)
        assertTrue("Disjoint GPIO pin assignments must validate successfully", validResult.isValid)
        assertTrue(validResult.duplicateAssignments.isEmpty())

        // Conflicting pins: two devices assigned to GPIO 4 on the same controller
        val conflictingPins = listOf(
            HardwarePinValidator.PinAssignment(ControllerTarget.Esp32DevKitV1, "dht22", "DATA", 4),
            HardwarePinValidator.PinAssignment(ControllerTarget.Esp32DevKitV1, "pump-relay", "IN1", 4)
        )
        val conflictResult = HardwarePinValidator.validateAssignments(conflictingPins)
        assertFalse("Duplicate GPIO assignments must be detected and flagged", conflictResult.isValid)
        assertTrue(conflictResult.duplicateAssignments.containsKey(4))
    }

    @Test
    fun mockFarmRepository_integratesBoundariesAndProducesSimulatedOrigin() {
        val repo = MockFarmRepository()
        val telemetry = repo.getTelemetry()

        assertEquals(DataOrigin.Simulated, telemetry.dataOrigin)
        assertTrue(telemetry.isSimulation)
        assertEquals("AgriRover Alpha-1", telemetry.roverName)

        // Verify hardware catalog source
        assertEquals(23, repo.hardwareCatalog.getAllComponents().size)

        // Verify multi-sensor readings map
        val sensorMap = repo.sensorReadingsFlow.value
        assertTrue(sensorMap.containsKey(SensorType.SoilMoisture))
        assertTrue(sensorMap.containsKey(SensorType.AirTemperature))
        assertTrue(sensorMap.containsKey(SensorType.Humidity))
        assertTrue(sensorMap.containsKey(SensorType.LightIntensity))
        assertTrue(sensorMap.containsKey(SensorType.SoilPh))
        assertTrue(sensorMap.containsKey(SensorType.UltrasonicDistance))

        // Soil pH & Ultrasonic readings
        assertEquals(6.8f, repo.getSensorReading(SensorType.SoilPh)?.value)
        assertEquals(45.0f, repo.getSensorReading(SensorType.UltrasonicDistance)?.value)
    }
}
