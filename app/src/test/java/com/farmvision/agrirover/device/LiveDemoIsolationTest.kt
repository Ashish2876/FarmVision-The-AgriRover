package com.farmvision.agrirover.device

import com.farmvision.agrirover.data.device.transport.MockFastApiBackendClient
import com.farmvision.agrirover.data.model.DataOrigin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDemoIsolationTest {

    @Test
    fun mockFastApiBackendClient_strictlyTaggedAsSimulated() = runBlocking {
        val mockClient = MockFastApiBackendClient(simulatedLatencyMs = 0L)

        // Verify client origin indicator
        assertEquals(DataOrigin.Simulated, mockClient.origin)
        assertNotEquals(DataOrigin.LiveHardware, mockClient.origin)

        // Fetch telemetry and verify payload
        val telemetryResult = mockClient.getTelemetry()
        assertTrue(telemetryResult.isSuccess)
        val payload = telemetryResult.getOrThrow()

        // Mock rover ID explicitly denotes simulated origin
        assertTrue(payload.roverId?.contains("mock", ignoreCase = true) == true)
    }

    @Test
    fun mockClient_errorInjection_failsPredictably() = runBlocking {
        val mockClient = MockFastApiBackendClient(
            simulatedLatencyMs = 0L,
            shouldFailNextRequest = true,
            failureReason = "Injected 503 Gateway Timeout"
        )

        val result = mockClient.getTelemetry()
        assertTrue(result.isFailure)
        assertEquals("Injected 503 Gateway Timeout", result.exceptionOrNull()?.message)
    }
}

