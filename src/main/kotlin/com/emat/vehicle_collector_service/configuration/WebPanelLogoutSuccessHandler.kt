package com.emat.vehicle_collector_service.configuration

import com.emat.vehicle_collector_service.api.web.dto.WebLogoutResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@Component
class WebPanelLogoutSuccessHandler(
    private val webAuthProperties: WebAuthProperties,
    private val objectMapper: ObjectMapper
) : ServerLogoutSuccessHandler {

    override fun onLogoutSuccess(exchange: WebFilterExchange, authentication: Authentication): Mono<Void> {
        val response = exchange.exchange.response
        val user = authentication.principal as? OidcUser
        if (user == null) {
            response.statusCode = HttpStatus.UNAUTHORIZED
            return response.setComplete()
        }

        val body = objectMapper.writeValueAsBytes(WebLogoutResponse(keycloakLogoutUrl(user.idToken.tokenValue)))
        response.statusCode = HttpStatus.OK
        response.headers.contentType = MediaType.APPLICATION_JSON
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
    }

    private fun keycloakLogoutUrl(idToken: String): String =
        UriComponentsBuilder.fromUriString(webAuthProperties.publicRealmEndpoint("logout"))
            .queryParam("id_token_hint", idToken)
            .queryParam("post_logout_redirect_uri", webAuthProperties.panelUrl)
            .queryParam("client_id", webAuthProperties.clientId)
            .encode()
            .build()
            .toUriString()
}
