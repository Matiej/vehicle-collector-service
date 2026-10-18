package com.emat.vehicle_collector_service.api.web.dto

data class WebMeResponse(
    val userId: String,
    val username: String,
    val displayName: String,
    val roles: List<String>
)
