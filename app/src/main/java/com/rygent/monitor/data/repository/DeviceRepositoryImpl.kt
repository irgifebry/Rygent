package com.rygent.monitor.data.repository

import com.rygent.monitor.data.local.TokenManager
import com.rygent.monitor.data.remote.SystemMonitorApiService
import com.rygent.monitor.data.local.dao.DeviceDao
import com.rygent.monitor.data.local.entity.DeviceEntity
import com.rygent.monitor.domain.repository.DeviceRepository
import com.rygent.monitor.domain.model.*
import com.rygent.monitor.data.remote.model.*
import com.rygent.monitor.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class DeviceRepositoryImpl @Inject constructor(
    private val dao: DeviceDao,
    private val apiService: SystemMonitorApiService,
    @Named("StatusApi") private val statusApiService: SystemMonitorApiService,
    private val tokenManager: TokenManager,
    private val moshi: Moshi
) : DeviceRepository {
    
    // Adapters for JSON Serialization
    private val processesAdapter: JsonAdapter<List<ProcessInfo>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, ProcessInfo::class.java)
    )
    private val hardwareAdapter: JsonAdapter<HardwareInfo> = moshi.adapter(HardwareInfo::class.java)
    private val systemAdapter: JsonAdapter<SystemDetailsInfo> = moshi.adapter(SystemDetailsInfo::class.java)
    private val itemsAdapter: JsonAdapter<List<AppInfo>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, AppInfo::class.java)
    )
    private val doubleListAdapter: JsonAdapter<List<Double>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, java.lang.Double::class.java)
    )
    private val batteryAdapter: JsonAdapter<BatteryInfo> = moshi.adapter(BatteryInfo::class.java)
    private val networkAdapter: JsonAdapter<NetworkInfo> = moshi.adapter(NetworkInfo::class.java)
    private val diskListAdapter: JsonAdapter<List<DiskInfo>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, DiskInfo::class.java)
    )
    private val gpuAdapter: JsonAdapter<GpuSensorInfo> = moshi.adapter(GpuSensorInfo::class.java)
    private val gpuListAdapter: JsonAdapter<List<GpuSensorInfo>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, GpuSensorInfo::class.java)
    )
    private val diskIoListAdapter: JsonAdapter<List<DiskIoInfo>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, DiskIoInfo::class.java)
    )

    override fun getAllDevices(): Flow<List<Device>> {
        return dao.getAllDevices().map { entities ->
            entities.map { it.toDevice() }
        }
    }

    override fun getDeviceById(id: String): Flow<Device?> {
        return dao.getDeviceById(id).map { it?.toDevice() }
    }

    override suspend fun getDeviceByName(name: String): Device? {
        return dao.getDeviceByName(name)?.toDevice()
    }

    override suspend fun insertDevice(device: Device) {
        dao.insertDevice(device.toEntity())
    }

    override suspend fun deleteDevice(device: Device) {
        dao.deleteDevice(device.toEntity())
        tokenManager.clearToken(device.id)
    }

    override suspend fun updateDeviceConfig(id: String, name: String, ip: String, port: Int, token: String?) {
        dao.updateDeviceConfig(id, name, ip, port, token)
        if (!token.isNullOrBlank()) {
            tokenManager.saveToken(id, token)
        }
    }

    override suspend fun refreshDevice(device: Device) {
        val start = System.currentTimeMillis()
        try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) {
                android.util.Log.w("DeviceRepo", "No token for device ${device.id}, marking offline")
                dao.insertDevice(device.copy(status = DeviceStatus.OFFLINE).toEntity())
                return
            }
            val token = rawToken.trim()

            // FIX: Support Global Tunnel URL and explicit port 443 handling
            val cleanIp = device.ipAddress.replace("https://", "").replace("http://", "")
            val isTunnel = cleanIp.contains("trycloudflare.com") || cleanIp.contains("cloudflare")
            val isHttps = device.port == 443 || isTunnel
            
            val url = if (isTunnel || device.port == 443) {
                // Use HTTPS for Cloudflare tunnel or explicit port 443
                "https://$cleanIp${Constants.ENDPOINT_SYSTEM}"
            } else {
                // Use HTTP for local network
                "http://$cleanIp:${device.port}${Constants.ENDPOINT_SYSTEM}"
            }
            android.util.Log.d("DeviceRepo", "Refreshing device via URL: $url (port=${device.port}, isTunnel=$isTunnel)")
            
            val response = statusApiService.getSystemInfo(url, "Bearer $token")
            val end = System.currentTimeMillis()
            val latencyMs = end - start
            
            if (response.isSuccessful && response.body() != null && response.body()?.hostname != null) {
                val systemInfo = response.body()!!
                val sensors = systemInfo.sensors
                android.util.Log.d("DeviceRepo", "SUCCESS: Received info for ${systemInfo.hostname}. CPU: ${systemInfo.cpu?.usagePercent}%, RAM: ${systemInfo.memory?.usagePercent}%")
                
                // Construct Domain Objects from DTOs
                
                val hardwareDetails = systemInfo.hardware?.let { h ->
                    HardwareDetails(
                        manufacturer = h.manufacturer,
                        model = h.model,
                        cpuModel = h.cpuModel,
                        cores = h.cores,
                        threads = h.threads,
                        architecture = h.architecture,
                        cpuTemp = sensors?.cpuTemp ?: h.cpuTemp,
                        boardName = h.boardName,
                        boardVendor = h.boardVendor,
                        boardTemp = sensors?.boardTemp ?: h.boardTemp,
                        biosVersion = h.biosVersion,
                        biosDate = h.biosDate,
                        storageTemp = sensors?.storageTemp ?: h.storageTemp,
                        coreTemps = sensors?.coreTemps ?: h.coreTemps,
                        fans = sensors?.fans?.map { FanDetails(it.name ?: "", (it.rpm ?: 0.0).toInt()) }
                            ?: h.fans?.map { FanDetails(it.name ?: "", (it.rpm ?: 0.0).toInt()) },
                        diskPartitions = systemInfo.disk?.map { d ->
                            DiskPartition(
                                device = d.name ?: "",
                                mountpoint = d.mountPoint ?: "",
                                fstype = "", // Not explicitly provided in DiskInfo
                                total = d.totalBytes ?: 0,
                                used = d.usedBytes ?: 0,
                                free = d.freeBytes ?: 0,
                                percent = d.usagePercent ?: 0.0
                            )
                        },
                        ramTotal = systemInfo.memory?.totalBytes?.toDouble(),
                        ramUsed = systemInfo.memory?.usedBytes?.toDouble(),
                        ramFree = systemInfo.memory?.availableBytes?.toDouble()
                    )
                }

                val systemDetails = systemInfo.system?.let { s ->
                    SystemDetails(
                        os = s.os,
                        kernel = s.kernel,
                        rootAccess = s.rootAccess
                    )
                }
                
                val updatedDevice = device.copy(
                    status = DeviceStatus.ONLINE,
                    cpuUsage = systemInfo.cpu?.usagePercent?.toInt() ?: 0,
                    ramUsage = systemInfo.memory?.usagePercent?.toInt() ?: 0,
                    diskUsage = systemInfo.disk?.firstOrNull()?.usagePercent?.toInt() ?: 0,
                    uptime = formatUptime(systemInfo.uptime ?: 0),
                    lastSeen = System.currentTimeMillis(),
                    latency = latencyMs,
                    
                    processes = systemInfo.processes?.map { 
                        Process(it.pid ?: 0, it.name ?: "", it.cpuPercent ?: 0.0, it.memoryBytes ?: 0)
                    } ?: emptyList(),
                    
                    hardware = hardwareDetails,
                    system = systemDetails,
                    battery = systemInfo.battery?.let { b ->
                        BatteryDetails(
                            percent = b.percent ?: 0.0,
                            powerPlugged = b.powerPlugged ?: false,
                            secsLeft = b.secsLeft ?: 0,
                            temp = b.temp
                        )
                    },
                    network = systemInfo.network?.let { n ->
                        NetworkDetails(
                            bytesSent = n.bytesSent ?: 0,
                            bytesRecv = n.bytesRecv ?: 0,
                            interfaces = n.interfaces?.map { i ->
                                NetworkInterface(
                                    name = i.name ?: "",
                                    ip = i.ip ?: "",
                                    mac = i.mac ?: "",
                                    speed = i.speed ?: 0.0,
                                    up = i.up ?: false
                                )
                            }
                        )
                    },
                    apps = systemInfo.apps?.map {
                        ApplicationData(it.name ?: "", it.packageName ?: "")
                    } ?: emptyList(),
                    
                    gpus = sensors?.gpus?.map { g ->
                         GpuDetails(
                             name = g.model ?: "Unknown",
                             usage = (g.usage ?: 0.0).toInt(),
                             memory = (g.memory ?: 0.0).toInt(),
                             temp = (g.temp ?: 0.0).toInt()
                         )
                    } ?: sensors?.gpu?.let { g ->
                        listOf(GpuDetails(
                            name = g.model ?: "Unknown",
                            usage = (g.usage ?: 0.0).toInt(),
                            memory = (g.memory ?: 0.0).toInt(),
                            temp = (g.temp ?: 0.0).toInt()
                        ))
                    },
                    
                    cpuUsagePerCore = systemInfo.cpu?.perCore?.map { it.toInt() } ?: emptyList(),
                    cpuFreqPerCore = systemInfo.cpu?.freqPerCore,
                    cpuFrequency = systemInfo.cpu?.frequencyMhz,
                    cpuTemp = sensors?.cpuTemp ?: systemInfo.cpu?.temperature,
                    
                    diskIo = systemInfo.diskIo?.map { io ->
                        DiskIo(
                            name = io.name ?: "",
                            readIOPS = io.readIOPS ?: 0.0,
                            writeIOPS = io.writeIOPS ?: 0.0,
                            readBytesPerSec = io.readBytesPerSec ?: 0.0,
                            writeBytesPerSec = io.writeBytesPerSec ?: 0.0
                        )
                    }
                )

                // B2: Support tunnel URL update but keep it persistent
                val finalDevice = systemInfo.tunnel_url?.let {
                    if (it.isNotBlank() && !it.contains(device.ipAddress)) {
                        updatedDevice.copy(ipAddress = it.replace("https://", ""))
                    } else updatedDevice
                } ?: updatedDevice
                
                // CRITICAL: Fetch LATEST entity from DB to avoid overwriting config changes (Name, IP, Port, Token)
                val currentEntity = dao.getDeviceByIdStatic(device.id)
                val baseEntity = currentEntity ?: finalDevice.toEntity()

                val entity = baseEntity.copy(
                    status = DeviceStatus.ONLINE,
                    cpuUsage = finalDevice.cpuUsage,
                    ramUsage = finalDevice.ramUsage,
                    diskUsage = finalDevice.diskUsage,
                    uptime = finalDevice.uptime,
                    lastSeen = finalDevice.lastSeen,
                    latency = finalDevice.latency,
                    cpuFrequency = finalDevice.cpuFrequency,
                    cpuTemp = finalDevice.cpuTemp,
                    
                    processesJson = processesAdapter.toJson(systemInfo.processes ?: emptyList()),
                    cpuUsagePerCoreJson = doubleListAdapter.toJson(systemInfo.cpu?.perCore ?: emptyList()),
                    cpuFreqPerCoreJson = doubleListAdapter.toJson(systemInfo.cpu?.freqPerCore ?: emptyList()),
                    appsJson = itemsAdapter.toJson(systemInfo.apps ?: emptyList()),
                    hardwareJson = hardwareAdapter.toJson(systemInfo.hardware?.copy(
                        ramTotal = systemInfo.memory?.totalBytes?.toDouble(),
                        ramUsed = systemInfo.memory?.usedBytes?.toDouble(),
                        ramFree = systemInfo.memory?.availableBytes?.toDouble()
                    )),
                    systemJson = systemAdapter.toJson(systemInfo.system),
                    batteryJson = batteryAdapter.toJson(systemInfo.battery),
                    networkJson = networkAdapter.toJson(systemInfo.network),
                    diskJson = diskListAdapter.toJson(systemInfo.disk ?: emptyList()),
                    gpuJson = systemInfo.sensors?.gpus?.let { gpuListAdapter.toJson(it) } 
                        ?: systemInfo.sensors?.gpu?.let { gpuListAdapter.toJson(listOf(it)) },
                    diskIoJson = diskIoListAdapter.toJson(systemInfo.diskIo ?: emptyList())
                )
                
                dao.insertDevice(entity)
            } else {
                 val currentEntity = dao.getDeviceByIdStatic(device.id)
                 currentEntity?.let {
                     dao.insertDevice(it.copy(status = DeviceStatus.OFFLINE))
                 }
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "REFRESH ERROR for ${device.name}: ${e.message}", e)
            val currentEntity = dao.getDeviceByIdStatic(device.id)
            currentEntity?.let {
                dao.insertDevice(it.copy(status = DeviceStatus.OFFLINE))
            }
        }
    }

    override suspend fun shutdownDevice(device: Device): Result<Unit> {
        return executePowerAction(device, Constants.ACTION_SHUTDOWN)
    }

    override suspend fun rebootDevice(device: Device): Result<Unit> {
        return executePowerAction(device, Constants.ACTION_REBOOT)
    }

    override suspend fun suspendDevice(device: Device): Result<Unit> {
        return executePowerAction(device, Constants.ACTION_SUSPEND)
    }

    override suspend fun killProcess(device: Device, pid: Int): Result<Unit> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val token = rawToken.trim()
            val url = buildUrl(device, "${Constants.ENDPOINT_PROCESS_KILL}/$pid")
            val response = apiService.killProcess(url, "Bearer $token")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "Kill process $pid failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun testConnection(ip: String, port: Int, token: String): Result<String> {
        return try {
            val cleanIp = ip.replace("https://", "").replace("http://", "")
            val isTunnel = cleanIp.contains("trycloudflare.com") || cleanIp.contains("cloudflare")
            
            val url = if (isTunnel || port == 443) {
                "https://$cleanIp${Constants.ENDPOINT_HEALTH}"
            } else {
                "http://$cleanIp:$port${Constants.ENDPOINT_HEALTH}"
            }
            android.util.Log.d("DeviceRepo", "Testing connection to: $url (port=$port, isTunnel=$isTunnel)")
            val response = apiService.healthCheck(url, "Bearer ${token.trim()}")

            if (response.isSuccessful) {
                Result.success("Connected via Health Check")
            } else {
                Result.failure(Exception("HTTP ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "Test connection failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun executePowerAction(device: Device, action: String): Result<Unit> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token"))
            val token = rawToken.trim()

            val cleanIp = device.ipAddress.replace("https://", "").replace("http://", "")
            val isTunnel = cleanIp.contains("trycloudflare.com") || cleanIp.contains("cloudflare")

            val url = if (isTunnel || device.port == 443) {
                "https://$cleanIp${Constants.ENDPOINT_POWER}/$action"
            } else {
                "http://$cleanIp:${device.port}${Constants.ENDPOINT_POWER}/$action"
            }

            val response = when(action) {
                Constants.ACTION_SHUTDOWN -> apiService.shutdown(url, "Bearer $token")
                Constants.ACTION_REBOOT -> apiService.reboot(url, "Bearer $token")
                Constants.ACTION_SUSPEND -> apiService.suspend(url, "Bearer $token")
                else -> throw IllegalArgumentException("Unknown action")
            }

            if (response.isSuccessful) {
                dao.insertDevice(device.copy(status = DeviceStatus.OFFLINE).toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "Power action $action failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun formatUptime(seconds: Long): String {
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60
        return if (days > 0) "${days}d ${hours}h" else "${hours}h ${minutes}m"
    }

    private fun DeviceEntity.toDevice(): Device {
        val hardwareInfo = hardwareJson?.let { 
             try { hardwareAdapter.fromJson(it) } catch(e:Exception) { 
                 android.util.Log.w("DeviceRepo", "Failed to parse hardwareJson for device $id: ${e.message}")
                 null 
             } 
        }
        val systemInfo = systemJson?.let { 
             try { systemAdapter.fromJson(it) } catch(e:Exception) { 
                 android.util.Log.w("DeviceRepo", "Failed to parse systemJson for device $id: ${e.message}")
                 null 
             } 
        }
        val batteryInfo = batteryJson?.let { 
             try { batteryAdapter.fromJson(it) } catch(e:Exception) { 
                 android.util.Log.w("DeviceRepo", "Failed to parse batteryJson for device $id: ${e.message}")
                 null 
             } 
        }
        val networkInfo = networkJson?.let {
            try { networkAdapter.fromJson(it) } catch(e:Exception) {
                android.util.Log.w("DeviceRepo", "Failed to parse networkJson for device $id: ${e.message}")
                null
            }
        }
        val diskInfos = diskJson?.let {
            try { diskListAdapter.fromJson(it) } catch(e:Exception) {
                android.util.Log.w("DeviceRepo", "Failed to parse diskJson for device $id: ${e.message}")
                emptyList()
            }
        } ?: emptyList()

        // Map DTO -> Domain
        val hardwareDetails = hardwareInfo?.let { h ->
            HardwareDetails(
                manufacturer = h.manufacturer,
                model = h.model,
                cpuModel = h.cpuModel,
                cores = h.cores,
                threads = h.threads,
                architecture = h.architecture,
                cpuTemp = h.cpuTemp,
                boardName = h.boardName,
                boardVendor = h.boardVendor,
                boardTemp = h.boardTemp,
                biosVersion = h.biosVersion,
                biosDate = h.biosDate,
                storageTemp = h.storageTemp,
                coreTemps = h.coreTemps,
                fans = h.fans?.map { FanDetails(it.name ?: "", (it.rpm ?: 0.0).toInt()) },
                diskPartitions = diskInfos.map { d ->
                    DiskPartition(
                        device = d.name ?: "",
                        mountpoint = d.mountPoint ?: "",
                        fstype = "",
                        total = d.totalBytes ?: 0,
                        used = d.usedBytes ?: 0,
                        free = d.freeBytes ?: 0,
                        percent = d.usagePercent ?: 0.0
                    )
                },
                ramTotal = h.ramTotal,
                ramUsed = h.ramUsed,
                ramFree = h.ramFree
            )
        }

        val systemDetails = systemInfo?.let { s ->
            SystemDetails(
                os = s.os,
                kernel = s.kernel,
                rootAccess = s.rootAccess
            )
        }

        val batteryDetails = batteryInfo?.let { b ->
            BatteryDetails(
                percent = b.percent ?: 0.0,
                powerPlugged = b.powerPlugged ?: false,
                secsLeft = b.secsLeft ?: 0,
                temp = b.temp
            )
        }

        return Device(
            id = id,
            name = name,
            ipAddress = ipAddress,
            port = port,
            status = status,
            cpuUsage = cpuUsage,
            ramUsage = ramUsage,
            diskUsage = diskUsage ?: 0,
            uptime = uptime,
            lastSeen = lastSeen,
            latency = latency,
            battery = batteryDetails,
            network = networkInfo?.let { n ->
                NetworkDetails(
                    bytesSent = n.bytesSent ?: 0,
                    bytesRecv = n.bytesRecv ?: 0,
                    interfaces = n.interfaces?.map { i ->
                        NetworkInterface(
                            name = i.name ?: "",
                            ip = i.ip ?: "",
                            mac = i.mac ?: "",
                            speed = i.speed ?: 0.0,
                            up = i.up ?: false
                        )
                    }
                )
            },
            
            // Restore lists
            processes = processesJson?.let { 
                 try { processesAdapter.fromJson(it)?.map { p ->
                     Process(p.pid ?: 0, p.name ?: "", p.cpuPercent ?: 0.0, p.memoryBytes ?: 0)
                 } } catch(e: Exception){ 
                     android.util.Log.w("DeviceRepo", "Failed to parse processesJson for device $id: ${e.message}")
                     emptyList() 
                 } 
            } ?: emptyList(),
            
            cpuUsagePerCore = cpuUsagePerCoreJson?.let {
                try { doubleListAdapter.fromJson(it)?.map { d -> d.toInt() } } catch(e: Exception) { 
                    android.util.Log.w("DeviceRepo", "Failed to parse cpuUsagePerCoreJson for device $id: ${e.message}")
                    emptyList() 
                }
            } ?: emptyList(),
            
            hardware = hardwareDetails,
            system = systemDetails,
            apps = appsJson?.let {
                 try { itemsAdapter.fromJson(it)?.map { a ->
                     ApplicationData(a.name ?: "", a.packageName ?: "")
                 } } catch(e:Exception){ 
                     android.util.Log.w("DeviceRepo", "Failed to parse appsJson for device $id: ${e.message}")
                     emptyList() 
                 }
            } ?: emptyList(),
            
            gpus = gpuJson?.let {
                try { 
                    gpuListAdapter.fromJson(it)?.map { g ->
                        GpuDetails(
                            name = g.model ?: "Unknown",
                            usage = (g.usage ?: 0.0).toInt(),
                            memory = (g.memory ?: 0.0).toInt(),
                            temp = (g.temp ?: 0.0).toInt()
                        )
                    }
                } catch(e: Exception) {
                    null
                }
            },
            
            cpuFreqPerCore = cpuFreqPerCoreJson?.let {
                try { doubleListAdapter.fromJson(it) } catch(e: Exception) { null }
            },
            cpuFrequency = cpuFrequency,
            cpuTemp = cpuTemp,
            diskIo = diskIoJson?.let {
                try {
                    diskIoListAdapter.fromJson(it)?.map { io ->
                        DiskIo(
                            name = io.name ?: "",
                            readIOPS = io.readIOPS ?: 0.0,
                            writeIOPS = io.writeIOPS ?: 0.0,
                            readBytesPerSec = io.readBytesPerSec ?: 0.0,
                            writeBytesPerSec = io.writeBytesPerSec ?: 0.0
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
        )
    }

    private fun Device.toEntity(): DeviceEntity {
        return DeviceEntity(
            id = id,
            name = name,
            ipAddress = ipAddress,
            port = port,
            status = status,
            cpuUsage = cpuUsage,
            ramUsage = ramUsage,
            diskUsage = if (diskUsage != 0) diskUsage else 0,
            latency = latency,
            uptime = uptime,
            lastSeen = lastSeen,
            processesJson = "[]",
            hardwareJson = null,
            systemJson = null,
            batteryJson = null,
            networkJson = null,
            diskJson = null,
            appsJson = "[]",
            gpuJson = null,
            cpuUsagePerCoreJson = "[]",
            cpuFreqPerCoreJson = null,
            cpuFrequency = cpuFrequency,
            cpuTemp = cpuTemp,
            diskIoJson = "[]"
        )
    }

    // ── Phase 3: Storage Intelligence Implementations ─────────

    private fun buildUrl(device: Device, endpoint: String): String {
        val cleanIp = device.ipAddress.replace("https://", "").replace("http://", "")
        val isTunnel = cleanIp.contains("trycloudflare.com") || cleanIp.contains("cloudflare")
        return if (isTunnel || device.port == 443) {
            "https://$cleanIp$endpoint"
        } else {
            "http://$cleanIp:${device.port}$endpoint"
        }
    }

    override suspend fun getSmartDrives(device: Device): Result<List<SmartDrive>> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val url = buildUrl(device, "/api/storage/smart")
            val response = apiService.getSmartInfo(url, "Bearer ${rawToken.trim()}")
            if (response.isSuccessful) {
                val drives = response.body()?.map { d ->
                    SmartDrive(
                        device = d.device ?: "",
                        model = d.model ?: "Unknown",
                        serial = d.serial ?: "",
                        firmware = d.firmware ?: "",
                        type = d.type ?: "SSD",
                        health = d.health ?: "Unknown",
                        temp = d.temp ?: 0,
                        capacityBytes = d.capacityBytes ?: 0,
                        powerOnHours = d.powerOnHours ?: 0,
                        powerCycleCount = d.powerCycleCount ?: 0,
                        ssdLifeLeft = d.ssdLifeLeft ?: -1,
                        reallocatedSectors = d.reallocatedSectors ?: 0,
                        totalWrittenBytes = d.totalWrittenBytes ?: 0
                    )
                } ?: emptyList()
                Result.success(drives)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "getSmartDrives failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getLargeFiles(device: Device, path: String, limit: Int, minSizeMb: Int): Result<List<LargeFile>> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val url = buildUrl(device, "/api/storage/large-files?path=$path&limit=$limit&min_size_mb=$minSizeMb")
            val response = apiService.getLargeFiles(url, "Bearer ${rawToken.trim()}")
            if (response.isSuccessful) {
                val files = response.body()?.map { f ->
                    LargeFile(
                        path = f.path ?: "",
                        sizeBytes = f.sizeBytes ?: 0,
                        modified = f.modified ?: 0
                    )
                } ?: emptyList()
                Result.success(files)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "getLargeFiles failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getDiskIo(device: Device): Result<List<DiskIo>> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val url = buildUrl(device, "/api/storage/io")
            val response = apiService.getDiskIo(url, "Bearer ${rawToken.trim()}")
            if (response.isSuccessful) {
                val ios = response.body()?.map { io ->
                    DiskIo(
                        name = io.name ?: "",
                        readIOPS = io.readIOPS ?: 0.0,
                        writeIOPS = io.writeIOPS ?: 0.0,
                        readBytesPerSec = io.readBytesPerSec ?: 0.0,
                        writeBytesPerSec = io.writeBytesPerSec ?: 0.0
                    )
                } ?: emptyList()
                Result.success(ios)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "getDiskIo failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Phase 4: Advanced Analytics & Connectivity ─────────────

    override suspend fun runSpeedTest(device: Device): Result<com.rygent.monitor.domain.model.SpeedTestResult> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val url = buildUrl(device, "/api/network/speedtest")
            val response = apiService.runSpeedTest(url, "Bearer ${rawToken.trim()}")
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    Result.success(
                        com.rygent.monitor.domain.model.SpeedTestResult(
                            ping = data.ping ?: 0.0,
                            downloadBits = data.downloadBits ?: 0.0,
                            uploadBits = data.uploadBits ?: 0.0,
                            server = data.server ?: "Unknown",
                            sponsor = data.sponsor ?: "Unknown",
                            error = data.error
                        )
                    )
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "runSpeedTest failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getProcessHistory(device: Device): Result<List<com.rygent.monitor.domain.model.ProcessHistoryEntry>> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val url = buildUrl(device, "/api/analytics/history")
            val response = apiService.getProcessHistory(url, "Bearer ${rawToken.trim()}")
            if (response.isSuccessful) {
                val data = response.body()?.map { entry ->
                    com.rygent.monitor.domain.model.ProcessHistoryEntry(
                        timestamp = entry.timestamp ?: 0L,
                        processes = entry.processes?.map { p -> 
                            com.rygent.monitor.domain.model.Process(
                                pid = p.pid ?: 0,
                                name = p.name ?: "Unknown",
                                cpuPercent = p.cpuPercent ?: 0.0,
                                memoryBytes = p.memoryBytes ?: 0L
                            )
                        } ?: emptyList()
                    )
                } ?: emptyList()
                Result.success(data)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "getProcessHistory failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getAlerts(device: Device): Result<List<com.rygent.monitor.domain.model.RunawayAlert>> {
        return try {
            val rawToken = tokenManager.getToken(device.id)
            if (rawToken.isNullOrBlank()) return Result.failure(Exception("No token found"))
            val url = buildUrl(device, "/api/analytics/alerts")
            val response = apiService.getAlerts(url, "Bearer ${rawToken.trim()}")
            if (response.isSuccessful) {
                val data = response.body()?.map { a ->
                    com.rygent.monitor.domain.model.RunawayAlert(
                        pid = a.pid ?: 0,
                        name = a.name ?: "Unknown",
                        cpuPercent = a.cpuPercent ?: 0.0,
                        durationMin = a.durationMin ?: 0,
                        message = a.message ?: "Unknown Process Alert"
                    )
                } ?: emptyList()
                Result.success(data)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "getAlerts failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun syncDeviceWithDiscovery(name: String, ip: String, tunnelUrl: String?) {
        try {
            // Find device by hostname (name)
            val device = dao.getDeviceByName(name)
            if (device != null) {
                // If tunnel URL changed or IP changed, update it
                val currentIp = device.ipAddress
                
                // Prioritize tunnel URL if we are remote, but update local IP for local access
                val newIp = tunnelUrl?.replace("https://", "") ?: ip
                
                if (currentIp != newIp) {
                    android.util.Log.d("DeviceRepo", "Syncing device $name: $currentIp -> $newIp")
                    dao.updateDeviceConfig(device.id, device.name, newIp, device.port, null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "Sync discovery failed for $name: ${e.message}")
        }
    }
}
