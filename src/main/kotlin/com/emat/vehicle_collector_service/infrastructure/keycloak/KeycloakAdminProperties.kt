package com.emat.vehicle_collector_service.infrastructure.keycloak

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "keycloak.admin")
data class KeycloakAdminProperties(
    val baseUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String,
    val frontendClientId: String,
    val roles: Map<String, String>
)
