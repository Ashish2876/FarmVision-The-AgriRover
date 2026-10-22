package com.farmvision.agrirover

import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionMetricType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Phase 2 design system tokens, metric types, and status models.
 */
class DesignSystemUnitTest {

    @Test
    fun statusTypes_allEightVariantsAreDefinedWithIconsAndLabels() {
        val allStatuses = FarmVisionStatusType.values()
        assertEquals(8, allStatuses.size)

        for (status in allStatuses) {
            assertTrue(status.defaultLabel.isNotBlank())
            assertNotNull(status.icon)
        }

        assertTrue(allStatuses.contains(FarmVisionStatusType.Online))
        assertTrue(allStatuses.contains(FarmVisionStatusType.Offline))
        assertTrue(allStatuses.contains(FarmVisionStatusType.Connecting))
        assertTrue(allStatuses.contains(FarmVisionStatusType.Active))
        assertTrue(allStatuses.contains(FarmVisionStatusType.Idle))
        assertTrue(allStatuses.contains(FarmVisionStatusType.Warning))
        assertTrue(allStatuses.contains(FarmVisionStatusType.Error))
        assertTrue(allStatuses.contains(FarmVisionStatusType.DemoData))
    }

    @Test
    fun metricTypes_fiveRequiredCategoriesArePresentWithUnits() {
        val metrics = FarmVisionMetricType.values()
        assertEquals(5, metrics.size)

        val soil = FarmVisionMetricType.SoilMoisture
        assertEquals("%", soil.defaultUnit)

        val temp = FarmVisionMetricType.Temperature
        assertEquals("°C", temp.defaultUnit)

        val humidity = FarmVisionMetricType.AirHumidity
        assertEquals("%", humidity.defaultUnit)

        val light = FarmVisionMetricType.LightIntensity
        assertEquals("lx", light.defaultUnit)

        val battery = FarmVisionMetricType.BatteryPercentage
        assertEquals("%", battery.defaultUnit)
    }

    @Test
    fun spacingTokens_adhereToFourDpGridConvention() {
        assertEquals(2.dp, FarmVisionSpacing.xxSmall)
        assertEquals(4.dp, FarmVisionSpacing.extraSmall)
        assertEquals(8.dp, FarmVisionSpacing.small)
        assertEquals(12.dp, FarmVisionSpacing.medium)
        assertEquals(16.dp, FarmVisionSpacing.large)
        assertEquals(24.dp, FarmVisionSpacing.extraLarge)
        assertEquals(32.dp, FarmVisionSpacing.xxLarge)
        assertEquals(48.dp, FarmVisionSpacing.huge)
    }

    @Test
    fun accessibleTouchTargets_areAtLeast48Dp() {
        assertTrue(FarmVisionSpacing.minTouchTarget >= 48.dp)
        assertTrue(FarmVisionSpacing.buttonHeight >= 48.dp)
    }
}
