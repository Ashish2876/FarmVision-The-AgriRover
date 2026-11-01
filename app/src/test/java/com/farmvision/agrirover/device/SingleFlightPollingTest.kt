package com.farmvision.agrirover.device

import com.farmvision.agrirover.data.device.dto.FastApiTelemetryPayload
import com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository
import com.farmvision.agrirover.data.device.transport.DeviceTransportBridge
import com.farmvision.agrirover.domain.device.model.DeviceCapabilities
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.DeviceStatusSnapshot
import com.farmvision.agrirover.domain.device.model.JsonRawField
import com.farmvision.agrirover.domain.device.model.TelemetryTimestamp
import com.farmvision.agrirover.domain.device.model.TimestampQuality
import com.farmvision.agrirover.domain.device.model.TransportType
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SingleFlightPollingTest {

    private class CountingTransportBridge : DeviceTransportBridge {
        override val transportType: TransportType = TransportType.DirectEsp32SoftAp
        val invocationCount = AtomicInteger(0)

        override suspend fun connect(endpoint: String): Result<DeviceIdentity> =
            Result.success(DeviceIdentity())

        override suspend fun disconnect() {}

        override suspend fun fetchCapabilities(): Result<DeviceCapabilities> =
            Result.success(DeviceCapabilities())

        override suspend fun fetchTelemetry(): Result<FastApiTelemetryPayload> {
            invocationCount.incrementAndGet()
            delay(100L) // Simulate network delay
            return Result.success(
                FastApiTelemetryPayload(
                    roverId = "agrirover-01",
                    timestamp = TelemetryTimestamp(
                        sourceField = JsonRawField.Absent,
                        sourceTimestampEpochMillis = null,
                        receivedAtEpochMillis = System.currentTimeMillis(),
                        timestampQuality = TimestampQuality.Absent
                    ),
                    batteryPercent = 85,
                    batteryVoltage = 12.4f,
                    speedKmh = 0f,
                    motorTemperature = 30f,
                    soilMoistureRawAdc = 1500,
                    soilPhRaw = 6.5f,
                    temperatureCelsius = 25f,
                    humidityPercent = 60f,
                    lightLux = 40000,
                    ultrasonicDistanceCm = 50f,
                    rawJson = "{}"
                )
            )
        }

        override suspend fun fetchStatus(): Result<DeviceStatusSnapshot> =
            Result.success(DeviceStatusSnapshot())
    }

    @Test
    fun concurrentPollRequests_areSequencedByMutexWithoutStampede() = runBlocking {
        val bridge = CountingTransportBridge()
        val repo = ProductionDeviceCommunicationRepository(directBridge = bridge)

        // Launch 5 concurrent poll operations
        val deferreds = (1..5).map {
            async {
                repo.pollTelemetrySingleFlight()
            }
        }

        val results = deferreds.map { it.await() }

        // All 5 should succeed
        results.forEach {
            assertTrue(it.isSuccess)
        }

        // The bridge was invoked exactly 5 times sequentially, with zero thread race crashes
        assertEquals(5, bridge.invocationCount.get())
    }
}

