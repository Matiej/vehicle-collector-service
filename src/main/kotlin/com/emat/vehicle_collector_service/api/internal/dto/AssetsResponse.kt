package com.emat.vehicle_collector_service.api.internal.dto

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.LocationSource
import com.emat.vehicle_collector_service.assets.domain.AssetType
import java.time.Instant

data class AssetsResponse(
    val assets: List<AssetResponse>
) {
}

data class AssetResponse(
    val id: String,
    val ownerId: String,
    val sessionId: String?,
    val spotId: String?,
    val assetType: AssetType,
    val assetStatus: AssetStatus,
    val thumbUrl: String,
    val location: AssetLocation,
    val createdAt: Instant
)

data class AssetLocation(
    val locationSource: LocationSource,
    val lat: String,
    val lng: String
)