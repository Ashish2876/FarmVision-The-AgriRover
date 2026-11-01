package com.farmvision.agrirover.device

import com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository
import com.farmvision.agrirover.domain.device.model.DeviceCommunicationConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyAndJitterTest {

    @Test
    fun computeBackoffDelay_respectsBaseAndMaxCap() {
        val config = DeviceCommunicationConfig(
            baseBackoffMs = 500L,
            maxBackoffMs = 8000L,
            jitterFactor = 0.0f // Zero jitter to test deterministic mathematical boundaries
        )
        val repo = ProductionDeviceCommunicationRepository(
            initialConfig = config,
            randomProvider = { 0.0 }
        )

        // Attempt 0: 500 * 2^0 = 500
        val delay0 = repo.computeBackoffDelay(0)
        assertTrue("Expected 500ms, got $delay0", delay0 == 500L)

        // Attempt 1: 500 * 2^1 = 1000
        val delay1 = repo.computeBackoffDelay(1)
        assertTrue("Expected 1000ms, got $delay1", delay1 == 1000L)

        // Attempt 2: 500 * 2^2 = 2000
        val delay2 = repo.computeBackoffDelay(2)
        assertTrue("Expected 2000ms, got $delay2", delay2 == 2000L)

        // Attempt 3: 500 * 2^3 = 4000
        val delay3 = repo.computeBackoffDelay(3)
        assertTrue("Expected 4000ms, got $delay3", delay3 == 4000L)

        // Attempt 4: 500 * 2^4 = 8000 (hits max cap)
        val delay4 = repo.computeBackoffDelay(4)
        assertTrue("Expected 8000ms, got $delay4", delay4 == 8000L)

        // Attempt 10: Still capped at maxBackoffMs (8000)
        val delay10 = repo.computeBackoffDelay(10)
        assertTrue("Expected 8000ms cap, got $delay10", delay10 == 8000L)
    }

    @Test
    fun computeBackoffDelay_appliesJitterWithinBounds() {
        val config = DeviceCommunicationConfig(
            baseBackoffMs = 1000L,
            maxBackoffMs = 10000L,
            jitterFactor = 0.4f // Up to 40% jitter reduction
        )

        // With random = 0.0 (no jitter offset)
        val repoMin = ProductionDeviceCommunicationRepository(initialConfig = config, randomProvider = { 0.0 })
        val delayMaxJitter = repoMin.computeBackoffDelay(1) // 1000 * 2 = 2000
        assertTrue(delayMaxJitter == 2000L)

        // With random = 1.0 (maximum jitter offset: 2000 - 2000 * 0.4 = 1200)
        val repoMax = ProductionDeviceCommunicationRepository(initialConfig = config, randomProvider = { 1.0 })
        val delayMinJitter = repoMax.computeBackoffDelay(1)
        assertTrue("Expected 1200ms, got $delayMinJitter", delayMinJitter == 1200L)

        // Delay is always >= baseBackoffMs
        assertTrue(delayMinJitter >= config.baseBackoffMs)
    }
}

