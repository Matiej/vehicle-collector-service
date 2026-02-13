package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.api.dto.AssetResponse
import com.emat.vehicle_collector_service.api.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.dto.AssetsResponse
import com.emat.vehicle_collector_service.assets.AssetsService
import com.emat.vehicle_collector_service.assets.domain.AssetRequest
import com.emat.vehicle_collector_service.assets.domain.AssetType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/public")
@Validated
class AssetController(
    private val assetsService: AssetsService
) {
    private val log = LoggerFactory.getLogger(AssetController::class.java)

    @Operation(
        summary = "Public POST: upload asset to session",
        description = "Uploads a new asset bound to a given session (public ID)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Asset created"),
        ApiResponse(responseCode = "400", description = "Bad request"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    @PostMapping("/sessions/{sessionPublicId}/assets", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadAsset(
        @PathVariable(name = "sessionPublicId") sessionPublicId: String,
        @RequestPart("file") filePart: FilePart,
        @RequestParam("ownerId") ownerId: String,
        @RequestParam("type") type: AssetType
    ): Mono<AssetResponse> {
        log.info(
            "Received POST request '/api/public/sessions/{sessionPublicId}/assets' sessionPublicId: {}. ownerId: {}, type: {}, fileName: {}",
            sessionPublicId,
            ownerId,
            type.name,
            filePart.filename()
        )
        return assetsService.saveAsset(
            AssetRequest(
                sessionPublicId = sessionPublicId,
                filePart = filePart,
                ownerId = ownerId,
                assetType = type
            )
        )
    }

    @Operation(
        summary = "Public GET endpoint to return all assets",
        description = "Fetches all available assets for owner"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Assets successful retrieved",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @GetMapping("/assets/owner/{ownerId}")
    fun allAssetsByOwnerId(
        @PathVariable ownerId: String,
        @ModelAttribute query: AssetsOwnerQuery,
    ): Mono<AssetsResponse> {
        log.info(
            "Received GET '/api/public/assets/owner/{ownerId}' ownerId={}, status={}, type={}, hasSpot={}, page={}, size={}, sort={}",
            ownerId, query.status, query.type, query.hasSpot, query.page, query.size, query.sortDir
        )
        return assetsService.getAllAssetsByOwnerId(ownerId, query)

    }

    @Operation(
        summary = "Public GET endpoint to return all assets for session",
        description = "Fetches all available assets by sessionId"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Assets successful retrieved",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @GetMapping("/assets/session/{sessionPublicId}")
    fun allAssetsBySessionId(
        @PathVariable sessionPublicId: String,
        @ModelAttribute query: AssetsOwnerQuery,
    ): Mono<AssetsResponse> {
        log.info(
            "Received GET '/api/public/assets/session/{sessionId}' ownerId={}, status={}, type={}, hasSpot={}, page={}, size={}, sort={}",
            sessionPublicId, query.status, query.type, query.hasSpot, query.page, query.size, query.sortDir
        )
        return assetsService.getAllAssetsBySessionPublicId(sessionPublicId, query)
    }
}