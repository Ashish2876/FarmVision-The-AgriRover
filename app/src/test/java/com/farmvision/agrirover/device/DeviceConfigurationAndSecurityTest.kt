package com.farmvision.agrirover.device

import com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository
import com.farmvision.agrirover.domain.device.model.DeviceCommunicationConfig
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.KeystoreHardwareLevel
import com.farmvision.agrirover.domain.device.model.RoverDeviceProfile
import com.farmvision.agrirover.domain.device.model.TransportType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceConfigurationAndSecurityTest {

    @Test
    fun updateConfig_modifiesCommunicationParameters() {
        val repo = ProductionDeviceCommunicationRepository()
        val customConfig = DeviceCommunicationConfig(
            pollingIntervalMs = 2500L,
            requestTimeoutMs = 5000L,
            maxRetries = 3,
            baseBackoffMs = 1000L,
            maxBackoffMs = 15000L,
            jitterFactor = 0.25f
        )

        repo.updateConfig(customConfig)
        assertEquals(customConfig, repo.communicationConfig.value)
        assertEquals(2500L, repo.communicationConfig.value.pollingIntervalMs)
        assertEquals(5000L, repo.communicationConfig.value.requestTimeoutMs)
        assertEquals(3, repo.communicationConfig.value.maxRetries)
    }

    @Test
    fun deviceRegistry_managesProfilesAndActiveSelection() = runBlocking {
        val repo = ProductionDeviceCommunicationRepository()

        val initialDevices = repo.knownDevices.value
        assertEquals(2, initialDevices.size)

        // Register a 3rd device
        val customRover = RoverDeviceProfile(
            identity = DeviceIdentity(deviceId = "rover-custom-99", deviceName = "Custom Rover Beta"),
            transportType = TransportType.DirectEsp32SoftAp,
            baseEndpointUrl = "http://192.168.4.5"
        )
        assertTrue(repo.registerDevice(customRover))
        assertEquals(3, repo.knownDevices.value.size)

        // Select active device
        assertTrue(repo.selectActiveDevice("rover-custom-99"))
        assertEquals("rover-custom-99", repo.activeDevice.value.identity.deviceId)
        assertTrue(repo.activeDevice.value.isPrimary)

        // Cannot remove active device
        assertFalse(repo.removeDevice("rover-custom-99"))

        // Switch active back to agrirover-01, then remove custom
        assertTrue(repo.selectActiveDevice("agrirover-01"))
        assertTrue(repo.removeDevice("rover-custom-99"))
        assertEquals(2, repo.knownDevices.value.size)
    }

    @Test
    fun keystoreHardwareLevel_hierarchy_distinguishesBacking() {
        // Assert that StrongBox, TEE, Software, and RamOnlyFallback are distinct sealed objects
        val strongBox: KeystoreHardwareLevel = KeystoreHardwareLevel.StrongBoxHardware
        val tee: KeystoreHardwareLevel = KeystoreHardwareLevel.TeeHardware
        val software: KeystoreHardwareLevel = KeystoreHardwareLevel.Software
        val ramOnly: KeystoreHardwareLevel = KeystoreHardwareLevel.RamOnlyFallback

        assertTrue(strongBox is KeystoreHardwareLevel.StrongBoxHardware)
        assertTrue(tee is KeystoreHardwareLevel.TeeHardware)
        assertTrue(software is KeystoreHardwareLevel.Software)
        assertTrue(ramOnly is KeystoreHardwareLevel.RamOnlyFallback)

        assertFalse(strongBox == tee)
        assertFalse(tee == software)
        assertFalse(software == ramOnly)
    }
}

