package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.Asset
import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AssetsService {
    fun getAllAssets(type: AssetType?, hasSpot: Boolean?, status: AssetStatus?): Flux<Asset>
    fun deleteAsset(assetId: String): Mono<Void>
}
