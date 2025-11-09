package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetRepository
import com.emat.vehicle_collector_service.infrastructure.storage.StorageService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

@Service
class AssetsServiceImpl(
    val assetRepository: AssetRepository,
    val validator: AssetUploadValidator,
    val exifExtractor: ExifMetadataExtractor,
    val storage: StorageService
) : AssetsService {
    override fun getAllAssets(type: AssetType?, hasSpot: Boolean?, status: AssetStatus?): Flux<Asset> {
        //todo when more assets(10k pictures) filter on db level
        return assetRepository.findAll()
            .filter { asset ->
                val typeOk = type?.let { asset.assetType == it } ?: true
                val hasSpotOk = hasSpot?.let { asset.spotId != null } ?: true
                val statusOk = status?.let { asset.assetStatus == status } ?: true
                typeOk && hasSpotOk && statusOk
            }
            .map { AssetMapper.toDomain(it) }
    }

    override fun deleteAsset(assetId: String): Mono<Void> {
        return assetRepository.findById(assetId)
            .switchIfEmpty(
                Mono.error(
                    AssetUploadException(
                        "Asset not found: $assetId",
                        HttpStatus.NOT_FOUND,
                        "ASSET_NOT_FOUND"
                    )
                )
            )
            .flatMap { storage.delete(it.storageKeyPath) }
            .then(assetRepository.deleteById(assetId))
    }

    override fun findBySessionId(sessionId: String): Flux<Asset> {
        return assetRepository.findBySessionId(sessionId)
            .map { AssetMapper.toDomain(it) }
    }

    override fun countAllBySessionId(sessionId: String): Mono<Long> {
        return assetRepository.countAllBySessionId(sessionId)
    }

    override fun findLastAssetThumbnail320BySessionId(sessionId: String): Mono<ThumbnailInfo> {
        return assetRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
            .flatMap { asset ->
                val thumb320 = asset.thumbnails
                    ?.firstOrNull { it.size == ThumbnailSize.THUMB_320 && it.storageKeyPath.isNotBlank() }
                Mono.justOrEmpty(thumb320)
            }
            .map { ThumbnailInfo(size = it.size, storageKeyPath = it.storageKeyPath) }
    }

    override fun saveAsset(assetRequest: AssetRequest): Mono<Asset> {
        val filePart = assetRequest.filePart
        val mime = filePart.headers().contentType?.toString()?.lowercase()
        val filename = filePart.filename()
        val fileExtension = filename.substringAfterLast(".", "").lowercase()
        val storageKeyPath = generateStorageKeyPath(assetRequest.assetType, fileExtension)
        return validator.assetUploadValidate(filePart, assetRequest.assetType)
            .flatMap { validatedFile ->
                val exifMono =
                    exifExtractor.extract(validatedFile.tmpFile, mime).defaultIfEmpty(ExifInfo(null, null, null, null))
                val storeMono = storage.store(validatedFile.tmpFile, storageKeyPath.first)

                exifMono.zipWith(storeMono)
                    .flatMap { tuple ->
                        val exif = tuple.t1
                        val storagePath = tuple.t2
                        val asset = Asset(
                            id = null,
                            assetPublicId = storageKeyPath.second,
                            ownerId = assetRequest.ownerId,
                            sessionId = assetRequest.sessionId,
                            spotId = null,
                            type = assetRequest.assetType,
                            status = AssetStatus.RAW,
                            mimeType = mime,
                            originalFilename = filename,
                            storageKeyPath = storagePath,
                            locationSource = LocationSource.UNKNOWN,
                            exif = exif,
                            deviceGeoLocation = null,
                            thumbnails = emptyList(),
                            createdAt = null,
                            updatedAt = null
                        )
                        assetRepository.save(AssetMapper.toDocument(asset))
                    }
                    .map(AssetMapper::toDomain)
                    .doFinally { _ -> validatedFile.tmpFile.delete() }
            }
    }

    fun generateStorageKeyPath(assetType: AssetType, extension: String): Pair<String, String> {
        val now = LocalDate.now()
        val publicId = generatePublicId("original")
        val storageKeyPath = assetType.name.lowercase() +
                "/" + now.year.toString() +
                "/" + now.month.value.toString() +
                "/" + publicId +
                "." + extension
        return Pair(storageKeyPath, publicId)
    }

    fun generatePublicId(type: String): String =
        "asset_" + java.time.LocalDate.now().year + "_" + type + "_" +
                java.util.UUID.randomUUID().toString().take(8)
}