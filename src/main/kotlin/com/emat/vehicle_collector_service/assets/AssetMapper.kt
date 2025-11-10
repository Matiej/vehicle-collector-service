package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.internal.dto.AssetLocation
import com.emat.vehicle_collector_service.api.internal.dto.AssetResponse
import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.AssetMeta
import com.emat.vehicle_collector_service.assets.infra.DeviceLocation
import com.emat.vehicle_collector_service.assets.infra.Thumbnail
import java.time.Instant

object AssetMapper {

    fun toDomain(assetDocument: AssetDocument): Asset {
        return Asset( //todo moze w bazie dla nullable nie trzeba bedzie = null
            id = assetDocument.id ?: "",
            assetPublicId = assetDocument.assetPublicId,
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
            deviceGeoLocation = assetDocument.deviceGeoLocation?.let {
                GeoPoint(it.lat, it.lng)
            },
            thumbnails = assetDocument.thumbnails.map {
                ThumbnailInfo(
                    size = it.size ?: ThumbnailSize.THUMB_320,
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
            assetPublicId = asset.assetPublicId,
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
            deviceGeoLocation = asset.deviceGeoLocation?.let {
                DeviceLocation(lat = it.lat, lng = it.lng)
            },
            assetStatus = asset.status,
            thumbnails = asset.thumbnails.map {
                Thumbnail(size = it.size, storageKeyPath = it.storageKeyPath)
            }
        )

    fun toResponse(asset: Asset): AssetResponse =
        AssetResponse(
            id = asset.id,
            assetPublicId = asset.assetPublicId,
            ownerId = asset.ownerId,
            sessionId = asset.sessionId,
            spotId = asset.spotId,
            assetType = asset.type,
            assetStatus = asset.status,
            thumbUrl = asset.thumbnails.firstOrNull()?.storageKeyPath ?: "",
            geoLocation = AssetLocation(
                locationSource = asset.locationSource,
                lat = (asset.exif?.lat ?: asset.deviceGeoLocation?.lat)?.toString() ?: "",
                lng = (asset.exif?.lng ?: asset.deviceGeoLocation?.lng)?.toString() ?: ""
            ),
            createdAt = asset.createdAt ?: asset.updatedAt ?: Instant.now()
        )

    fun toAssetResponse(assetDocument: AssetDocument): AssetResponse =
        AssetResponse(
            id = assetDocument.id,
            assetPublicId = assetDocument.assetPublicId,
            ownerId = assetDocument.ownerId,
            sessionId = assetDocument.sessionId,
            spotId = assetDocument.spotId,
            assetType = assetDocument.assetType,
            assetStatus = assetDocument.assetStatus,
            thumbUrl = assetDocument.thumbnails.firstOrNull()?.storageKeyPath ?: "",
            geoLocation = AssetLocation(
                locationSource = assetDocument.locationSource,
                lat = (assetDocument.exif?.lat ?: assetDocument.deviceGeoLocation?.lat)?.toString() ?: "",
                lng = (assetDocument.exif?.lng ?: assetDocument.deviceGeoLocation?.lng)?.toString() ?: ""
            ),
            createdAt = assetDocument.createdAt ?: assetDocument.updatedAt ?: Instant.now()
        )
}