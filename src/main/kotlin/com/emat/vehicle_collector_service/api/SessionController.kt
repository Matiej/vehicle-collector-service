package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.api.internal.InternalSessionController
import com.emat.vehicle_collector_service.api.internal.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.internal.dto.SessionResponse
import com.emat.vehicle_collector_service.api.internal.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.session.SessionService
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import io.swagger.v3.oas.annotations.Operation

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/public/sessions")
class SessionController(
    private val sessionService: SessionService
) {

    private val log = LoggerFactory.getLogger(InternalSessionController::class.java)

    @Operation(
        summary = "Public GET endpoint to list all sessions for given owner",
        description = "Fetches all available sessions, using pagination. Default values page=0, size=50"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Sessions successful retrieved",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @GetMapping
    fun listAllByOwner(
        @RequestParam ownerId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "DESC") sortDir: Sort.Direction
    ): Flux<SessionSummaryResponse> {
        log.info(
            "Received GET request '/api/internal/sessions' for page: {}, size: {} and owner {}",
            page, size, ownerId
        )
        return sessionService.listSessions(ownerId, page, size, sortDir)
    }

    @Operation(
        summary = "Public POST creating session",
        description = "Create session for frontend api, to get session numer and upload files"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "201",
            description = "Sessions successful created",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSession(
        @RequestBody createSessionRequest: CreateSessionRequest
    ): Mono<SessionResponse> {
        log.info(
            "Received POST request '/api/internal/sessions' to create session of the owner {}, mode: {}, device: {}",
            createSessionRequest.ownerId, createSessionRequest.mode, createSessionRequest.device
        )
        return sessionService.createSession(createSessionRequest)
    }

    @Operation(
        summary = "Public POST closing session",
        description = "Close session for frontend api when files upload finished"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "201",
            description = "Sessions successful created",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @PutMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun closeSession(
        @PathVariable() sessionId: String,
        @RequestParam(required = true) sessionStatus: SessionStatus
    ): Mono<SessionResponse> {
        log.info(
            "Received PUT request '/api/internal/sessions/{sessionId}' to change session status session to {}, for sessionId {}",
            sessionStatus.name, sessionId
        )
        return sessionService.changeSessionStatus(sessionId, sessionStatus)
    }


    @Operation(
        summary = "Public GET endpoint to get session by ID",
        description = "Fetches session by Id"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Sessions successful retrieved",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @GetMapping("/{sessionId}")
    fun get(@PathVariable sessionId: String): Mono<SessionResponse> {
        log.info("Received GET request '/api/internal/sessions/{sessionId}' for sessionId={}", sessionId)
        return sessionService.getSession(sessionId)
    }
}