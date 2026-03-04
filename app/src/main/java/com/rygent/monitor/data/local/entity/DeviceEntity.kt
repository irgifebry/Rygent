package com.rygent.monitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rygent.monitor.domain.model.DeviceStatus

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val token: String? = null,
    val status: DeviceStatus,
    val cpuUsage: Int,
    val ramUsage: Int,
    val diskUsage: Int? = null,
    val latency: Long,
    val uptime: String,
    val lastSeen: Long,
    val processesJson: String? = null, // Storing complex object as JSON string to simplify migration
    
    // Detailed Info as JSON Strings
    val hardwareJson: String? = null,
    val systemJson: String? = null,
    val batteryJson: String? = null,
    val networkJson: String? = null,
    val diskJson: String? = null,
    val appsJson: String? = null,
    val gpuJson: String? = null,
    val cpuUsagePerCoreJson: String? = null,
    val cpuFreqPerCoreJson: String? = null,
    val cpuFrequency: Double? = null,
    val cpuTemp: Double? = null,
    val diskIoJson: String? = null
)
