package com.emat.vehicle_collector_service.session.infra

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface SessionRepository: ReactiveMongoRepository<SessionDocument, String> {
    fun findByOwnerId(ownerId: String): Flux<SessionDocument>
    fun findByOwnerId(ownerId: String, pageRequest: PageRequest): Flux<SessionDocument>
    fun findAllBy(pageRequest: Pageable): Flux<SessionDocument>
}