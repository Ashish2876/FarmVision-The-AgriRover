package com.farmvision.agrirover.device

import com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository
import com.farmvision.agrirover.data.device.transport.DirectEsp32TransportBridge
import com.farmvision.agrirover.data.device.transport.FastApiBackendTransportBridge
import com.farmvision.agrirover.domain.device.transport.RoverReadOnlyClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class ActuatorCapabilityIsolationTest {

    private val forbiddenSubstrings = listOf(
        "command",
        "emergency",
        "estop",
        "stop",
        "motor",
        "pump",
        "relay",
        "steer",
        "drive",
        "throttle",
        "actuator",
        "move"
    )

    @Test
    fun roverReadOnlyClient_containsZeroActuatorOrCommandMethods() {
        val methods = RoverReadOnlyClient::class.java.declaredMethods
        assertTrue("RoverReadOnlyClient must declare read-only methods", methods.isNotEmpty())

        for (method in methods) {
            val nameLower = method.name.lowercase()
            for (forbidden in forbiddenSubstrings) {
                assertFalse(
                    "RoverReadOnlyClient method '${method.name}' violates read-only isolation (contains forbidden token '$forbidden')",
                    nameLower.contains(forbidden)
                )
            }
        }
    }

    @Test
    fun directEsp32TransportBridge_containsZeroActuatorOrCommandMethods() {
        val methods = DirectEsp32TransportBridge::class.java.declaredMethods
        for (method in methods) {
            if (Modifier.isPublic(method.modifiers)) {
                val nameLower = method.name.lowercase()
                for (forbidden in forbiddenSubstrings) {
                    assertFalse(
                        "DirectEsp32TransportBridge public method '${method.name}' violates actuator isolation",
                        nameLower.contains(forbidden)
                    )
                }
            }
        }
    }

    @Test
    fun fastApiBackendTransportBridge_containsZeroActuatorOrCommandMethods() {
        val methods = FastApiBackendTransportBridge::class.java.declaredMethods
        for (method in methods) {
            if (Modifier.isPublic(method.modifiers)) {
                val nameLower = method.name.lowercase()
                for (forbidden in forbiddenSubstrings) {
                    assertFalse(
                        "FastApiBackendTransportBridge public method '${method.name}' violates actuator isolation",
                        nameLower.contains(forbidden)
                    )
                }
            }
        }
    }

    @Test
    fun productionCommunicationRepository_hasNoCommandPipelineReference() {
        val fields = ProductionDeviceCommunicationRepository::class.java.declaredFields
        for (field in fields) {
            val typeName = field.type.simpleName.lowercase()
            assertFalse(
                "ProductionDeviceCommunicationRepository must not hold reference to command pipeline or sink",
                typeName.contains("pipeline") || typeName.contains("commandsink")
            )
        }
    }
}

