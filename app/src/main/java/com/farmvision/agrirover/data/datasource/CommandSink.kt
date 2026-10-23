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
    /** Dispatches a directional piloting vector command. */
    fun updateRoverMovement(command: String, speedKmh: Float)

    /** Selects the active drive mode envelope (Precision, Normal, Turbo). */
    fun setDriveMode(mode: String)

    /** Engages or disengages the latching emergency stop cutoff. */
    fun setEmergencyStop(engaged: Boolean)

    fun toggleFrontLight()
    fun toggleSideLight()
    fun triggerHorn()
    fun toggleSpray()
    fun toggleAuxIrrigation()
    fun setCameraTilt(angle: Int)
}
