package com.rygent.monitor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rygent.monitor.data.local.dao.DeviceDao
import com.rygent.monitor.data.local.entity.DeviceEntity

@Database(entities = [DeviceEntity::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class) // Need converters for Enums usually, or Room handles them if simple string? Room handles Enums as Strings/Ints? 
// Room connects enums to string/int by default in newer versions or needs converter. 
// Safest to add a converter for DeviceStatus.
abstract class SystemMonitorDatabase : RoomDatabase() {
    abstract val deviceDao: DeviceDao
}
