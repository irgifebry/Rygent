package com.rygent.monitor.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rygent.monitor.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "system_monitor_alerts"
    
    // F2: Rate limiting — track last notification time per device+metric
    private val lastNotificationTime = mutableMapOf<String, Long>()
    private val NOTIFICATION_COOLDOWN_MS = 3600_000L // 1 hour cooldown

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "System Alerts"
            val descriptionText = "Notifications for high CPU/RAM usage"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showThresholdAlert(deviceName: String, metric: String, value: Int) {
        // F2: Rate limit notifications — max 1 per device+metric per hour
        val key = "${deviceName}_$metric"
        val now = System.currentTimeMillis()
        val lastTime = lastNotificationTime[key] ?: 0L
        
        if (now - lastTime < NOTIFICATION_COOLDOWN_MS) {
            return // Skip — too soon since last alert for this device+metric
        }
        
        lastNotificationTime[key] = now
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ $deviceName")
            .setContentText("High $metric usage: $value%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(key.hashCode(), builder.build())
    }

    // ── Phase 4: Runaway Detection Push Notification ──────────

    fun showRunawayProcessAlert(deviceName: String, processName: String, cpuPercent: Double, durationMin: Int) {
        val key = "${deviceName}_runaway_$processName"
        val now = System.currentTimeMillis()
        val lastTime = lastNotificationTime[key] ?: 0L
        
        if (now - lastTime < NOTIFICATION_COOLDOWN_MS) {
            return // Skip — we already warned about this process recently
        }
        
        lastNotificationTime[key] = now
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 $deviceName")
            .setContentText("Runaway Process: $processName using ${String.format("%.1f", cpuPercent)}% CPU for >$durationMin min!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        notificationManager.notify(key.hashCode(), builder.build())
    }
}
