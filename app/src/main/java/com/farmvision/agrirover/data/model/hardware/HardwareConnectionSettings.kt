package com.farmvision.agrirover.data.model.hardware

import com.farmvision.agrirover.ui.hardware.model.HardwareOperationMode

/**
 * Persistent network, endpoint, and authentication settings for the FarmVision rover microcontrollers.
 *
 * Secret Handling Rule:
 * - [authToken] is NEVER printed in cleartext by [toString] and is excluded from logs.
 */
data class HardwareConnectionSettings(
    val operationMode: HardwareOperationMode = HardwareOperationMode.Demo,
    val mainEsp32Host: String = DEFAULT_MAIN_ESP32_HOST,
    val mainEsp32Port: Int = DEFAULT_MAIN_ESP32_PORT,
    val esp32CamHost: String = DEFAULT_ESP32_CAM_HOST,
    val esp32CamPort: Int = DEFAULT_ESP32_CAM_PORT,
    val authToken: String = "",
    val connectionTimeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val telemetryPollingIntervalMs: Long = DEFAULT_POLLING_INTERVAL_MS
) {
    val isAuthTokenSet: Boolean
        get() = authToken.isNotBlank()

    val mainEsp32Url: String
        get() = "http://$mainEsp32Host:$mainEsp32Port"

    val esp32CamUrl: String
        get() = "http://$esp32CamHost:$esp32CamPort"

    override fun toString(): String {
        val maskedToken = if (authToken.isEmpty()) "<empty>" else "<redacted>"
        return "HardwareConnectionSettings(" +
            "operationMode=$operationMode, " +
            "mainEsp32Host='$mainEsp32Host', " +
            "mainEsp32Port=$mainEsp32Port, " +
            "esp32CamHost='$esp32CamHost', " +
            "esp32CamPort=$esp32CamPort, " +
            "authToken=$maskedToken, " +
            "connectionTimeoutMs=$connectionTimeoutMs, " +
            "telemetryPollingIntervalMs=$telemetryPollingIntervalMs)"
    }

    companion object {
        const val DEFAULT_MAIN_ESP32_HOST = "192.168.4.1"
        const val DEFAULT_MAIN_ESP32_PORT = 80
        const val DEFAULT_ESP32_CAM_HOST = "192.168.4.2"
        const val DEFAULT_ESP32_CAM_PORT = 81
        const val DEFAULT_TIMEOUT_MS = 2000L
        const val DEFAULT_POLLING_INTERVAL_MS = 500L
    }
}
