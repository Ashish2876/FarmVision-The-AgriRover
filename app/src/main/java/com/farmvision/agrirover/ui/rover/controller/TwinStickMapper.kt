package com.farmvision.agrirover.ui.rover.controller

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Pure mathematical mapper combining twin-stick inputs (Move throttle & Steer heading)
 * into a single unified [JoystickVector] for differential drive transmission.
 *
 * Coordinate Convention (source-verified against JoystickMath & FarmRepository):
 * - +Y: Forward (upward screen gesture)
 * - -Y: Reverse (downward screen gesture)
 * - +X: Turn Right (rightward screen gesture)
 * - -X: Turn Left (leftward screen gesture)
 */
object TwinStickMapper {

    const val DEFAULT_DEAD_ZONE = 0.08f

    /**
     * Combines throttle (Y-axis) and steering (X-axis) into a clamped [JoystickVector].
     *
     * @param rawThrottleY Raw Y value from Move stick (-1.0 to +1.0)
     * @param rawSteerX Raw X value from Steer stick (-1.0 to +1.0)
     * @param deadZone Dead-band threshold below which inputs are treated as zero
     * @param pivotTurnEnabled Whether in-place rotation is permitted when throttle is zero.
     *                          Default is false (safety lockout for agricultural row crops).
     */
    fun combineTwinStick(
        rawThrottleY: Float,
        rawSteerX: Float,
        deadZone: Float = DEFAULT_DEAD_ZONE,
        pivotTurnEnabled: Boolean = false
    ): JoystickVector {
        val safeThrottle = sanitize(rawThrottleY)
        val safeSteer = sanitize(rawSteerX)

        val throttle = applyDeadZone(safeThrottle, deadZone)
        val steer = applyDeadZone(safeSteer, deadZone)

        // Safety lockout: if pivot turns are disabled and throttle is zero,
        // steering alone must not activate the drive motors.
        if (!pivotTurnEnabled && throttle == 0f) {
            return JoystickVector.NEUTRAL
        }

        // Both are neutral
        if (throttle == 0f && steer == 0f) {
            return JoystickVector.NEUTRAL
        }

        val rawMag = sqrt(steer * steer + throttle * throttle)
        if (rawMag == 0f) {
            return JoystickVector.NEUTRAL
        }

        val clampedMag = rawMag.coerceAtMost(1.0f)
        val scale = clampedMag / rawMag

        val finalX = (steer * scale).coerceIn(-1.0f, 1.0f)
        val finalY = (throttle * scale).coerceIn(-1.0f, 1.0f)

        return JoystickVector(
            x = (finalX * 1000f).toInt() / 1000f,
            y = (finalY * 1000f).toInt() / 1000f
        )
    }

    /**
     * Linear dead zone with smooth re-scaling so movement begins continuously at 0.
     */
    fun applyDeadZone(value: Float, deadZone: Float): Float {
        val safe = sanitize(value)
        val mag = abs(safe)
        if (mag <= deadZone) return 0f
        val adjusted = (mag - deadZone) / (1.0f - deadZone)
        return sign(safe) * adjusted.coerceIn(0f, 1.0f)
    }

    private fun sanitize(value: Float): Float {
        return if (value.isNaN() || value.isInfinite()) 0f else value.coerceIn(-1.0f, 1.0f)
    }
}

