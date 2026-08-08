package com.emat.vehicle_collector_service.assets.domain

import java.time.Instant

data class Asset(
    val id: String?,
    val assetPublicId: String,
    val ownerId: String,
    val sessionPublicId: String?,
    val type: AssetType,
    val status: AssetStatus,
    val thumbnails: List<ThumbnailInfo>,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class ThumbnailInfo(
    val size: ThumbnailSize,
    val storageKeyPath: String
)
