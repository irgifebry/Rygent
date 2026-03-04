package com.rygent.monitor.data.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rygent.monitor.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val udpDiscoveryManager: UdpDiscoveryManager
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    private val _discoveredDevices = MutableStateFlow<Set<DiscoveredDevice>>(emptySet())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    // Unified log
    private val _discoveryLog = MutableStateFlow("Discovery status: Idle")
    val discoveryLog = _discoveryLog.asStateFlow()

    private val SERVICE_TYPE = Constants.SERVICE_TYPE_MDNS

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            _discoveryLog.value = "Start Failed: error $errorCode"
            try { nsdManager.stopServiceDiscovery(this) } catch (e: Exception) {}
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            _discoveryLog.value = "Stop Failed: error $errorCode"
        }

        override fun onDiscoveryStarted(serviceType: String) {
            _discoveryLog.value = "Discovery Started: $serviceType"
        }

        override fun onDiscoveryStopped(serviceType: String) {
            _discoveryLog.value = "Discovery Stopped"
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            _discoveryLog.value = "Service candidate: ${serviceInfo.serviceName} (${serviceInfo.serviceType})"
            if (serviceInfo.serviceType.contains("rygent")) {
                _discoveryLog.value = "Resolving: ${serviceInfo.serviceName}..."
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        _discoveryLog.value = "Resolve Failed: ${info.serviceName} (error $errorCode)"
                    }

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val ip = info.host.hostAddress
                        _discoveryLog.value = "Resolved: ${info.serviceName} -> $ip"
                        if (ip != null) {
                            val hostname = info.serviceName.substringBefore(".")
                            val tunnelUrl = info.attributes["tunnel_url"]?.let { String(it) }.takeIf { !it.isNullOrBlank() }
                            val newDevice = DiscoveredDevice(ip, hostname, tunnelUrl)
                            // Atomic update to avoid race condition
                            _discoveredDevices.update { currentSet ->
                                val mutableSet = currentSet.toMutableSet()
                                mutableSet.removeAll { it.address == ip }
                                mutableSet.add(newDevice)
                                mutableSet
                            }
                        }
                    }
                })
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            _discoveryLog.value = "Service lost: ${serviceInfo.serviceName}"
        }
    }

    fun startDiscovery(scope: kotlinx.coroutines.CoroutineScope) {
        _discoveredDevices.value = emptySet()
        _discoveryLog.value = "Starting Hybrid Discovery..."
        
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock(Constants.MULTICAST_LOCK_NAME)
                multicastLock?.setReferenceCounted(false) // B8: false prevents double-release issues
            }
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
            }
            _discoveryLog.value = "Acquired Lock. Scanning..."
        } catch (e: Exception) {
            _discoveryLog.value = "Lock error: ${e.message}"
        }

        // 1. Start mDNS (NsdManager)
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            _discoveryLog.value = "mDNS Error: ${e.message}"
        }

        // 2. Start UDP Broadcast Listener
        scope.launch {
            udpDiscoveryManager.startListening()
        }

        // 3. Collect from UDP and merge
        scope.launch {
            udpDiscoveryManager.discoveredDevices.collect { udpDevices ->
                // Atomic update to avoid race condition
                _discoveredDevices.update { currentSet ->
                    val mutableSet = currentSet.toMutableSet()
                    udpDevices.forEach { udpDev ->
                        // Only add if IP doesn't exist
                        if (mutableSet.none { it.address == udpDev.address }) {
                            mutableSet.add(udpDev)
                        }
                    }
                    mutableSet
                }
                if (udpDevices.isNotEmpty()) {
                    _discoveryLog.value = "Found ${udpDevices.size} via UDP"
                }
            }
        }
    }

    fun stopDiscovery() {
        _discoveryLog.value = "Stopping..."
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            // Ignore
        }
        
        udpDiscoveryManager.stopListening()
        
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
