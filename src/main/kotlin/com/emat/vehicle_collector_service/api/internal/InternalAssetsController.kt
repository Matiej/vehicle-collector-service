package com.emat.vehicle_collector_service.api.internal

import com.emat.vehicle_collector_service.api.internal.dto.AssetResponse
import com.emat.vehicle_collector_service.api.internal.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.internal.dto.AssetsResponse
import com.emat.vehicle_collector_service.assets.AssetMapper
import com.emat.vehicle_collector_service.assets.AssetsService
import com.emat.vehicle_collector_service.assets.domain.AssetRequest
import com.emat.vehicle_collector_service.assets.domain.AssetStatus
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
@RequestMapping("/api/internal")
@Validated
class InternalAssetsController(
    private val assetsService: AssetsService
) {
    private val log = LoggerFactory.getLogger(InternalAssetsController::class.java)

    @Operation(
        summary = "Internal GET endpoint to return all assets",
        description = "Fetches all available assets"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Assets successful retrieved",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @GetMapping("/assets")
    fun allAssets(
        @ModelAttribute query: AssetsOwnerQuery,
    ): Mono<AssetsResponse> {
        log.info(
            "Received GET '/api/internal/assets'  status={}, type={}, hasSpot={}, page={}, size={}, sort={}",
            query.status, query.type, query.hasSpot, query.page, query.size, query.sortDir
        )
        return assetsService.getAllAssets(query)
    }

    @Operation(
        summary = "Internal GET endpoint to delete asset",
        description = "Delete asset by assetID. Internal endpoint for web usage, no jwt needed"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "202",
            description = "Asset deleted",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @DeleteMapping("/assets/{assetId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun deleteById(@PathVariable assetId: String): Mono<Void> {
        log.info("Received DELETE request '/api/internal/assets/{assetId}' for assetId: {}", assetId)
        return assetsService.deleteAsset(assetId)
    }

    @Operation(
        summary = "Internal POST endpoint to create asset",
        description = "Create asset for sessionId. Internal endpoint for web usage, no jwt needed"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "202",
            description = "Asset deleted",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @PostMapping("/sessions/{sessionId}/assets", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadAssetFromInternal(
        @PathVariable(name = "sessionId") sessionId: String,
        @RequestPart("file") filePart: FilePart,
        @RequestParam("ownerId") ownerId: String,
        @RequestParam("type") type: AssetType
    ): Mono<AssetResponse> {
        log.info(
            "Received POST request '/api/internal/sessions/{sessionId}/assets' sessionsId: {}. ownerId: {}, type: {}, fileName: {}",
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


}