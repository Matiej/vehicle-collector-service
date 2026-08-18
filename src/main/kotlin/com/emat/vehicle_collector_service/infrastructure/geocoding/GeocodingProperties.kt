package com.emat.vehicle_collector_service.infrastructure.geocoding

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.geocoding")
data class GeocodingProperties(
    val enabled: Boolean = false,
    val providerUrl: String = "https://nominatim.openstreetmap.org/reverse",
    val userAgent: String = "vehicle-collector-service/1.0",
    val language: String = "pl",
    val rateLimitPerSecond: Int = 1,
    val cachePrecision: Int = 3,
    val timeoutMs: Long = 5000,
    val maxRetries: Long = 2,
    val retryBackoffMs: Long = 2000,
    val cacheMaxEntries: Int = 10_000
)
