package com.emat.vehicle_collector_service.infrastructure.error

import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val path: String?,
    val status: Int,
    val error: String,
    val code: String,
    val message: String?
)
