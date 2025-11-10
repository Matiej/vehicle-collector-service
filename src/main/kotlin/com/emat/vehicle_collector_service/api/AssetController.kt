package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.api.internal.dto.AssetResponse
import com.emat.vehicle_collector_service.api.internal.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.internal.dto.AssetsResponse
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
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/public")
class AssetController(
    private val assetsService: AssetsService
) {
    private val log = LoggerFactory.getLogger(AssetController::class.java)

    @Operation(
        summary = "Public POST endpoint to delete asset",
        description = "Create asset for specific sessionId. Public endpoint for PWA app using"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "202",
            description = "Asset deleted",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @PostMapping("/sessions/{sessionId}/assets", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadAsset(
        @PathVariable(name = "sessionId") sessionId: String,
        @RequestPart("file") filePart: FilePart,
        @RequestParam("ownerId") ownerId: String,
        @RequestParam("type") type: AssetType
    ): Mono<AssetResponse> {
        log.info(
            "Received POST request '/sessions/{sessionId}/assets' sessionsId: {}. ownerId: {}, type: {}, fileName: {}",
            sessionId,
            ownerId,
            type.name,
            filePart.filename()
        )
        return assetsService.saveAsset(
            AssetRequest(
                sessionId = sessionId,
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
            "Received GET '/api/public/assets/ownerId' ownerId={}, status={}, type={}, hasSpot={}, page={}, size={}, sort={}",
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
    @GetMapping("/assets/session/{sessionId}")
    fun allAssetsBySessionId(
        @PathVariable sessionId: String,
        @ModelAttribute query: AssetsOwnerQuery,
    ): Mono<AssetsResponse> {
        log.info(
            "Received GET '/api/public/assets/sessionId' ownerId={}, status={}, type={}, hasSpot={}, page={}, size={}, sort={}",
            sessionId, query.status, query.type, query.hasSpot, query.page, query.size, query.sortDir
        )
        return assetsService.getAllAssetsBySessionId(sessionId, query)
    }
}