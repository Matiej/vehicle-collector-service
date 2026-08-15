package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = AssetDocument.ASSET_COLLECTION_NAME)
@CompoundIndexes(
    value = [
        CompoundIndex(name = "owner_created_at", def = "{'ownerId': 1, 'createdAt': -1}"),
        CompoundIndex(name = "file_sha256", def = "{'file.sha256': 1}"),
        CompoundIndex(name = "capture_taken_at", def = "{'capture.takenAt': -1}")
    ]
)
data class AssetDocument(
    @Id
    val id: String?,
    @Indexed(unique = true)
    val assetPublicId: String,
    val ownerId: String,
    @Indexed
    val sessionPublicId: String?,
    val assetType: AssetType,
    val file: FileInfo,
    val capture: CaptureInfo,
    val curation: CurationInfo = CurationInfo(),
    val vehicleRecognition: VehicleRecognition = VehicleRecognition(),
    @CreatedDate
    var createdAt: Instant? = null,
    @LastModifiedDate
    var updatedAt: Instant? = null,
    @Version
    var version: Long? = null

) {

    companion object {
        const val ASSET_COLLECTION_NAME: String = "assets"
    }
}

data class FileInfo(
    val storageKeyPath: String,
    val originalFilename: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val sha256: String,
    val status: AssetStatus = AssetStatus.RAW,
    val failureReason: String? = null,
    val thumbnails: List<Thumbnail> = emptyList()
)

data class Thumbnail(
    val size: ThumbnailSize,
    val storageKeyPath: String
)
