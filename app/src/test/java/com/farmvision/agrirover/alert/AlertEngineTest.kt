package com.farmvision.agrirover.alert

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SensorReading
import com.farmvision.agrirover.data.model.SensorType
import com.farmvision.agrirover.domain.alert.engine.AlertEngine
import com.farmvision.agrirover.domain.alert.engine.AlertSeverityPolicy
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlertEngineTest {

    private lateinit var engine: AlertEngine
    private val now = 100_000L

    @Before
    fun setUp() {
        val config = AlertThresholdConfig(
            tempMaxWarning = 35.0f,
            tempMaxCritical = 42.0f,
            tempHysteresis = 1.5f,
            obstacleCriticalCm = 10.0f,
            obstacleWarningCm = 20.0f,
            obstacleHysteresisCm = 5.0f,
            confirmationCount = 2,
            recoveryConfirmationCount = 2
        )
        engine = AlertEngine(config)
    }

    @Test
    fun testDebounceConfirmationPreventsPrematureAlert() {
        val readings = mapOf(
            SensorType.AirTemperature to SensorReading(
                sensorId = "dht22",
                sensorType = SensorType.AirTemperature,
                value = 38.0f, // > 35.0 (Warning)
                timestampEpochMillis = now
            )
        )
        val telemetry = RoverTelemetry(isSimulation = true)

        // Sample 1: breach count = 1 < confirmationCount (2) -> NO alert raised yet
        val output1 = engine.evaluate(telemetry, readings, now)
        assertEquals(0, output1.newAlerts.size)

        // Sample 2: breach count = 2 >= confirmationCount (2) -> Alert RAISED!
        val output2 = engine.evaluate(telemetry, readings, now + 1000L)
        assertEquals(1, output2.newAlerts.size)
        val alert = output2.newAlerts.first()
        assertEquals(AlertSeverity.Warning, alert.severity)
        assertEquals(AlertCategory.Environment, alert.category)
        assertEquals(AlertSeverityPolicy.RULE_TEMP_HIGH_WARNING, alert.ruleId)
    }

    @Test
    fun testHysteresisPreventsFlappingOnRecovery() {
        val highTemp = mapOf(
            SensorType.AirTemperature to SensorReading("dht22", SensorType.AirTemperature, 38.0f, timestampEpochMillis = now)
        )
        val telemetry = RoverTelemetry(isSimulation = true)

        // Raise alert (2 samples)
        engine.evaluate(telemetry, highTemp, now)
        val output = engine.evaluate(telemetry, highTemp, now + 1000L)
        assertEquals(1, output.newAlerts.size)

        // Temperature drops to 34.5°C: between 35.0°C and (35.0 - 1.5 = 33.5°C). Inside hysteresis! Must NOT resolve yet!
        val boundaryTemp = mapOf(
            SensorType.AirTemperature to SensorReading("dht22", SensorType.AirTemperature, 34.5f, timestampEpochMillis = now + 2000L)
        )
        val res1 = engine.evaluate(telemetry, boundaryTemp, now + 2000L)
        assertEquals(0, res1.resolvedKeys.size)

        // Temperature drops to 32.0°C: below 33.5°C.
        val normalTemp = mapOf(
            SensorType.AirTemperature to SensorReading("dht22", SensorType.AirTemperature, 32.0f, timestampEpochMillis = now + 3000L)
        )
        // Recovery sample 1: count = 1 < recoveryCount (2) -> Not resolved yet
        val res2 = engine.evaluate(telemetry, normalTemp, now + 3000L)
        assertEquals(0, res2.resolvedKeys.size)

        // Recovery sample 2: count = 2 >= recoveryCount (2) -> RESOLVED!
        val res3 = engine.evaluate(telemetry, normalTemp, now + 4000L)
        assertEquals(1, res3.resolvedKeys.size)
    }

    @Test
    fun testEmergencyStopRaisesInstantaneousCriticalAlert() {
        val telemetry = RoverTelemetry(isEmergencyCutoff = true, isSimulation = true)
        val output = engine.evaluate(telemetry, emptyMap(), now)

        assertEquals(1, output.newAlerts.size)
        val alert = output.newAlerts.first()
        assertEquals(AlertSeverity.Critical, alert.severity)
        assertEquals(AlertCategory.Rover, alert.category)
        assertEquals(AlertSeverityPolicy.RULE_ESTOP_ACTIVE, alert.ruleId)
    }

    @Test
    fun testSonarProximityAlertIsReadOnlyAndCriticalAt10cm() {
        val sonar = mapOf(
            SensorType.UltrasonicDistance to SensorReading("sonar", SensorType.UltrasonicDistance, 8.0f, timestampEpochMillis = now)
        )
        val telemetry = RoverTelemetry(isSimulation = true)

        // 2 samples for debounce
        engine.evaluate(telemetry, sonar, now)
        val output = engine.evaluate(telemetry, sonar, now + 1000L)

        assertEquals(1, output.newAlerts.size)
        val alert = output.newAlerts.first()
        assertEquals(AlertSeverity.Critical, alert.severity)
        assertEquals(AlertCategory.Rover, alert.category)
        assertEquals(AlertSeverityPolicy.RULE_OBSTACLE_CRITICAL, alert.ruleId)
    }

    @Test
    fun testLivePhDisplaysUncalibratedAdvisoryWithoutNumericAlert() {
        val phReading = mapOf(
            SensorType.SoilPh to SensorReading("ph-probe", SensorType.SoilPh, 4.2f, timestampEpochMillis = now, dataOrigin = DataOrigin.LiveHardware)
        )
        // Live Hardware telemetry
        val telemetry = RoverTelemetry(isSimulation = false)

        // 2 samples
        engine.evaluate(telemetry, phReading, now)
        val output = engine.evaluate(telemetry, phReading, now + 1000L)

        assertEquals(1, output.newAlerts.size)
        val alert = output.newAlerts.first()
        assertEquals(AlertSeverityPolicy.RULE_SOIL_PH_UNCALIBRATED, alert.ruleId)
        assertEquals("Uncalibrated Soil pH Probe", alert.title)
    }
}

