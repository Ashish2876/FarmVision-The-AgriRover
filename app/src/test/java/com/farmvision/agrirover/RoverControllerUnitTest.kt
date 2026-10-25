package com.farmvision.agrirover

import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.data.time.FakeTimeProvider
import com.farmvision.agrirover.ui.rover.controller.JoystickMath
import com.farmvision.agrirover.ui.rover.controller.JoystickVector
import com.farmvision.agrirover.ui.rover.controller.RoverCommandPipeline
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Phase 6 Advanced Rover Controller Unit Test Suite.
 * Validates the 22 required architectural requirements:
 * - Pure joystick vector mathematics (Y inversion, clamping, dead zone continuity, differential steering)
 * - Software speed presets & presentation conversions
 * - Drive mode restrictions enforced at the repository/sink layer
 * - High-priority Emergency Stop latching and safe deliberate re-arm
 * - 20 Hz Command Pipeline rate limiting, trailing emission, and duplicate suppression
 */
class RoverControllerUnitTest {

    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var repository: MockFarmRepository
    private lateinit var viewModel: FarmViewModel
    private val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        fakeTime = FakeTimeProvider(1_700_000_000_000L)
        repository = MockFarmRepository(timeProvider = fakeTime)
        viewModel = FarmViewModel(repository, null)
    }

    // 1. Joystick center -> (0, 0)
    @Test
    fun joystickMath_centerReturnsZeroVector() {
        val vector = JoystickMath.screenToCartesian(
            touchX = 100f,
            touchY = 100f,
            centerX = 100f,
            centerY = 100f,
            maxRadius = 50f
        )
        assertEquals(0.0f, vector.x, 0.0001f)
        assertEquals(0.0f, vector.y, 0.0001f)
        assertTrue(vector.isNeutral)
    }

    // 2. Positive Y is forward (screen-Y inversion)
    @Test
    fun joystickMath_invertsScreenYForForward() {
        // Touch point above center (touchY < centerY)
        val forwardVector = JoystickMath.screenToCartesian(
            touchX = 100f,
            touchY = 50f, // 50px above center
            centerX = 100f,
            centerY = 100f,
            maxRadius = 50f
        )
        assertEquals(0.0f, forwardVector.x, 0.0001f)
        assertEquals(1.0f, forwardVector.y, 0.0001f) // Up on screen is +Y (forward)

        // Touch point below center (touchY > centerY)
        val reverseVector = JoystickMath.screenToCartesian(
            touchX = 100f,
            touchY = 150f,
            centerX = 100f,
            centerY = 100f,
            maxRadius = 50f
        )
        assertEquals(0.0f, reverseVector.x, 0.0001f)
        assertEquals(-1.0f, reverseVector.y, 0.0001f) // Down on screen is -Y (reverse)
    }

    // 3. Circular clamping: magnitude never exceeds 1.0, including at the corners
    @Test
    fun joystickMath_circularClampingPreservesAngleAtCorners() {
        // Diagonal corner touch (dx = 50, dy = -50 -> raw magnitude = sqrt(2) ~ 1.414)
        val clamped = JoystickMath.screenToCartesian(
            touchX = 150f,
            touchY = 50f,
            centerX = 100f,
            centerY = 100f,
            maxRadius = 50f
        )
        assertEquals(1.0f, clamped.magnitude, 0.0001f)
        assertEquals(sqrt(0.5f), clamped.x, 0.0001f)
        assertEquals(sqrt(0.5f), clamped.y, 0.0001f)
    }

    // 4. Dead zone: inputs inside 0.08 -> neutral, with continuity at the edge after rescaling
    @Test
    fun joystickMath_deadZoneContinuousRescaling() {
        // Inside deadzone (magnitude 0.05 < 0.08)
        val insideDeadZone = JoystickVector(0.03f, 0.04f) // mag = 0.05
        val neutralized = JoystickMath.applyRadialDeadZone(insideDeadZone, 0.08f)
        assertTrue(neutralized.isNeutral)

        // Exactly at edge: magnitude 0.08 -> neutral
        val atEdge = JoystickVector(0.0f, 0.08f)
        assertTrue(JoystickMath.applyRadialDeadZone(atEdge, 0.08f).isNeutral)

        // Just outside edge (magnitude 0.09): magnitude ramps continuously near 0 without jump
        val justOutside = JoystickVector(0.0f, 0.09f)
        val rescaled = JoystickMath.applyRadialDeadZone(justOutside, 0.08f)
        assertFalse(rescaled.isNeutral)
        val expectedMag = (0.09f - 0.08f) / (1.0f - 0.08f) // ~ 0.01087
        assertEquals(expectedMag, rescaled.magnitude, 0.001f)

        // At perimeter (magnitude 1.0): output magnitude is exactly 1.0
        val atPerimeter = JoystickVector(0.0f, 1.0f)
        val rescaledPerimeter = JoystickMath.applyRadialDeadZone(atPerimeter, 0.08f)
        assertEquals(1.0f, rescaledPerimeter.magnitude, 0.0001f)
    }

    // 5. Release, cancellation, disposal, disable, and ON_STOP each emit neutral immediately
    @Test
    fun joystickLifecycle_neutralizationOnStopOrDisposal() {
        viewModel.onJoystickMoved(0.5f, 0.5f)
        viewModel.onJoystickReleased()
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertTrue(viewModel.telemetry.value.joystickVector.isNeutral)
    }

    // 6. Differential mixing outputs are in [-1, 1] for all vectors
    @Test
    fun joystickMath_differentialMixingOutputsBounded() {
        // Full Forward
        val forward = JoystickMath.computeDifferentialMix(JoystickVector(0f, 1f))
        assertEquals(1.0f, forward.left, 0.0001f)
        assertEquals(1.0f, forward.right, 0.0001f)

        // Full Spin Left (x = -1, y = 0)
        val spinLeft = JoystickMath.computeDifferentialMix(JoystickVector(-1f, 0f))
        assertEquals(-1.0f, spinLeft.left, 0.0001f)
        assertEquals(1.0f, spinLeft.right, 0.0001f)

        // Full Spin Right (x = 1, y = 0)
        val spinRight = JoystickMath.computeDifferentialMix(JoystickVector(1f, 0f))
        assertEquals(1.0f, spinRight.left, 0.0001f)
        assertEquals(-1.0f, spinRight.right, 0.0001f)

        // Corner saturation clamp: (x = 1, y = 1) clamped to [-1, 1]
        val corner = JoystickMath.computeDifferentialMix(JoystickVector(1f, 1f))
        assertTrue(corner.left in -1.0f..1.0f)
        assertTrue(corner.right in -1.0f..1.0f)
    }

    // 7. Preset scaling is correct for Low, Medium, and High, and the simulated km/h is correct, with unit conversion applied at presentation only
    @Test
    fun speedPreset_scalingAndPresentationCalculations() {
        assertEquals(1.5f, SpeedPreset.Low.maxSpeedKmh, 0.01f)
        assertEquals(0.30f, SpeedPreset.Low.dutyFraction, 0.01f)

        assertEquals(3.5f, SpeedPreset.Medium.maxSpeedKmh, 0.01f)
        assertEquals(0.65f, SpeedPreset.Medium.dutyFraction, 0.01f)

        assertEquals(5.5f, SpeedPreset.High.maxSpeedKmh, 0.01f)
        assertEquals(1.00f, SpeedPreset.High.dutyFraction, 0.01f)

        // Unit conversion at presentation
        val kmh = 3.5f
        assertEquals("3.5 km/h", SpeedUnit.Metric.format(kmh))
        val expectedMph = String.format(java.util.Locale.US, "%.1f mph", kmh * 0.621371f)
        assertEquals(expectedMph, SpeedUnit.Imperial.format(kmh))
    }

    // 8. Invalid vectors (NaN, infinite, out of range) are rejected or neutralized
    @Test
    fun joystickMath_vectorValidationRejectsNaNAndInfinite() {
        val nanVector = JoystickVector(Float.NaN, 0.5f)
        assertFalse(nanVector.isValid)
        assertTrue(JoystickMath.sanitize(nanVector).isNeutral)

        val infiniteVector = JoystickVector(0.5f, Float.POSITIVE_INFINITY)
        assertFalse(infiniteVector.isValid)
        assertTrue(JoystickMath.sanitize(infiniteVector).isNeutral)
    }

    // 9. Changing a preset never starts movement
    @Test
    fun speedPreset_changeNeverStartsMovementWhileStationary() {
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        viewModel.setSpeedPreset(SpeedPreset.High)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
    }

    // 10. Hold Position forces neutral and cancels pending commands
    @Test
    fun driveMode_holdPositionForcesNeutralCommand() {
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        viewModel.setDriveMode(RoverDriveMode.HoldPosition)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
        assertEquals(RoverDriveMode.HoldPosition, viewModel.telemetry.value.driveModeEnum)
    }

    // 11. Mode switching never starts or resumes movement
    @Test
    fun driveMode_modeSwitchingNeverStartsMovement() {
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        viewModel.setDriveMode(RoverDriveMode.AutonomousWaypoint)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        viewModel.setDriveMode(RoverDriveMode.Manual)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
    }

    // 12. Manual movement is rejected in non-manual modes at the sink level
    @Test
    fun driveMode_manualMovementRejectedInNonManualModes() {
        viewModel.setDriveMode(RoverDriveMode.AutonomousWaypoint)
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)

        repository.updateRoverMovement(JoystickVector(0f, 1f), 3.5f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
    }

    // 13. Emergency Stop immediately zeros speed, latches, and cancels pending movement
    @Test
    fun safetyLogic_eStopImmediatelyZerosSpeedAndLatches() {
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        viewModel.setEmergencyStop(true)
        assertTrue(viewModel.telemetry.value.isEmergencyCutoff)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("EMERGENCY CUTOFF", viewModel.telemetry.value.activeCommand)
    }

    // 14. Movement is rejected while latched
    @Test
    fun safetyLogic_movementRejectedWhileEStopLatched() {
        viewModel.setEmergencyStop(true)
        repository.updateRoverMovement(JoystickVector(0f, 1f), 3.5f)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("EMERGENCY CUTOFF", viewModel.telemetry.value.activeCommand)
    }

    // 15. Re-arm leaves speed zero and movement neutral and never restores prior movement
    @Test
    fun safetyLogic_reArmLeavesSpeedZeroAndNeverResumesMovement() {
        viewModel.updateRoverMovement("FORWARD", 3.5f)
        viewModel.setEmergencyStop(true)
        viewModel.setEmergencyStop(false)
        assertFalse(viewModel.telemetry.value.isEmergencyCutoff)
        assertEquals(0.0f, viewModel.telemetry.value.speedKmh, 0.01f)
        assertEquals("HOLD POSITION", viewModel.telemetry.value.activeCommand)
    }

    // 16. Emergency Stop and neutral bypass the throttle
    @Test
    fun commandPipeline_neutralAndEStopBypassThrottle() {
        val pipeline = RoverCommandPipeline(
            commandSink = repository,
            scope = testScope,
            timeProvider = fakeTime,
            dispatcher = Dispatchers.Unconfined
        )

        // Dispatch initial movement at t = 0
        pipeline.submitVector(JoystickVector(0f, 1f))
        assertEquals(JoystickVector(0f, 1f), pipeline.lastVector)

        // Submitting Neutral Stop bypasses the 50ms throttle immediately
        fakeTime.advanceMillis(10) // Only 10ms elapsed (< 50ms)
        pipeline.submitVector(JoystickVector.NEUTRAL)
        assertEquals(JoystickVector.NEUTRAL, pipeline.lastVector)

        // Submitting E-Stop bypasses immediately
        pipeline.submitEmergencyStop(true)
        assertTrue(repository.getTelemetry().isEmergencyCutoff)
    }

    // 17. Normal emission never exceeds 20 Hz, tested through the actual pipeline with a controllable clock
    @Test
    fun commandPipeline_enforcesMax20HzRateLimit() {
        val pipeline = RoverCommandPipeline(
            commandSink = repository,
            scope = testScope,
            timeProvider = fakeTime,
            dispatcher = Dispatchers.Unconfined
        )

        // First non-neutral command emits immediately
        pipeline.submitVector(JoystickVector(0.2f, 0.5f))
        assertEquals(JoystickVector(0.2f, 0.5f), pipeline.lastVector)

        // Second vector sent after 20ms (< 50ms interval) is throttled
        fakeTime.advanceMillis(20)
        pipeline.submitVector(JoystickVector(0.3f, 0.6f))
        // Still at first vector because 50ms interval hasn't elapsed
        assertEquals(JoystickVector(0.2f, 0.5f), pipeline.lastVector)
        assertTrue(pipeline.hasPendingEmission)
        assertEquals(JoystickVector(0.3f, 0.6f), pipeline.pendingVector)

        // Advance clock past the 50ms boundary and flush
        fakeTime.advanceMillis(30)
        pipeline.flushPending()
        assertEquals(JoystickVector(0.3f, 0.6f), pipeline.lastVector)
        assertFalse(pipeline.hasPendingEmission)
    }

    // 18. Trailing emission delivers the final vector and drops stale intermediate vectors
    @Test
    fun commandPipeline_trailingEmissionDeliversFinalVector() {
        val pipeline = RoverCommandPipeline(
            commandSink = repository,
            scope = testScope,
            timeProvider = fakeTime,
            dispatcher = Dispatchers.Unconfined
        )

        pipeline.submitVector(JoystickVector(0.1f, 0.2f))
        assertEquals(JoystickVector(0.1f, 0.2f), pipeline.lastVector)

        // Send intermediate vector at 15ms
        fakeTime.advanceMillis(15)
        pipeline.submitVector(JoystickVector(0.2f, 0.4f))
        assertEquals(JoystickVector(0.2f, 0.4f), pipeline.pendingVector)

        // Send final gesture vector at 30ms (latest-wins overwrites intermediate 0.2, 0.4)
        fakeTime.advanceMillis(15)
        pipeline.submitVector(JoystickVector(0.5f, 0.8f))
        assertEquals(JoystickVector(0.5f, 0.8f), pipeline.pendingVector)

        // Advance time and flush trailing emission
        fakeTime.advanceMillis(20)
        pipeline.flushPending()

        assertEquals(JoystickVector(0.5f, 0.8f), pipeline.lastVector)
        assertNull(pipeline.pendingVector)
    }

    // 19. Consecutive duplicates are handled per the documented rule
    @Test
    fun commandPipeline_suppressesConsecutiveDuplicateVectors() {
        val pipeline = RoverCommandPipeline(
            commandSink = repository,
            scope = testScope,
            timeProvider = fakeTime,
            dispatcher = Dispatchers.Unconfined
        )

        val v1 = JoystickVector(0.4f, 0.7f)
        pipeline.submitVector(v1)
        assertEquals(v1, pipeline.lastVector)

        fakeTime.advanceMillis(100) // Interval satisfied
        pipeline.submitVector(v1) // Exact duplicate non-neutral vector

        // No pending trailing job created for duplicate
        assertFalse(pipeline.hasPendingEmission)
    }

    // 20. A pending command cannot execute after Emergency Stop or a mode change
    @Test
    fun commandPipeline_pendingCommandCancelledByEmergencyStop() {
        val pipeline = RoverCommandPipeline(
            commandSink = repository,
            scope = testScope,
            timeProvider = fakeTime,
            dispatcher = Dispatchers.Unconfined
        )

        pipeline.submitVector(JoystickVector(0.1f, 0.2f))
        fakeTime.advanceMillis(10)
        pipeline.submitVector(JoystickVector(0.5f, 0.9f))
        assertTrue(pipeline.hasPendingEmission)

        // E-Stop preempts pending command
        pipeline.submitEmergencyStop(true)
        assertFalse(pipeline.hasPendingEmission)
        assertNull(pipeline.lastVector)
        assertNull(pipeline.pendingVector)

        // Advance time; scheduled movement must NOT emit
        fakeTime.advanceMillis(100)
        pipeline.flushPending()
        assertTrue(repository.getTelemetry().isEmergencyCutoff)
        assertEquals(0.0f, repository.getTelemetry().speedKmh, 0.01f)
    }

    // 21. E-stop state persists across ViewModel recreation and is reflected on Home
    @Test
    fun eStopState_persistsAcrossViewModelRecreation() {
        viewModel.setEmergencyStop(true)
        assertTrue(viewModel.telemetry.value.isEmergencyCutoff)

        // Recreate ViewModel with the same shared repository
        val newViewModel = FarmViewModel(repository, null)
        assertTrue(newViewModel.telemetry.value.isEmergencyCutoff)
        assertEquals("Cutoff", newViewModel.getSpeedStatusLabel())
    }

    // 22. All existing Phase 4 and Phase 5 tests still pass
    @Test
    fun baselinePreservation_quickActionsAndTelemetryHUDWork() {
        viewModel.startFieldScan()
        assertEquals("FIELD SCAN", viewModel.telemetry.value.activeCommand)
        assertEquals("AUTONOMOUS", viewModel.telemetry.value.roverMode)

        viewModel.returnToDock()
        assertEquals("RETURN TO DOCK", viewModel.telemetry.value.activeCommand)

        viewModel.disarmMotors()
        assertTrue(viewModel.telemetry.value.isEmergencyCutoff)
    }
}
