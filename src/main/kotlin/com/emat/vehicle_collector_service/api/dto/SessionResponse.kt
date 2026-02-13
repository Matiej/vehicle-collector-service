package com.emat.vehicle_collector_service.api.dto

import com.emat.vehicle_collector_service.session.domain.SessionAsset
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.domain.SessionMode

data class SessionResponse(
    val sessionPublicId: String,
    val sessionName: String?,
    val mode: SessionMode,
    val ownerId: String,
    val spotId: String?,                  // only for spot in the future
    val sessionStatus: SessionStatus,
    val createdAt: String,
    val assets: List<SessionAsset>
) {

}
