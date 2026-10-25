package com.farmvision.agrirover.data.datasource

/**
 * Replaceable domain boundary accepting rover actuation, speed, mode, and safety commands.
 * Encapsulates industrial safety rules:
 * - Emergency Stop immediately transitions rover command to STOP and latches.
 * - While latched, movement commands are rejected.
 * - Re-arming requires a deliberate, separate action.
 * - Drive mode or speed adjustments while stationary never initiate movement.
 *
 * In Phase 4, implemented by [SimulatedCommandSink] which updates local StateFlow and never transmits to physical hardware.
 */
interface CommandSink {
    /** Dispatches a discrete directional piloting command (e.g. "FORWARD", "HOLD POSITION"). */
    fun updateRoverMovement(command: String, speedKmh: Float)

    /** Dispatches a continuous 2D joystick velocity vector command. */
    fun updateRoverMovement(vector: com.farmvision.agrirover.ui.rover.controller.JoystickVector, speedKmh: Float)

    /** Selects the active drive mode via string key. */
    fun setDriveMode(mode: String)

    /** Selects the active drive mode envelope ([com.farmvision.agrirover.data.model.RoverDriveMode]). */
    fun setDriveMode(mode: com.farmvision.agrirover.data.model.RoverDriveMode)

    /** Selects the active speed preset ([com.farmvision.agrirover.data.model.SpeedPreset]). */
    fun setSpeedPreset(preset: com.farmvision.agrirover.data.model.SpeedPreset)

    /** Engages or disengages the latching emergency stop cutoff. */
    fun setEmergencyStop(engaged: Boolean)

    fun toggleFrontLight()
    fun toggleSideLight()
    fun triggerHorn()
    fun toggleSpray()
    fun toggleAuxIrrigation()
    fun setCameraTilt(angle: Int)
}
