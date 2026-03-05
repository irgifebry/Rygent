package com.rygent.monitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rygent.monitor.MainActivity
import com.rygent.monitor.data.local.SettingsManager
import com.rygent.monitor.domain.repository.DeviceRepository
import com.rygent.monitor.ui.components.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var repository: DeviceRepository
    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    companion object {
        const val CHANNEL_ID = "monitoring_service"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.rygent.monitor.START_MONITORING"
        const val ACTION_STOP = "com.rygent.monitor.STOP_MONITORING"

        fun start(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification("Monitoring devices..."))
                startPolling()
            }
        }
        return START_STICKY
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val devices = repository.getAllDevices().first()
                    val onlineCount = devices.count { dev ->
                        try {
                            repository.refreshDevice(dev)
                            true
                        } catch (_: Exception) { false }
                    }

                    // Update notification with live status
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, createNotification("$onlineCount/${devices.size} devices online"))

                    // Check thresholds
                    val updated = repository.getAllDevices().first()
                    updated.forEach { device ->
                        if (device.cpuUsage >= settingsManager.cpuThreshold) {
                            notificationHelper.showThresholdAlert(device.name, "CPU", device.cpuUsage)
                        }
                        if (device.ramUsage >= settingsManager.ramThreshold) {
                            notificationHelper.showThresholdAlert(device.name, "RAM", device.ramUsage)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MonitoringService", "Poll error: ${e.message}")
                }
                delay(settingsManager.dashboardRefreshRateMs.coerceAtLeast(5000))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Persistent Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background device monitoring service"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.rygent.monitor.R.drawable.ic_stat_monitor)
            .setContentTitle(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        settingsManager.isBackgroundMonitoring = false
        super.onDestroy()
    }
}
