package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.assets.domain.Asset
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface AssetRepository: ReactiveMongoRepository<AssetDocument, String> {
    fun findBySessionId(sessionId: String): Flux<AssetDocument>
    fun countAllBySessionId(sessionId: String): Mono<Int>
    fun findFirstBySessionIdOrderByCreatedAtAsc(sessionId: String): Mono<AssetDocument>
}