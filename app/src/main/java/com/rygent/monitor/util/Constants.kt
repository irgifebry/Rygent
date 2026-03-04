package com.rygent.monitor.util

/**
 * Application-wide constants
 */
object Constants {

    // Power Actions
    const val ACTION_SHUTDOWN = "shutdown"
    const val ACTION_REBOOT = "reboot"
    const val ACTION_SUSPEND = "suspend"

    // Network
    const val DEFAULT_PORT = 5000  // Changed from 8080 to match Rygent default
    const val UDP_DISCOVERY_PORT = 30001
    const val SERVICE_TYPE_MDNS = "_rygent._tcp."
    const val SERVICE_NAME = "rygent"

    // Network Timeouts (milliseconds)
    const val NETWORK_CONNECT_TIMEOUT = 5000L
    const val NETWORK_READ_TIMEOUT = 120000L // 120s for long queries (Speedtest/FileScan)
    const val NETWORK_WRITE_TIMEOUT = 5000L

    // API Endpoints
    const val ENDPOINT_SYSTEM = "/api/system"
    const val ENDPOINT_HEALTH = "/health"
    const val ENDPOINT_POWER = "/api/power"
    const val ENDPOINT_PROCESS_KILL = "/api/process/kill"

    // Database
    const val DATABASE_NAME = "system_monitor_db"

    // WorkManager
    const val POLLING_WORK_NAME = "device_polling"
    const val POLLING_INTERVAL_MINUTES = 15L

    // Multicast Lock
    const val MULTICAST_LOCK_NAME = "RygentDiscovery"

    // Logging Tags
    const val TAG_DEVICE_REPO = "DeviceRepo"
    const val TAG_POLLING_WORKER = "PollingWorker"
    const val TAG_DISCOVERY = "Discovery"

    // Default Authentication Token (for development/testing)
    // In production, users should set their own token
    const val DEFAULT_AUTH_TOKEN = "debug_token_123"
    
    // App Info
    const val APP_NAME = "Rygent Agent"
    const val APP_VERSION = "1.0.0"
}
