package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ConfigurationStatus
import com.farmvision.agrirover.data.model.hardware.HardwareCatalogSeed
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareFilterCategory
import com.farmvision.agrirover.ui.hardware.model.HardwareUiDataMapper
import com.farmvision.agrirover.ui.hardware.model.SensorHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for Milestone H1: Hardware Center domain models, counting rules,
 * catalog identities, and status honesty semantics.
 */
class HardwareCenterUnitTest {

    @Test
    fun catalog_containsExactly23ComponentsFromMasterInventory() {
        val items = HardwareCatalogSeed.INVENTORY_ITEMS
        assertEquals("Master catalog must contain exactly 23 components", 23, items.size)
    }

    @Test
    fun catalog_noComponentIsMarkedVerified() {
        HardwareCatalogSeed.INVENTORY_ITEMS.forEach { item ->
            assertFalse(
                "Component '${item.id}' must NOT be marked Verified without physical evidence",
                item.status == ConfigurationStatus.Verified
            )
        }
    }

    @Test
    fun electronicVsPassiveCategorization_hasExactly13ElectronicAnd10Passive() {
        val items = HardwareCatalogSeed.INVENTORY_ITEMS
        val electronic = items.filter { HardwareUiDataMapper.isElectronicComponent(it.id) }
        val passive = items.filter { !HardwareUiDataMapper.isElectronicComponent(it.id) }

        assertEquals("Must identify exactly 13 electronic components", 13, electronic.size)
        assertEquals("Must identify exactly 10 passive/mechanical/inspection components", 10, passive.size)
    }

    @Test
    fun countingRules_initialUnverifiedStateHasZeroConnectedAnd13Unknown() {
        val uiItems = HardwareUiDataMapper.createUiItems(isDemoMode = false)
        val counts = HardwareUiDataMapper.computeSummaryCounts(uiItems)

        assertEquals("Confirmed Connected must be 0 without physical evidence", 0, counts.confirmedConnected)
        assertEquals("Disconnected must be 0 when unpolled", 0, counts.disconnected)
        assertEquals("Warning or fault must be 0", 0, counts.warningOrFault)
        assertEquals("Unknown / Draft must be exactly 13 for all electronic components", 13, counts.unknownOrDraft)
        assertEquals("Passive inventory must be exactly 10", 10, counts.passiveInventory)
    }

    @Test
    fun demoMode_neverMarksAnyComponentAsConnectedWithoutEvidence() {
        val uiItems = HardwareUiDataMapper.createUiItems(isDemoMode = true)
        val connectedItems = uiItems.filter { it.connectionStatus == EndpointConnectionStatus.Connected }

        assertTrue(
            "Demo mode must never fabricate a 'Connected' status for any hardware item",
            connectedItems.isEmpty()
        )
    }

    @Test
    fun honestyDisclaimers_presentForActuatorsAndUncalibratedSensors() {
        val uiItems = HardwareUiDataMapper.createUiItems(isDemoMode = true)

        val pumpItem = uiItems.first { it.component.id == "hw-irrigation-pump" }
        assertNotNull("Pump must have an honesty disclaimer", pumpItem.honestyDisclaimer)
        assertTrue("Pump disclaimer must mention no flow sensor", pumpItem.honestyDisclaimer!!.contains("NO FLOW SENSOR"))

        val motorItem = uiItems.first { it.component.id == "hw-motors-bo-geared" }
        assertNotNull("Motors must have an honesty disclaimer", motorItem.honestyDisclaimer)
        assertTrue("Motor disclaimer must mention no encoders", motorItem.honestyDisclaimer!!.contains("NO ENCODERS"))

        val servoItem = uiItems.first { it.component.id == "hw-servo-camera" }
        assertNotNull("Servo must have an honesty disclaimer", servoItem.honestyDisclaimer)
        assertTrue("Servo disclaimer must mention no position feedback", servoItem.honestyDisclaimer!!.contains("NO SERVO POSITION FEEDBACK"))

        val batteryItem = uiItems.first { it.component.id == "hw-power-battery" }
        assertNotNull("Battery must have an honesty disclaimer", batteryItem.honestyDisclaimer)
        assertTrue("Battery disclaimer must mention no voltage sensing", batteryItem.honestyDisclaimer!!.contains("NO BATTERY VOLTAGE SENSING"))
    }

    @Test
    fun draftCandidatePins_strictlyExcludesStrappingAndFlashPins() {
        val uiItems = HardwareUiDataMapper.createUiItems(isDemoMode = true)
        val moistureItem = uiItems.first { it.component.id == "hw-sensor-soil-moisture-probes" }

        // Moisture probes must use ADC1 pins (e.g. 32, 34)
        val probe1Pin = moistureItem.draftPins["Probe 1 ADC (ADC1)"]
        val probe2Pin = moistureItem.draftPins["Probe 2 ADC (ADC1)"]

        assertEquals(32, probe1Pin)
        assertEquals(34, probe2Pin)
    }
}
