package com.emat.vehicle_collector_service.assets.infra

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface AssetRepository: ReactiveMongoRepository<AssetDocument, String> {
    fun findAllBySessionPublicId(sessionPublicId: String, pageRequest: Pageable): Flux<AssetDocument>
    fun findAllBySessionPublicIdOrderByCreatedAtDesc(sessionId: String):  Flux<AssetDocument>
    fun countAllBySessionPublicId(sessionPublicId: String): Mono<Long>
    fun findFirstBySessionPublicIdOrderByCreatedAtDesc(sessionPublicId: String): Mono<AssetDocument>
    fun findByAssetPublicId(assetPublicId: String): Mono<AssetDocument>
}