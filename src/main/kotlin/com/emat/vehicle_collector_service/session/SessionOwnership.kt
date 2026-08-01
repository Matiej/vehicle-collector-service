package com.emat.vehicle_collector_service.session

import com.emat.vehicle_collector_service.infrastructure.error.ResourceNotFoundException
import com.emat.vehicle_collector_service.session.infra.SessionRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class SessionOwnership(
    private val sessionRepository: SessionRepository
) {

    fun requireOwned(sessionPublicId: String, ownerId: String): Mono<Void> =
        sessionRepository.existsBySessionPublicIdAndOwnerId(sessionPublicId, ownerId)
            .filter { owned -> owned }
            .switchIfEmpty(
                Mono.error(
                    ResourceNotFoundException("Session $sessionPublicId not found for owner $ownerId")
                )
            )
            .then()
}
