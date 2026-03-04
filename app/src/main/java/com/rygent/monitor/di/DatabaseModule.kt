package com.rygent.monitor.di

import android.app.Application
import androidx.room.Room
import com.rygent.monitor.data.local.SystemMonitorDatabase
import com.rygent.monitor.data.local.dao.DeviceDao
import com.rygent.monitor.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): SystemMonitorDatabase {
        return Room.databaseBuilder(
            app,
            SystemMonitorDatabase::class.java,
            Constants.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideDeviceDao(db: SystemMonitorDatabase): DeviceDao {
        return db.deviceDao
    }
}
