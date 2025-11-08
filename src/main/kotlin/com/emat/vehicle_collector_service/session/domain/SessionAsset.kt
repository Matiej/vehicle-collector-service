package com.emat.vehicle_collector_service.session.domain

data class SessionAsset(
    val id: String,
    val type: String,                     // image | audio
    val status: String,                   // RAW, VECTORIZED, TRANSCRIBED, ERROR
    val thumbnailUrl: String?
) {

}
