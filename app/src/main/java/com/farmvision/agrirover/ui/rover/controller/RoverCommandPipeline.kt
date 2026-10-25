package com.farmvision.agrirover.ui.rover.controller

import com.farmvision.agrirover.data.datasource.CommandSink
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.data.time.SystemTimeProvider
import com.farmvision.agrirover.data.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-performance, rate-limiting Command Pipeline governing rover actuation dispatch.
 *
 * Rules:
 * - Maximum normal movement emission rate: 20 Hz (min 50 ms between non-neutral commands).
 * - Latest-wins queuing: Never builds a backlog; always retains only the most recent vector.
 * - Trailing emission: Delivers the final vector of a continuous gesture even after rapid touch release.
 * - Consecutive duplicate non-neutral suppression: Identical subsequent non-neutral vectors are dropped.
 * - Neutral bypass: Neutral Stop (0f, 0f) bypasses the throttle and emits immediately, canceling pending jobs.
 * - Emergency Stop bypass: Emergency Stop preempts any pending job and emits immediately.
 * - Clock injection: Accepts [TimeProvider] for deterministic, non-flaky JVM unit testing.
 */
class RoverCommandPipeline(
    private val commandSink: CommandSink,
    private val scope: CoroutineScope,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val minIntervalMillis: Long = 50L // 20 Hz
) {
    private var lastEmittedVector: JoystickVector? = null
    private var scheduledVector: JoystickVector? = null
    private var lastEmissionTimeMillis: Long = 0L
    private var pendingJob: Job? = null
    private var activePreset: SpeedPreset = SpeedPreset.Medium

    fun setSpeedPreset(preset: SpeedPreset) {
        activePreset = preset
        commandSink.setSpeedPreset(preset)
    }

    @Synchronized
    fun submitVector(vector: JoystickVector, speedPreset: SpeedPreset = activePreset) {
        val sanitized = JoystickMath.sanitize(vector)
        activePreset = speedPreset

        // Rule 1: Neutral Stop bypasses the throttle and emits immediately
        if (sanitized.isNeutral) {
            cancelPending()
            emitImmediately(sanitized, 0.0f)
            return
        }

        // Rule 2: Suppress consecutive duplicate non-neutral vectors if no trailing job is pending
        if (sanitized == lastEmittedVector && pendingJob == null) {
            return
        }

        val now = timeProvider.nowEpochMillis()
        val elapsed = now - lastEmissionTimeMillis

        if (elapsed >= minIntervalMillis) {
            // Immediate dispatch: Interval satisfied
            cancelPending()
            val speedKmh = sanitized.magnitude * speedPreset.maxSpeedKmh
            emitImmediately(sanitized, speedKmh)
        } else {
            // Latest-wins trailing emission: schedule newest vector after remaining interval
            val remainingDelay = (minIntervalMillis - elapsed).coerceAtLeast(1L)
            cancelPending()
            scheduledVector = sanitized
            pendingJob = scope.launch(dispatcher) {
                delay(remainingDelay)
                synchronized(this@RoverCommandPipeline) {
                    val targetSpeed = sanitized.magnitude * speedPreset.maxSpeedKmh
                    emitImmediately(sanitized, targetSpeed)
                }
            }
        }
    }

    @Synchronized
    fun submitEmergencyStop(engaged: Boolean) {
        cancelPending()
        lastEmittedVector = null
        commandSink.setEmergencyStop(engaged)
    }

    @Synchronized
    fun cancelPending() {
        pendingJob?.cancel()
        pendingJob = null
        scheduledVector = null
    }

    @Synchronized
    fun flushPending() {
        val vector = scheduledVector
        if (vector != null) {
            cancelPending()
            val targetSpeed = vector.magnitude * activePreset.maxSpeedKmh
            emitImmediately(vector, targetSpeed)
        }
    }

    @Synchronized
    private fun emitImmediately(vector: JoystickVector, speedKmh: Float) {
        lastEmittedVector = vector
        scheduledVector = null
        lastEmissionTimeMillis = timeProvider.nowEpochMillis()
        pendingJob = null
        commandSink.updateRoverMovement(vector, speedKmh)
    }

    val lastVector: JoystickVector?
        get() = lastEmittedVector

    val pendingVector: JoystickVector?
        get() = scheduledVector

    val hasPendingEmission: Boolean
        get() = pendingJob?.isActive == true || scheduledVector != null
}
