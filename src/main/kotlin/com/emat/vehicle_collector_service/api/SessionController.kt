package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.api.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.api.dto.SessionResponse
import com.emat.vehicle_collector_service.api.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.api.dto.SessionsQuery
import com.emat.vehicle_collector_service.session.SessionService
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/public/sessions")
@Validated
class SessionController(
    private val sessionService: SessionService
) {

    private val log = LoggerFactory.getLogger(SessionController::class.java)

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
        @AuthenticationPrincipal jwt: Jwt,
        @ModelAttribute @Valid query: SessionsQuery
    ): Mono<PageResponse<SessionSummaryResponse>> {
        val ownerId = jwt.subject
        log.info(
            "Received GET request '/api/public/sessions/' for page: {}, size: {} and owner {}",
            query.page, query.size, ownerId
        )
        return sessionService.listSessions(ownerId, query.page, query.size, query.sortDir)
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
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid createSessionRequest: CreateSessionRequest
    ): Mono<SessionResponse> {
        val ownerId = jwt.subject
        log.info(
            "Received POST request '/api/public/sessions' to create session of the owner {}, mode: {}, device: {}",
            ownerId, createSessionRequest.mode, createSessionRequest.device
        )
        return sessionService.createSession(createSessionRequest, ownerId)
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
    @PutMapping("/{sessionPublicId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun closeSession(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable() sessionPublicId: String,
        @RequestParam(required = true) sessionStatus: SessionStatus
    ): Mono<SessionResponse> {
        log.info(
            "Received PUT request '/api/public/sessions/{sessionPublicId}' to change session status session to {}, for sessionPublicId {}",
            sessionStatus.name, sessionPublicId
        )
        return sessionService.changeSessionStatus(sessionPublicId, jwt.subject, sessionStatus)
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
    @GetMapping("/{sessionPublicId}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionPublicId: String
    ): Mono<SessionResponse> {
        log.info("Received GET request '/api/public/sessions/{sessionPublicId}' for sessionPublicId={}", sessionPublicId)
        return sessionService.getSessionBySessionPublicId(sessionPublicId, jwt.subject)
    }
}