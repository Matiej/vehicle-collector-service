package com.emat.vehicle_collector_service.configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource


@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class GeneralConfiguration {

    @Bean
    fun appData(): AppData {
        return AppData()
    }

    @Bean
    fun corsWebFilter(corsProperties: CorsProperties): CorsWebFilter {
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.allowedOriginPatterns = corsProperties.allowedOriginPatterns
        config.allowedMethods = corsProperties.allowedMethods
        config.allowedHeaders = corsProperties.allowedHeaders
        config.exposedHeaders = corsProperties.exposedHeaders

        val source: UrlBasedCorsConfigurationSource = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsWebFilter(source)
    }
}