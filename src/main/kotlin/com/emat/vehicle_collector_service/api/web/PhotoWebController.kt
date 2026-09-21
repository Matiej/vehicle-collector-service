package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.api.web.dto.PhotoDetailResponse
import com.emat.vehicle_collector_service.api.web.dto.PhotoListItemResponse
import com.emat.vehicle_collector_service.api.web.dto.PhotoListQuery
import com.emat.vehicle_collector_service.assets.PhotoQueryService
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.thumbnail.ThumbnailFileResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

    @Operation(
        summary = "Web GET: list photos",
        description = "Returns a paginated and filtered list of photos for the web panel."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Photos retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    @GetMapping("/photos")
    fun listPhotos(@ModelAttribute @Valid query: PhotoListQuery): Mono<PageResponse<PhotoListItemResponse>> {
        val ownerId = query.ownerId ?: LOCAL_OWNER_PLACEHOLDER
        log.info(
            "Received GET request for web photos, ownerId={}, page={}, size={}, badge={}, sort={}",
            ownerId, query.page, query.size, query.badge, query.sort
        )
        return photoQueryService.listPhotos(ownerId, query)
    }

    @Operation(
        summary = "Web GET: photo details",
        description = "Returns the complete photo details used by the web panel."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Photo found"),
        ApiResponse(responseCode = "404", description = "Photo not found for owner"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    @GetMapping("/photos/{assetPublicId}")
    fun getPhoto(
        @PathVariable assetPublicId: String,
        @RequestParam(required = false) ownerId: String?
    ): Mono<PhotoDetailResponse> {
        val resolvedOwnerId = ownerId ?: LOCAL_OWNER_PLACEHOLDER
        log.info(
            "Received GET request for web photo detail, ownerId={}, assetPublicId={}",
            resolvedOwnerId,
            assetPublicId
        )
        return photoQueryService.getPhotoDetail(resolvedOwnerId, assetPublicId)
    }

    @Operation(
        summary = "Web GET: serve photo thumbnail",
        description = "Returns a JPG thumbnail for the requested photo and supported pixel size."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Thumbnail served"),
        ApiResponse(responseCode = "404", description = "Photo, thumbnail, or size not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
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

    @Operation(
        summary = "Web GET: serve photo preview",
        description = "Returns the 640px preview or a browser-safe original when the preview is not ready."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Preview served"),
        ApiResponse(responseCode = "404", description = "Photo or renderable preview not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    @GetMapping("/assets/{assetPublicId}/preview")
    fun getPreview(@PathVariable assetPublicId: String): Mono<ResponseEntity<Resource>> =
        photoQueryService.findAssetForThumbnail(assetPublicId)
            .map { asset -> thumbnailFileResolver.servePreview(asset) }

    companion object {
        private const val LOCAL_OWNER_PLACEHOLDER = "local-web-owner"
    }
}
