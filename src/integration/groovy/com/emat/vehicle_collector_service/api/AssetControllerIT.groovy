package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import spock.util.concurrent.PollingConditions
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.util.MultiValueMap
import com.emat.vehicle_collector_service.support.PublicApiSpec

class AssetControllerIT extends PublicApiSpec {

    def "assets list returns only assets of the token owner"() {
        given:
        AssetDocument ownAsset = givenAsset(USER_A, null)
        givenAsset(USER_B, null)

        expect:
        asUser(USER_A).get().uri("/api/public/assets")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(ownAsset.assetPublicId)
                .jsonPath('$.content[0].ownerId').isEqualTo(USER_A)
    }

    def "session assets list returns own assets"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument asset = givenAsset(USER_A, session.sessionPublicId)

        expect:
        asUser(USER_A).get().uri("/api/public/assets/session/${session.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(asset.assetPublicId)
    }

    def "session assets list is empty for a session of another user"() {
        given:
        SessionDocument foreignSession = givenSession(USER_B)
        givenAsset(USER_B, foreignSession.sessionPublicId)

        expect:
        asUser(USER_A).get().uri("/api/public/assets/session/${foreignSession.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(0)
                .jsonPath('$.totalElements').isEqualTo(0)
                .jsonPath('$.totalPages').isEqualTo(0)
    }

    def "totalElements is the size of the whole set, not of the page"() {
        given:
        5.times { givenAsset(USER_A, null) }
        3.times { givenAsset(USER_B, null) }

        expect:
        asUser(USER_A).get().uri("/api/public/assets?page=${page}&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(expectedOnPage)
                .jsonPath('$.page').isEqualTo(page)
                .jsonPath('$.size').isEqualTo(2)
                .jsonPath('$.totalElements').isEqualTo(5)
                .jsonPath('$.totalPages').isEqualTo(3)

        where:
        page || expectedOnPage
        0    || 2
        1    || 2
        2    || 1
    }

    def "session assets list returns the same envelope as the owner list"() {
        given:
        SessionDocument session = givenSession(USER_A)
        3.times { givenAsset(USER_A, session.sessionPublicId) }
        givenAsset(USER_A, null)

        expect:
        asUser(USER_A).get().uri("/api/public/assets/session/${session.sessionPublicId}?page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(2)
                .jsonPath('$.page').isEqualTo(0)
                .jsonPath('$.size').isEqualTo(2)
                .jsonPath('$.totalElements').isEqualTo(3)
                .jsonPath('$.totalPages').isEqualTo(2)
    }

    def "assets list rejects size=0 with 400 instead of dividing by zero"() {
        expect:
        asUser(USER_A).get().uri("/api/public/assets?size=0")
                .exchange()
                .expectStatus().isBadRequest()
    }

    def "assets list rejects a negative page with 400"() {
        expect:
        asUser(USER_A).get().uri("/api/public/assets?page=-1")
                .exchange()
                .expectStatus().isBadRequest()
    }

    def "type filter narrows both the page and totalElements"() {
        given:
        3.times { givenAsset(USER_A, null) }

        expect:
        asUser(USER_A).get().uri("/api/public/assets?type=AUDIO")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(0)
                .jsonPath('$.totalElements').isEqualTo(0)
    }

    def "thumbnail of own asset is served"() {
        given:
        AssetDocument asset = givenAssetWithThumbnail(USER_A, null)

        expect:
        asUser(USER_A).get().uri("/api/public/assets/${asset.assetPublicId}/thumbnail?size=THUMB_320")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
    }

    def "thumbnail of another users asset returns 404"() {
        given:
        AssetDocument foreignAsset = givenAssetWithThumbnail(USER_B, null)

        expect:
        asUser(USER_A).get().uri("/api/public/assets/${foreignAsset.assetPublicId}/thumbnail?size=THUMB_320")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "upload to own session succeeds"() {
        given:
        SessionDocument session = givenSession(USER_A)

        expect:
        asUser(USER_A).post()
                .uri("/api/public/sessions/${session.sessionPublicId}/assets?type=IMAGE")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(sampleImageMultipart())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.ownerId').isEqualTo(USER_A)
                .jsonPath('$.sessionPublicId').isEqualTo(session.sessionPublicId)
                .jsonPath('$.file.originalFilename').isEqualTo("sample.jpg")
                .jsonPath('$.file.mimeType').isEqualTo("image/jpeg")
                .jsonPath('$.file.width').isEqualTo(2048)
                .jsonPath('$.file.height').isEqualTo(1536)
                .jsonPath('$.file.sizeBytes').isEqualTo(509768)
                .jsonPath('$.file.sha256')
                .isEqualTo("0153cfff78fc38bb7ec85879549f9c2bd4a6b3ff363c93927ad8cb85f1faabe6")
                .jsonPath('$.capture.gpsSource').isEqualTo("EXIF")
                .jsonPath('$.id').doesNotExist()
                .jsonPath('$.spotId').doesNotExist()
                .jsonPath('$.assetStatus').doesNotExist()
    }

    def "asset response exposes the active gps according to gpsSource"() {
        given:
        AssetDocument asset = givenAsset(
                USER_A,
                null,
                [],
                new CaptureInfo(null, new GeoPoint(50.0614d, 19.9383d), new GeoPoint(52.2297d, 21.0122d),
                        GpsSource.USER, null, null)
        )

        expect:
        asUser(USER_A).get().uri("/api/public/assets")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content[0].assetPublicId').isEqualTo(asset.assetPublicId)
                .jsonPath('$.content[0].capture.gps.lat').isEqualTo(52.2297d)
                .jsonPath('$.content[0].capture.gps.lng').isEqualTo(21.0122d)
                .jsonPath('$.content[0].capture.gpsSource').isEqualTo("USER")
    }

    def "generated thumbnails land in the file block"() {
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
        new PollingConditions(timeout: 10).eventually {
            AssetDocument stored = assetRepository.findAll().blockFirst()
            assert stored.file.thumbnails.size() == ThumbnailSize.values().length
            assert stored.file.thumbnails.every { it.storageKeyPath.startsWith("thumbnails/") }
            assert stored.file.status == AssetStatus.THUMBS_READY
            assert stored.file.failureReason == null
        }
    }

    def "uploaded audio stays UPLOADED because it has no thumbnails"() {
        given:
        SessionDocument session = givenSession(USER_A)

        when:
        asUser(USER_A).post()
                .uri("/api/public/sessions/${session.sessionPublicId}/assets?type=AUDIO")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(sampleAudioMultipart())
                .exchange()
                .expectStatus().isCreated()

        then:
        AssetDocument stored = assetRepository.findAll().blockFirst()
        stored.file.status == AssetStatus.UPLOADED
        stored.file.failureReason == null
        stored.file.thumbnails.isEmpty()
    }

    def "status is not a filter of the public assets list anymore"() {
        given:
        givenAsset(USER_A, null)

        expect:
        asUser(USER_A).get().uri("/api/public/assets?status=WHATEVER")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
    }

    def "upload to a session of another user returns 404 and stores nothing"() {
        given:
        SessionDocument foreignSession = givenSession(USER_B)

        when:
        asUser(USER_A).post()
                .uri("/api/public/sessions/${foreignSession.sessionPublicId}/assets?type=IMAGE")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(sampleImageMultipart())
                .exchange()
                .expectStatus().isNotFound()

        then:
        assetRepository.count().block() == 0L
    }

    private static MultiValueMap sampleImageMultipart() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder()
        builder.part("file", new ClassPathResource("assets/sample.jpg")).contentType(MediaType.IMAGE_JPEG)
        builder.build()
    }

    private static MultiValueMap sampleAudioMultipart() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder()
        builder.part("file", new ClassPathResource("assets/sample.mp3")).contentType(MediaType.parseMediaType("audio/mpeg"))
        builder.build()
    }
}
