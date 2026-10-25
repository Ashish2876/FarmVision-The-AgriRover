package com.farmvision.agrirover.ui.hardware.diagnostic

import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.network.RoverProtocolClient
import com.farmvision.agrirover.ui.hardware.model.ConnectionDiagnosticResult

/**
 * Diagnostic link tester for controller endpoints.
 */
interface ConnectionTester {
    suspend fun testConnection(
        target: ControllerTarget,
        host: String,
        port: Int,
        timeoutMs: Long,
        isDemoMode: Boolean,
        authToken: String? = null
    ): ConnectionDiagnosticResult
}

/**
 * Default implementation of [ConnectionTester] performing real REST/HTTP endpoint probes
 * in Live Mode via [RoverProtocolClient], and providing truthful demo mode advisories in Demo Mode.
 */
class DefaultConnectionTester(
    private val protocolClient: RoverProtocolClient = RoverProtocolClient()
) : ConnectionTester {

    override suspend fun testConnection(
        target: ControllerTarget,
        host: String,
        port: Int,
        timeoutMs: Long,
        isDemoMode: Boolean,
        authToken: String?
    ): ConnectionDiagnosticResult {
        val now = System.currentTimeMillis()

        if (isDemoMode) {
            return ConnectionDiagnosticResult(
                target = target,
                success = false,
                statusCode = null,
                latencyMs = null,
                message = "DEMO MODE ACTIVE: Physical link probe skipped. Telemetry is simulated locally. Switch to Live mode to test physical Wi-Fi connection.",
                timestampMs = now
            )
        }

        return protocolClient.probeEndpoint(
            target = target,
            host = host,
            port = port,
            authToken = authToken,
            timeoutMs = timeoutMs
        )
    }
}

/**
 * In-memory test implementation of [ConnectionTester] for deterministic unit tests.
 */
class FakeConnectionTester(
    var simulatedSuccess: Boolean = false,
    var simulatedLatencyMs: Long = 15L,
    var simulatedMessage: String? = null
) : ConnectionTester {

    override suspend fun testConnection(
        target: ControllerTarget,
        host: String,
        port: Int,
        timeoutMs: Long,
        isDemoMode: Boolean,
        authToken: String?
    ): ConnectionDiagnosticResult {
        val now = System.currentTimeMillis()
        if (isDemoMode) {
            return ConnectionDiagnosticResult(
                target = target,
                success = false,
                statusCode = null,
                latencyMs = null,
                message = "DEMO MODE ACTIVE: Diagnostic probe skipped.",
                timestampMs = now
            )
        }

        return ConnectionDiagnosticResult(
            target = target,
            success = simulatedSuccess,
            statusCode = if (simulatedSuccess) 200 else null,
            latencyMs = simulatedLatencyMs,
            message = simulatedMessage ?: if (simulatedSuccess) "Simulated Link OK" else "Simulated Link Failed",
            timestampMs = now
        )
    }
}
