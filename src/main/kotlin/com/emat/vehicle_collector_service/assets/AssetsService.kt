package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.dto.AssetResponse
import com.emat.vehicle_collector_service.api.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AssetsService {
    fun findByPublicId(assetPublicId: String, ownerId: String): Mono<AssetDocument>
    fun getAllAssets(assetsOwnerQuery: AssetsOwnerQuery): Mono<PageResponse<AssetResponse>>
    fun getAllAssetsByOwnerId(ownerId: String, assetsOwnerQuery: AssetsOwnerQuery): Mono<PageResponse<AssetResponse>>
    fun deleteAssetByPublicId(assetPublicId: String): Mono<Void>
    fun saveAsset(assetRequest: AssetRequest): Mono<AssetResponse>

    fun getAllAssetsBySessionPublicId(
        sessionPublicId: String,
        ownerId: String,
        assetsOwnerQuery: AssetsOwnerQuery
    ): Mono<PageResponse<AssetResponse>>
    fun getAllAssetsBySessionPublicIdDescByCreatedAt(sessionPublicId: String): Flux<Asset>
    fun countAllBySessionPublicId(sessionPublicId: String): Mono<Long>
    fun findLastAssetThumbnail320BySessionPublicId(sessionPublicId: String): Mono<ThumbnailInfo>
}
