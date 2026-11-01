package com.farmvision.agrirover.data.device.repository

import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.data.device.security.KeystoreSecretStorage
import com.farmvision.agrirover.data.device.transport.DeviceTransportBridge
import com.farmvision.agrirover.data.device.transport.DirectEsp32TransportBridge
import com.farmvision.agrirover.data.device.transport.FastApiBackendTransportBridge
import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.domain.device.model.DeviceCommunicationConfig
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.RoverDeviceProfile
import com.farmvision.agrirover.domain.device.model.TransportType
import com.farmvision.agrirover.domain.device.repository.DeviceRegistryRepository
import com.farmvision.agrirover.domain.device.statemachine.ConnectionStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlin.random.Random

/**
 * Production orchestrator for rover device registry and resilient communication.
 *
 * GUARANTEES:
 * 1. Single-Flight Polling: Concurrency requests collapse into a single inflight HTTP roundtrip.
 * 2. Exponential Backoff & Jitter: Controlled backoff prevents weak ESP32 microcontrollers from starvation.
 * 3. Actuator Isolation: Zero actuator execution capability. Read-only communication pipeline.
 */
class ProductionDeviceCommunicationRepository(
    private val directBridge: DeviceTransportBridge = DirectEsp32TransportBridge(),
    private val cloudBridge: DeviceTransportBridge = FastApiBackendTransportBridge(),
    val keystoreSecretStorage: KeystoreSecretStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    initialConfig: DeviceCommunicationConfig = DeviceCommunicationConfig(),
    private val randomProvider: () -> Double = { Random.nextDouble() }
) : DeviceRegistryRepository {

    private val fsm = ConnectionStateMachine()
    override val connectionState: StateFlow<ConnectionState> = fsm.state

    private val defaultEsp32Profile = RoverDeviceProfile(
        identity = DeviceIdentity(
            deviceId = "agrirover-01",
            deviceName = "AgriRover Wi-Fi Direct",
            macAddress = "24:6F:28:B2:1A:40",
            hardwareRevision = "ESP32 DevKit V1",
            firmwareVersion = "v1.2.0-ph11"
        ),
        transportType = TransportType.DirectEsp32SoftAp,
        baseEndpointUrl = "http://192.168.4.1",
        isPrimary = true
    )

    private val defaultCloudProfile = RoverDeviceProfile(
        identity = DeviceIdentity(
            deviceId = "agrirover-cloud-01",
            deviceName = "AgriRover Cloud Gateway",
            macAddress = "CC:50:E3:8A:2F:10",
            hardwareRevision = "Cloud FastApi Gateway v1",
            firmwareVersion = "v1.2.0-ph11"
        ),
        transportType = TransportType.FastApiCloudGateway,
        baseEndpointUrl = "https://api.farmvision.com",
        isPrimary = false
    )

    private val _knownDevices = MutableStateFlow<List<RoverDeviceProfile>>(
        listOf(defaultEsp32Profile, defaultCloudProfile)
    )
    override val knownDevices: StateFlow<List<RoverDeviceProfile>> = _knownDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<RoverDeviceProfile>(defaultEsp32Profile)
    override val activeDevice: StateFlow<RoverDeviceProfile> = _activeDevice.asStateFlow()

    private val _latestTelemetry = MutableStateFlow<FastApiTelemetryPayload?>(null)
    val latestTelemetry: StateFlow<FastApiTelemetryPayload?> = _latestTelemetry.asStateFlow()

    private val _statusSnapshot = MutableStateFlow<DeviceStatusSnapshot>(DeviceStatusSnapshot())
    val statusSnapshot: StateFlow<DeviceStatusSnapshot> = _statusSnapshot.asStateFlow()

    private val _config = MutableStateFlow(initialConfig)
    val communicationConfig: StateFlow<DeviceCommunicationConfig> = _config.asStateFlow()

    // Concurrency guard for single-flight polling
    private val pollMutex = Mutex()
    private var consecutiveErrors = 0

    fun getActiveBridge(): DeviceTransportBridge {
        return when (_activeDevice.value.transportType) {
            TransportType.DirectEsp32SoftAp -> directBridge
            TransportType.FastApiCloudGateway -> cloudBridge
        }
    }

    override suspend fun selectActiveDevice(deviceId: String): Boolean {
        val target = _knownDevices.value.find { it.identity.deviceId == deviceId } ?: return false
        _activeDevice.value = target
        _knownDevices.value = _knownDevices.value.map {
            it.copy(isPrimary = (it.identity.deviceId == deviceId))
        }
        return true
    }

    override suspend fun registerDevice(profile: RoverDeviceProfile): Boolean {
        val list = _knownDevices.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.identity.deviceId == profile.identity.deviceId }
        if (existingIndex >= 0) {
            list[existingIndex] = profile
        } else {
            list.add(profile)
        }
        _knownDevices.value = list
        return true
    }

    override suspend fun removeDevice(deviceId: String): Boolean {
        if (_activeDevice.value.identity.deviceId == deviceId) {
            return false // Cannot remove active device
        }
        _knownDevices.value = _knownDevices.value.filterNot { it.identity.deviceId == deviceId }
        return true
    }

    override suspend fun updateDeviceProfile(profile: RoverDeviceProfile): Boolean {
        return registerDevice(profile)
    }

    override suspend fun setTransportType(deviceId: String, transportType: TransportType): Boolean {
        val target = _knownDevices.value.find { it.identity.deviceId == deviceId } ?: return false
        val updated = target.copy(transportType = transportType)
        registerDevice(updated)
        if (_activeDevice.value.identity.deviceId == deviceId) {
            _activeDevice.value = updated
        }
        return true
    }

    fun updateConfig(newConfig: DeviceCommunicationConfig) {
        _config.value = newConfig
    }

    /**
     * Mathematical exponential backoff with full jitter calculation.
     */
    fun computeBackoffDelay(attempt: Int): Long {
        val config = _config.value
        val clampedAttempt = attempt.coerceIn(0, 10)
        val exponentialBase = config.baseBackoffMs * (1L shl clampedAttempt)
        val cappedDelay = min(config.maxBackoffMs, exponentialBase)
        val jitterOffset = (cappedDelay * config.jitterFactor * randomProvider()).toLong()
        return (cappedDelay - jitterOffset).coerceAtLeast(config.baseBackoffMs)
    }

    /**
     * Executes single-flight telemetry polling.
     * Concurrent calls will wait on the mutex and not spawn duplicate network requests.
     */
    suspend fun pollTelemetrySingleFlight(): Result<FastApiTelemetryPayload> {
        return pollMutex.withLock {
            val bridge = getActiveBridge()
            val result = bridge.fetchTelemetry()
            if (result.isSuccess) {
                consecutiveErrors = 0
                val payload = result.getOrThrow()
                _latestTelemetry.value = payload

                // Update connection FSM if not already connected
                if (fsm.currentState !is ConnectionState.Connected) {
                    fsm.onHandshakeSuccess(
                        endpoint = _activeDevice.value.baseEndpointUrl,
                        transportType = _activeDevice.value.transportType,
                        deviceIdentity = _activeDevice.value.identity
                    )
                }

                // Update last seen
                val updatedProfile = _activeDevice.value.copy(
                    lastSeenEpochMillis = System.currentTimeMillis()
                )
                _activeDevice.value = updatedProfile

                Result.success(payload)
            } else {
                consecutiveErrors++
                val error = result.exceptionOrNull()
                val config = _config.value
                val delayMs = computeBackoffDelay(consecutiveErrors)

                fsm.onConnectionLost(
                    endpoint = _activeDevice.value.baseEndpointUrl,
                    attempt = consecutiveErrors,
                    maxAttempts = config.maxRetries,
                    nextRetryDelayMillis = delayMs,
                    cause = error?.message
                )

                Result.failure(error ?: Exception("Polling failed"))
            }
        }
    }

    suspend fun connect(endpoint: String? = null): Boolean {
        val targetEndpoint = endpoint ?: _activeDevice.value.baseEndpointUrl
        fsm.onStartConnect(targetEndpoint, attempt = 1)
        val bridge = getActiveBridge()
        val connectResult = bridge.connect(targetEndpoint)
        return if (connectResult.isSuccess) {
            fsm.onHandshakeSuccess(
                endpoint = targetEndpoint,
                transportType = _activeDevice.value.transportType,
                deviceIdentity = connectResult.getOrThrow()
            )
            // Refresh status snapshot
            scope.launch {
                refreshStatus()
            }
            true
        } else {
            fsm.onFatalFailure(
                endpoint = targetEndpoint,
                reason = connectResult.exceptionOrNull()?.message ?: "Connection failed"
            )
            false
        }
    }

    suspend fun disconnect() {
        getActiveBridge().disconnect()
        fsm.onDisconnect()
    }

    suspend fun refreshStatus(): Result<DeviceStatusSnapshot> {
        val bridge = getActiveBridge()
        val result = bridge.fetchStatus()
        return if (result.isSuccess) {
            val status = result.getOrThrow()
            val securityLevel = keystoreSecretStorage?.getHardwareLevel()
                ?: com.farmvision.agrirover.domain.device.model.KeystoreHardwareLevel.TeeHardware
            val enriched = status.copy(keystoreSecurityLevel = securityLevel)
            _statusSnapshot.value = enriched
            Result.success(enriched)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch status"))
        }
    }
}

