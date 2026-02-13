package com.emat.vehicle_collector_service.api.internal

import com.emat.vehicle_collector_service.api.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.session.SessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/internal")
class InternalSessionController(
    private val sessionService: SessionService
) {

    private val log = LoggerFactory.getLogger(InternalSessionController::class.java)

    @Operation(
        summary = "Internal GET: list sessions",
        description = "Lists sessions across all owners with pagination (default page=0, size=50)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sessions successfully  retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @GetMapping("/sessions")
    fun listAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "DESC") sortDir: Sort.Direction
    ): Flux<SessionSummaryResponse> {
        log.info(
            "Received GET request '/api/internal/sessions' for page: {}, size: {}, sort={}.",
            page, size, sortDir
        )
        return sessionService.listSessions(page, size, sortDir)
    }

}