package com.emat.vehicle_collector_service.assets.domain

import java.time.Instant

data class Asset(
    val id: String?,
    val assetPublicId: String,
    val ownerId: String,
    val sessionId: String?,
    val spotId: String?,
    val type: AssetType,
    val status: AssetStatus,
    val mimeType: String?,
    val originalFilename: String?,
    val storageKeyPath: String,
    val locationSource: LocationSource,
    val exif: ExifInfo?,
    val deviceGeoLocation: GeoPoint?,
    val thumbnails: List<ThumbnailInfo>,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {

}

data class ExifInfo(
    val takenAt: Instant?,
    val lat: Double?,
    val lng: Double?,
    val camera: String?
)

data class GeoPoint(
    val lat: Double?,
    val lng: Double?
)

data class ThumbnailInfo(
    val size: String,
    val storageKeyPath: String
)
