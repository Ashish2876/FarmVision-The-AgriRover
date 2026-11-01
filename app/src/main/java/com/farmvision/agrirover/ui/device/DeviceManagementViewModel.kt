package com.farmvision.agrirover.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository
import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.domain.device.model.DeviceCommunicationConfig
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.RoverDeviceProfile
import com.farmvision.agrirover.domain.device.model.TransportType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel governing device connection lifecycle, profile switching, diagnostics, and configurations.
 */
class DeviceManagementViewModel(
    val communicationRepository: ProductionDeviceCommunicationRepository = ProductionDeviceCommunicationRepository()
) : ViewModel() {

    val activeDevice: StateFlow<RoverDeviceProfile> = communicationRepository.activeDevice
    val knownDevices: StateFlow<List<RoverDeviceProfile>> = communicationRepository.knownDevices
    val connectionState: StateFlow<ConnectionState> = communicationRepository.connectionState
    val statusSnapshot: StateFlow<DeviceStatusSnapshot> = communicationRepository.statusSnapshot
    val latestTelemetry: StateFlow<FastApiTelemetryPayload?> = communicationRepository.latestTelemetry
    val communicationConfig: StateFlow<DeviceCommunicationConfig> = communicationRepository.communicationConfig

    fun selectDevice(deviceId: String) {
        viewModelScope.launch {
            communicationRepository.selectActiveDevice(deviceId)
        }
    }

    fun setTransportType(transportType: TransportType) {
        viewModelScope.launch {
            communicationRepository.setTransportType(activeDevice.value.identity.deviceId, transportType)
        }
    }

    fun connect() {
        viewModelScope.launch {
            communicationRepository.connect()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            communicationRepository.disconnect()
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            communicationRepository.connect()
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            communicationRepository.refreshStatus()
        }
    }

    fun updateConfig(newConfig: DeviceCommunicationConfig) {
        communicationRepository.updateConfig(newConfig)
    }

    fun saveBearerToken(token: String) {
        communicationRepository.keystoreSecretStorage?.saveBearerToken(token)
    }

    fun clearBearerToken() {
        communicationRepository.keystoreSecretStorage?.clearBearerToken()
    }
}

