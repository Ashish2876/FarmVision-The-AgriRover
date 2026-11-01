package com.farmvision.agrirover.ui.alert.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.farmvision.agrirover.MainActivity
import com.farmvision.agrirover.R
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.domain.alert.lifecycle.AlertLifecycleEvent
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity

/**
 * System notification manager dispatching prioritized alerts to the Android notification shade.
 * Strictly respects user permission, channel configurations, and cooldown intervals.
 */
class AlertNotificationManager(
    private val context: Context,
    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
) {

    companion object {
        const val EXTRA_DESTINATION = "extra_nav_destination"
        const val EXTRA_ALERT_ID = "extra_alert_id"
        const val DESTINATION_ALERTS = "alerts_centre"

        const val COOLDOWN_CRITICAL_MS = 5L * 60 * 1000 // 5 minutes
        const val COOLDOWN_WARNING_MS = 15L * 60 * 1000 // 15 minutes
    }

    private val lastNotifiedTimestamps = mutableMapOf<String, Long>()

    init {
        AlertNotificationChannels.registerChannels(context)
    }

    /**
     * Checks whether runtime notification permission is granted.
     */
    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Verifies if notifications are globally enabled for the application.
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager?.areNotificationsEnabled() == true && hasNotificationPermission()
    }

    /**
     * Dispatches a lifecycle event to Android notifications if allowed and not throttled.
     */
    fun handleLifecycleEvent(
        event: AlertLifecycleEvent,
        nowEpochMillis: Long = System.currentTimeMillis(),
        isRecoveryNotificationsEnabled: Boolean = true
    ) {
        if (!areNotificationsEnabled()) return

        when (event) {
            is AlertLifecycleEvent.Raised -> dispatchAlertNotification(event.alert, nowEpochMillis, isEscalation = false)
            is AlertLifecycleEvent.Escalated -> dispatchAlertNotification(event.alert, nowEpochMillis, isEscalation = true)
            is AlertLifecycleEvent.Resolved -> {
                cancelNotification(event.alert.dedupKey)
                if (isRecoveryNotificationsEnabled) {
                    dispatchRecoveryNotification(event.alert, nowEpochMillis)
                }
            }
        }
    }

    private fun dispatchAlertNotification(alert: AlertItem, nowEpochMillis: Long, isEscalation: Boolean) {
        // Enforce cooldown per dedupKey unless escalated
        val lastNotified = lastNotifiedTimestamps[alert.dedupKey] ?: 0L
        val cooldown = if (alert.severity == AlertSeverity.Critical) COOLDOWN_CRITICAL_MS else COOLDOWN_WARNING_MS

        if (!isEscalation && (nowEpochMillis - lastNotified) < cooldown) {
            return
        }

        val channelId = if (alert.severity == AlertSeverity.Critical) {
            AlertNotificationChannels.CHANNEL_CRITICAL
        } else {
            AlertNotificationChannels.CHANNEL_WARNING
        }

        val notificationId = alert.dedupKey.hashCode()
        val contentIntent = createPendingIntent(alert.id)

        val simPrefix = if (alert.origin == DataOrigin.Simulated) "[DEMO] " else ""
        val severityTag = if (alert.severity == AlertSeverity.Critical) "🚨 CRITICAL: " else "⚠️ WARNING: "
        val displayTitle = "$simPrefix$severityTag${alert.title}"

        val priority = if (alert.severity == AlertSeverity.Critical) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setWhen(alert.createdTimestampEpochMillis)

        try {
            notificationManager?.notify(notificationId, builder.build())
            lastNotifiedTimestamps[alert.dedupKey] = nowEpochMillis
        } catch (_: SecurityException) {
            // Handled gracefully when permission revoked at runtime
        }
    }

    private fun dispatchRecoveryNotification(alert: AlertItem, nowEpochMillis: Long) {
        val channelId = AlertNotificationChannels.CHANNEL_INFO
        val notificationId = (alert.dedupKey + "_recovery").hashCode()
        val contentIntent = createPendingIntent(alert.id)

        val simPrefix = if (alert.origin == DataOrigin.Simulated) "[DEMO] " else ""
        val title = "$simPrefix✅ RECOVERED: ${alert.title}"
        val desc = "Condition has resolved. Operating values returned to normal parameters."

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setWhen(nowEpochMillis)

        try {
            notificationManager?.notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun cancelNotification(dedupKey: String) {
        val notificationId = dedupKey.hashCode()
        notificationManager?.cancel(notificationId)
    }

    private fun createPendingIntent(alertId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_ALERTS)
            putExtra(EXTRA_ALERT_ID, alertId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, alertId.hashCode(), intent, flags)
    }
}

