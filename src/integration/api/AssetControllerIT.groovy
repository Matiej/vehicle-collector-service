package api

import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.util.MultiValueMap
import support.PublicApiSpec

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
                .jsonPath('$.assets.length()').isEqualTo(1)
                .jsonPath('$.assets[0].assetPublicId').isEqualTo(ownAsset.assetPublicId)
                .jsonPath('$.assets[0].ownerId').isEqualTo(USER_A)
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
                .jsonPath('$.assets.length()').isEqualTo(1)
                .jsonPath('$.assets[0].assetPublicId').isEqualTo(asset.assetPublicId)
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
                .jsonPath('$.assets.length()').isEqualTo(0)
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
}
