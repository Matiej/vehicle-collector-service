package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.LocationSource
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = AssetDocument.assetDocumentName)
data class AssetDocument(
    @Id
    val id: String?,
    val assetPublicId: String,
    val ownerId: String,
    val sessionId: String?,
    val spotId: String?,
    val assetType: AssetType,
    val mimeType: String?,
    val originalFilename: String?,
    val storageKeyPath: String,
    val locationSource: LocationSource = LocationSource.UNKNOWN,
    val exif: AssetMeta?,
    val deviceGeoLocation: DeviceLocation?,
    val assetStatus: AssetStatus = AssetStatus.RAW,
    val thumbnails: List<Thumbnail> = emptyList(),
    @CreatedDate
    var createdAt: Instant? = null,
    @LastModifiedDate
    var updatedAt: Instant? = null,
    @Version
    var version: Long? = null

) {

 companion object {
    const val assetDocumentName: String = "assets"
 }
}

data class AssetMeta(
    val takenAt: Instant?,
    val lat: Double?,
    val lng: Double?,
    val camera: String?,
) {
}

data class DeviceLocation(
    val lat: Double?,
    val lng: Double?
)

data class Thumbnail(
    val size: ThumbnailSize?,
    val storageKeyPath: String?
)