package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.dto.AssetResponse
import com.emat.vehicle_collector_service.api.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.AssetRepository
import com.emat.vehicle_collector_service.assets.geocoding.AssetGeocodingService
import com.emat.vehicle_collector_service.assets.infra.FileInfo
import com.emat.vehicle_collector_service.assets.thumbnail.ThumbnailService
import com.emat.vehicle_collector_service.infrastructure.error.ResourceNotFoundException
import com.emat.vehicle_collector_service.infrastructure.storage.StorageService
import com.emat.vehicle_collector_service.session.SessionOwnership
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.Instant

@Service
class AssetsServiceImpl(
    private val assetRepository: AssetRepository,
    private val template: ReactiveMongoTemplate,
    private val validator: AssetUploadValidator,
    private val exifExtractor: ExifMetadataExtractor,
    private val fileMetadataReader: FileMetadataReader,
    private val storage: StorageService,
    private val thumbnailService: ThumbnailService,
    private val assetGeocodingService: AssetGeocodingService,
    private val sessionOwnership: SessionOwnership
) : AssetsService {

    private val log = LoggerFactory.getLogger(AssetsService::class.java)

    override fun findByPublicId(assetPublicId: String, ownerId: String): Mono<AssetDocument> {
        return assetRepository.findByAssetPublicIdAndOwnerId(assetPublicId, ownerId)
            .switchIfEmpty(
                Mono.error(
                    ResourceNotFoundException("Asset $assetPublicId not found for owner $ownerId")
                )
            )
    }

    override fun getAllAssets(assetsOwnerQuery: AssetsOwnerQuery): Mono<PageResponse<AssetResponse>> =
        findPage(filters(assetsOwnerQuery), assetsOwnerQuery)

    override fun getAllAssetsByOwnerId(
        ownerId: String,
        assetsOwnerQuery: AssetsOwnerQuery
    ): Mono<PageResponse<AssetResponse>> =
        findPage(
            listOf(Criteria.where("ownerId").`is`(ownerId)) + filters(assetsOwnerQuery),
            assetsOwnerQuery
        )

    override fun deleteAssetByPublicId(assetPublicId: String): Mono<Void> {
        return assetRepository.findByAssetPublicId(assetPublicId)
            .switchIfEmpty(
                Mono.error(
                    AssetUploadException(
                        "Asset not found: $assetPublicId",
                        HttpStatus.NOT_FOUND,
                        "ASSET_NOT_FOUND"
                    )
                )
            )
            .flatMap { asset ->
                storage.delete(asset.file.storageKeyPath)
                    .doOnSuccess { log.info("Deleted file ${asset.file.storageKeyPath}") }
                    .then(assetRepository.deleteById(asset.id!!))
            }.then()
    }

    override fun getAllAssetsBySessionPublicId(
        sessionPublicId: String,
        ownerId: String,
        assetsOwnerQuery: AssetsOwnerQuery
    ): Mono<PageResponse<AssetResponse>> =
        findPage(
            listOf(
                Criteria.where("ownerId").`is`(ownerId),
                Criteria.where("sessionPublicId").`is`(sessionPublicId)
            ) + filters(assetsOwnerQuery),
            assetsOwnerQuery
        )

    private fun findPage(
        criteria: List<Criteria>,
        assetsOwnerQuery: AssetsOwnerQuery
    ): Mono<PageResponse<AssetResponse>> {
        val filter = combine(criteria)
        val countQuery = Query(filter)
        val dataQuery = Query(filter).with(pageRequest(assetsOwnerQuery))

        return Mono.zip(
            template.count(countQuery, AssetDocument::class.java),
            template.find(dataQuery, AssetDocument::class.java)
                .map { AssetMapper.toAssetResponse(it) }
                .collectList()
        ).map { tuple ->
            PageResponse.of(
                content = tuple.t2,
                page = assetsOwnerQuery.page,
                size = assetsOwnerQuery.size,
                totalElements = tuple.t1
            )
        }
    }

    private fun filters(assetsOwnerQuery: AssetsOwnerQuery): List<Criteria> =
        listOfNotNull(assetsOwnerQuery.type?.let { Criteria.where("assetType").`is`(it) })

    private fun combine(criteria: List<Criteria>): Criteria =
        if (criteria.isEmpty()) Criteria() else Criteria().andOperator(*criteria.toTypedArray())

    private fun pageRequest(assetsOwnerQuery: AssetsOwnerQuery): PageRequest =
        PageRequest.of(
            assetsOwnerQuery.page,
            assetsOwnerQuery.size,
            Sort.by(assetsOwnerQuery.sortDir, "createdAt")
        )

    override fun getAllAssetsBySessionPublicIdDescByCreatedAt(sessionPublicId: String): Flux<Asset> =
        assetRepository.findAllBySessionPublicIdOrderByCreatedAtDesc(sessionPublicId)
            .map { AssetMapper.toDomain(it) }

    override fun countAllBySessionPublicId(sessionPublicId: String): Mono<Long> {
        return assetRepository.countAllBySessionPublicId(sessionPublicId)
    }

    override fun findLastAssetThumbnail320BySessionPublicId(sessionPublicId: String): Mono<String> {
        return assetRepository.findFirstBySessionPublicIdOrderByCreatedAtDesc(sessionPublicId)
            .flatMap { asset ->
                val hasThumb320 = asset.file.thumbnails
                    .any { it.size == ThumbnailSize.THUMB_320 && it.storageKeyPath.isNotBlank() }
                Mono.justOrEmpty(
                    if (hasThumb320) "/api/public/assets/${asset.assetPublicId}/thumbnail?size=THUMB_320" else null
                )
            }
    }

    override fun saveAsset(assetRequest: AssetRequest): Mono<AssetResponse> {
        val guard: Mono<Void> = assetRequest.sessionPublicId
            ?.let { sessionOwnership.requireOwned(it, assetRequest.ownerId) }
            ?: Mono.empty()
        return guard.then(Mono.defer { storeAsset(assetRequest) })
    }

    override fun updateLocation(
        assetPublicId: String,
        ownerId: String,
        gps: GeoPoint
    ): Mono<AssetResponse> =
        changeLocation(assetPublicId, ownerId, GpsSource.USER, gps)

    override fun resetLocation(assetPublicId: String, ownerId: String): Mono<AssetResponse> =
        changeLocation(assetPublicId, ownerId, GpsSource.EXIF, null)

    private fun changeLocation(
        assetPublicId: String,
        ownerId: String,
        gpsSource: GpsSource,
        userGps: GeoPoint?
    ): Mono<AssetResponse> =
        findByPublicId(assetPublicId, ownerId)
            .flatMap { current ->
                val query = Query.query(
                    Criteria.where("assetPublicId").`is`(assetPublicId)
                        .and("ownerId").`is`(ownerId)
                        .and("version").`is`(current.version)
                )
                val update = Update()
                    .set("capture.gpsSource", gpsSource)
                    .unset("capture.place")
                    .set("updatedAt", Instant.now())
                    .inc("version", 1)
                if (userGps != null) {
                    update.set("capture.userGps", userGps)
                }

                template.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    AssetDocument::class.java
                ).switchIfEmpty(
                    Mono.error(OptimisticLockingFailureException("Asset $assetPublicId was modified concurrently"))
                )
            }
            .doOnNext(::scheduleGeocoding)
            .map(AssetMapper::toAssetResponse)

    private fun scheduleGeocoding(asset: AssetDocument) {
        assetGeocodingService.geocodeAndSave(
            assetId = asset.id!!,
            assetPublicId = asset.assetPublicId,
            gps = asset.capture.activeGps()
        ).subscribe(
            {},
            { e -> log.error("Geocoding background job failed: {}", e.message) }
        )
    }

    private fun storeAsset(assetRequest: AssetRequest): Mono<AssetResponse> {
        val filePart = assetRequest.filePart
        val mime = filePart.headers().contentType?.toString()?.lowercase()
        val filename = filePart.filename()
        val fileExtension = filename.substringAfterLast(".", "").lowercase()
        val storageKeyPath = generateStorageKeyPathAndPublicId(assetRequest.assetType, fileExtension)
        return validator.assetUploadValidate(filePart, assetRequest.assetType)
            .flatMap { validatedFile ->
                val metadataMono: Mono<ExtractedMetadata> =
                    exifExtractor.extract(validatedFile.tmpFile, mime).defaultIfEmpty(ExtractedMetadata())
                val storeMono: Mono<String> = storage.store(validatedFile.tmpFile, storageKeyPath.first)
                val fileMetadataMono: Mono<FileMetadata> = fileMetadataReader.read(validatedFile.tmpFile)

                Mono.zip(metadataMono, storeMono, fileMetadataMono)
                    .flatMap { tuple ->
                        val metadata = tuple.t1
                        val fileMetadata = tuple.t3
                        val document = AssetDocument(
                            id = null,
                            assetPublicId = storageKeyPath.second,
                            ownerId = assetRequest.ownerId,
                            sessionPublicId = assetRequest.sessionPublicId,
                            assetType = assetRequest.assetType,
                            file = FileInfo(
                                storageKeyPath = tuple.t2,
                                originalFilename = filename,
                                mimeType = mime,
                                sizeBytes = fileMetadata.sizeBytes,
                                width = metadata.width,
                                height = metadata.height,
                                sha256 = fileMetadata.sha256,
                                status = AssetStatus.UPLOADED
                            ),
                            capture = metadata.capture
                        )
                        assetRepository.save(document)
                    }
                    .doOnNext { savedDoc ->
                        if (savedDoc.assetType == AssetType.IMAGE) {
                            thumbnailService.generateAndSave(
                                assetId = savedDoc.id!!,
                                assetPublicId = savedDoc.assetPublicId,
                                originalStorageKeyPath = savedDoc.file.storageKeyPath
                            ).subscribe(
                                {},
                                { e -> log.error("Thumbnail background job failed: {}", e.message) }
                            )
                        }
                        scheduleGeocoding(savedDoc)
                    }
                    .map(AssetMapper::toAssetResponse)
                    .doFinally { _ -> validatedFile.tmpFile.delete() }
            }
    }

    private fun generateStorageKeyPathAndPublicId(assetType: AssetType, extension: String): Pair<String, String> {
        val now = LocalDate.now()
        val publicId = generatePublicId("original")
        val storageKeyPath = assetType.name.lowercase() +
                "/" + now.year.toString() +
                "/" + now.month.value.toString() +
                "/" + publicId +
                "." + extension
        return Pair(storageKeyPath, publicId)
    }

    private fun generatePublicId(type: String): String =
        "asset_" + LocalDate.now().year + "_" + LocalDate.now().month.value + "_" + type + "_" +
                java.util.UUID.randomUUID().toString().take(8)
}
