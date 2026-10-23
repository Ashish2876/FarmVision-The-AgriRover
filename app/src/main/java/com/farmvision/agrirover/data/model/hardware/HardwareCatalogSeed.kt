package com.farmvision.agrirover.data.model.hardware

/**
 * Static in-code seed representing the exact 23-item agricultural rover hardware inventory.
 * Source of truth: Section 4 of Phase 4 specification.
 * Quantities not explicitly specified are stored as null (Unspecified).
 * No component is marked Verified; all items start as [ConfigurationStatus.NotConfigured] or [ConfigurationStatus.Draft].
 */
object HardwareCatalogSeed {

    val INVENTORY_ITEMS: List<HardwareComponent> = listOf(
        // 1. Controller
        HardwareComponent(
            id = "hw-controller-esp32",
            displayName = "ESP32 DevKit V1",
            category = ComponentCategory.Controller,
            intendedRole = "Main controller: sensors, motors, servos, irrigation",
            modelDesignation = "ESP32-WROOM-32 / DevKit V1 (30-pin)",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#esp32-devkit-v1",
            provisionalNotes = "Primary telemetry and actuation control unit"
        ),
        // 2. Camera
        HardwareComponent(
            id = "hw-camera-esp32cam",
            displayName = "ESP32-CAM (OV2640)",
            category = ComponentCategory.Camera,
            intendedRole = "Camera module (separate controller)",
            modelDesignation = "AI-Thinker ESP32-CAM with OV2640 2MP Sensor",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#esp32-cam",
            provisionalNotes = "Separate controller dedicated to vision stream"
        ),
        // 3. Programming tool
        HardwareComponent(
            id = "hw-tool-esp32cam-mb",
            displayName = "ESP32-CAM-MB USB Programmer",
            category = ComponentCategory.ProgrammingTool,
            intendedRole = "Flashing the ESP32-CAM; not a runtime component",
            modelDesignation = "ESP32-CAM-MB Micro-USB Daughterboard (CH340G)",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#programming-tools",
            provisionalNotes = "Development/flashing accessory; not mounted for rover runtime operation"
        ),
        // 4. Sensor - Soil Moisture
        HardwareComponent(
            id = "hw-sensor-soil-moisture-probes",
            displayName = "Capacitive Soil Moisture Sensor",
            category = ComponentCategory.Sensor,
            intendedRole = "Soil moisture (two distinct sensors)",
            modelDesignation = "Capacitive Soil Moisture Sensor v1.2 / v2.0",
            quantity = 2,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#soil-moisture",
            provisionalNotes = "Two distinct analog capacitive probes (Probe 1 surface, Probe 2 deep rootzone)"
        ),
        // 5. Sensor - DHT22
        HardwareComponent(
            id = "hw-sensor-dht22",
            displayName = "DHT22 (AM2302)",
            category = ComponentCategory.Sensor,
            intendedRole = "Air temperature and humidity (one component, two readings)",
            modelDesignation = "Aosong DHT22 / AM2302 Digital Sensor",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#dht22",
            provisionalNotes = "Single physical component providing two readings: Temperature and Humidity"
        ),
        // 6. Sensor - Soil pH
        HardwareComponent(
            id = "hw-sensor-soil-ph",
            displayName = "Soil pH Sensor Kit",
            category = ComponentCategory.Sensor,
            intendedRole = "Soil pH measurement",
            modelDesignation = "Analog Soil pH Sensor Probe & Signal Conditioning Board",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#soil-ph",
            provisionalNotes = "Analog pH probe requiring two-point buffer calibration"
        ),
        // 7. Sensor - Ultrasonic
        HardwareComponent(
            id = "hw-sensor-ultrasonic",
            displayName = "HC-SR04 Ultrasonic Sensor",
            category = ComponentCategory.Sensor,
            intendedRole = "Distance / obstacle detection",
            modelDesignation = "HC-SR04 Sonar Transceiver Module (5V)",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#hc-sr04",
            provisionalNotes = "5V Echo pin requires voltage divider before connection to 3.3V ESP32 GPIO"
        ),
        // 8. Motor Driver
        HardwareComponent(
            id = "hw-driver-tb6612fng",
            displayName = "TB6612FNG Dual Motor Driver",
            category = ComponentCategory.MotorDriver,
            intendedRole = "Drives the four drive motors",
            modelDesignation = "Toshiba TB6612FNG Dual MOSFET H-Bridge Breakout",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#tb6612fng",
            provisionalNotes = "Dual-channel driver; controls 4 motors via left/right pairwise channel grouping"
        ),
        // 9. Motors
        HardwareComponent(
            id = "hw-motors-bo-geared",
            displayName = "BO Geared DC Motor with Wheel",
            category = ComponentCategory.Motor,
            intendedRole = "Drive motors",
            modelDesignation = "Yellow BO Dual-Shaft 3-6V TT Gearbox Motor + Rubber Tire Wheel",
            quantity = 4,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#bo-motors",
            provisionalNotes = "Four drive wheels; two left motors wired in parallel, two right motors in parallel"
        ),
        // 10. Servo - Camera
        HardwareComponent(
            id = "hw-servo-camera",
            displayName = "SG90 Micro Servo (Camera Movement)",
            category = ComponentCategory.Servo,
            intendedRole = "Camera movement (movement axis unconfirmed)",
            modelDesignation = "TowerPro SG90 9g Micro Servo",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#servos",
            provisionalNotes = "Single-axis tilt actuation; do not claim pan-and-tilt capability until hardware verifies"
        ),
        // 11. Servo - Soil Probe
        HardwareComponent(
            id = "hw-servo-soil-probe",
            displayName = "MG90S Metal Gear Micro Servo (Soil Probe)",
            category = ComponentCategory.Servo,
            intendedRole = "Soil probe positioning",
            modelDesignation = "TowerPro MG90S Metal Gear 9g Servo",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#servos",
            provisionalNotes = "Higher torque metal gear servo for lowering probe into soil"
        ),
        // 12. Servo - Ultrasonic
        HardwareComponent(
            id = "hw-servo-ultrasonic",
            displayName = "SG90 Micro Servo (Ultrasonic Scanning)",
            category = ComponentCategory.Servo,
            intendedRole = "Ultrasonic sensor scanning",
            modelDesignation = "TowerPro SG90 9g Micro Servo",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#servos",
            provisionalNotes = "Horizontal sweep scanning for obstacle mapping"
        ),
        // 13. Servo Hardware
        HardwareComponent(
            id = "hw-servo-hardware-mounts",
            displayName = "Servo Mounting Brackets and Horns",
            category = ComponentCategory.ServoHardware,
            intendedRole = "Mechanical mounting and horns for servos",
            modelDesignation = "Plastic & Metal Horns, Linkages, and Bracket Adapters",
            quantity = null, // unspecified
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#servos",
            provisionalNotes = "Quantity unspecified in master inventory; recorded honestly as unspecified"
        ),
        // 14. Irrigation - Pump
        HardwareComponent(
            id = "hw-irrigation-pump",
            displayName = "5–6 V Mini DC Water Pump",
            category = ComponentCategory.Irrigation,
            intendedRole = "Watering",
            modelDesignation = "Submersible Mini 5V-6V DC Centrifugal Pump",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#irrigation",
            provisionalNotes = "Switched via relay module Channel 1"
        ),
        // 15. Irrigation - Relay
        HardwareComponent(
            id = "hw-irrigation-relay",
            displayName = "2-Channel Relay Module",
            category = ComponentCategory.Irrigation,
            intendedRole = "Switching the pump (and a spare channel)",
            modelDesignation = "5V 2-Channel Optocoupler Isolated Relay Board (10A 250VAC / 30VDC)",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#irrigation",
            provisionalNotes = "Channel 1 dedicated to water pump; Channel 2 provisioned as spare/auxiliary"
        ),
        // 16. Irrigation - Tubing
        HardwareComponent(
            id = "hw-irrigation-tubing",
            displayName = "Water Tubing",
            category = ComponentCategory.Irrigation,
            intendedRole = "Plumbing fluid delivery line",
            modelDesignation = "Flexible Silicone / PVC Drip Tubing",
            quantity = null, // unspecified
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#irrigation",
            provisionalNotes = "Length and inner diameter unspecified; recorded as unspecified"
        ),
        // 17. Irrigation - Reservoir
        HardwareComponent(
            id = "hw-irrigation-reservoir",
            displayName = "Water Reservoir",
            category = ComponentCategory.Irrigation,
            intendedRole = "Water storage",
            modelDesignation = "Onboard Fluid Storage Container",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#irrigation",
            provisionalNotes = "Fluid tank capacity uncalibrated"
        ),
        // 18. Power - Battery Pack
        HardwareComponent(
            id = "hw-power-battery",
            displayName = "Protected Battery Pack",
            category = ComponentCategory.Power,
            intendedRole = "Main power source",
            modelDesignation = "Rechargeable Lithium/Li-Ion Pack with Internal BMS Protection",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#power-system",
            provisionalNotes = "Main DC power source; voltage capacity uncalibrated; no voltage divider in inventory"
        ),
        // 19. Power - Buck Converter
        HardwareComponent(
            id = "hw-power-buck-converter",
            displayName = "LM2596 DC-DC Buck Converter",
            category = ComponentCategory.Power,
            intendedRole = "Voltage regulation",
            modelDesignation = "LM2596 Step-Down Adjustable DC-DC Regulator Module",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#power-system",
            provisionalNotes = "Steps battery voltage down to regulated 5V system rail"
        ),
        // 20. Power - Switch
        HardwareComponent(
            id = "hw-power-switch",
            displayName = "Main Power Switch",
            category = ComponentCategory.Power,
            intendedRole = "Power cutoff",
            modelDesignation = "High-Current Rocker / Toggle SPST Physical Switch",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#power-system",
            provisionalNotes = "Master physical power disconnect"
        ),
        // 21. Power - Fuse
        HardwareComponent(
            id = "hw-power-fuse",
            displayName = "Inline Fuse and Fuse Holder",
            category = ComponentCategory.Power,
            intendedRole = "Overcurrent protection",
            modelDesignation = "Blade / Glass Tube Inline Fuse Holder",
            quantity = null, // unspecified
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#power-system",
            provisionalNotes = "Fuse rating (amperes) unspecified; recorded as unspecified"
        ),
        // 22. Assembly - Fasteners
        HardwareComponent(
            id = "hw-assembly-fasteners",
            displayName = "Jumper Wires, Brackets, Screws, Nuts, Cable Ties",
            category = ComponentCategory.Assembly,
            intendedRole = "Mechanical mounting, electrical interconnect, and cable management",
            modelDesignation = "M3 Standoffs/Hardware, DuPont Jumper Wires, Zip Ties",
            quantity = null, // unspecified
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#assembly",
            provisionalNotes = "Kit quantities unspecified in master inventory; recorded as unspecified"
        ),
        // 23. Assembly - Chassis
        HardwareComponent(
            id = "hw-assembly-chassis",
            displayName = "Cardboard Chassis",
            category = ComponentCategory.Assembly,
            intendedRole = "Chassis body and structural mounting plate",
            modelDesignation = "Prototyping Cardboard Laser/Hand-Cut Structural Frame",
            quantity = 1,
            status = ConfigurationStatus.Draft,
            documentationRef = "docs/hardware-data-model.md#assembly",
            provisionalNotes = "Lightweight prototype frame for Stage A & Stage B evaluation"
        )
    )

    /**
     * Look up a component by its stable ID.
     */
    fun findById(componentId: String): HardwareComponent? =
        INVENTORY_ITEMS.firstOrNull { it.id == componentId }

    /**
     * Filter components by category.
     */
    fun getByCategory(category: ComponentCategory): List<HardwareComponent> =
        INVENTORY_ITEMS.filter { it.category == category }
}
