package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.HardwareConnectionSettings
import com.farmvision.agrirover.data.network.FakeRoverNetworkBinder
import com.farmvision.agrirover.data.network.HttpResponse
import com.farmvision.agrirover.data.network.RoverAuthenticationException
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.RoverNetworkException
import com.farmvision.agrirover.data.network.RoverProtocolClient
import com.farmvision.agrirover.data.network.StandardRoverHttpClient
import com.farmvision.agrirover.domain.protocol.CommandAckStatus
import com.farmvision.agrirover.domain.protocol.CommandPriority
import com.farmvision.agrirover.domain.protocol.FirmwareStage
import com.farmvision.agrirover.domain.protocol.ProtocolConstants
import com.farmvision.agrirover.domain.protocol.RoverCommand
import com.farmvision.agrirover.ui.hardware.diagnostic.DefaultConnectionTester
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareOperationMode
import com.farmvision.agrirover.ui.hardware.viewmodel.HardwareCenterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Milestone H4 Comprehensive Unit Test Suite.
 * Validates Android networking layer, scoped cleartext security config,
 * Wi-Fi SoftAP binding lifecycle, token redaction, HTTP REST protocol client,
 * timeout/error mappings, and ViewModel live telemetry integration.
 */
class HardwareNetworkingH4UnitTest {

    // =========================================================================
    // 1. Scoped Cleartext Security Configuration Verification
    // =========================================================================

    @Test
    fun verifyNetworkSecurityConfigFile_ScopedCleartextOnly() {
        val configFile = File("src/main/res/xml/network_security_config.xml")
        val fallbackFile = File("app/src/main/res/xml/network_security_config.xml")
        val file = if (configFile.exists()) configFile else fallbackFile
        assertTrue("network_security_config.xml must exist", file.exists())

        val content = file.readText()

        // Verify cleartext is explicitly permitted for local rover subnet and loopback
        assertTrue(content.contains("192.168.4.1"))
        assertTrue(content.contains("192.168.4.2"))
        assertTrue(content.contains("192.168.4.0"))
        assertTrue(content.contains("10.0.2.2"))
        assertTrue(content.contains("127.0.0.1"))
        assertTrue(content.contains("localhost"))

        // Verify base-config prohibits app-wide cleartext
        assertTrue(
            "Base config must prohibit cleartext to external internet",
            content.contains("""<base-config cleartextTrafficPermitted="false"""")
        )
    }

    @Test
    fun verifyAndroidManifest_DeclaresNetworkPermissionsAndSecurityConfig() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val fallbackFile = File("app/src/main/AndroidManifest.xml")
        val file = if (manifestFile.exists()) manifestFile else fallbackFile
        assertTrue("AndroidManifest.xml must exist", file.exists())

        val content = file.readText()
        assertTrue(content.contains("android.permission.INTERNET"))
        assertTrue(content.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue(content.contains("android.permission.ACCESS_WIFI_STATE"))
        assertTrue(content.contains("""android:networkSecurityConfig="@xml/network_security_config""""))
    }

    // =========================================================================
    // 2. Token Redaction & URL Sanitization Invariants
    // =========================================================================

    @Test
    fun redactToken_HidesSensitiveSecret() {
        assertEquals("<none>", StandardRoverHttpClient.redactToken(null))
        assertEquals("<none>", StandardRoverHttpClient.redactToken(""))
        assertEquals("****", StandardRoverHttpClient.redactToken("abc"))
        assertEquals("ro****en", StandardRoverHttpClient.redactToken("rover-token"))
    }

    @Test
    fun redactUrl_RemovesTokenFromQueryParameters() {
        val url = "http://192.168.4.1/api/v1/telemetry?token=supersecret123&sensor=all"
        val redacted = StandardRoverHttpClient.redactUrl(url)
        assertFalse(redacted.contains("supersecret123"))
        assertTrue(redacted.contains("token=REDACTED"))
    }

    // =========================================================================
    // 3. Wi-Fi SoftAP Network Binder Tests
    // =========================================================================

    @Test
    fun fakeRoverNetworkBinder_Lifecycle() {
        val binder = FakeRoverNetworkBinder(initialBound = false)
        assertFalse(binder.isBoundToRoverWifi.value)
        assertNull(binder.boundNetworkInfo.value)

        binder.startListening()
        assertTrue(binder.isBoundToRoverWifi.value)
        assertNotNull(binder.boundNetworkInfo.value)

        binder.unbind()
        assertFalse(binder.isBoundToRoverWifi.value)
        assertNull(binder.boundNetworkInfo.value)
    }

    // =========================================================================
    // 4. RoverProtocolClient with Mock HTTP
    // =========================================================================

    private class MockRoverHttpClient : RoverHttpClient {
        var nextGetResponse: HttpResponse? = null
        var nextPostResponse: HttpResponse? = null
        var shouldThrow: Exception? = null

        var lastGetUrl: String? = null
        var lastGetHeaders: Map<String, String>? = null
        var lastPostBody: String? = null
        var lastPostHeaders: Map<String, String>? = null

        override suspend fun get(
            url: String,
            headers: Map<String, String>,
            timeoutMs: Long
        ): HttpResponse {
            lastGetUrl = url
            lastGetHeaders = headers
            shouldThrow?.let { throw it }
            return nextGetResponse ?: HttpResponse(200, "{}", 10L)
        }

        override suspend fun post(
            url: String,
            body: String,
            headers: Map<String, String>,
            timeoutMs: Long
        ): HttpResponse {
            lastPostBody = body
            lastPostHeaders = headers
            shouldThrow?.let { throw it }
            return nextPostResponse ?: HttpResponse(200, "{}", 10L)
        }
    }

    @Test
    fun fetchCapabilities_SuccessF0() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/capabilities_response_f0.json")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/capabilities_response_f0.json")
        val json = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

        mockHttp.nextGetResponse = HttpResponse(200, json, 12L)
        val client = RoverProtocolClient(mockHttp)

        val result = client.fetchCapabilities("http://192.168.4.1", authToken = "secret-token")
        assertTrue(result.isSuccess)

        val caps = result.getOrThrow()
        assertEquals(FirmwareStage.F0, caps.firmwareStage)
        assertEquals("agrirover-main-esp32", caps.deviceId)
        assertEquals(4, caps.supportedSensors.size)
        assertEquals(2, caps.supportedActuators.size)
        assertFalse(caps.isActuatorEnabled("hw-motors-bo-geared"))

        // Verify token header was passed
        assertEquals("secret-token", mockHttp.lastGetHeaders?.get(ProtocolConstants.HEADER_AUTH_TOKEN))
    }

    @Test
    fun fetchTelemetry_SuccessValidPacket() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/telemetry_packet_valid.json")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/telemetry_packet_valid.json")
        val json = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

        mockHttp.nextGetResponse = HttpResponse(200, json, 8L)
        val client = RoverProtocolClient(mockHttp)

        val result = client.fetchTelemetry("http://192.168.4.1")
        assertTrue(result.isSuccess)

        val packet = result.getOrThrow()
        assertEquals(1042L, packet.sequenceNumber)
        assertEquals(5, packet.readings.size)
        assertEquals(42.5, packet.readings["hw-sensor-soil-moisture-probes"]?.value ?: 0.0, 0.001)
        assertEquals(2200.0, packet.readings["hw-sensor-soil-moisture-probe-2"]?.value ?: 0.0, 0.001)
        assertEquals(26.8, packet.readings["hw-sensor-dht22"]?.value ?: 0.0, 0.001)
    }

    @Test
    fun probeEndpoint_MainEsp32_Success() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/capabilities_response_f0.json")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/capabilities_response_f0.json")
        val json = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

        mockHttp.nextGetResponse = HttpResponse(200, json, 15L)
        val client = RoverProtocolClient(mockHttp)

        val probe = client.probeEndpoint(
            target = ControllerTarget.Esp32DevKitV1,
            host = "192.168.4.1",
            port = 80,
            authToken = "rover-token"
        )
        assertTrue(probe.success)
        assertEquals(200, probe.statusCode)
        assertEquals(15L, probe.latencyMs)
        assertTrue(probe.message.contains("Stage F0"))
    }

    @Test
    fun probeEndpoint_MainEsp32_401Unauthorized() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        mockHttp.nextGetResponse = HttpResponse(401, "{\"error\":\"ERR_AUTH_INVALID\"}", 8L)
        val client = RoverProtocolClient(mockHttp)

        val probe = client.probeEndpoint(
            target = ControllerTarget.Esp32DevKitV1,
            host = "192.168.4.1",
            port = 80,
            authToken = "wrong-token"
        )
        assertFalse(probe.success)
        assertEquals(401, probe.statusCode)
        assertTrue(probe.message.contains("401 Unauthorized"))
    }

    @Test
    fun probeEndpoint_NetworkTimeout_MappedCleanly() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        mockHttp.shouldThrow = RoverNetworkException(
            message = "Request timed out after 2000ms",
            isTimeout = true
        )
        val client = RoverProtocolClient(mockHttp)

        val probe = client.probeEndpoint(
            target = ControllerTarget.Esp32DevKitV1,
            host = "192.168.4.1",
            port = 80
        )
        assertFalse(probe.success)
        assertNull(probe.statusCode)
        assertTrue(probe.message.contains("timed out"))
    }

    @Test
    fun probeEndpoint_Esp32Cam_Success() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        mockHttp.nextGetResponse = HttpResponse(200, "{\"status\":\"STREAMING\"}", 9L)
        val client = RoverProtocolClient(mockHttp)

        val probe = client.probeEndpoint(
            target = ControllerTarget.Esp32Cam,
            host = "192.168.4.2",
            port = 81
        )
        assertTrue(probe.success)
        assertEquals(200, probe.statusCode)
        assertTrue(probe.message.contains("ESP32-CAM reachable"))
        assertEquals("http://192.168.4.2:81/status", mockHttp.lastGetUrl)
    }

    @Test
    fun validateCameraStream_SuccessWithMjpegContentTypeAndBody() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        mockHttp.nextGetResponse = HttpResponse(
            statusCode = 200,
            body = "--frame\r\nContent-Type: image/jpeg\r\n\r\n<frame_bytes>",
            latencyMs = 25L,
            headers = mapOf("Content-Type" to listOf("multipart/x-mixed-replace; boundary=frame"))
        )
        val client = RoverProtocolClient(mockHttp)

        val result = client.validateCameraStream("http://192.168.4.2:81/stream")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("Live Video Validated"))
    }

    @Test
    fun validateCameraStream_FailsWhenContentTypeNotVideo() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        // Generic 200 with text/html does NOT count as a valid video stream (Default D6)
        mockHttp.nextGetResponse = HttpResponse(
            statusCode = 200,
            body = "<html><body>Index</body></html>",
            latencyMs = 12L,
            headers = mapOf("Content-Type" to listOf("text/html"))
        )
        val client = RoverProtocolClient(mockHttp)

        val result = client.validateCameraStream("http://192.168.4.2:81/stream")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not a valid video stream") == true)
    }

    @Test
    fun validateCameraStream_FailsWhenEmptyBody() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        mockHttp.nextGetResponse = HttpResponse(
            statusCode = 200,
            body = "",
            latencyMs = 10L,
            headers = mapOf("Content-Type" to listOf("image/jpeg"))
        )
        val client = RoverProtocolClient(mockHttp)

        val result = client.validateCameraStream("http://192.168.4.2:81/stream")
        assertTrue(result.isFailure)
    }

    @Test
    fun dispatchCommand_SuccessAck() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/command_ack_accepted.json")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/command_ack_accepted.json")
        val json = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

        mockHttp.nextPostResponse = HttpResponse(200, json, 14L)
        val client = RoverProtocolClient(mockHttp)

        val cmd = RoverCommand(
            cmdId = "cmd-170000-001",
            sequence = 501L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1700000000000L,
            priority = CommandPriority.NORMAL,
            parameters = mapOf("throttle" to 0.65, "steering" to 0.0)
        )

        val result = client.dispatchCommand("http://192.168.4.1", cmd, authToken = "valid-token")
        assertTrue(result.isSuccess)

        val ack = result.getOrThrow()
        assertEquals("cmd-170000-001", ack.cmdId)
        assertEquals(CommandAckStatus.ACCEPTED, ack.status)
        assertTrue(ack.isAccepted)
    }

    @Test
    fun dispatchEmergencyStop_SendsCriticalPost() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val ackJson = """
            {
              "protocolVersion": "1.0.0",
              "cmdId": "estop-101",
              "status": "ACCEPTED",
              "reasonCode": "ESTOP_ENGAGED",
              "latencyMs": 2,
              "timestampMs": 1700000000012
            }
        """.trimIndent()

        mockHttp.nextPostResponse = HttpResponse(200, ackJson, 2L)
        val client = RoverProtocolClient(mockHttp)

        val result = client.dispatchEmergencyStop("http://192.168.4.1", authToken = "valid-token")
        assertTrue(result.isSuccess)
        assertTrue(mockHttp.lastPostBody?.contains("EMERGENCY_STOP") == true)
        assertTrue(mockHttp.lastPostBody?.contains("CRITICAL") == true)
    }

    @Test
    fun dispatchCommand_FailsClosedWhenAuthTokenMissing() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val client = RoverProtocolClient(mockHttp)
        val cmd = RoverCommand(
            cmdId = "cmd-001",
            sequence = 1L,
            commandType = "DRIVE_DIFFERENTIAL",
            createdTimestampMs = 1700000000000L,
            priority = CommandPriority.NORMAL,
            parameters = mapOf("throttle" to 0.5)
        )

        // Null token fails closed without hitting network
        val resultNull = client.dispatchCommand("http://192.168.4.1", cmd, authToken = null)
        assertTrue(resultNull.isFailure)
        assertTrue(resultNull.exceptionOrNull() is RoverAuthenticationException)
        assertTrue(resultNull.exceptionOrNull()?.message?.contains("Bearer token is not provisioned") == true)

        // Blank token fails closed
        val resultBlank = client.dispatchCommand("http://192.168.4.1", cmd, authToken = "   ")
        assertTrue(resultBlank.isFailure)
        assertTrue(resultBlank.exceptionOrNull() is RoverAuthenticationException)
    }

    @Test
    fun dispatchEmergencyStop_SucceedsEvenWithoutAuthToken() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val ackJson = """
            {
              "protocolVersion": "1.0.0",
              "cmdId": "estop-engaged",
              "status": "ACCEPTED",
              "reasonCode": "ESTOP_ENGAGED",
              "latencyMs": 1,
              "timestampMs": 1700000000012
            }
        """.trimIndent()
        mockHttp.nextPostResponse = HttpResponse(200, ackJson, 1L)
        val client = RoverProtocolClient(mockHttp)

        // Unconditional E-Stop succeeds with null auth token
        val result = client.dispatchEmergencyStop("http://192.168.4.1", authToken = null)
        assertTrue(result.isSuccess)
        val ack = result.getOrThrow()
        assertTrue(ack.isAccepted)
        assertEquals("estop-engaged", ack.cmdId)
    }

    @Test
    fun dispatchEmergencyStopReset_RequiresAuthToken() = runBlocking {
        val mockHttp = MockRoverHttpClient()
        val client = RoverProtocolClient(mockHttp)

        // Blank auth token fails closed
        val failResult = client.dispatchEmergencyStopReset("http://192.168.4.1", authToken = "")
        assertTrue(failResult.isFailure)
        assertTrue(failResult.exceptionOrNull() is RoverAuthenticationException)

        // Valid auth token dispatches request and parses response
        val ackJson = """
            {
              "protocolVersion": "1.0.0",
              "cmdId": "estop-reset",
              "status": "ACCEPTED",
              "reasonCode": "ESTOP_RESET",
              "latencyMs": 2,
              "timestampMs": 1700000000020
            }
        """.trimIndent()
        mockHttp.nextPostResponse = HttpResponse(200, ackJson, 2L)

        val successResult = client.dispatchEmergencyStopReset("http://192.168.4.1", authToken = "valid-secret-token")
        assertTrue(successResult.isSuccess)
        val ack = successResult.getOrThrow()
        assertTrue(ack.isAccepted)
        assertEquals("ESTOP_RESET", ack.reasonCode)
        assertEquals("valid-secret-token", mockHttp.lastPostHeaders?.get(ProtocolConstants.HEADER_AUTH_TOKEN))
    }

    // =========================================================================
    // 5. ViewModel Live Mode Telemetry Polling Integration
    // =========================================================================

    private class FakeRepo(initialSettings: HardwareConnectionSettings) : com.farmvision.agrirover.data.repository.HardwareSettingsRepository {
        private val _flow = MutableStateFlow(initialSettings)
        override val settingsFlow: StateFlow<HardwareConnectionSettings> = _flow.asStateFlow()
        override fun getSettings(): HardwareConnectionSettings = _flow.value
        override fun updateSettings(settings: HardwareConnectionSettings) { _flow.value = settings }
        override fun setOperationMode(mode: HardwareOperationMode) {
            _flow.value = _flow.value.copy(operationMode = mode)
        }
        override fun resetToDefaults() {
            _flow.value = HardwareConnectionSettings()
        }
    }

    @Test
    fun viewModel_LiveModeRefresh_SuccessfulTelemetryUpdatesUI() = runBlocking {
        val liveSettings = HardwareConnectionSettings(
            operationMode = HardwareOperationMode.Live,
            mainEsp32Host = "192.168.4.1",
            mainEsp32Port = 80,
            authToken = "rover-live-token"
        )
        val repo = FakeRepo(liveSettings)
        val mockHttp = MockRoverHttpClient()

        val stream = javaClass.classLoader?.getResourceAsStream("protocol_fixtures/telemetry_packet_valid.json")
            ?: javaClass.getResourceAsStream("/protocol_fixtures/telemetry_packet_valid.json")
        val json = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        mockHttp.nextGetResponse = HttpResponse(200, json, 10L)

        val protocolClient = RoverProtocolClient(mockHttp)
        val tester = DefaultConnectionTester(protocolClient)
        val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)

        val viewModel = HardwareCenterViewModel(
            settingsRepository = repo,
            connectionTester = tester,
            protocolClient = protocolClient,
            externalScope = testScope
        )

        // Invoke refreshTelemetry in Live mode
        viewModel.refreshTelemetry()

        val state = viewModel.uiState.value
        assertFalse(state.isDemoMode)

        // Find main controller item
        val esp32Item = state.items.find { it.component.id == "hw-controller-esp32" }
        assertNotNull(esp32Item)
        assertEquals(EndpointConnectionStatus.Connected, esp32Item?.connectionStatus)
        assertTrue(esp32Item?.freshnessText?.contains("seq #1042") == true)

        // Find soil moisture item
        val moistureItem = state.items.find { it.component.id == "hw-sensor-soil-moisture-probes" }
        assertNotNull(moistureItem)
        assertEquals(EndpointConnectionStatus.Connected, moistureItem?.connectionStatus)
        assertTrue(moistureItem?.latestValueDisplay?.contains("42.5%") == true)
        assertEquals("Live (Fresh)", moistureItem?.freshnessText)

        // Summary counts updated
        assertTrue(state.summaryCounts.confirmedConnected > 0)
    }

    @Test
    fun viewModel_LiveModeRefresh_FailureHandlesGracefullyWithoutCrash() = runBlocking {
        val liveSettings = HardwareConnectionSettings(
            operationMode = HardwareOperationMode.Live,
            mainEsp32Host = "192.168.4.1",
            mainEsp32Port = 80
        )
        val repo = FakeRepo(liveSettings)
        val mockHttp = MockRoverHttpClient()
        mockHttp.shouldThrow = RoverNetworkException("Connection refused to 192.168.4.1:80", isUnreachable = true)

        val protocolClient = RoverProtocolClient(mockHttp)
        val tester = DefaultConnectionTester(protocolClient)
        val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)

        val viewModel = HardwareCenterViewModel(
            settingsRepository = repo,
            connectionTester = tester,
            protocolClient = protocolClient,
            externalScope = testScope
        )

        // Refresh should fail cleanly and mark items disconnected
        viewModel.refreshTelemetry()

        val state = viewModel.uiState.value
        val esp32Item = state.items.find { it.component.id == "hw-controller-esp32" }
        assertNotNull(esp32Item)
        assertEquals(EndpointConnectionStatus.Disconnected, esp32Item?.connectionStatus)
        assertEquals("Unreachable", esp32Item?.freshnessText)
        assertTrue(state.summaryCounts.disconnected > 0)
    }
}
