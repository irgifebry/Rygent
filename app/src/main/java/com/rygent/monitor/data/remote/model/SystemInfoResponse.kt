package com.rygent.monitor.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class SystemInfoResponse(
    @Json(name = "hostname") val hostname: String?,
    @Json(name = "os") val os: String?,
    @Json(name = "uptime") val uptime: Long? = 0,
    @Json(name = "cpu") val cpu: CpuInfo?,
    @Json(name = "memory") val memory: MemoryInfo?,
    @Json(name = "disk") val disk: List<DiskInfo>? = null,
    @Json(name = "network") val network: NetworkInfo?,
    @Json(name = "processes") val processes: List<ProcessInfo>? = null,
    
    @Json(name = "hardware") val hardware: HardwareInfo? = null,
    @Json(name = "system") val system: SystemDetailsInfo? = null,
    @Json(name = "battery") val battery: BatteryInfo? = null,
    @Json(name = "apps") val apps: List<AppInfo>? = null,
    @Json(name = "sensors") val sensors: SensorsInfo? = null,
    @Json(name = "tunnel_url") val tunnel_url: String? = null,
    @Json(name = "diskIo") val diskIo: List<DiskIoInfo>? = null
)

data class SensorsInfo(
    @Json(name = "cpuTemp") val cpuTemp: Double? = null,
    @Json(name = "coreTemps") val coreTemps: List<Double>? = null,
    @Json(name = "boardTemp") val boardTemp: Double? = null,
    @Json(name = "batteryTemp") val batteryTemp: Double? = null,
    @Json(name = "storageTemp") val storageTemp: Double? = null,
    @Json(name = "gpu") val gpu: GpuSensorInfo? = null,
    @Json(name = "gpus") val gpus: List<GpuSensorInfo>? = null,
    @Json(name = "fans") val fans: List<FanInfo>? = null
)

data class FanInfo(
    @Json(name = "name") val name: String?,
    @Json(name = "rpm") val rpm: Double?
)

data class GpuSensorInfo(
    @Json(name = "model") val model: String? = null,
    @Json(name = "usage") val usage: Double? = null,
    @Json(name = "temp") val temp: Double? = null,
    @Json(name = "memory") val memory: Double? = null
)

data class AppInfo(
    @Json(name = "name") val name: String?,
    @Json(name = "package") val packageName: String?
)

data class HardwareInfo(
    @Json(name = "manufacturer") val manufacturer: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "cpuModel") val cpuModel: String? = null,
    @Json(name = "cores") val cores: Int? = null,
    @Json(name = "threads") val threads: Int? = null,
    @Json(name = "architecture") val architecture: String? = null,
    @Json(name = "boardName") val boardName: String? = null,
    @Json(name = "boardVendor") val boardVendor: String? = null,
    @Json(name = "biosVersion") val biosVersion: String? = null,
    @Json(name = "biosDate") val biosDate: String? = null,
    @Json(name = "cpuTemp") val cpuTemp: Double? = null,
    @Json(name = "boardTemp") val boardTemp: Double? = null,
    @Json(name = "storageTemp") val storageTemp: Double? = null,
    @Json(name = "coreTemps") val coreTemps: List<Double>? = null,
    @Json(name = "fans") val fans: List<FanInfo>? = null,
    val ramTotal: Double? = null,
    val ramUsed: Double? = null,
    val ramFree: Double? = null
)

data class SystemDetailsInfo(
    @Json(name = "os") val os: String? = null,
    @Json(name = "kernel") val kernel: String? = null,
    @Json(name = "rootAccess") val rootAccess: Boolean = false
)

data class BatteryInfo(
    @Json(name = "percent") val percent: Double?,
    @Json(name = "powerPlugged") val powerPlugged: Boolean?,
    @Json(name = "secsLeft") val secsLeft: Long?,
    @Json(name = "temp") val temp: Double? = null
)

data class CpuInfo(
    @Json(name = "usage") val usagePercent: Double?,
    @Json(name = "usagePerCore") val perCore: List<Double>? = null,
    @Json(name = "frequency") val frequencyMhz: Double? = null,
    @Json(name = "freqPerCore") val freqPerCore: List<Double>? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

data class MemoryInfo(
    @Json(name = "total") val totalBytes: Long?,
    @Json(name = "used") val usedBytes: Long?,
    @Json(name = "available") val availableBytes: Long?,
    @Json(name = "percent") val usagePercent: Double?
)

data class DiskInfo(
    @Json(name = "device") val name: String?,
    @Json(name = "mountpoint") val mountPoint: String? = null,
    @Json(name = "total") val totalBytes: Long?,
    @Json(name = "used") val usedBytes: Long?,
    @Json(name = "free") val freeBytes: Long?,
    @Json(name = "percent") val usagePercent: Double?
)

data class NetworkInfo(
    @Json(name = "bytesSent") val bytesSent: Long?,
    @Json(name = "bytesRecv") val bytesRecv: Long?,
    @Json(name = "interfaces") val interfaces: List<NetworkInterfaceInfo>? = null
)

data class NetworkInterfaceInfo(
    @Json(name = "name") val name: String?,
    @Json(name = "ip") val ip: String?,
    @Json(name = "mac") val mac: String?,
    @Json(name = "speed") val speed: Double?,
    @Json(name = "up") val up: Boolean?
)

data class ProcessInfo(
    @Json(name = "pid") val pid: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "cpuPercent") val cpuPercent: Double?,
    @Json(name = "memoryBytes") val memoryBytes: Long?
)

// ── Phase 3: Storage Intelligence DTOs ────────────────────

data class SmartDriveInfo(
    @Json(name = "device") val device: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "serial") val serial: String? = null,
    @Json(name = "firmware") val firmware: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "health") val health: String? = null,
    @Json(name = "temp") val temp: Int? = null,
    @Json(name = "capacityBytes") val capacityBytes: Long? = null,
    @Json(name = "powerOnHours") val powerOnHours: Long? = null,
    @Json(name = "powerCycleCount") val powerCycleCount: Long? = null,
    @Json(name = "ssdLifeLeft") val ssdLifeLeft: Int? = null,
    @Json(name = "reallocatedSectors") val reallocatedSectors: Int? = null,
    @Json(name = "totalWrittenBytes") val totalWrittenBytes: Long? = null
)

data class DiskIoInfo(
    @Json(name = "name") val name: String? = null,
    @Json(name = "readBytes") val readBytes: Long? = null,
    @Json(name = "writeBytes") val writeBytes: Long? = null,
    @Json(name = "readCount") val readCount: Long? = null,
    @Json(name = "writeCount") val writeCount: Long? = null,
    @Json(name = "readIOPS") val readIOPS: Double? = null,
    @Json(name = "writeIOPS") val writeIOPS: Double? = null,
    @Json(name = "readBytesPerSec") val readBytesPerSec: Double? = null,
    @Json(name = "writeBytesPerSec") val writeBytesPerSec: Double? = null
)

data class LargeFileInfo(
    @Json(name = "path") val path: String? = null,
    @Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @Json(name = "modified") val modified: Long? = null
)

// ── Phase 4: Advanced Analytics DTOs ──────────────────────

data class SpeedTestResultInfo(
    @Json(name = "ping") val ping: Double? = null,
    @Json(name = "downloadBits") val downloadBits: Double? = null,
    @Json(name = "uploadBits") val uploadBits: Double? = null,
    @Json(name = "server") val server: String? = null,
    @Json(name = "sponsor") val sponsor: String? = null,
    @Json(name = "error") val error: String? = null
)

data class ProcessHistoryEntryInfo(
    @Json(name = "timestamp") val timestamp: Long?,
    @Json(name = "processes") val processes: List<ProcessInfo>?
)

data class RunawayAlertInfo(
    @Json(name = "pid") val pid: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "cpuPercent") val cpuPercent: Double?,
    @Json(name = "durationMin") val durationMin: Int?,
    @Json(name = "message") val message: String?
)
