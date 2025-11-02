package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.internal.dto.AssetLocation
import com.emat.vehicle_collector_service.api.internal.dto.AssetResponse
import com.emat.vehicle_collector_service.assets.domain.Asset
import com.emat.vehicle_collector_service.assets.domain.ExifInfo
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.ThumbnailInfo
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.AssetMeta
import com.emat.vehicle_collector_service.assets.infra.DeviceLocation
import com.emat.vehicle_collector_service.assets.infra.Thumbnail
import java.time.Instant

object AssetMapper {

    fun toDomain(assetDocument: AssetDocument): Asset {
        return Asset(
            id = assetDocument.id,
            ownerId = assetDocument.ownerId,
            sessionId = assetDocument.sessionId,
            spotId = assetDocument.spotId,
            type = assetDocument.assetType,
            status = assetDocument.assetStatus,
            mimeType = assetDocument.mimeType,
            originalFilename = assetDocument.originalFilename,
            storageKeyPath = assetDocument.storageKeyPath,
            locationSource = assetDocument.locationSource,
            exif = assetDocument.exif?.let {
                ExifInfo(
                    takenAt = it.takenAt,
                    lat = it.lat,
                    lng = it.lng,
                    camera = it.camera
                )
            },
            deviceLocation = assetDocument.deviceLocation?.let {
                GeoPoint(it.lat, it.lng)
            },
            thumbnails = assetDocument.thumbnails.map {
                ThumbnailInfo(
                    size = it.size ?: "",
                    storageKeyPath = it.storageKeyPath ?: ""
                )
            },
            createdAt = assetDocument.createdAt,
            updatedAt = assetDocument.updatedAt
        )
    }

    fun toDocument(asset: Asset): AssetDocument =
        AssetDocument(
            id = asset.id,
            ownerId = asset.ownerId,
            sessionId = asset.sessionId,
            spotId = asset.spotId,
            assetType = asset.type,
            mimeType = asset.mimeType,
            originalFilename = asset.originalFilename,
            storageKeyPath = asset.storageKeyPath,
            locationSource = asset.locationSource,
            exif = asset.exif?.let {
                AssetMeta(
                    takenAt = it.takenAt,
                    lat = it.lat,
                    lng = it.lng,
                    camera = it.camera
                )
            },
            deviceLocation = asset.deviceLocation?.let {
                DeviceLocation(lat = it.lat, lng = it.lng)
            },
            assetStatus = asset.status,
            thumbnails = asset.thumbnails.map {
                Thumbnail(size = it.size, storageKeyPath = it.storageKeyPath)
            }
        )

    fun toResponse(asset: Asset): AssetResponse =
        AssetResponse(
            id = asset.id ?: "",
            ownerId = asset.ownerId,
            sessionId = asset.sessionId,
            spotId = asset.spotId,
            assetType = asset.type,
            assetStatus = asset.status,
            thumbUrl = asset.thumbnails.firstOrNull()?.storageKeyPath ?: "",
            location = AssetLocation(
                locationSource = asset.locationSource,
                lat = (asset.exif?.lat ?: asset.deviceLocation?.lat)?.toString() ?: "",
                lng = (asset.exif?.lng ?: asset.deviceLocation?.lng)?.toString() ?: ""
            ),
            createdAt = asset.createdAt ?: asset.updatedAt ?: Instant.now()
        )
}