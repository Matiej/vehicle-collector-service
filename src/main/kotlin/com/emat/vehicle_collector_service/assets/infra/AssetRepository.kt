package com.emat.vehicle_collector_service.assets.infra

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface AssetRepository: ReactiveMongoRepository<AssetDocument, String> {
    fun findBySessionId(sessionId: String, pageRequest: Pageable): Flux<AssetDocument>
    fun findBySessionId(sessionId: String): Flux<AssetDocument>
    fun countAllBySessionId(sessionId: String): Mono<Long>
    fun findFirstBySessionIdOrderByCreatedAtDesc(sessionId: String): Mono<AssetDocument>
    fun findAllByOwnerId(ownerId: String, pageRequest: Pageable): Flux<AssetDocument>
}