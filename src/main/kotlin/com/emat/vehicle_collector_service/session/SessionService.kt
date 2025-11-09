package com.emat.vehicle_collector_service.session

import com.emat.vehicle_collector_service.api.internal.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.internal.dto.SessionResponse
import com.emat.vehicle_collector_service.api.internal.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface SessionService {
    fun createSession(req: CreateSessionRequest): Mono<SessionResponse>
    fun getSession(sessionId: String): Mono<SessionResponse>
    fun listSessions(ownerId: String, page: Int, size: Int, sort: Sort.Direction): Flux<SessionSummaryResponse>
    fun listSessions(page: Int, size: Int, sort: Sort.Direction): Flux<SessionSummaryResponse>
    fun changeSessionStatus(sessionId: String, sessionStatus: SessionStatus): Mono<SessionResponse>
}