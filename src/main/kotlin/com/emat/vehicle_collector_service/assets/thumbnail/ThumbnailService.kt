package com.emat.vehicle_collector_service.assets.thumbnail

import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.Thumbnail
import com.emat.vehicle_collector_service.configuration.AppData
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Path

@Service
class ThumbnailService(
    private val generator: ThumbnailGenerator,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val appData: AppData
) {
    private val log = LoggerFactory.getLogger(ThumbnailService::class.java)
    private val thumbnailScheduler = Schedulers.newBoundedElastic(2, 100, "thumbnail")

    fun generateAndSave(assetId: String, assetPublicId: String, originalStorageKeyPath: String): Mono<Void> {
        return Mono.fromCallable {
            val assetsDir = Path.of(appData.getAssetsDir())
            val originalPath = assetsDir.resolve(originalStorageKeyPath)

            ThumbnailSize.entries.map { size ->
                val relativePath = "thumbnails/${assetPublicId}_${size.name.lowercase()}.jpg"
                val outputPath = assetsDir.resolve(relativePath)
                generator.generate(originalPath, outputPath, size.maxDimension)
                log.debug("Generated {} for asset {}: {}", size.name, assetPublicId, outputPath)
                Thumbnail(size = size, storageKeyPath = relativePath)
            }
        }
            .subscribeOn(thumbnailScheduler)
            .flatMap { thumbnails -> updateAssetThumbnails(assetId, thumbnails) }
            .doOnSuccess { log.info("Thumbnails READY for asset={}", assetPublicId) }
            .doOnError { e -> log.error("Thumbnail FAILED for asset={}: {}", assetPublicId, e.message, e) }
            .then()
    }

    private fun updateAssetThumbnails(assetId: String, thumbnails: List<Thumbnail>): Mono<com.mongodb.client.result.UpdateResult> {
        val update = Update().set("thumbnails", thumbnails)
        return reactiveMongoTemplate.updateFirst(
            Query.query(Criteria.where("_id").`is`(assetId)),
            update,
            AssetDocument::class.java
        )
    }
}
