package com.emat.vehicle_collector_service.session

import com.emat.vehicle_collector_service.api.internal.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.internal.dto.SessionResponse
import com.emat.vehicle_collector_service.api.internal.dto.SessionSummaryResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface SessionService {
    fun createSession(req: CreateSessionRequest): Mono<SessionResponse>
    fun getSession(sessionId: String): Mono<SessionResponse>
    fun listSessions(ownerId: String, page: Int, size: Int): Flux<SessionSummaryResponse>
}