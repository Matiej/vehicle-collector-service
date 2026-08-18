package com.emat.vehicle_collector_service.infrastructure.geocoding

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import reactor.core.publisher.Mono

interface ReverseGeocoder {
    fun reverse(gps: GeoPoint): Mono<ResolvedPlace>
}

data class ResolvedPlace(
    val countryCode: String?,
    val country: String?,
    val city: String?,
    val region: String?
) {
    fun isEmpty(): Boolean = countryCode == null && country == null && city == null && region == null
}
