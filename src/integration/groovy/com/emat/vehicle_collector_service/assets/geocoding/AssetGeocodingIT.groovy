package com.emat.vehicle_collector_service.assets.geocoding

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.infrastructure.geocoding.ResolvedPlace
import com.emat.vehicle_collector_service.infrastructure.geocoding.ReverseGeocoder
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import com.emat.vehicle_collector_service.support.PublicApiSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.MultiValueMap
import org.spockframework.spring.SpringBean
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import spock.util.concurrent.PollingConditions

import java.util.function.Supplier

class AssetGeocodingIT extends PublicApiSpec {

    static final GeoPoint KRAKOW = new GeoPoint(50.0614d, 19.9383d)
    static final GeoPoint WARSAW = new GeoPoint(52.2297d, 21.0122d)

    @SpringBean
    ReverseGeocoder geocoder = Mock()

    @Autowired
    AssetGeocodingService assetGeocodingService

    @DynamicPropertySource
    static void enableGeocoding(DynamicPropertyRegistry registry) {
        registry.add("app.geocoding.enabled", { "true" } as Supplier)
    }

    def "exif coordinates are turned into a place and stamped with their source"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null, [], new CaptureInfo(null, KRAKOW, null, GpsSource.EXIF, null, null))

        when:
        assetGeocodingService.geocodeAndSave(asset.id, asset.assetPublicId, asset.capture.activeGps()).block()

        then:
        1 * geocoder.reverse(KRAKOW) >> Mono.just(new ResolvedPlace("PL", "Polska", "Kraków", "małopolskie"))

        and:
        AssetDocument stored = assetRepository.findByAssetPublicId(asset.assetPublicId).block()
        stored.capture.place.countryCode == "PL"
        stored.capture.place.city == "Kraków"
        stored.capture.place.region == "małopolskie"
        stored.capture.place.geocodedAt != null
        stored.capture.place.geocodedFrom == KRAKOW
    }

    def "a user pin wins over exif, so the place is resolved from the user coordinates"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null, [], new CaptureInfo(null, KRAKOW, WARSAW, GpsSource.USER, null, null))

        when:
        assetGeocodingService.geocodeAndSave(asset.id, asset.assetPublicId, asset.capture.activeGps()).block()

        then:
        1 * geocoder.reverse(WARSAW) >> Mono.just(new ResolvedPlace("PL", "Polska", "Warszawa", "mazowieckie"))

        and:
        AssetDocument stored = assetRepository.findByAssetPublicId(asset.assetPublicId).block()
        stored.capture.place.city == "Warszawa"
        stored.capture.place.geocodedFrom == WARSAW
    }

    def "an asset without coordinates is never sent to the geocoder"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        when:
        assetGeocodingService.geocodeAndSave(asset.id, asset.assetPublicId, asset.capture.activeGps()).block()

        then:
        0 * geocoder.reverse(_)

        and:
        assetRepository.findByAssetPublicId(asset.assetPublicId).block().capture.place == null
    }

    def "a geocoder failure leaves the asset usable, just without a place"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null, [], new CaptureInfo(null, KRAKOW, null, GpsSource.EXIF, null, null))

        when:
        assetGeocodingService.geocodeAndSave(asset.id, asset.assetPublicId, asset.capture.activeGps()).block()

        then:
        1 * geocoder.reverse(KRAKOW) >> Mono.error(new RuntimeException("nominatim is down"))

        and:
        noExceptionThrown()
        assetRepository.findByAssetPublicId(asset.assetPublicId).block().capture.place == null
    }

    def "a provider that knows nothing about the spot leaves the place empty"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null, [], new CaptureInfo(null, KRAKOW, null, GpsSource.EXIF, null, null))

        when:
        assetGeocodingService.geocodeAndSave(asset.id, asset.assetPublicId, asset.capture.activeGps()).block()

        then:
        1 * geocoder.reverse(KRAKOW) >> Mono.empty()

        and:
        assetRepository.findByAssetPublicId(asset.assetPublicId).block().capture.place == null
    }

    def "location endpoint returns before geocoding and saves the resolved place later"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null, [], new CaptureInfo(null, KRAKOW, null, GpsSource.EXIF, null, null))
        Sinks.One<ResolvedPlace> result = Sinks.one()

        when:
        asUser(USER_A).put().uri("/api/public/assets/${asset.assetPublicId}/location")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([lat: WARSAW.lat, lng: WARSAW.lng])
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.capture.gps.lat').isEqualTo(WARSAW.lat)
                .jsonPath('$.capture.gps.lng').isEqualTo(WARSAW.lng)
                .jsonPath('$.capture.gpsSource').isEqualTo("USER")
                .jsonPath('$.capture.place').doesNotExist()

        then:
        1 * geocoder.reverse(WARSAW) >> result.asMono()

        and:
        assetRepository.findByAssetPublicId(asset.assetPublicId).block().capture.place == null

        when:
        result.tryEmitValue(new ResolvedPlace("PL", "Polska", "Warszawa", "mazowieckie"))

        then:
        new PollingConditions(timeout: 5).eventually {
            AssetDocument stored = assetRepository.findByAssetPublicId(asset.assetPublicId).block()
            assert stored.capture.place.city == "Warszawa"
            assert stored.capture.place.geocodedFrom == WARSAW
        }
    }

    def "a stale geocoding result cannot overwrite place for newer coordinates"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null, [], new CaptureInfo(null, KRAKOW, null, GpsSource.EXIF, null, null))
        Sinks.One<ResolvedPlace> staleResult = Sinks.one()
        1 * geocoder.reverse(KRAKOW) >> staleResult.asMono()
        assetGeocodingService.geocodeAndSave(asset.id, asset.assetPublicId, KRAKOW).subscribe()

        when:
        asUser(USER_A).put().uri("/api/public/assets/${asset.assetPublicId}/location")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([lat: WARSAW.lat, lng: WARSAW.lng])
                .exchange()
                .expectStatus().isOk()

        then:
        1 * geocoder.reverse(WARSAW) >> Mono.empty()

        when:
        staleResult.tryEmitValue(new ResolvedPlace("PL", "Polska", "Kraków", "małopolskie"))

        then:
        new PollingConditions(timeout: 5, initialDelay: 0.1).eventually {
            AssetDocument stored = assetRepository.findByAssetPublicId(asset.assetPublicId).block()
            assert stored.capture.gpsSource == GpsSource.USER
            assert stored.capture.userGps == WARSAW
            assert stored.capture.place == null
        }
    }

    def "uploading a photo without gps costs no geocoding request at all"() {
        given:
        SessionDocument session = givenSession(USER_A)

        when:
        asUser(USER_A).post()
                .uri("/api/public/sessions/${session.sessionPublicId}/assets?type=IMAGE")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(sampleImageMultipart())
                .exchange()
                .expectStatus().isCreated()

        then:
        0 * geocoder.reverse(_)

        and:
        new PollingConditions(timeout: 5).eventually {
            assert assetRepository.findAll().blockFirst().capture.place == null
        }
    }

    private static MultiValueMap sampleImageMultipart() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder()
        builder.part("file", new ClassPathResource("assets/sample.jpg")).contentType(MediaType.IMAGE_JPEG)
        builder.build()
    }
}
