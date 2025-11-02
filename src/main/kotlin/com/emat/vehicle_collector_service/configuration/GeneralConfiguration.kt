package com.emat.vehicle_collector_service.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeneralConfiguration {

    @Bean
    fun appData(): AppData {
        return AppData()
    }
}