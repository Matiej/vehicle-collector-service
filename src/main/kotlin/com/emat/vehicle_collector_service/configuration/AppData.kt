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
    @Value("\${app.tmp-assets-dir}")
    private lateinit var tmpDir: String
    @Value("\${app.assets-dir}")
    private lateinit var assetsDir: String
    @Value("\${app.max-file-size}")
    private lateinit var maxFileSize: String
    @Value("\${app.security.swagger-public:false}")
    private var swaggerPublic: Boolean = false
    @Value("\${app.security.swagger-token:}")
    private lateinit var swaggerTokenUrl: String
    @Value("\${app.security.swagger-url:}")
    private lateinit var swaggerUrl: String

    private fun now(): LocalDateTime {
        return LocalDateTime.now(clock)
    }

    fun getApplicationVersion(): String {
        return appVersion.plus(" on the day: ").plus(now().withSecond(0).withNano(0).toString())
    }

    fun getTmpDir(): String {
        return tmpDir
    }

    fun getAssetsDir(): String {
        return assetsDir
    }

    fun getMaxFileSize(): String {
        return maxFileSize
    }

    fun isSwaggerPublic(): Boolean = swaggerPublic
    fun getSwaggerTokenUrl(): String = swaggerTokenUrl
    fun getSwaggerUrl(): String = swaggerUrl
}