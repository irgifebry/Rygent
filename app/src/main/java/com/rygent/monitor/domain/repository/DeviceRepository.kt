package com.rygent.monitor.domain.repository

import com.rygent.monitor.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getAllDevices(): Flow<List<Device>>
    fun getDeviceById(id: String): Flow<Device?>
    suspend fun getDeviceByName(name: String): Device?
    suspend fun insertDevice(device: Device)
    suspend fun deleteDevice(device: Device)
    suspend fun updateDeviceConfig(id: String, name: String, ip: String, port: Int, token: String?)
    suspend fun refreshDevice(device: Device)
    
    // Power Actions now return Result
    suspend fun shutdownDevice(device: Device): Result<Unit>
    suspend fun rebootDevice(device: Device): Result<Unit>
    suspend fun suspendDevice(device: Device): Result<Unit>
    suspend fun killProcess(device: Device, pid: Int): Result<Unit>
    
    // New connectivity test
    suspend fun testConnection(ip: String, port: Int, token: String): Result<String>
    
    // Phase 3: Storage Intelligence
    suspend fun getSmartDrives(device: Device): Result<List<com.rygent.monitor.domain.model.SmartDrive>>
    suspend fun getLargeFiles(device: Device, path: String = "/", limit: Int = 20, minSizeMb: Int = 50): Result<List<com.rygent.monitor.domain.model.LargeFile>>
    suspend fun getDiskIo(device: Device): Result<List<com.rygent.monitor.domain.model.DiskIo>>

    // Phase 4: Advanced Analytics
    suspend fun runSpeedTest(device: Device): Result<com.rygent.monitor.domain.model.SpeedTestResult>
    suspend fun getProcessHistory(device: Device): Result<List<com.rygent.monitor.domain.model.ProcessHistoryEntry>>
    suspend fun getAlerts(device: Device): Result<List<com.rygent.monitor.domain.model.RunawayAlert>>
    
    // Auto-reconnect sync
    suspend fun syncDeviceWithDiscovery(name: String, ip: String, tunnelUrl: String?)
}
