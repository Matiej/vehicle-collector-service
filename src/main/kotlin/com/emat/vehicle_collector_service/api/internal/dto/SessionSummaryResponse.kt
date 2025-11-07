package com.emat.vehicle_collector_service.api.internal.dto

import com.emat.vehicle_collector_service.session.domain.SessionMode

data class SessionSummaryResponse(
    val sessionId: String,
    val sessionMode: SessionMode,
    val ownerId: String,
    val assetsCount: Int,
    val coverThumbnailUrl: String?,       // first asset thumb, in the future asset will be marked as main or so
    val createdAt: String
) {
}