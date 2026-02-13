package com.emat.vehicle_collector_service.infrastructure.keycloak

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

@Component
class KeycloakTokenProvider(
    private val keycloakAdminProperties: KeycloakAdminProperties,
    private val webclientBuilder: WebClient.Builder
) {
    private val log = LoggerFactory.getLogger(KeycloakTokenProvider::class.java)

    fun getToken() =
        webclientBuilder.build()
            .post()
            .uri(keycloakAdminProperties.baseUrl + "/realms/" + keycloakAdminProperties.realm + "/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("grant_type", "client_credentials")
                    .with("client_id", keycloakAdminProperties.clientId)
                    .with("client_secret", keycloakAdminProperties.clientSecret)
            )
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { json -> json.get("access_token").asText() }
            .doOnSuccess { log.info("Token retrieved successful") }
}
