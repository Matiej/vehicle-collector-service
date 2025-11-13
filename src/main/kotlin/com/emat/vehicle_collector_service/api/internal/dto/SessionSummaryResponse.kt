package com.emat.vehicle_collector_service.api.internal.dto

import com.emat.vehicle_collector_service.session.domain.SessionMode
import com.emat.vehicle_collector_service.session.domain.SessionStatus

data class SessionSummaryResponse(
    val sessionPublicId: String,
    val sessionName: String?,
    val sessionMode: SessionMode,
    val ownerId: String,
    val assetsCount: Int,
    val coverThumbnailUrl: String?,       // first asset thumb, in the future asset will be marked as main or so
    val sessionStatus: SessionStatus,
    val createdAt: String
) {
}