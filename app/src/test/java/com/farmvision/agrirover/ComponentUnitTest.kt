package com.farmvision.agrirover

import com.farmvision.agrirover.ui.components.AgriStatusType
import com.farmvision.agrirover.ui.components.ConnectionProtocol
import com.farmvision.agrirover.ui.components.LinkQuality
import com.farmvision.agrirover.ui.components.MetricTrend
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusOnline
import com.farmvision.agrirover.ui.theme.StatusWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Phase 2 design system components, semantic models, and gauge math.
 */
class ComponentUnitTest {

    @Test
    fun agriStatusType_hasCorrectSemanticColors() {
        assertEquals(StatusOnline, AgriStatusType.Nominal.color)
        assertEquals(StatusWarning, AgriStatusType.Warning.color)
        assertEquals(StatusAlert, AgriStatusType.Alert.color)
    }

    @Test
    fun connectionProtocol_labelsAreValid() {
        assertEquals("BLE 5.0", ConnectionProtocol.BluetoothLowEnergy.label)
        assertEquals("Wi-Fi Direct", ConnectionProtocol.WiFiSoftAP.label)
        assertEquals("Offline", ConnectionProtocol.Disconnected.label)
    }

    @Test
    fun linkQuality_evaluatesCorrectly() {
        assertEquals("Strong", LinkQuality.Excellent.label)
        assertEquals("Disconnected", LinkQuality.Offline.label)
    }

    @Test
    fun metricTrend_containsAllVariations() {
        val trends = MetricTrend.values()
        assertTrue(trends.contains(MetricTrend.Rising))
        assertTrue(trends.contains(MetricTrend.Falling))
        assertTrue(trends.contains(MetricTrend.Stable))
    }

    @Test
    fun gaugeRatio_isProperlyClamped() {
        val currentSpeed = 7.5f
        val maxSpeed = 6.0f
        val clampedRatio = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
        assertEquals(1.0f, clampedRatio, 0.001f)

        val negativeSpeed = -1.0f
        val clampedNegative = (negativeSpeed / maxSpeed).coerceIn(0f, 1f)
        assertEquals(0.0f, clampedNegative, 0.001f)
    }
}
