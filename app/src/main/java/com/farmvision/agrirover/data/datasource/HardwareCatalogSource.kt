package com.farmvision.agrirover.data.datasource

import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ControllerProfile
import com.farmvision.agrirover.data.model.hardware.DriveSystemConfig
import com.farmvision.agrirover.data.model.hardware.HardwareCatalogSeed
import com.farmvision.agrirover.data.model.hardware.HardwareComponent
import com.farmvision.agrirover.data.model.hardware.IrrigationSystemConfig
import com.farmvision.agrirover.data.model.hardware.PowerSystemConfig

/**
 * Replaceable read-only source for the physical rover component inventory and configuration.
 * In Phase 4, backed by the static in-code seed [HardwareCatalogSeed].
 */
interface HardwareCatalogSource {
    fun getAllComponents(): List<HardwareComponent>
    fun getComponentById(id: String): HardwareComponent?
    fun getComponentsByCategory(category: ComponentCategory): List<HardwareComponent>
    fun getControllerProfiles(): List<ControllerProfile>
    fun getDriveSystemConfig(): DriveSystemConfig
    fun getIrrigationConfig(): IrrigationSystemConfig
    fun getPowerConfig(): PowerSystemConfig
}

/**
 * Static in-memory implementation of [HardwareCatalogSource] using the Section 4 inventory.
 */
class StaticHardwareCatalogSource : HardwareCatalogSource {

    override fun getAllComponents(): List<HardwareComponent> =
        HardwareCatalogSeed.INVENTORY_ITEMS

    override fun getComponentById(id: String): HardwareComponent? =
        HardwareCatalogSeed.findById(id)

    override fun getComponentsByCategory(category: ComponentCategory): List<HardwareComponent> =
        HardwareCatalogSeed.getByCategory(category)

    override fun getControllerProfiles(): List<ControllerProfile> = listOf(
        ControllerProfile(
            target = com.farmvision.agrirover.data.model.hardware.ControllerTarget.Esp32DevKitV1,
            name = "ESP32 DevKit V1 (Main)",
            role = "Sensors, motors, servos, irrigation relay"
        ),
        ControllerProfile(
            target = com.farmvision.agrirover.data.model.hardware.ControllerTarget.Esp32Cam,
            name = "ESP32-CAM (Vision)",
            role = "OV2640 camera streaming module"
        )
    )

    override fun getDriveSystemConfig(): DriveSystemConfig = DriveSystemConfig()

    override fun getIrrigationConfig(): IrrigationSystemConfig = IrrigationSystemConfig()

    override fun getPowerConfig(): PowerSystemConfig = PowerSystemConfig()
}
