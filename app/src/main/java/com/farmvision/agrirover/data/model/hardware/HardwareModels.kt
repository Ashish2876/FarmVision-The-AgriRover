package com.farmvision.agrirover.data.model.hardware

import com.farmvision.agrirover.data.model.SensorType

/**
 * Functional taxonomy categories for physical agricultural rover hardware.
 */
enum class ComponentCategory(val displayName: String) {
    Controller("Microcontroller & Compute"),
    Camera("Vision & Imaging"),
    ProgrammingTool("Development & Flashing Tool"),
    Sensor("Environmental & Telemetry Sensor"),
    MotorDriver("Actuation & Power Electronics"),
    Motor("Locomotion Motor"),
    Servo("Kinematic Servo Actuator"),
    ServoHardware("Mechanical Mounting & Horns"),
    Irrigation("Fluid Delivery & Plumbing"),
    Power("Electrical Energy & Regulation"),
    Assembly("Chassis & Fasteners")
}

/**
 * Configuration maturity lifecycle status.
 * IMPORTANT: In Phase 4, no component or profile may ever be marked [Verified].
 */
enum class ConfigurationStatus {
    NotConfigured,
    Draft,
    Verified
}

/**
 * Single immutable physical rover component catalog record.
 * Quantities not explicitly stated in the source of truth inventory are recorded as null ([isQuantitySpecified] == false).
 */
data class HardwareComponent(
    val id: String,
    val displayName: String,
    val category: ComponentCategory,
    val intendedRole: String,
    val modelDesignation: String,
    val quantity: Int?,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured,
    val documentationRef: String? = null,
    val provisionalNotes: String? = null
) {
    val isQuantitySpecified: Boolean = quantity != null

    init {
        require(status != ConfigurationStatus.Verified) {
            "Phase 4 rule violation: Component '$id' cannot be marked Verified. All hardware specs remain draft/unverified."
        }
    }
}

/**
 * Controller profile distinction.
 * The architecture models ESP32 DevKit V1 and ESP32-CAM as two distinct controller profiles.
 */
enum class ControllerTarget {
    Esp32DevKitV1,
    Esp32Cam
}

/**
 * Profile for microcontroller compute units.
 */
data class ControllerProfile(
    val target: ControllerTarget,
    val name: String,
    val role: String,
    val assignedPins: Map<String, Int?> = emptyMap(),
    val status: ConfigurationStatus = ConfigurationStatus.Draft
) {
    init {
        require(status != ConfigurationStatus.Verified) {
            "Controller profile cannot be marked Verified in Phase 4."
        }
    }
}

/**
 * Draft calibration and configuration profile for soil moisture probes.
 */
data class SoilMoistureSensorConfig(
    val id: String,
    val probeName: String,
    val targetController: ControllerTarget = ControllerTarget.Esp32DevKitV1,
    val analogPin: Int? = null,
    val rawDryCalibration: Int? = null,
    val rawWetCalibration: Int? = null,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Draft configuration profile for the single DHT22 atmospheric sensor.
 * Supports exactly two reading types: [SensorType.AirTemperature] and [SensorType.Humidity].
 */
data class Dht22SensorConfig(
    val id: String = "sensor-dht22",
    val sensorName: String = "DHT22 Air Temperature & Humidity Sensor",
    val targetController: ControllerTarget = ControllerTarget.Esp32DevKitV1,
    val digitalPin: Int? = null,
    val supportedTypes: List<SensorType> = listOf(SensorType.AirTemperature, SensorType.Humidity),
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Draft configuration profile for the Soil pH sensor kit.
 */
data class SoilPhSensorConfig(
    val id: String = "sensor-soil-ph",
    val sensorName: String = "Soil pH Sensor Kit",
    val targetController: ControllerTarget = ControllerTarget.Esp32DevKitV1,
    val analogPin: Int? = null,
    val neutralVoltageMv: Float? = null,
    val acidVoltageMv: Float? = null,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Draft configuration profile for the HC-SR04 ultrasonic distance sensor.
 */
data class HcSr04SensorConfig(
    val id: String = "sensor-ultrasonic",
    val sensorName: String = "HC-SR04 Ultrasonic Distance Sensor",
    val targetController: ControllerTarget = ControllerTarget.Esp32DevKitV1,
    val triggerPin: Int? = null,
    val echoPin: Int? = null,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Identifiers for the 4 drive motors.
 */
enum class DriveMotorId(val label: String, val provisionalSideGroup: String) {
    FrontLeft("Front Left Motor", "Left Channel (A)"),
    RearLeft("Rear Left Motor", "Left Channel (A)"),
    FrontRight("Front Right Motor", "Right Channel (B)"),
    RearRight("Rear Right Motor", "Right Channel (B)")
}

/**
 * Motor driver and actuation configuration profile.
 * PROVISIONAL ASSUMPTION: The four BO geared DC motors are mapped into left-side and right-side
 * pairs of two motors parallel-connected to the TB6612FNG dual channels.
 * This is an architecture assumption to be validated by the physical hardware team.
 */
data class DriveSystemConfig(
    val driverId: String = "driver-tb6612fng",
    val driverName: String = "TB6612FNG Dual Motor Driver",
    val pwmaPin: Int? = null,
    val ain1Pin: Int? = null,
    val ain2Pin: Int? = null,
    val pwmbPin: Int? = null,
    val bin1Pin: Int? = null,
    val bin2Pin: Int? = null,
    val standbyPin: Int? = null,
    val leftMotors: List<DriveMotorId> = listOf(DriveMotorId.FrontLeft, DriveMotorId.RearLeft),
    val rightMotors: List<DriveMotorId> = listOf(DriveMotorId.FrontRight, DriveMotorId.RearRight),
    val provisionalNotes: String = "Provisional 2x2 motor grouping to be verified for current draw by hardware team",
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Kinematic servo actuator configuration.
 * Camera servo movement axis is unconfirmed (single-axis SG90; do not claim pan-and-tilt).
 */
data class ServoActuatorConfig(
    val id: String,
    val name: String,
    val model: String,
    val intendedRole: String,
    val targetController: ControllerTarget,
    val pwmPin: Int? = null,
    val minAngleLimit: Int? = null,
    val maxAngleLimit: Int? = null,
    val isDualAxis: Boolean = false,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Fluid delivery and irrigation configuration profile.
 */
data class IrrigationSystemConfig(
    val pumpComponentId: String = "irrigation-pump",
    val relayComponentId: String = "irrigation-relay",
    val pumpName: String = "5-6V Mini DC Water Pump",
    val relayName: String = "2-Channel Relay Module (Channel 1: Pump, Channel 2: Spare)",
    val relayControlPin: Int? = null,
    val reservoirCapacityLiters: Float? = null,
    val tubingInnerDiameterMm: Float? = null,
    val isSimulatedWateringActive: Boolean = true,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured
)

/**
 * Power storage and voltage regulation configuration.
 * IMPORTANT: No battery voltage/current sensing component exists in the Section 4 inventory.
 * Real battery telemetry cannot be gathered without physical sensing additions.
 */
data class PowerSystemConfig(
    val batteryPackId: String = "power-battery",
    val buckConverterId: String = "power-buck-converter",
    val switchId: String = "power-switch",
    val fuseId: String = "power-fuse",
    val nominalPackVoltageVolts: Float? = null,
    val regulatedBusVoltageVolts: Float? = null,
    val fuseRatingAmperes: Float? = null,
    val hasHardwareVoltageDivider: Boolean = false,
    val status: ConfigurationStatus = ConfigurationStatus.NotConfigured,
    val note: String = "No voltage divider or sensing circuit in inventory; battery HUD is purely simulated"
)

/**
 * Planned communication profile for controllers.
 * A configured profile never implies an active physical connection.
 */
data class CommunicationProfile(
    val controllerTarget: ControllerTarget,
    val plannedProtocol: String = "Wi-Fi SoftAP",
    val isRealConnectionEstablished: Boolean = false,
    val status: ConfigurationStatus = ConfigurationStatus.Draft
) {
    init {
        require(!isRealConnectionEstablished) {
            "Phase 4 rule: Real hardware connection cannot be established or claimed."
        }
    }
}

/**
 * Structural pin assignment validator.
 * Ensures no duplicate GPIO pin assignments occur across configured components.
 * Does NOT invent pin numbers; checks only assigned (non-null) pins.
 */
object HardwarePinValidator {

    data class PinAssignment(
        val controller: ControllerTarget,
        val componentId: String,
        val pinRole: String,
        val gpioNumber: Int?
    )

    data class ValidationResult(
        val isValid: Boolean,
        val duplicateAssignments: Map<Int, List<String>>
    )

    fun validateAssignments(assignments: List<PinAssignment>): ValidationResult {
        val nonNullPins = assignments.filter { it.gpioNumber != null }
        val duplicatesByController = nonNullPins
            .groupBy { "${it.controller.name}-GPIO-${it.gpioNumber}" to it.gpioNumber!! }
            .filter { it.value.size > 1 }

        val duplicateMap = duplicatesByController.map { (key, list) ->
            key.second to list.map { "${it.componentId} (${it.pinRole})" }
        }.toMap()

        return ValidationResult(
            isValid = duplicateMap.isEmpty(),
            duplicateAssignments = duplicateMap
        )
    }
}
