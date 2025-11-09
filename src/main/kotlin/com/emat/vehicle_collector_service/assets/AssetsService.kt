package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.Thumbnail
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AssetsService {
    fun getAllAssets(type: AssetType?, hasSpot: Boolean?, status: AssetStatus?): Flux<Asset>
    fun deleteAsset(assetId: String): Mono<Void>
    fun saveAsset(assetRequest: AssetRequest): Mono<Asset>

    fun findBySessionId(sessionId: String): Flux<Asset>
    fun countAllBySessionId(sessionId: String): Mono<Long>
    fun findLastAssetThumbnail320BySessionId(sessionId: String): Mono<ThumbnailInfo>
}
