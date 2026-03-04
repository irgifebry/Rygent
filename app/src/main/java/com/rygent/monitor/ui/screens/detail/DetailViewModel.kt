package com.rygent.monitor.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.DeviceStatus
import com.rygent.monitor.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: DeviceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    
    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _ramHistory = MutableStateFlow<List<Float>>(emptyList())
    val ramHistory: StateFlow<List<Float>> = _ramHistory.asStateFlow()

    private val _diskHistory = MutableStateFlow<List<Float>>(emptyList())
    val diskHistory: StateFlow<List<Float>> = _diskHistory.asStateFlow()

    private val _gpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val gpuHistory: StateFlow<List<Float>> = _gpuHistory.asStateFlow()

    private val _coreUsageHistory = MutableStateFlow<Map<Int, List<Float>>>(emptyMap())
    val coreUsageHistory: StateFlow<Map<Int, List<Float>>> = _coreUsageHistory.asStateFlow()

    private val _netDownloadHistory = MutableStateFlow<List<Float>>(emptyList())
    val netDownloadHistory: StateFlow<List<Float>> = _netDownloadHistory.asStateFlow()

    private val _netUploadHistory = MutableStateFlow<List<Float>>(emptyList())
    val netUploadHistory: StateFlow<List<Float>> = _netUploadHistory.asStateFlow()

    private var lastNetBytes = Pair(0L, 0L)
    private var lastNetTimestamp = 0L

    // B4: Use SharedFlow properly for one-shot UI events
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    // Phase 3: Storage Intelligence
    private val _smartDrives = MutableStateFlow<List<com.rygent.monitor.domain.model.SmartDrive>>(emptyList())
    val smartDrives: StateFlow<List<com.rygent.monitor.domain.model.SmartDrive>> = _smartDrives.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<com.rygent.monitor.domain.model.LargeFile>>(emptyList())
    val largeFiles: StateFlow<List<com.rygent.monitor.domain.model.LargeFile>> = _largeFiles.asStateFlow()

    private val _isLoadingSmart = MutableStateFlow(false)
    val isLoadingSmart: StateFlow<Boolean> = _isLoadingSmart.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDeviceById(deviceId).collect { dev ->
                dev?.let { 
                    if (_device.value?.lastSeen != it.lastSeen) {
                        updateHistory(it)
                    }
                    val wasNull = _device.value == null
                    _device.value = it
                    if (wasNull) {
                        loadAnalyticsData()
                        // FIX: Trigger immediate refresh when device is first loaded
                        // This handles the case where the device was saved but refresh
                        // hasn't completed yet (e.g. navigated from AddDevice too fast)
                        viewModelScope.launch {
                            try {
                                repository.refreshDevice(it)
                            } catch (e: Exception) {
                                android.util.Log.e("DetailVM", "Initial refresh failed: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
        if (savedStateHandle.get<Boolean>("disablePolling") != true) {
            startPolling()
        }
    }

    private fun updateHistory(device: Device) {
        _cpuHistory.value = (_cpuHistory.value + device.cpuUsage.toFloat()).takeLast(100)
        _ramHistory.value = (_ramHistory.value + device.ramUsage.toFloat()).takeLast(100)
        device.diskUsage?.let { 
            _diskHistory.value = (_diskHistory.value + it.toFloat()).takeLast(100)
        }
        device.gpus?.firstOrNull()?.let {
            _gpuHistory.value = (_gpuHistory.value + it.usage.toFloat()).takeLast(100)
        }
        
        // Per-core history
        device.cpuUsagePerCore?.let { cores ->
            val currentMap = _coreUsageHistory.value.toMutableMap()
            cores.forEachIndexed { index, usage ->
                val list = (currentMap[index] ?: emptyList()) + usage.toFloat()
                currentMap[index] = list.takeLast(50)
            }
            _coreUsageHistory.value = currentMap
        }

        // Network history calculation
        device.network?.let { net ->
            val now = System.currentTimeMillis()
            if (lastNetTimestamp > 0) {
                val elapsedSecs = (now - lastNetTimestamp) / 1000.0
                if (elapsedSecs > 0) {
                    val dlDiff = (net.bytesRecv - lastNetBytes.first).coerceAtLeast(0)
                    val ulDiff = (net.bytesSent - lastNetBytes.second).coerceAtLeast(0)
                    
                    val dlRate = (dlDiff / elapsedSecs) / 1024.0 / 1024.0 // MB/s
                    val ulRate = (ulDiff / elapsedSecs) / 1024.0 / 1024.0 // MB/s
                    
                    _netDownloadHistory.value = (_netDownloadHistory.value + dlRate.toFloat()).takeLast(50)
                    _netUploadHistory.value = (_netUploadHistory.value + ulRate.toFloat()).takeLast(50)
                }
            }
            lastNetBytes = Pair(net.bytesRecv, net.bytesSent)
            lastNetTimestamp = now
        }
    }

    // B7: Added isActive check and skip polling when device is OFFLINE
    private fun startPolling() {
        viewModelScope.launch {
            // FIX: Wait briefly for initial DB load before starting polling
            delay(1500L)
            while (isActive) {
                try {
                    _device.value?.let { dev ->
                        // Only poll if device was last known online or we want to check recovery
                        repository.refreshDevice(dev)
                    }
                } catch (e: Exception) {
                    // Ignore transient errors in loop
                }
                // Adaptive polling: faster when online, slower when offline
                val interval = if (_device.value?.status == DeviceStatus.ONLINE) 1000L else 5000L
                delay(interval)
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _device.value?.let { repository.refreshDevice(it) }
        }
    }

    fun shutdown() {
        viewModelScope.launch {
            _device.value?.let { 
                repository.shutdownDevice(it)
                    .onSuccess { _uiEvent.emit("Shutdown command sent") }
                    .onFailure { _uiEvent.emit("Shutdown Failed: ${it.message}") }
            }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            _device.value?.let { 
                repository.rebootDevice(it)
                    .onSuccess { _uiEvent.emit("Reboot command sent") }
                    .onFailure { _uiEvent.emit("Reboot Failed: ${it.message}") }
            }
        }
    }

    fun suspend() {
        viewModelScope.launch {
            _device.value?.let { 
                repository.suspendDevice(it)
                    .onSuccess { _uiEvent.emit("Suspend command sent") }
                    .onFailure { _uiEvent.emit("Suspend Failed: ${it.message}") }
            }
        }
    }

    fun killProcess(pid: Int) {
        viewModelScope.launch {
            _device.value?.let { dev -> 
                repository.killProcess(dev, pid)
                    .onSuccess { 
                        _uiEvent.emit("Kill command sent for PID $pid")
                        repository.refreshDevice(dev)
                    }
                    .onFailure { _uiEvent.emit("Kill Process Failed: ${it.message}") }
            }
        }
    }

    fun deleteDevice(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _device.value?.let { dev ->
                try {
                    repository.deleteDevice(dev)
                    _uiEvent.emit("Device removed")
                    onDeleted()
                } catch (e: Exception) {
                    _uiEvent.emit("Failed to remove device: ${e.message}")
                }
            }
        }
    }

    fun updateDeviceConfig(name: String, ip: String, port: Int, token: String?) {
        viewModelScope.launch {
            try {
                repository.updateDeviceConfig(deviceId, name, ip, port, token)
                _uiEvent.emit("Device configuration updated")
                // Refresh to apply new connection details
                delay(300)
                refresh()
            } catch (e: Exception) {
                _uiEvent.emit("Failed to update: ${e.message}")
            }
        }
    }

    // ── Phase 3: Storage Intelligence ─────────────────────────

    fun loadSmartDrives() {
        viewModelScope.launch {
            _isLoadingSmart.value = true
            _device.value?.let { dev ->
                repository.getSmartDrives(dev)
                    .onSuccess { _smartDrives.value = it }
                    .onFailure { _uiEvent.emit("S.M.A.R.T. scan failed: ${it.message}") }
            }
            _isLoadingSmart.value = false
        }
    }

    fun scanLargeFiles() {
        viewModelScope.launch {
            _isLoadingFiles.value = true
            _device.value?.let { dev ->
                repository.getLargeFiles(dev)
                    .onSuccess { _largeFiles.value = it }
                    .onFailure { _uiEvent.emit("File scan failed: ${it.message}") }
            }
            _isLoadingFiles.value = false
        }
    }

    // ── Phase 4: Advanced Analytics & Connectivity ─────────────

    private val _speedTestResult = MutableStateFlow<com.rygent.monitor.domain.model.SpeedTestResult?>(null)
    val speedTestResult: StateFlow<com.rygent.monitor.domain.model.SpeedTestResult?> = _speedTestResult.asStateFlow()

    private val _isSpeedTestRunning = MutableStateFlow(false)
    val isSpeedTestRunning: StateFlow<Boolean> = _isSpeedTestRunning.asStateFlow()

    fun runSpeedTest() {
        viewModelScope.launch {
            _isSpeedTestRunning.value = true
            _speedTestResult.value = null // clear previous
            _device.value?.let { dev ->
                repository.runSpeedTest(dev)
                    .onSuccess { 
                        if (it.error != null) {
                            _uiEvent.emit("Speedtest error: ${it.error}")
                        } else {
                            _speedTestResult.value = it
                        }
                    }
                    .onFailure { _uiEvent.emit("Speedtest failed: ${it.message}") }
            }
            _isSpeedTestRunning.value = false
        }
    }

    private val _processHistory = MutableStateFlow<List<com.rygent.monitor.domain.model.ProcessHistoryEntry>>(emptyList())
    val processHistory: StateFlow<List<com.rygent.monitor.domain.model.ProcessHistoryEntry>> = _processHistory.asStateFlow()

    private val _runawayAlerts = MutableStateFlow<List<com.rygent.monitor.domain.model.RunawayAlert>>(emptyList())
    val runawayAlerts: StateFlow<List<com.rygent.monitor.domain.model.RunawayAlert>> = _runawayAlerts.asStateFlow()

    private val _isLoadingAnalytics = MutableStateFlow(false)
    val isLoadingAnalytics: StateFlow<Boolean> = _isLoadingAnalytics.asStateFlow()

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _isLoadingAnalytics.value = true
            _device.value?.let { dev ->
                repository.getProcessHistory(dev)
                    .onSuccess { _processHistory.value = it }
                
                repository.getAlerts(dev)
                    .onSuccess { _runawayAlerts.value = it }
            }
            _isLoadingAnalytics.value = false
        }
    }
}
