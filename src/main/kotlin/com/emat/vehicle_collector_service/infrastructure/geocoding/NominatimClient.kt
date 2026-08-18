package com.emat.vehicle_collector_service.infrastructure.geocoding

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@Component
class NominatimClient(
    private val properties: GeocodingProperties,
    webClientBuilder: WebClient.Builder
) : ReverseGeocoder {

    private val log = LoggerFactory.getLogger(NominatimClient::class.java)

    private val client: WebClient = webClientBuilder
        .baseUrl(properties.providerUrl)
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent)
        .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, properties.language)
        .build()

    private val resolved = ConcurrentHashMap<String, ResolvedPlace>()
    private val inFlight = ConcurrentHashMap<String, Mono<ResolvedPlace>>()

    private val minIntervalMs: Long =
        if (properties.rateLimitPerSecond <= 0) 0 else 1000L / properties.rateLimitPerSecond
    private val nextSlotAt = AtomicLong(0)
    private val geocodingScheduler = Schedulers.newSingle("geocoding")

    override fun reverse(gps: GeoPoint): Mono<ResolvedPlace> {
        if (!properties.enabled) {
            return Mono.empty()
        }
        val key = cacheKey(gps)
        resolved[key]?.let { return Mono.just(it) }

        return inFlight.computeIfAbsent(key) { cacheKey ->
            fetch(gps)
                .doOnNext { place -> remember(cacheKey, place) }
                .onErrorResume { e ->
                    log.warn("Reverse geocoding failed for {}: {}", cacheKey, e.message)
                    Mono.empty()
                }
                .doFinally { inFlight.remove(cacheKey) }
                .cache()
        }
    }

    private fun fetch(gps: GeoPoint): Mono<ResolvedPlace> =
        Mono.defer {
            client.get()
                .uri { builder ->
                    builder
                        .queryParam("format", "jsonv2")
                        .queryParam("lat", gps.lat)
                        .queryParam("lon", gps.lng)
                        .build()
                }
                .retrieve()
                .bodyToMono(NominatimResponse::class.java)
                .delaySubscription(reserveSlot(), geocodingScheduler)
        }
            .timeout(Duration.ofMillis(properties.timeoutMs), geocodingScheduler)
            .retryWhen(
                Retry.backoff(properties.maxRetries, Duration.ofMillis(properties.retryBackoffMs))
                    .scheduler(geocodingScheduler)
                    .transientErrors(true)
            )
            .flatMap { response -> Mono.justOrEmpty(response.address?.toResolvedPlace()) }
            .filter { place -> !place.isEmpty() }

    private fun reserveSlot(): Duration {
        if (minIntervalMs == 0L) {
            return Duration.ZERO
        }
        val slot = nextSlotAt.updateAndGet { previous ->
            max(System.currentTimeMillis(), previous + minIntervalMs)
        }
        val wait = slot - System.currentTimeMillis()
        return if (wait > 0) Duration.ofMillis(wait) else Duration.ZERO
    }

    private fun remember(key: String, place: ResolvedPlace) {
        if (resolved.size < properties.cacheMaxEntries) {
            resolved[key] = place
        }
    }

    fun cacheKey(gps: GeoPoint): String {
        val format = "%.${properties.cachePrecision}f"
        return String.format(Locale.ROOT, "$format,$format", gps.lat, gps.lng)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class NominatimResponse(
        val address: NominatimAddress? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class NominatimAddress(
        @JsonProperty("country_code") val countryCode: String? = null,
        val country: String? = null,
        val city: String? = null,
        val town: String? = null,
        val village: String? = null,
        val municipality: String? = null,
        val state: String? = null
    ) {
        fun toResolvedPlace(): ResolvedPlace =
            ResolvedPlace(
                countryCode = countryCode?.uppercase(Locale.ROOT),
                country = country,
                city = city ?: town ?: village ?: municipality,
                region = state
            )
    }
}
