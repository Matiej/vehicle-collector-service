package com.emat.vehicle_collector_service.api.dto

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.LocationSource
import java.time.Instant

data class AssetsResponse(
    val assets: List<AssetResponse>,
    val page: Int?,
    val size: Int?,
    val totalCount: Int?,
    val totalPages: Int?
) {
}

data class AssetResponse(
    val id: String?,
    val assetPublicId: String,
    val ownerId: String,
    val sessionPublicId: String?,
    val spotId: String?,
    val assetType: AssetType,
    val assetStatus: AssetStatus,
    val thumbUrl: String,
    val geoLocation: AssetLocation,
    val createdAt: Instant
)

data class AssetLocation(
    val locationSource: LocationSource,
    val lat: String,
    val lng: String
)