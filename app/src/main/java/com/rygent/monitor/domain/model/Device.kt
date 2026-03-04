package com.rygent.monitor.domain.model

data class Device(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val status: DeviceStatus,
    val cpuUsage: Int, // Percentage 0-100
    val ramUsage: Int, // Percentage 0-100
    val diskUsage: Int? = null, // Percentage 0-100
    val latency: Long = 0, // ms
    val uptime: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val processes: List<Process>? = null,
    
    // New Detailed Info
    val hardware: HardwareDetails? = null,
    val system: SystemDetails? = null,
    val battery: BatteryDetails? = null,
    val network: NetworkDetails? = null,
    val apps: List<ApplicationData>? = null,
    val gpus: List<GpuDetails>? = null,
    val cpuUsagePerCore: List<Int>? = null,
    val cpuFreqPerCore: List<Double>? = null,
    val cpuFrequency: Double? = null,
    val cpuTemp: Double? = null,
    
    // Phase 3: Storage Intelligence
    val smartDrives: List<SmartDrive>? = null,
    val diskIo: List<DiskIo>? = null,
    val largeFiles: List<LargeFile>? = null
)

data class ApplicationData(
    val name: String,
    val packageName: String
)

enum class DeviceStatus {
    ONLINE, OFFLINE
}

data class Process(
    val pid: Int,
    val name: String,
    val cpuPercent: Double,
    val memoryBytes: Long
)

// Domain Models for Details
data class HardwareDetails(
    val manufacturer: String? = null,
    val model: String? = null,
    val cpuModel: String? = null,
    val cores: Int? = null,
    val threads: Int? = null,
    val architecture: String? = null,
    val cpuTemp: Double? = null,
    val boardName: String? = null,
    val boardVendor: String? = null,
    val boardTemp: Double? = null,
    val biosVersion: String? = null,
    val biosDate: String? = null,
    val storageTemp: Double? = null,
    val coreTemps: List<Double>? = null,
    val fans: List<FanDetails>? = null,
    val diskPartitions: List<DiskPartition>? = null,
    val ramTotal: Double? = null,
    val ramUsed: Double? = null,
    val ramFree: Double? = null,
    val swapTotal: Double? = null,
    val swapUsed: Double? = null
)

data class DiskPartition(
    val device: String,
    val mountpoint: String,
    val fstype: String,
    val total: Long,
    val used: Long,
    val free: Long,
    val percent: Double
)

data class FanDetails(
    val name: String,
    val rpm: Int
)

data class SystemDetails(
    val os: String? = null,
    val kernel: String? = null,
    val rootAccess: Boolean = false
)

data class BatteryDetails(
    val percent: Double,
    val powerPlugged: Boolean,
    val secsLeft: Long,
    val temp: Double? = null
)

data class NetworkDetails(
    val bytesSent: Long,
    val bytesRecv: Long,
    val interfaces: List<NetworkInterface>? = null
)

data class NetworkInterface(
    val name: String,
    val ip: String,
    val mac: String,
    val speed: Double,
    val up: Boolean
)

data class GpuDetails(
    val name: String,
    val usage: Int,
    val memory: Int,
    val temp: Int
)

// ── Phase 3: Storage Intelligence Domain Models ───────────

data class SmartDrive(
    val device: String,
    val model: String,
    val serial: String = "",
    val firmware: String = "",
    val type: String = "SSD",       // SSD or HDD
    val health: String = "Unknown", // PASSED, FAILED, Unknown
    val temp: Int = 0,
    val capacityBytes: Long = 0,
    val powerOnHours: Long = 0,
    val powerCycleCount: Long = 0,
    val ssdLifeLeft: Int = -1,      // -1 = not available
    val reallocatedSectors: Int = 0,
    val totalWrittenBytes: Long = 0
)

data class DiskIo(
    val name: String,
    val readIOPS: Double = 0.0,
    val writeIOPS: Double = 0.0,
    val readBytesPerSec: Double = 0.0,
    val writeBytesPerSec: Double = 0.0
)

data class LargeFile(
    val path: String,
    val sizeBytes: Long,
    val modified: Long
)

// ── Phase 4: Advanced Analytics Domain Models ──────────────

data class SpeedTestResult(
    val ping: Double,
    val downloadBits: Double,
    val uploadBits: Double,
    val server: String,
    val sponsor: String,
    val error: String? = null
)

data class ProcessHistoryEntry(
    val timestamp: Long,
    val processes: List<Process>
)

data class RunawayAlert(
    val pid: Int,
    val name: String,
    val cpuPercent: Double,
    val durationMin: Int,
    val message: String
)
