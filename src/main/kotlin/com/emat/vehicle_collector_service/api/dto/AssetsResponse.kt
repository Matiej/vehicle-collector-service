package com.emat.vehicle_collector_service.api.dto

import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import java.time.Instant

data class AssetResponse(
    val assetPublicId: String,
    val ownerId: String,
    val sessionPublicId: String?,
    val assetType: AssetType,
    val file: AssetFileResponse,
    val capture: AssetCaptureResponse,
    val thumbnailSmallUrl: String?,
    val thumbnailMediumUrl: String?,
    val createdAt: Instant
)

data class AssetFileResponse(
    val originalFilename: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val sha256: String
)

data class AssetCaptureResponse(
    val takenAt: Instant?,
    val gps: GeoPointResponse?,
    val gpsSource: GpsSource,
    val place: PlaceResponse?
)

data class GeoPointResponse(
    val lat: Double,
    val lng: Double
)

data class PlaceResponse(
    val countryCode: String?,
    val country: String?,
    val city: String?,
    val region: String?
)
