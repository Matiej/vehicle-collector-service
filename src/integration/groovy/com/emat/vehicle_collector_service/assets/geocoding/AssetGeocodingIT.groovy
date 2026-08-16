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
