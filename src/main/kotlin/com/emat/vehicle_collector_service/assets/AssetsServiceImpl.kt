package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.internal.dto.AssetResponse
import com.emat.vehicle_collector_service.api.internal.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.internal.dto.AssetsResponse
import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.AssetRepository
import com.emat.vehicle_collector_service.infrastructure.storage.StorageService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

@Service
class AssetsServiceImpl(
    private val assetRepository: AssetRepository,
    private val template: ReactiveMongoTemplate,
    private val validator: AssetUploadValidator,
    private val exifExtractor: ExifMetadataExtractor,
    private val storage: StorageService
) : AssetsService {

    private val log = LoggerFactory.getLogger(AssetsService::class.java)

    override fun getAllAssets(assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse> {
        val pageRequest = pageRequest(assetsOwnerQuery)
        var criteria = emptyList<Criteria>()
        assetsOwnerQuery.type?.let { criteria += Criteria.where("assetType").`is`(it) }
        assetsOwnerQuery.status?.let { criteria += Criteria.where("assetStatus").`is`(it) }
        assetsOwnerQuery.hasSpot?.let { want ->
            criteria += if (want) Criteria.where("spotId").ne(null)
            else Criteria.where("spotId").`is`(null)
        }

        val query = Query().addCriteria(Criteria().andOperator(*criteria.toTypedArray()))
            .with(pageRequest)

        return template.find(query, AssetDocument::class.java)
            .map { AssetMapper.toAssetResponse(it) }
            .collectList()
            .map { assets ->
                val totalCount = assets.size
                val pages = getNumberOfPages(totalCount, assetsOwnerQuery.size)
                AssetsResponse(
                    assets = assets,
                    page = assetsOwnerQuery.page,
                    size = assetsOwnerQuery.size,
                    totalCount = totalCount,
                    totalPages = pages
                )
            }
    }

    override fun getAllAssetsByOwnerId(ownerId: String, assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse> {
        val pageRequest = pageRequest(assetsOwnerQuery)
        val criteria = mutableListOf(
            Criteria.where("ownerId").`is`(ownerId)
        )
        assetsOwnerQuery.type?.let { criteria += Criteria.where("assetType").`is`(it) }
        assetsOwnerQuery.status?.let { criteria += Criteria.where("assetStatus").`is`(it) }
        assetsOwnerQuery.hasSpot?.let { want ->
            criteria += if (want) Criteria.where("spotId").ne(null)
            else Criteria.where("spotId").`is`(null)
        }

        val query = Query().addCriteria(Criteria().andOperator(*criteria.toTypedArray()))
            .with(pageRequest)

        return template.find(query, AssetDocument::class.java)
            .map { AssetMapper.toAssetResponse(it) }
            .collectList()
            .map { assets ->
                val totalCount = assets.size
                val pages = getNumberOfPages(totalCount, assetsOwnerQuery.size)
                AssetsResponse(
                    assets = assets,
                    page = assetsOwnerQuery.page,
                    size = assetsOwnerQuery.size,
                    totalCount = totalCount,
                    totalPages = pages
                )
            }
    }

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
                storage.delete(asset.storageKeyPath)
                    .doOnSuccess { log.info("Deleted file ${asset.storageKeyPath}") }
                    .then(assetRepository.deleteById(asset.id!!))
            }.then()
    }

    override fun getAllAssetsBySessionPublicId(sessionPublicId: String, assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse> {
        val pageRequest = pageRequest(assetsOwnerQuery)
        return assetRepository.findAllBySessionPublicId(sessionPublicId, pageRequest)
            .map { AssetMapper.toAssetResponse(it) }
            .collectList()
            .map { assets ->
                AssetsResponse(
                    assets = assets,
                    page = null,
                    size = null,
                    totalCount = null,
                    totalPages = null
                )
            }
    }

    private fun pageRequest(assetsOwnerQuery: AssetsOwnerQuery): PageRequest {
        val pageRequest = PageRequest.of(
            assetsOwnerQuery.page,
            assetsOwnerQuery.size, Sort.by(assetsOwnerQuery.sortDir, "createdAt")
        )
        return pageRequest
    }

    override fun getAllAssetsBySessionPublicIdDescByCreatedAt(sessionPublicId: String): Flux<Asset> =
        assetRepository.findAllBySessionPublicIdOrderByCreatedAtDesc(sessionPublicId)
            .map { AssetMapper.toDomain(it) }

    override fun countAllBySessionPublicIdId(sessionPublicId: String): Mono<Long> {
        return assetRepository.countAllBySessionPublicId(sessionPublicId)
    }

    override fun findLastAssetThumbnail320BySessionPublicIdId(sessionPublicId: String): Mono<ThumbnailInfo> {
        return assetRepository.findFirstBySessionPublicIdOrderByCreatedAtDesc(sessionPublicId)
            .flatMap { asset ->
                val thumb320 = asset.thumbnails
                    ?.firstOrNull { it.size == ThumbnailSize.THUMB_320 && it.storageKeyPath.isNotBlank() }
                Mono.justOrEmpty(thumb320)
            }
            .map { ThumbnailInfo(size = it.size, storageKeyPath = it.storageKeyPath) }
    }

    override fun saveAsset(assetRequest: AssetRequest): Mono<AssetResponse> {
        val filePart = assetRequest.filePart
        val mime = filePart.headers().contentType?.toString()?.lowercase()
        val filename = filePart.filename()
        val fileExtension = filename.substringAfterLast(".", "").lowercase()
        val storageKeyPath = generateStorageKeyPathAndPublicId(assetRequest.assetType, fileExtension)
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
                            sessionPublicId = assetRequest.sessionPublicId,
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

    private fun getNumberOfPages(totalCount: Int, pageSize: Int): Int {
        val pages: Double = (totalCount.toDouble() / pageSize.toDouble())
        return if (pages - pages.toInt() > 0) (pages + 1).toInt() else pages.toInt()
    }
}