package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.internal.dto.AssetResponse
import com.emat.vehicle_collector_service.api.internal.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.api.internal.dto.AssetsResponse
import com.emat.vehicle_collector_service.assets.domain.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AssetsService {
    fun getAllAssets(assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse>
    fun getAllAssetsByOwnerId(ownerId: String, assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse>
    fun deleteAssetByPublicId(assetPublicId: String): Mono<Void>
    fun saveAsset(assetRequest: AssetRequest): Mono<AssetResponse>

    fun getAllAssetsBySessionPublicId(sessionPublicId: String, assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse>
    fun getAllAssetsBySessionPublicIdDescByCreatedAt(sessionPublicId: String): Flux<Asset>
    fun countAllBySessionPublicIdId(sessionPublicId: String): Mono<Long>
    fun findLastAssetThumbnail320BySessionPublicIdId(sessionPublicId: String): Mono<ThumbnailInfo>
}
