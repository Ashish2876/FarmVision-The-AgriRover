package com.farmvision.agrirover.ui.rover.controller

import kotlin.math.sqrt

/**
 * Pure Kotlin data model and vector math functions for the Virtual Joystick.
 * Zero Android framework or Jetpack Compose dependencies to ensure 100% deterministic JVM unit testing.
 *
 * Coordinate Convention:
 * - Normalized X: [-1.0, 1.0] where -1.0 is full Left, +1.0 is full Right.
 * - Normalized Y: [-1.0, 1.0] where +1.0 is full Forward, -1.0 is full Reverse.
 *   (Screen Y coordinates increase downward in UI rendering, so screen Y is inverted during conversion).
 */
data class JoystickVector(
    val x: Float = 0.0f,
    val y: Float = 0.0f
) {
    val magnitude: Float
        get() = sqrt(x * x + y * y)

    val isNeutral: Boolean
        get() = x == 0.0f && y == 0.0f

    val isValid: Boolean
        get() = !x.isNaN() && !y.isNaN() &&
                !x.isInfinite() && !y.isInfinite() &&
                x in -1.0001f..1.0001f && y in -1.0001f..1.0001f

    companion object {
        val NEUTRAL = JoystickVector(0.0f, 0.0f)
        const val DEFAULT_DEAD_ZONE = 0.08f
    }
}

/**
 * Simulated differential drive wheel velocities output in [-1.0, 1.0].
 * Left and right motor groups for skid-steer / differential steering kinematics.
 * Note: Purely software simulation for UI visualization and hardware interface contract.
 */
data class DifferentialDriveOutput(
    val left: Float,
    val right: Float
)

object JoystickMath {

    /**
     * Converts raw touch coordinates relative to the joystick center into a normalized,
     * circularly clamped Cartesian vector with positive Y meaning forward.
     *
     * @param touchX Raw touch X coordinate
     * @param touchY Raw touch Y coordinate
     * @param centerX Center X of the joystick bounding circle
     * @param centerY Center Y of the joystick bounding circle
     * @param maxRadius Maximum outer radius of the joystick boundary (in pixels or dp)
     * @return Clamped and Y-inverted [JoystickVector]
     */
    fun screenToCartesian(
        touchX: Float,
        touchY: Float,
        centerX: Float,
        centerY: Float,
        maxRadius: Float
    ): JoystickVector {
        if (maxRadius <= 0f) return JoystickVector.NEUTRAL

        val deltaX = touchX - centerX
        val deltaY = touchY - centerY

        // Screen Y grows downward; invert deltaY so upward drag produces positive forward Y
        val normalizedX = deltaX / maxRadius
        val normalizedY = -deltaY / maxRadius

        return clampToCircle(normalizedX, normalizedY)
    }

    /**
     * Clamps a 2D vector to the unit circle so magnitude never exceeds 1.0.
     */
    fun clampToCircle(x: Float, y: Float): JoystickVector {
        if (x.isNaN() || y.isNaN() || x.isInfinite() || y.isInfinite()) {
            return JoystickVector.NEUTRAL
        }

        val rawMag = sqrt(x * x + y * y)
        return when {
            rawMag <= 0.0001f -> JoystickVector.NEUTRAL
            rawMag > 1.0f -> JoystickVector(x / rawMag, y / rawMag)
            else -> JoystickVector(x, y)
        }
    }

    /**
     * Applies a continuous radial dead zone to the input vector.
     *
     * Inside the dead zone ([deadZoneRadius]), output is strictly (0f, 0f).
     * Outside the dead zone, magnitude is continuously rescaled from 0 at the dead-zone edge
     * to 1.0 at the outer rim, preserving direction and eliminating jump discontinuities:
     *
     *   r' = (r - r_dead) / (1.0 - r_dead)
     *
     * @param vector Input normalized vector
     * @param deadZoneRadius Normalized radius threshold (default 0.08)
     */
    fun applyRadialDeadZone(
        vector: JoystickVector,
        deadZoneRadius: Float = JoystickVector.DEFAULT_DEAD_ZONE
    ): JoystickVector {
        if (!vector.isValid || vector.isNeutral) return JoystickVector.NEUTRAL

        val r = vector.magnitude
        if (r <= deadZoneRadius) {
            return JoystickVector.NEUTRAL
        }

        // Continuous linear rescaling: r' in [0.0, 1.0] as r goes from deadZoneRadius to 1.0
        val effectiveRange = (1.0f - deadZoneRadius).coerceAtLeast(0.0001f)
        val rescaledMagnitude = ((r - deadZoneRadius) / effectiveRange).coerceIn(0.0f, 1.0f)

        // Direction unit vector (x/r, y/r) scaled by rescaledMagnitude
        val scaledX = (vector.x / r) * rescaledMagnitude
        val scaledY = (vector.y / r) * rescaledMagnitude

        return JoystickVector(scaledX, scaledY)
    }

    /**
     * Computes simulated differential steering outputs for skid-steer locomotion.
     * Left = clamp(y + x, -1.0, 1.0)
     * Right = clamp(y - x, -1.0, 1.0)
     */
    fun computeDifferentialMix(vector: JoystickVector): DifferentialDriveOutput {
        if (!vector.isValid || vector.isNeutral) {
            return DifferentialDriveOutput(0.0f, 0.0f)
        }

        val left = (vector.y + vector.x).coerceIn(-1.0f, 1.0f)
        val right = (vector.y - vector.x).coerceIn(-1.0f, 1.0f)
        return DifferentialDriveOutput(left, right)
    }

    /**
     * Sanitizes any vector, replacing NaN, infinite, or out-of-bounds coordinates with neutral.
     */
    fun sanitize(vector: JoystickVector): JoystickVector {
        return if (vector.isValid) {
            clampToCircle(vector.x, vector.y)
        } else {
            JoystickVector.NEUTRAL
        }
    }
}
