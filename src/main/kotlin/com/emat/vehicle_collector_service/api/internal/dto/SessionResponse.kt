package com.emat.vehicle_collector_service.api.internal.dto

import com.emat.vehicle_collector_service.session.domain.SessionAsset
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.domain.SessionMode

data class SessionResponse(
    val sessionId: String,
    val mode: SessionMode,
    val ownerId: String,
    val spotId: String?,                  // only for spot in the future
    val status: SessionStatus,
    val createdAt: String,
    val assets: List<SessionAsset>
) {

}
