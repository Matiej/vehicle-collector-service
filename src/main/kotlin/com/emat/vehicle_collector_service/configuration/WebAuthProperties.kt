package com.emat.vehicle_collector_service.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.web.auth")
data class WebAuthProperties(
    val panelUrl: String,
    val keycloakPublicUrl: String,
    val keycloakInternalUrl: String,
    val realm: String,
    val issuer: String,
    val clientId: String,
    val clientSecret: String
) {
    fun publicRealmEndpoint(path: String) = "$keycloakPublicUrl/realms/$realm/protocol/openid-connect/$path"

    fun internalRealmEndpoint(path: String) = "$keycloakInternalUrl/realms/$realm/protocol/openid-connect/$path"
}
