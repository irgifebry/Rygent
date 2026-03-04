package com.rygent.monitor.data.remote

import com.rygent.monitor.data.remote.model.SystemInfoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import retrofit2.http.POST

interface SystemMonitorApiService {

    @GET
    suspend fun getSystemInfo(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<SystemInfoResponse>

    @GET
    suspend fun healthCheck(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<com.rygent.monitor.data.remote.model.HealthResponse>

    @retrofit2.http.POST
    suspend fun shutdown(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    @retrofit2.http.POST
    suspend fun reboot(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    @retrofit2.http.POST
    suspend fun suspend(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    @retrofit2.http.POST
    suspend fun killProcess(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    // ── Phase 3: Storage Intelligence ─────────────────────────

    @GET
    suspend fun getSmartInfo(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<List<com.rygent.monitor.data.remote.model.SmartDriveInfo>>

    @GET
    suspend fun getDiskIo(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<List<com.rygent.monitor.data.remote.model.DiskIoInfo>>

    @GET
    suspend fun getLargeFiles(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<List<com.rygent.monitor.data.remote.model.LargeFileInfo>>

    // ── Phase 4: Advanced Analytics & Connectivity ─────────────

    @POST
    suspend fun runSpeedTest(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<com.rygent.monitor.data.remote.model.SpeedTestResultInfo>

    @GET
    suspend fun getProcessHistory(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<List<com.rygent.monitor.data.remote.model.ProcessHistoryEntryInfo>>
    
    @GET
    suspend fun getAlerts(
        @Url url: String,
        @Header("Authorization") token: String
    ): Response<List<com.rygent.monitor.data.remote.model.RunawayAlertInfo>>
}
