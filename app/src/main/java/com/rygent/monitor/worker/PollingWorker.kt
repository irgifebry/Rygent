package com.rygent.monitor.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rygent.monitor.domain.repository.DeviceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@HiltWorker
class PollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DeviceRepository,
    private val settingsManager: com.rygent.monitor.data.local.SettingsManager,
    private val notificationHelper: com.rygent.monitor.ui.components.NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val devices = repository.getAllDevices().first()
            
            // Parallel refresh: prevents timeout delays from stacking
            coroutineScope {
                devices.map { device ->
                    async {
                        repository.refreshDevice(device)
                    }
                }.awaitAll()
            }
            
            // Re-read latest list to check thresholds
            val updatedDevices = repository.getAllDevices().first()
            updatedDevices.forEach { device ->
                if (device.cpuUsage >= settingsManager.cpuThreshold) {
                    notificationHelper.showThresholdAlert(device.name, "CPU", device.cpuUsage)
                }
                if (device.ramUsage >= settingsManager.ramThreshold) {
                    notificationHelper.showThresholdAlert(device.name, "RAM", device.ramUsage)
                }

                // Phase 4 Analytics: Runaway Process Detection Alerts
                // Background query to see if the Python agent found runaway processes and trigger a notification
                val alertsResult = repository.getAlerts(device)
                alertsResult.onSuccess { alerts ->
                    alerts.forEach { alert ->
                        notificationHelper.showRunawayProcessAlert(
                            deviceName = device.name,
                            processName = alert.name,
                            cpuPercent = alert.cpuPercent,
                            durationMin = alert.durationMin
                        )
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("PollingWorker", "Polling failed: ${e.message}", e)
            Result.retry()
        }
    }
}
