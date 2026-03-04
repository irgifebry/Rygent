package com.rygent.monitor.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.DeviceStatus
import com.rygent.monitor.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val discoveryManager: com.rygent.monitor.data.remote.NetworkDiscoveryManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // Track refresh jobs per device to avoid overlaps
    private val refreshJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    
    val devices: StateFlow<List<Device>> = repository.getAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        startPolling()
        startSilentDiscovery()
    }

    private fun startSilentDiscovery() {
        discoveryManager.startDiscovery(viewModelScope)
        viewModelScope.launch {
            discoveryManager.discoveredDevices.collect { discoveredList ->
                val currentSavedDevices = devices.value
                discoveredList.forEach { discovered ->
                    val match = currentSavedDevices.find { it.name == discovered.name }
                    if (match != null) {
                        // Check if we need to update IP or Tunnel
                        val newIp = discovered.address
                        val newTunnel = discovered.tunnelUrl?.replace("https://", "")?.replace("http://", "")
                        
                        // F9: Improved Auto-Reconnect logic
                        // If we have a tunnel URL, prioritize it. If not, use local IP.
                        val targetAddress = newTunnel ?: newIp
                        val isDifferent = match.ipAddress != targetAddress
                        
                        if (isDifferent) {
                            android.util.Log.d("MainVM", "Auto-reconnecting ${match.name}: ${match.ipAddress} -> $targetAddress")
                            viewModelScope.launch {
                                repository.updateDeviceConfig(
                                    match.id,
                                    match.name,
                                    targetAddress,
                                    match.port,
                                    null // Token preserved in TokenManager
                                )
                                // Force an immediate refresh to mark it ONLINE
                                repository.refreshDevice(match.copy(ipAddress = targetAddress))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val currentDevices = devices.value
                    if (currentDevices.isNotEmpty()) {
                        currentDevices.forEach { device ->
                            // Cancel any existing job for this device before starting a new one
                            refreshJobs[device.id]?.cancel()
                            
                            refreshJobs[device.id] = launch {
                                try {
                                    repository.refreshDevice(device)
                                } catch (e: Exception) {
                                    // Error handled in repository
                                } finally {
                                    // Clean up map when done (optional, but good practice)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainVM", "Polling error: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    // B12: Made parallel instead of sequential
    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val jobs = devices.value.map { device ->
                    launch {
                        try {
                            repository.refreshDevice(device)
                        } catch (_: Exception) {}
                    }
                }
                jobs.forEach { it.join() }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            repository.deleteDevice(device)
        }
    }

    fun updateDeviceConfig(id: String, name: String, ip: String, port: Int, token: String?) {
        viewModelScope.launch {
            repository.updateDeviceConfig(id, name, ip, port, token)
        }
    }

    // B11: Removed — addDevice with hardcoded "New Device" name is dead code.
    // Device creation is done via AddDeviceScreen/AddDeviceViewModel with proper user input.
}
