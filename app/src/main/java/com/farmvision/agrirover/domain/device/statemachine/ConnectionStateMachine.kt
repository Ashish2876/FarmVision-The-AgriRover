package com.farmvision.agrirover.domain.device.statemachine

import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic finite state machine for rover device connection lifecycle.
 * Enforces valid state transitions and rejects illegal transitions.
 */
class ConnectionStateMachine(
    initialState: ConnectionState = ConnectionState.Disconnected
) {
    private val _state = MutableStateFlow<ConnectionState>(initialState)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    val currentState: ConnectionState get() = _state.value

    @Synchronized
    fun onStartConnect(endpoint: String, attempt: Int = 1): Boolean {
        return when (val current = _state.value) {
            is ConnectionState.Disconnected,
            is ConnectionState.Failed -> {
                _state.value = ConnectionState.Connecting(endpoint, attempt)
                true
            }
            is ConnectionState.Reconnecting -> {
                _state.value = ConnectionState.Connecting(endpoint, attempt)
                true
            }
            is ConnectionState.Connecting -> {
                // Allow refreshing attempt count
                _state.value = ConnectionState.Connecting(endpoint, attempt)
                true
            }
            is ConnectionState.Handshaking,
            is ConnectionState.Connected -> false // Reject starting connect while already active
        }
    }

    @Synchronized
    fun onSocketOpened(endpoint: String): Boolean {
        return when (_state.value) {
            is ConnectionState.Connecting -> {
                _state.value = ConnectionState.Handshaking(endpoint)
                true
            }
            else -> false
        }
    }

    @Synchronized
    fun onHandshakeSuccess(
        endpoint: String,
        transportType: TransportType,
        deviceIdentity: DeviceIdentity,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return when (_state.value) {
            is ConnectionState.Handshaking,
            is ConnectionState.Connecting -> {
                _state.value = ConnectionState.Connected(
                    endpoint = endpoint,
                    transportType = transportType,
                    connectedAtEpochMillis = nowEpochMillis,
                    deviceIdentity = deviceIdentity
                )
                true
            }
            else -> false
        }
    }

    @Synchronized
    fun onConnectionLost(
        endpoint: String,
        attempt: Int,
        maxAttempts: Int,
        nextRetryDelayMillis: Long,
        cause: String?,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return when (_state.value) {
            is ConnectionState.Connected,
            is ConnectionState.Handshaking,
            is ConnectionState.Connecting,
            is ConnectionState.Reconnecting -> {
                if (attempt > maxAttempts) {
                    _state.value = ConnectionState.Failed(
                        endpoint = endpoint,
                        reason = cause ?: "Maximum retry attempts ($maxAttempts) exhausted",
                        failedAtEpochMillis = nowEpochMillis
                    )
                } else {
                    _state.value = ConnectionState.Reconnecting(
                        endpoint = endpoint,
                        attempt = attempt,
                        maxAttempts = maxAttempts,
                        nextRetryDelayMillis = nextRetryDelayMillis,
                        cause = cause
                    )
                }
                true
            }
            is ConnectionState.Disconnected -> false
            is ConnectionState.Failed -> false
        }
    }

    @Synchronized
    fun onFatalFailure(
        endpoint: String,
        reason: String,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Boolean {
        _state.value = ConnectionState.Failed(
            endpoint = endpoint,
            reason = reason,
            failedAtEpochMillis = nowEpochMillis
        )
        return true
    }

    @Synchronized
    fun onDisconnect(): Boolean {
        _state.value = ConnectionState.Disconnected
        return true
    }
}

