package com.emat.vehicle_collector_service.assets.geocoding

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.Place
import com.emat.vehicle_collector_service.infrastructure.geocoding.GeocodingProperties
import com.emat.vehicle_collector_service.infrastructure.geocoding.ResolvedPlace
import com.emat.vehicle_collector_service.infrastructure.geocoding.ReverseGeocoder
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class AssetGeocodingService(
    private val geocoder: ReverseGeocoder,
    private val properties: GeocodingProperties,
    private val reactiveMongoTemplate: ReactiveMongoTemplate
) {
    private val log = LoggerFactory.getLogger(AssetGeocodingService::class.java)

    fun geocodeAndSave(assetId: String, assetPublicId: String, gps: GeoPoint?): Mono<Void> {
        if (!properties.enabled || gps == null) {
            return Mono.empty()
        }
        return geocoder.reverse(gps)
            .flatMap { place -> savePlace(assetId, place, gps) }
            .doOnSuccess { log.info("Place resolved for asset={}", assetPublicId) }
            .onErrorResume { e ->
                log.warn("Geocoding skipped for asset={}: {}", assetPublicId, e.message)
                Mono.empty()
            }
            .then()
    }

    private fun savePlace(assetId: String, place: ResolvedPlace, gps: GeoPoint): Mono<*> {
        val update = Update().set(
            "capture.place",
            Place(
                countryCode = place.countryCode,
                country = place.country,
                city = place.city,
                region = place.region,
                geocodedAt = Instant.now(),
                geocodedFrom = gps
            )
        )
        val activeGps = Criteria().orOperator(
            Criteria.where("capture.gpsSource").`is`(GpsSource.USER)
                .and("capture.userGps.lat").`is`(gps.lat)
                .and("capture.userGps.lng").`is`(gps.lng),
            Criteria.where("capture.gpsSource").`is`(GpsSource.EXIF)
                .and("capture.exifGps.lat").`is`(gps.lat)
                .and("capture.exifGps.lng").`is`(gps.lng)
        )
        return reactiveMongoTemplate.updateFirst(
            Query.query(Criteria.where("_id").`is`(assetId).andOperator(activeGps)),
            update,
            AssetDocument::class.java
        )
    }
}
