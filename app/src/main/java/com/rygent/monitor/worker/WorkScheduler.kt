package com.rygent.monitor.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rygent.monitor.data.local.SettingsManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    // B3: Use SettingsManager interval instead of hardcoded 15 minutes
    fun schedulePolling(context: Context) {
        val settingsManager = SettingsManager(context)
        val intervalMs = settingsManager.pollingIntervalMs
        val intervalMinutes = (intervalMs / 60000).coerceAtLeast(15) // WorkManager minimum is 15 min
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val pollingRequest = PeriodicWorkRequestBuilder<PollingWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "SystemMonitorPolling",
            ExistingPeriodicWorkPolicy.UPDATE, // UPDATE instead of KEEP so interval changes take effect
            pollingRequest
        )
    }
}
