package com.rygent.monitor.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.rygent.monitor.data.local.SettingsManager
import com.rygent.monitor.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _cpuThreshold = MutableStateFlow(settingsManager.cpuThreshold)
    val cpuThreshold: StateFlow<Int> = _cpuThreshold

    private val _ramThreshold = MutableStateFlow(settingsManager.ramThreshold)
    val ramThreshold: StateFlow<Int> = _ramThreshold

    private val _pollingInterval = MutableStateFlow(settingsManager.pollingIntervalMs / 60000) // Convert to mins
    val pollingInterval: StateFlow<Long> = _pollingInterval

    fun updateCpuThreshold(value: Int) {
        _cpuThreshold.value = value
        settingsManager.cpuThreshold = value
    }

    fun updateRamThreshold(value: Int) {
        _ramThreshold.value = value
        settingsManager.ramThreshold = value
    }

    fun updatePollingInterval(minutes: Long) {
        _pollingInterval.value = minutes
        settingsManager.pollingIntervalMs = minutes * 60000
        // B3: Reschedule WorkManager when interval changes
        WorkScheduler.schedulePolling(context)
    }
}
