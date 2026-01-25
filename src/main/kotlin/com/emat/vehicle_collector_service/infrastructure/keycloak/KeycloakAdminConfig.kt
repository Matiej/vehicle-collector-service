package com.emat.vehicle_collector_service.infrastructure.keycloak

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties::class)
class KeycloakAdminConfig {

    @Bean
    fun keycloakAdminWebClient(properties: KeycloakAdminProperties) =
        WebClient.builder()
            .baseUrl(properties.baseUrl + "/admin/realms/" + properties.realm)
            .build()
}