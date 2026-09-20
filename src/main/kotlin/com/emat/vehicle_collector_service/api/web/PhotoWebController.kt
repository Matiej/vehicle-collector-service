package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.api.web.dto.PhotoDetailDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoListItemDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoListQuery
import com.emat.vehicle_collector_service.assets.PhotoQueryService
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.thumbnail.ThumbnailFileResolver
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/web")
@Validated
class PhotoWebController(
    private val photoQueryService: PhotoQueryService,
    private val thumbnailFileResolver: ThumbnailFileResolver
) {

    private val log = LoggerFactory.getLogger(PhotoWebController::class.java)

    @GetMapping("/photos")
    fun listPhotos(@ModelAttribute @Valid query: PhotoListQuery): Mono<PageResponse<PhotoListItemDto>> {
        val ownerId = query.ownerId ?: LOCAL_OWNER_PLACEHOLDER
        log.info(
            "Received GET request for web photos, ownerId={}, page={}, size={}, badge={}, sort={}",
            ownerId, query.page, query.size, query.badge, query.sort
        )
        return photoQueryService.listPhotos(ownerId, query)
    }

    @GetMapping("/photos/{assetPublicId}")
    fun getPhoto(
        @PathVariable assetPublicId: String,
        @RequestParam(required = false) ownerId: String?
    ): Mono<PhotoDetailDto> {
        val resolvedOwnerId = ownerId ?: LOCAL_OWNER_PLACEHOLDER
        log.info("Received GET request for web photo detail, ownerId={}, assetPublicId={}", resolvedOwnerId, assetPublicId)
        return photoQueryService.getPhotoDetail(resolvedOwnerId, assetPublicId)
    }

    @GetMapping("/assets/{assetPublicId}/thumb/{size}")
    fun getThumbnail(
        @PathVariable assetPublicId: String,
        @PathVariable size: Int
    ): Mono<ResponseEntity<Resource>> {
        val thumbnailSize = ThumbnailSize.entries.firstOrNull { it.maxDimension == size }
            ?: return Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown thumbnail size: $size"))

        return photoQueryService.findAssetForThumbnail(assetPublicId)
            .map { asset -> thumbnailFileResolver.serve(asset, thumbnailSize) }
    }

    @GetMapping("/assets/{assetPublicId}/preview")
    fun getPreview(@PathVariable assetPublicId: String): Mono<ResponseEntity<Resource>> =
        photoQueryService.findAssetForThumbnail(assetPublicId)
            .map { asset -> thumbnailFileResolver.servePreview(asset) }

    companion object {
        private const val LOCAL_OWNER_PLACEHOLDER = "local-web-owner"
    }
}
