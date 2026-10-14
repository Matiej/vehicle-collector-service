package com.emat.vehicle_collector_service.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.web.auth")
data class WebAuthProperties(
    val panelUrl: String,
    val keycloakPublicUrl: String,
    val keycloakInternalUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String
)
