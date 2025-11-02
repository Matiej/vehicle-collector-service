package com.emat.vehicle_collector_service.assets.infra

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetRepository: ReactiveMongoRepository<AssetDocument, String> {
}