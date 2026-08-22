package com.emat.vehicle_collector_service.session.domain

data class SessionAsset(
    val assetPublicId: String,
    val type: String,
    val status: String,
    val thumbnailSmallUrl: String?,
    val thumbnailMediumUrl: String?
) {

}
