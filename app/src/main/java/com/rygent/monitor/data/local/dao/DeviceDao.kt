package com.rygent.monitor.data.local.dao

import androidx.room.*
import com.rygent.monitor.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id")
    fun getDeviceById(id: String): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceByIdStatic(id: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE name = :name")
    suspend fun getDeviceByName(name: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)
    
    @Query("UPDATE devices SET name = :name, ipAddress = :ip, port = :port, token = :token WHERE id = :id")
    suspend fun updateDeviceConfig(id: String, name: String, ip: String, port: Int, token: String?)
    
    @Query("DELETE FROM devices")
    suspend fun clearAll()
}
