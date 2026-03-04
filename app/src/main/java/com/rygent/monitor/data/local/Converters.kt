package com.rygent.monitor.data.local

import androidx.room.TypeConverter
import com.rygent.monitor.data.remote.model.ProcessInfo
import com.rygent.monitor.domain.model.DeviceStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    
    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        private val processListType = Types.newParameterizedType(List::class.java, ProcessInfo::class.java)
        private val processListAdapter = moshi.adapter<List<ProcessInfo>>(processListType)
    }

    @TypeConverter
    fun fromDeviceStatus(status: DeviceStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDeviceStatus(value: String): DeviceStatus {
        return try {
            DeviceStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            DeviceStatus.OFFLINE
        }
    }

    @TypeConverter
    fun fromProcessList(value: List<ProcessInfo>?): String? {
        return value?.let { processListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toProcessList(value: String?): List<ProcessInfo>? {
        return value?.let {
            try {
                processListAdapter.fromJson(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
