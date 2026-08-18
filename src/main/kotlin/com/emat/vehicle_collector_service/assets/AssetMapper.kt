package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.dto.AssetCaptureResponse
import com.emat.vehicle_collector_service.api.dto.AssetFileResponse
import com.emat.vehicle_collector_service.api.dto.AssetResponse
import com.emat.vehicle_collector_service.api.dto.GeoPointResponse
import com.emat.vehicle_collector_service.api.dto.PlaceResponse
import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import java.time.Instant

object AssetMapper {

    fun toDomain(assetDocument: AssetDocument): Asset =
        Asset(
            id = assetDocument.id ?: "",
            assetPublicId = assetDocument.assetPublicId,
            ownerId = assetDocument.ownerId,
            sessionPublicId = assetDocument.sessionPublicId,
            type = assetDocument.assetType,
            status = assetDocument.file.status,
            thumbnails = assetDocument.file.thumbnails.map {
                ThumbnailInfo(size = it.size, storageKeyPath = it.storageKeyPath)
            },
            createdAt = assetDocument.createdAt,
            updatedAt = assetDocument.updatedAt
        )

    fun toAssetResponse(assetDocument: AssetDocument): AssetResponse =
        AssetResponse(
            assetPublicId = assetDocument.assetPublicId,
            ownerId = assetDocument.ownerId,
            sessionPublicId = assetDocument.sessionPublicId,
            assetType = assetDocument.assetType,
            file = AssetFileResponse(
                originalFilename = assetDocument.file.originalFilename,
                mimeType = assetDocument.file.mimeType,
                sizeBytes = assetDocument.file.sizeBytes,
                width = assetDocument.file.width,
                height = assetDocument.file.height,
                sha256 = assetDocument.file.sha256
            ),
            capture = AssetCaptureResponse(
                takenAt = assetDocument.capture.takenAt,
                gps = assetDocument.capture.activeGps()?.let { GeoPointResponse(it.lat, it.lng) },
                gpsSource = assetDocument.capture.gpsSource,
                place = assetDocument.capture.place?.let {
                    PlaceResponse(
                        countryCode = it.countryCode,
                        country = it.country,
                        city = it.city,
                        region = it.region
                    )
                }
            ),
            thumbnailSmallUrl = assetDocument.file.thumbnails
                .firstOrNull { it.size == ThumbnailSize.THUMB_320 }
                ?.let { "/api/public/assets/${assetDocument.assetPublicId}/thumbnail?size=THUMB_320" },
            thumbnailMediumUrl = assetDocument.file.thumbnails
                .firstOrNull { it.size == ThumbnailSize.THUMB_640 }
                ?.let { "/api/public/assets/${assetDocument.assetPublicId}/thumbnail?size=THUMB_640" },
            createdAt = assetDocument.createdAt ?: assetDocument.updatedAt ?: Instant.now()
        )
}
