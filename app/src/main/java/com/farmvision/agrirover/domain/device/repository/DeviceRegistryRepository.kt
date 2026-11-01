package com.farmvision.agrirover.domain.device.repository

import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.domain.device.model.RoverDeviceProfile
import com.farmvision.agrirover.domain.device.model.TransportType
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository managing registered rover device profiles and active connection selection.
 */
interface DeviceRegistryRepository {
    val knownDevices: StateFlow<List<RoverDeviceProfile>>
    val activeDevice: StateFlow<RoverDeviceProfile>
    val connectionState: StateFlow<ConnectionState>

    suspend fun selectActiveDevice(deviceId: String): Boolean
    suspend fun registerDevice(profile: RoverDeviceProfile): Boolean
    suspend fun removeDevice(deviceId: String): Boolean
    suspend fun updateDeviceProfile(profile: RoverDeviceProfile): Boolean
    suspend fun setTransportType(deviceId: String, transportType: TransportType): Boolean
}

