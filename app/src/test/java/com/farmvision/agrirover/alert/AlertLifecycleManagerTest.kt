package com.farmvision.agrirover.alert

import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.domain.alert.engine.AlertEvaluationOutput
import com.farmvision.agrirover.domain.alert.lifecycle.AlertLifecycleEvent
import com.farmvision.agrirover.domain.alert.lifecycle.AlertLifecycleManager
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.AlertStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlertLifecycleManagerTest {

    private lateinit var manager: AlertLifecycleManager
    private val now = 500_000L

    @Before
    fun setUp() {
        manager = AlertLifecycleManager()
    }

    private fun createSampleAlert(
        id: String,
        dedupKey: String,
        severity: AlertSeverity = AlertSeverity.Warning,
        triggerVal: Float = 36.0f
    ) = AlertItem(
        id = id,
        dedupKey = dedupKey,
        category = AlertCategory.Environment,
        ruleId = "env-temp-high-warning",
        severity = severity,
        title = "High Temperature",
        message = "Air temperature high",
        sourceSensor = "dht22",
        triggerValue = triggerVal,
        origin = DataOrigin.Simulated,
        createdTimestampEpochMillis = now
    )

    @Test
    fun testProcessNewAlertsAddsToActive() {
        val alert = createSampleAlert("alert-1", "env:temp:dht22:Simulated")
        val output = AlertEvaluationOutput(
            newAlerts = listOf(alert),
            repeatKeys = emptyList(),
            resolvedKeys = emptyList()
        )

        val events = manager.processEvaluationOutput(output, now)

        assertEquals(1, manager.activeAlertsFlow.value.size)
        assertEquals("alert-1", manager.activeAlertsFlow.value.first().id)
        assertEquals(1, events.size)
        assertTrue(events.first() is AlertLifecycleEvent.Raised)
    }

    @Test
    fun testRepeatBreachIncrementsCountWithoutDuplicateEntry() {
        val alert = createSampleAlert("alert-1", "env:temp:dht22:Simulated", triggerVal = 36.0f)
        manager.processEvaluationOutput(
            AlertEvaluationOutput(listOf(alert), emptyList(), emptyList()),
            now
        )

        // Repeat breach with same dedupKey
        val repeatOutput = AlertEvaluationOutput(
            newAlerts = emptyList(),
            repeatKeys = listOf("env:temp:dht22:Simulated" to 37.0f),
            resolvedKeys = emptyList()
        )
        manager.processEvaluationOutput(repeatOutput, now + 5000L)

        assertEquals(1, manager.activeAlertsFlow.value.size)
        val active = manager.activeAlertsFlow.value.first()
        assertEquals(2, active.occurrenceCount)
        assertEquals(37.0f, active.triggerValue)
    }

    @Test
    fun testAcknowledgeRecordsTimestampAndDoesNotResolve() {
        val alert = createSampleAlert("alert-1", "env:temp:dht22:Simulated")
        manager.processEvaluationOutput(
            AlertEvaluationOutput(listOf(alert), emptyList(), emptyList()),
            now
        )

        val acknowledged = manager.acknowledgeAlert("alert-1", now + 10_000L)
        assertTrue(acknowledged)

        val active = manager.activeAlertsFlow.value.first()
        assertEquals(AlertStatus.Acknowledged, active.status)
        assertTrue(active.isActive)
        assertFalse(active.isResolved)
        assertEquals(now + 10_000L, active.acknowledgedTimestampEpochMillis)
    }

    @Test
    fun testAutoResolveMovesAlertToResolvedList() {
        val alert = createSampleAlert("alert-1", "env:temp:dht22:Simulated")
        manager.processEvaluationOutput(
            AlertEvaluationOutput(listOf(alert), emptyList(), emptyList()),
            now
        )

        val resolveOutput = AlertEvaluationOutput(
            newAlerts = emptyList(),
            repeatKeys = emptyList(),
            resolvedKeys = listOf("env:temp:dht22:Simulated" to 31.0f)
        )
        val events = manager.processEvaluationOutput(resolveOutput, now + 15_000L)

        assertEquals(0, manager.activeAlertsFlow.value.size)
        assertEquals(1, manager.resolvedAlertsFlow.value.size)

        val resolved = manager.resolvedAlertsFlow.value.first()
        assertEquals(AlertStatus.Resolved, resolved.status)
        assertEquals(31.0f, resolved.resolvingValue)
        assertEquals(now + 15_000L, resolved.resolvedTimestampEpochMillis)

        assertEquals(1, events.size)
        assertTrue(events.first() is AlertLifecycleEvent.Resolved)
    }

    @Test
    fun testSeverityEscalationGeneratesEscalatedEvent() {
        val alert = createSampleAlert("alert-1", "env:temp:dht22:Simulated", severity = AlertSeverity.Warning)
        manager.processEvaluationOutput(
            AlertEvaluationOutput(listOf(alert), emptyList(), emptyList()),
            now
        )

        // Incoming Critical alert with same dedupKey
        val criticalAlert = createSampleAlert("alert-2", "env:temp:dht22:Simulated", severity = AlertSeverity.Critical, triggerVal = 44.0f)
        val events = manager.processEvaluationOutput(
            AlertEvaluationOutput(listOf(criticalAlert), emptyList(), emptyList()),
            now + 2000L
        )

        assertEquals(1, manager.activeAlertsFlow.value.size)
        val active = manager.activeAlertsFlow.value.first()
        assertEquals(AlertSeverity.Critical, active.severity)
        assertEquals(1, events.size)
        assertTrue(events.first() is AlertLifecycleEvent.Escalated)
    }
}

