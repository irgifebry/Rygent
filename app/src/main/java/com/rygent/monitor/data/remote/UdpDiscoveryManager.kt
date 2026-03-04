package com.rygent.monitor.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rygent.monitor.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _discoveredDevices = MutableStateFlow<Set<DiscoveredDevice>>(emptySet())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private var socket: DatagramSocket? = null
    private var isRunning = false

    suspend fun startListening() {
        if (isRunning) return
        isRunning = true
        _discoveredDevices.value = emptySet()

        withContext(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(Constants.UDP_DISCOVERY_PORT))
                }
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isRunning) {
                    try {
                        socket?.receive(packet)
                        val message = String(packet.data, 0, packet.length)
                        
                        val json = JSONObject(message)
                        if (json.optString("service") == Constants.SERVICE_NAME) {
                            val ip = json.optString("ip")
                            val name = json.optString("name")
                            val tunnelUrl = json.optString("tunnel_url").takeIf { it.isNotEmpty() }
                            if (ip.isNotEmpty()) {
                                _discoveredDevices.value = _discoveredDevices.value + DiscoveredDevice(ip, name, tunnelUrl)
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning) e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopListening()
            }
        }
    }

    fun stopListening() {
        isRunning = false
        socket?.close()
        socket = null
    }
}
