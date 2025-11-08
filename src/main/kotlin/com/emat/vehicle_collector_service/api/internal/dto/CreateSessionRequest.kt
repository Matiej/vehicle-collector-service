package com.emat.vehicle_collector_service.api.internal.dto

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.session.domain.SessionMode

data class CreateSessionRequest(
    val mode: SessionMode,
    val device: String?,
    val clientVersion: String?,
    val ownerId: String,
    val uploadLocation: GeoPoint? = null
) {
}