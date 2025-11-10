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
    fun deleteAsset(assetId: String): Mono<Void>
    fun saveAsset(assetRequest: AssetRequest): Mono<AssetResponse>

    fun getAllAssetsBySessionId(sessionId: String, assetsOwnerQuery: AssetsOwnerQuery): Mono<AssetsResponse>
    fun getAllAssetsBySessionId(sessionId: String): Flux<Asset>
    fun countAllBySessionId(sessionId: String): Mono<Long>
    fun findLastAssetThumbnail320BySessionId(sessionId: String): Mono<ThumbnailInfo>
}
