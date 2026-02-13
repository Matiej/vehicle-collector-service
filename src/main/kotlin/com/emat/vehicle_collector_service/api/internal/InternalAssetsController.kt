package com.emat.vehicle_collector_service.api.internal

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
@RequestMapping("/api/internal")
@Validated
class InternalAssetsController(
    private val assetsService: AssetsService
) {
    private val log = LoggerFactory.getLogger(InternalAssetsController::class.java)

    @Operation(
        summary = "Internal GET: list assets",
        description = "Lists assets with filtering and pagination (internal tooling)."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Assets successfully retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
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
        summary = "Internal DELETE: delete asset",
        description = "Deletes asset by assetPublicId (no JWT for internal tools, secure by network)."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Asset deleted"),
            ApiResponse(responseCode = "404", description = "Asset not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @DeleteMapping("/assets/{assetPublicId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun deleteById(@PathVariable assetPublicId: String): Mono<Void> {
        log.info("Received DELETE request '/api/internal/assets/{assetPublicId}' for assetPublicId: {}", assetPublicId)
        return assetsService.deleteAssetByPublicId(assetPublicId)
    }

    @Operation(
        summary = "Internal POST: upload asset to session",
        description = "Creates an asset bound to sessionPublicId (multipart)"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Asset created"),
        ApiResponse(responseCode = "400", description = "Bad request"),
        ApiResponse(responseCode = "404", description = "Session not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    @PostMapping("/sessions/{sessionPublicId}/assets", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadAssetFromInternal(
        @PathVariable(name = "sessionPublicId") sessionPublicId: String,
        @RequestPart("file") filePart: FilePart,
        @RequestParam("ownerId") ownerId: String,
        @RequestParam("type") type: AssetType
    ): Mono<AssetResponse> {
        log.info(
            "Received POST request '/api/internal/sessions/{sessionPublicId}/assets' sessionPublicId: {}. ownerId: {}, type: {}, fileName: {}",
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


}