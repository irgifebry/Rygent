package com.rygent.monitor.data.remote.model

import com.squareup.moshi.Json

data class HealthResponse(
    @Json(name = "status") val status: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "tunnel_url") val tunnel_url: String? = null
)
