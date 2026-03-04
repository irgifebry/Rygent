package com.rygent.monitor.data.remote

data class DiscoveredDevice(
    val address: String,
    val name: String? = null,
    val tunnelUrl: String? = null
)
