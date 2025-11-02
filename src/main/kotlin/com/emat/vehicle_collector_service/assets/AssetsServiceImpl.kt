package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.Asset
import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.infra.AssetRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AssetsServiceImpl(
    val assetRepository: AssetRepository
) : AssetsService {
    override fun getAllAssets(type: AssetType?, hasSpot: Boolean?, status: AssetStatus?): Flux<Asset> {
        //todo when more assets filter on db level
        return assetRepository.findAll()
            .filter { asset ->
                val typeOk = type?.let { asset.assetType == it } ?: true
                val hasSpotOk = hasSpot?.let { asset.spotId != null } ?: true
                val statusOk = status?.let { asset.assetStatus == status } ?: true
                typeOk && hasSpotOk && statusOk
            }
            .map { AssetMapper.toDomain(it) }
    }

    override fun deleteAsset(assetId: String): Mono<Void> {
        return assetRepository.deleteById(assetId)
    }
}