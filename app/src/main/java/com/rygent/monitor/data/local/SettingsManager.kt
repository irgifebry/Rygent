package com.rygent.monitor.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    var pollingIntervalMs: Long
        get() = prefs.getLong("polling_interval", 900000L) // Default 15 mins
        set(value) = prefs.edit().putLong("polling_interval", value).apply()

    var cpuThreshold: Int
        get() = prefs.getInt("cpu_threshold", 90)
        set(value) = prefs.edit().putInt("cpu_threshold", value).apply()

    var ramThreshold: Int
        get() = prefs.getInt("ram_threshold", 90)
        set(value) = prefs.edit().putInt("ram_threshold", value).apply()

    var isBackgroundMonitoring: Boolean
        get() = prefs.getBoolean("is_background_monitoring", false)
        set(value) = prefs.edit().putBoolean("is_background_monitoring", value).apply()

    var dashboardRefreshRateMs: Long
        get() = prefs.getLong("dashboard_refresh_rate", 5000L)
        set(value) = prefs.edit().putLong("dashboard_refresh_rate", value).apply()
}
