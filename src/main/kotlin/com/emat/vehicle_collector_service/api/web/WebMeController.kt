package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.web.dto.WebMeResponse
import com.emat.vehicle_collector_service.configuration.RealmRoles
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/web")
class WebMeController {

    @Operation(
        summary = "Web GET: current user",
        description = "Returns the identity and application roles of the user logged in to the web panel."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Current user returned"),
        ApiResponse(responseCode = "401", description = "No panel session")
    )
    @GetMapping("/me")
    fun me(): Mono<WebMeResponse> =
        ReactiveSecurityContextHolder.getContext()
            .mapNotNull { context -> context.authentication }
            .filter { authentication -> authentication.principal is OidcUser }
            .map { authentication -> toResponse(authentication) }
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED, "No panel session")))

    private fun toResponse(authentication: Authentication): WebMeResponse {
        val user = authentication.principal as OidcUser
        val username = user.preferredUsername ?: user.subject
        return WebMeResponse(
            userId = user.subject,
            username = username,
            displayName = user.fullName ?: username,
            roles = RealmRoles.applicationRoles(authentication.authorities)
        )
    }
}
