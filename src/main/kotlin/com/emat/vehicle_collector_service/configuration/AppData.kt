package com.emat.vehicle_collector_service.configuration

import org.springframework.beans.factory.annotation.Value
import java.time.Clock
import java.time.LocalDateTime

class AppData {
    var clock: Clock = Clock.systemDefaultZone()
        set(value) {
            field = value
        }

    @Value("\${app.version}")
    private lateinit var appVersion: String

    private fun now(): LocalDateTime {
        return LocalDateTime.now(clock)
    }

    fun getApplicationVersion(): String {
        return appVersion.plus(" on the day: ").plus(now().withSecond(0).withNano(0).toString())
    }
}