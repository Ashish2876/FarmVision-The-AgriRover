package com.farmvision.agrirover.alert

import com.farmvision.agrirover.data.alert.repository.PreferencesAlertSettingsRepository
import com.farmvision.agrirover.domain.alert.engine.AlertThresholdConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertSettingsRepositoryTest {

    @Test
    fun testValidConfigurationPasses() {
        val valid = AlertThresholdConfig()
        val errors = PreferencesAlertSettingsRepository.validate(valid)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testRejectsMinGreaterThanMax() {
        val invalidTemp = AlertThresholdConfig(
            tempMinWarning = 40.0f,
            tempMaxWarning = 30.0f // min > max
        )
        val errors1 = PreferencesAlertSettingsRepository.validate(invalidTemp)
        assertFalse(errors1.isEmpty())
        assertTrue(errors1.any { it.contains("Temperature min warning") })

        val invalidHumidity = AlertThresholdConfig(
            humidityMinCritical = 90.0f,
            humidityMaxCritical = 80.0f // min > max
        )
        val errors2 = PreferencesAlertSettingsRepository.validate(invalidHumidity)
        assertFalse(errors2.isEmpty())
        assertTrue(errors2.any { it.contains("Humidity min critical") })
    }

    @Test
    fun testRejectsNonPositiveHysteresisAndDebounce() {
        val invalidHyst = AlertThresholdConfig(
            tempHysteresis = 0.0f
        )
        val errors = PreferencesAlertSettingsRepository.validate(invalidHyst)
        assertTrue(errors.any { it.contains("hysteresis must be greater than zero") })

        val invalidDebounce = AlertThresholdConfig(
            confirmationCount = 0
        )
        val debounceErrors = PreferencesAlertSettingsRepository.validate(invalidDebounce)
        assertTrue(debounceErrors.any { it.contains("Debounce confirmation count must be at least 1") })
    }

    @Test
    fun testRejectsInvertedObstacleDistances() {
        val invalidObstacle = AlertThresholdConfig(
            obstacleCriticalCm = 30.0f,
            obstacleWarningCm = 20.0f // critical > warning distance
        )
        val errors = PreferencesAlertSettingsRepository.validate(invalidObstacle)
        assertTrue(errors.any { it.contains("Obstacle critical distance") })
    }

    @Test
    fun testRejectsInvertedLinkTimeouts() {
        val invalidTimeouts = AlertThresholdConfig(
            linkWarningTimeoutSeconds = 15L,
            offlineCriticalTimeoutSeconds = 5L // offline timeout <= link warning timeout
        )
        val errors = PreferencesAlertSettingsRepository.validate(invalidTimeouts)
        assertTrue(errors.any { it.contains("Offline critical timeout") })
    }
}

