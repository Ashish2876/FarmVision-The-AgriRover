package com.farmvision.agrirover.ui.alert.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Android Notification Channel configuration for FarmVision AgriRover.
 * Establishes 3 distinct user-configurable channels matching alert severities.
 */
object AlertNotificationChannels {

    const val CHANNEL_CRITICAL = "farmvision_alerts_critical"
    const val CHANNEL_WARNING = "farmvision_alerts_warning"
    const val CHANNEL_INFO = "farmvision_alerts_info"

    /**
     * Registers system notification channels on Android 8.0 (API 26) and higher.
     */
    fun registerChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Critical Channel
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL,
                "Critical Safety & Farm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate operational hazards, Emergency Stop, and severe drought / frost breaches."
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            // 2. Warning Channel
            val warningChannel = NotificationChannel(
                CHANNEL_WARNING,
                "Warning & Advisory Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Agronomic parameter warnings, telemetry latency, and obstacle proximity advisories."
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Information Channel
            val infoChannel = NotificationChannel(
                CHANNEL_INFO,
                "Information & Condition Recovery",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Metric recovery, system status updates, and normal state transitions."
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(criticalChannel, warningChannel, infoChannel)
            )
        }
    }
}

