package com.farmvision.agrirover.alert

import com.farmvision.agrirover.domain.alert.engine.AlertSeverityPolicy
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertSeverityPolicyTest {

    @Test
    fun testCriticalRulesMapToCriticalSeverity() {
        assertEquals(AlertSeverity.Critical, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_ESTOP_ACTIVE))
        assertEquals(AlertSeverity.Critical, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_ROVER_OFFLINE))
        assertEquals(AlertSeverity.Critical, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_OBSTACLE_CRITICAL))
        assertEquals(AlertSeverity.Critical, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_TEMP_HIGH_CRITICAL))
        assertEquals(AlertSeverity.Critical, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_TEMP_LOW_CRITICAL))
    }

    @Test
    fun testWarningRulesMapToWarningSeverity() {
        assertEquals(AlertSeverity.Warning, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_TEMP_HIGH_WARNING))
        assertEquals(AlertSeverity.Warning, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_HUMIDITY_LOW_WARNING))
        assertEquals(AlertSeverity.Warning, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_SOIL_ADC_SATURATION))
        assertEquals(AlertSeverity.Warning, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_SOIL_PH_UNCALIBRATED))
        assertEquals(AlertSeverity.Warning, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_OBSTACLE_WARNING))
        assertEquals(AlertSeverity.Warning, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_ROVER_LINK_UNSTABLE))
    }

    @Test
    fun testInformationRulesMapToInformationSeverity() {
        assertEquals(AlertSeverity.Information, AlertSeverityPolicy.getSeverityForRule(AlertSeverityPolicy.RULE_RECOVERY_INFO))
    }

    @Test
    fun testCategoryMapping() {
        assertEquals(AlertCategory.Soil, AlertSeverityPolicy.getCategoryForRule(AlertSeverityPolicy.RULE_SOIL_ADC_SATURATION))
        assertEquals(AlertCategory.Environment, AlertSeverityPolicy.getCategoryForRule(AlertSeverityPolicy.RULE_TEMP_HIGH_CRITICAL))
        assertEquals(AlertCategory.SoilPh, AlertSeverityPolicy.getCategoryForRule(AlertSeverityPolicy.RULE_SOIL_PH_UNCALIBRATED))
        assertEquals(AlertCategory.Rover, AlertSeverityPolicy.getCategoryForRule(AlertSeverityPolicy.RULE_ESTOP_ACTIVE))
        assertEquals(AlertCategory.Rover, AlertSeverityPolicy.getCategoryForRule(AlertSeverityPolicy.RULE_OBSTACLE_CRITICAL))
        assertEquals(AlertCategory.System, AlertSeverityPolicy.getCategoryForRule(AlertSeverityPolicy.RULE_RECOVERY_INFO))
    }

    @Test
    fun testSeverityHierarchyOrdering() {
        assertTrue(AlertSeverity.Critical.isHigherThan(AlertSeverity.Warning))
        assertTrue(AlertSeverity.Warning.isHigherThan(AlertSeverity.Information))
        assertTrue(AlertSeverity.Critical.isHigherThan(AlertSeverity.Information))
    }
}

