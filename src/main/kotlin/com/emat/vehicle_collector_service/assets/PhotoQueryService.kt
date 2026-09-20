package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.api.web.PhotoWebMapper
import com.emat.vehicle_collector_service.api.web.dto.PhotoListItemDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoListQuery
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.AssetRepository
import com.emat.vehicle_collector_service.configuration.AppData
import com.emat.vehicle_collector_service.infrastructure.error.ResourceNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.regex.Pattern

@Service
class PhotoQueryService(
    private val template: ReactiveMongoTemplate,
    private val assetRepository: AssetRepository,
    private val appData: AppData
) {

    fun listPhotos(ownerId: String, query: PhotoListQuery): Mono<PageResponse<PhotoListItemDto>> {
        val filter = Criteria().andOperator(*buildCriteria(ownerId, query).toTypedArray())
        val size = query.size.coerceAtMost(MAX_SIZE)
        val pageRequest = PageRequest.of(query.page, size, resolveSort(query.sort))

        val countQuery = Query(filter)
        val dataQuery = Query(filter).with(pageRequest)
        dataQuery.fields().exclude(
            "vehicleRecognition.candidates",
            "vehicleRecognition.metaData",
            "annotations.notes",
            "annotations.externalInfo"
        )

        return Mono.zip(
            template.count(countQuery, AssetDocument::class.java),
            template.find(dataQuery, AssetDocument::class.java)
                .map(PhotoWebMapper::toListItem)
                .collectList()
        ).map { tuple ->
            PageResponse.of(content = tuple.t2, page = query.page, size = size, totalElements = tuple.t1)
        }
    }

    fun findAssetForThumbnail(assetPublicId: String): Mono<AssetDocument> =
        assetRepository.findByAssetPublicId(assetPublicId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Photo not found: $assetPublicId")))

    private fun buildCriteria(ownerId: String, query: PhotoListQuery): List<Criteria> {
        val criteria = mutableListOf(Criteria.where("assetType").`is`(AssetType.IMAGE))

        if (appData.isWebOwnerFilterEnabled()) {
            criteria += Criteria.where("ownerId").`is`(ownerId)
        }

        query.badge?.let { criteria += badgeCriteria(it) }

        if (query.sessionPublicId != null) {
            criteria += Criteria.where("sessionPublicId").`is`(query.sessionPublicId)
        } else if (query.unassigned) {
            criteria += Criteria.where("sessionPublicId").`is`(null)
        }

        if (query.favorite) {
            criteria += Criteria.where("annotations.favorite").`is`(true)
        }

        query.q?.takeIf { it.isNotBlank() }?.let { criteria += searchCriteria(it) }

        return criteria
    }

    private fun badgeCriteria(rawBadge: String): Criteria {
        val badge = runCatching { RecognitionBadge.valueOf(rawBadge) }.getOrNull()
            ?: return Criteria.where("vehicleRecognition.badge").`is`(NO_SUCH_BADGE)
        return Criteria.where("vehicleRecognition.badge").`is`(badge)
    }

    private fun searchCriteria(q: String): Criteria {
        val pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE)
        return Criteria().orOperator(
            Criteria.where("annotations.title").regex(pattern),
            Criteria.where("file.originalFilename").regex(pattern),
            Criteria.where("vehicleRecognition.best.brand").regex(pattern),
            Criteria.where("vehicleRecognition.best.model").regex(pattern)
        )
    }

    private fun resolveSort(sort: String): Sort {
        val parts = sort.split(",")
        val mongoField = SORT_FIELDS[parts.getOrNull(0)]
            ?: return Sort.by(Sort.Direction.DESC, "createdAt")
        val direction = parts.getOrNull(1)
            ?.let { runCatching { Sort.Direction.fromString(it) }.getOrNull() }
            ?: Sort.Direction.DESC
        return Sort.by(direction, mongoField)
    }

    companion object {
        private const val MAX_SIZE = 100
        private const val NO_SUCH_BADGE = "__NO_SUCH_BADGE__"
        private val SORT_FIELDS = mapOf(
            "createdAt" to "createdAt",
            "takenAt" to "capture.takenAt",
            "title" to "annotations.title"
        )
    }
}
