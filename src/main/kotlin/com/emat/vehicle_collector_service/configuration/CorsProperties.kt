package com.emat.vehicle_collector_service.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security.cors")
data class CorsProperties(
    var allowedOriginPatterns: List<String> = listOf("http://localhost:*"),
    var allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
    var allowedHeaders: List<String> = listOf("*"),
    var exposedHeaders: List<String> = listOf("Location", "Content-Disposition")

) {
}