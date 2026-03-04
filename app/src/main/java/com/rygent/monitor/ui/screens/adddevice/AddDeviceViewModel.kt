package com.rygent.monitor.ui.screens.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rygent.monitor.data.local.TokenManager
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.DeviceStatus
import com.rygent.monitor.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val tokenManager: TokenManager,
    private val discoveryManager: com.rygent.monitor.data.remote.NetworkDiscoveryManager
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val discoveredDevices: StateFlow<List<com.rygent.monitor.data.remote.DiscoveredDevice>> = discoveryManager.discoveredDevices
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveryLog: StateFlow<String> = discoveryManager.discoveryLog

    fun scanNetwork() {
        viewModelScope.launch {
            _isScanning.value = true
            discoveryManager.startDiscovery(this)
            delay(7000) // Scan for 7 seconds
            discoveryManager.stopDiscovery()
            _isScanning.value = false
        }
    }

    fun saveDevice(name: String, ip: String, port: String, token: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Deduplication: check if device name already exists
                val existingDevice = repository.getDeviceByName(name)
                val deviceId = existingDevice?.id ?: UUID.randomUUID().toString()

                val device = Device(
                    id = deviceId,
                    name = name.trim(),
                    ipAddress = ip.trim(),
                    port = port.toIntOrNull() ?: 5000,
                    // FIX: Set initial status to ONLINE (we're actively connecting)
                    status = DeviceStatus.ONLINE,
                    cpuUsage = existingDevice?.cpuUsage ?: 0,
                    ramUsage = existingDevice?.ramUsage ?: 0
                )

                repository.insertDevice(device)
                tokenManager.saveToken(deviceId, token)

                // FIX: Attempt initial refresh BEFORE navigating away with longer timeout
                // This ensures the device gets real data and stays ONLINE
                // Increased timeout to 15s to allow Cloudflare Tunnel to establish
                val refreshed = withTimeoutOrNull(15000L) {
                    try {
                        android.util.Log.d("AddDeviceVM", "Starting initial refresh for ${device.name} at ${device.ipAddress}:${device.port}")
                        repository.refreshDevice(device)
                        true
                    } catch (e: Exception) {
                        android.util.Log.e("AddDeviceVM", "Initial refresh failed: ${e.message}", e)
                        false
                    }
                } ?: false

                android.util.Log.d("AddDeviceVM", "Device saved, initial refresh ${if (refreshed) "succeeded" else "timed out/failed"}, navigating to detail")

                // Navigate to detail screen AFTER refresh attempt
                // Device will stay ONLINE if refresh succeeded, or will be retried by polling
                onSuccess(deviceId)

            } catch (e: Exception) {
                android.util.Log.e("AddDeviceVM", "Failed to save device: ${e.message}", e)
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    fun testConnection(ip: String, port: String, token: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val p = port.toIntOrNull() ?: 5000
            repository.testConnection(ip, p, token)
                .onSuccess { msg -> onResult(true, msg) }
                .onFailure { err -> onResult(false, err.message ?: "Connection Failed") }
        }
    }
}
