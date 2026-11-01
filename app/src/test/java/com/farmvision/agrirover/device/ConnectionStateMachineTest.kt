package com.farmvision.agrirover.device

import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.domain.device.model.DeviceIdentity
import com.farmvision.agrirover.domain.device.model.TransportType
import com.farmvision.agrirover.domain.device.statemachine.ConnectionStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {

    @Test
    fun initialState_isDisconnected() {
        val fsm = ConnectionStateMachine()
        assertEquals(ConnectionState.Disconnected, fsm.currentState)
        assertFalse(fsm.currentState.isConnected)
    }

    @Test
    fun happyPathLifecycle_transitionsProperly() {
        val fsm = ConnectionStateMachine()
        val endpoint = "http://192.168.4.1"

        // 1. Disconnected -> Connecting
        assertTrue(fsm.onStartConnect(endpoint, 1))
        assertTrue(fsm.currentState is ConnectionState.Connecting)

        // 2. Connecting -> Handshaking
        assertTrue(fsm.onSocketOpened(endpoint))
        assertTrue(fsm.currentState is ConnectionState.Handshaking)

        // 3. Handshaking -> Connected
        val identity = DeviceIdentity(deviceId = "agrirover-01")
        assertTrue(fsm.onHandshakeSuccess(endpoint, TransportType.DirectEsp32SoftAp, identity))
        assertTrue(fsm.currentState is ConnectionState.Connected)
        assertTrue(fsm.currentState.isConnected)

        val connected = fsm.currentState as ConnectionState.Connected
        assertEquals(endpoint, connected.endpoint)
        assertEquals(TransportType.DirectEsp32SoftAp, connected.transportType)
        assertEquals("agrirover-01", connected.deviceIdentity.deviceId)
    }

    @Test
    fun invalidTransition_handshakeFromDisconnected_isRejected() {
        val fsm = ConnectionStateMachine()
        val identity = DeviceIdentity(deviceId = "agrirover-01")

        // Cannot jump directly from Disconnected to HandshakeSuccess
        assertFalse(fsm.onHandshakeSuccess("http://192.168.4.1", TransportType.DirectEsp32SoftAp, identity))
        assertEquals(ConnectionState.Disconnected, fsm.currentState)
    }

    @Test
    fun connectionLoss_transitionsToReconnecting_untilBudgetExhausted() {
        val fsm = ConnectionStateMachine()
        val endpoint = "http://192.168.4.1"
        val identity = DeviceIdentity(deviceId = "agrirover-01")

        fsm.onStartConnect(endpoint, 1)
        fsm.onSocketOpened(endpoint)
        fsm.onHandshakeSuccess(endpoint, TransportType.DirectEsp32SoftAp, identity)

        // Retry 1: Budget not exhausted
        assertTrue(fsm.onConnectionLost(endpoint, attempt = 1, maxAttempts = 3, nextRetryDelayMillis = 500L, cause = "Timeout"))
        assertTrue(fsm.currentState is ConnectionState.Reconnecting)
        val reconnecting = fsm.currentState as ConnectionState.Reconnecting
        assertEquals(1, reconnecting.attempt)

        // Retry 4: Exceeds maxAttempts (3) -> Transitions to Failed
        assertTrue(fsm.onConnectionLost(endpoint, attempt = 4, maxAttempts = 3, nextRetryDelayMillis = 4000L, cause = "Unreachable"))
        assertTrue(fsm.currentState is ConnectionState.Failed)
        val failed = fsm.currentState as ConnectionState.Failed
        assertEquals("Unreachable", failed.reason)
    }

    @Test
    fun onDisconnect_resetsToDisconnected() {
        val fsm = ConnectionStateMachine()
        fsm.onStartConnect("http://192.168.4.1", 1)
        assertTrue(fsm.onDisconnect())
        assertEquals(ConnectionState.Disconnected, fsm.currentState)
    }
}

