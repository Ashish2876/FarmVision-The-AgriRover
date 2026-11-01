package com.farmvision.agrirover.domain.device.model

/**
 * Transport bridge type used to communicate with the rover.
 */
enum class TransportType(val displayName: String) {
    DirectEsp32SoftAp("Direct ESP32 Wi-Fi SoftAP"),
    FastApiCloudGateway("FastAPI Cloud Gateway")
}

/**
 * Active protocol mode configuration.
 */
enum class DeviceProtocolMode {
    DirectWifiSoftAp,
    BackendCloudRest
}

/**
 * Hardware and firmware identity metadata of an AgriRover unit.
 */
data class DeviceIdentity(
    val deviceId: String = "agrirover-01",
    val deviceName: String = "AgriRover DevKit V1",
    val macAddress: String = "24:6F:28:B2:1A:40",
    val hardwareRevision: String = "ESP32-WROOM-32D / DevKit V1",
    val firmwareVersion: String = "v1.2.0-ph11"
)

/**
 * Declared read-only hardware capabilities retrieved from /api/v1/capabilities.
 */
data class DeviceCapabilities(
    val supportsTelemetry: Boolean = true,
    val supportsCapabilities: Boolean = true,
    val supportsStatus: Boolean = true,
    val maxPollingHz: Float = 2.0f,
    val availableSensors: List<String> = listOf("soil_moisture", "dht22", "bh1750", "hc_sr04", "ina219"),
    val actuatorControlEnabled: Boolean = false
)

/**
 * Persisted profile for a known rover device.
 */
data class RoverDeviceProfile(
    val identity: DeviceIdentity = DeviceIdentity(),
    val transportType: TransportType = TransportType.DirectEsp32SoftAp,
    val baseEndpointUrl: String = "http://192.168.4.1",
    val isPrimary: Boolean = true,
    val lastSeenEpochMillis: Long? = null,
    val capabilities: DeviceCapabilities = DeviceCapabilities()
)

/**
 * Deterministic connection states for rover communication.
 */
sealed interface ConnectionState {
    val isConnected: Boolean get() = this is Connected

    data object Disconnected : ConnectionState

    data class Connecting(
        val endpoint: String,
        val attempt: Int = 1
    ) : ConnectionState

    data class Handshaking(
        val endpoint: String
    ) : ConnectionState

    data class Connected(
        val endpoint: String,
        val transportType: TransportType,
        val connectedAtEpochMillis: Long,
        val deviceIdentity: DeviceIdentity
    ) : ConnectionState

    data class Reconnecting(
        val endpoint: String,
        val attempt: Int,
        val maxAttempts: Int,
        val nextRetryDelayMillis: Long,
        val cause: String?
    ) : ConnectionState

    data class Failed(
        val endpoint: String,
        val reason: String,
        val failedAtEpochMillis: Long
    ) : ConnectionState
}

/**
 * Diagnostic metrics snapshot representing link health.
 */
data class DeviceStatusSnapshot(
    val isOnline: Boolean = false,
    val uptimeSeconds: Long = 0L,
    val wifiRssiDbm: Int = -100,
    val freeHeapBytes: Long = 0L,
    val transportType: TransportType = TransportType.DirectEsp32SoftAp,
    val lastPollLatencyMs: Long = 0L,
    val totalPacketsReceived: Long = 0L,
    val failedPacketsCount: Long = 0L,
    val keystoreSecurityLevel: KeystoreHardwareLevel = KeystoreHardwareLevel.TeeHardware
)

/**
 * Tunable parameters for device communication polling, timeouts, and backoff.
 */
data class DeviceCommunicationConfig(
    val pollingIntervalMs: Long = 1000L,
    val requestTimeoutMs: Long = 3000L,
    val maxRetries: Int = 5,
    val baseBackoffMs: Long = 500L,
    val maxBackoffMs: Long = 10000L,
    val jitterFactor: Float = 0.5f
)

