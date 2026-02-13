package com.emat.vehicle_collector_service.api.dto

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.session.domain.SessionMode
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateSessionRequest(
    val mode: SessionMode,
    val device: String?,
    @field:Size(max = 100, message = "sessionName must be at most 100 characters")
    @field:Pattern(
        regexp = "^[\\p{L}0-9 _\\-\\.,:\\(\\)]*$",
        message = "sessionName contains invalid characters"
    )
    val sessionName: String?,
    val clientVersion: String?,
    val ownerId: String,
    val uploadLocation: GeoPoint? = null
) {
}