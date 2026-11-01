package com.farmvision.agrirover

import com.farmvision.agrirover.ui.rover.controller.JoystickVector
import com.farmvision.agrirover.ui.rover.controller.TwinStickMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Unit tests for [TwinStickMapper].
 * Verifies pure twin-stick coordinate mapping, dead-zones, pivot turn rules, and safety clamping.
 */
class TwinStickMapperTest {

    @Test
    fun neutralInputs_produceNeutralVector() {
        val result = TwinStickMapper.combineTwinStick(0f, 0f)
        assertTrue(result.isNeutral)
        assertEquals(0f, result.x, 0.001f)
        assertEquals(0f, result.y, 0.001f)
    }

    @Test
    fun forwardThrottle_producesPositiveYVector() {
        val result = TwinStickMapper.combineTwinStick(rawThrottleY = 0.8f, rawSteerX = 0f)
        assertFalse(result.isNeutral)
        assertEquals(0f, result.x, 0.001f)
        assertTrue("Forward gesture must produce positive Y", result.y > 0f)
    }

    @Test
    fun reverseThrottle_producesNegativeYVector() {
        val result = TwinStickMapper.combineTwinStick(rawThrottleY = -0.8f, rawSteerX = 0f)
        assertFalse(result.isNeutral)
        assertEquals(0f, result.x, 0.001f)
        assertTrue("Reverse gesture must produce negative Y", result.y < 0f)
    }

    @Test
    fun steerRightWithForwardThrottle_producesPositiveXAndPositiveY() {
        val result = TwinStickMapper.combineTwinStick(rawThrottleY = 0.6f, rawSteerX = 0.6f)
        assertTrue("Steer right must produce positive X", result.x > 0f)
        assertTrue("Forward throttle must produce positive Y", result.y > 0f)
        assertTrue("Magnitude must be clamped within unit circle", result.magnitude <= 1.0001f)
    }

    @Test
    fun steerLeftWithForwardThrottle_producesNegativeXAndPositiveY() {
        val result = TwinStickMapper.combineTwinStick(rawThrottleY = 0.6f, rawSteerX = -0.6f)
        assertTrue("Steer left must produce negative X", result.x < 0f)
        assertTrue("Forward throttle must produce positive Y", result.y > 0f)
    }

    @Test
    fun pivotTurnLockout_whenPivotTurnDisabled_steeringWithoutThrottleProducesNeutral() {
        // Agricultural safety rule: steering alone without forward/reverse throttle
        // must not activate motors in row crops when pivotTurnEnabled = false.
        val result = TwinStickMapper.combineTwinStick(
            rawThrottleY = 0f,
            rawSteerX = 0.9f,
            pivotTurnEnabled = false
        )
        assertTrue("Steering without throttle must be locked out", result.isNeutral)
        assertEquals(0f, result.x, 0.001f)
        assertEquals(0f, result.y, 0.001f)
    }

    @Test
    fun pivotTurnAllowed_whenEnabled_producesSteerVector() {
        val result = TwinStickMapper.combineTwinStick(
            rawThrottleY = 0f,
            rawSteerX = 0.9f,
            pivotTurnEnabled = true
        )
        assertFalse("Steering allowed when pivot enabled", result.isNeutral)
        assertTrue(result.x > 0f)
        assertEquals(0f, result.y, 0.001f)
    }

    @Test
    fun deadZone_suppressesSubThresholdNoise() {
        val subThreshold = TwinStickMapper.combineTwinStick(
            rawThrottleY = 0.05f,
            rawSteerX = 0.04f,
            deadZone = 0.08f
        )
        assertTrue("Inputs inside dead zone must be zeroed", subThreshold.isNeutral)

        val aboveThreshold = TwinStickMapper.combineTwinStick(
            rawThrottleY = 0.12f,
            rawSteerX = 0f,
            deadZone = 0.08f
        )
        assertTrue("Input above dead zone must produce movement", aboveThreshold.y > 0f)
    }

    @Test
    fun circularClamping_diagonalCornerNeverExceedsUnity() {
        val maxCorner = TwinStickMapper.combineTwinStick(
            rawThrottleY = 1.0f,
            rawSteerX = 1.0f
        )
        assertTrue("Magnitude must never exceed 1.0", maxCorner.magnitude <= 1.001f)
        val expected = sqrt(0.5f)
        assertEquals(expected, maxCorner.x, 0.01f)
        assertEquals(expected, maxCorner.y, 0.01f)
    }

    @Test
    fun nonFiniteValues_areSanitizedToNeutral() {
        val nanResult = TwinStickMapper.combineTwinStick(
            rawThrottleY = Float.NaN,
            rawSteerX = Float.POSITIVE_INFINITY
        )
        assertTrue("Non-finite inputs must produce neutral vector", nanResult.isNeutral)
    }
}

