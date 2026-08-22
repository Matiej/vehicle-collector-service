package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import org.springframework.http.MediaType
import com.emat.vehicle_collector_service.support.PublicApiSpec

class SessionControllerIT extends PublicApiSpec {

    def "sessions list returns only sessions of the token owner, wrapped in a page envelope"() {
        given:
        SessionDocument ownSession = givenSession(USER_A)
        givenSession(USER_B)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].sessionPublicId').isEqualTo(ownSession.sessionPublicId)
                .jsonPath('$.content[0].ownerId').isEqualTo(USER_A)
                .jsonPath('$.page').isEqualTo(0)
                .jsonPath('$.size').isEqualTo(50)
                .jsonPath('$.totalElements').isEqualTo(1)
                .jsonPath('$.totalPages').isEqualTo(1)
    }

    def "sessions totalElements counts only own sessions, not the whole page"() {
        given:
        5.times { givenSession(USER_A) }
        3.times { givenSession(USER_B) }

        expect:
        asUser(USER_A).get().uri("/api/public/sessions?page=1&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(2)
                .jsonPath('$.totalElements').isEqualTo(5)
                .jsonPath('$.totalPages').isEqualTo(3)
                .jsonPath('$.page').isEqualTo(1)
    }

    def "sessions list rejects size=0 with 400 instead of dividing by zero"() {
        expect:
        asUser(USER_A).get().uri("/api/public/sessions?size=0")
                .exchange()
                .expectStatus().isBadRequest()
    }

    def "getting own session succeeds"() {
        given:
        SessionDocument session = givenSession(USER_A)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions/${session.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.sessionPublicId').isEqualTo(session.sessionPublicId)
    }

    def "getting a session of another user returns 404 and never 403"() {
        given:
        SessionDocument foreignSession = givenSession(USER_B)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions/${foreignSession.sessionPublicId}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath('$.status').isEqualTo(404)
                .jsonPath('$.code').isEqualTo("NOT_FOUND")
    }

    def "closing a session of another user returns 404 and leaves its status untouched"() {
        given:
        SessionDocument foreignSession = givenSession(USER_B, SessionStatus.CREATED)

        when:
        asUser(USER_A).put()
                .uri("/api/public/sessions/${foreignSession.sessionPublicId}?sessionStatus=CLOSED")
                .exchange()
                .expectStatus().isNotFound()

        then:
        sessionRepository.findBySessionPublicIdAndOwnerId(foreignSession.sessionPublicId, USER_B)
                .block().status == SessionStatus.CREATED
    }

    def "closing own session succeeds"() {
        given:
        SessionDocument session = givenSession(USER_A, SessionStatus.CREATED)

        expect:
        asUser(USER_A).put()
                .uri("/api/public/sessions/${session.sessionPublicId}?sessionStatus=CLOSED")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.sessionStatus').isEqualTo("CLOSED")
    }

    def "created session takes its owner from the token"() {
        expect:
        asUser(USER_A).post().uri("/api/public/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([mode: "BULK", device: "phone", sessionName: "test", clientVersion: "1.0"])
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.ownerId').isEqualTo(USER_A)
    }

    def "session assets expose assetPublicId instead of the internal Mongo id"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument asset = givenAsset(USER_A, session.sessionPublicId)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions/${session.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.assets.length()').isEqualTo(1)
                .jsonPath('$.assets[0].assetPublicId').isEqualTo(asset.assetPublicId)
                .jsonPath('$.assets[0].id').doesNotExist()
    }

    def "session list cover thumbnail is a servable URL, not a raw storage key"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument asset = givenAssetWithThumbnail(USER_A, session.sessionPublicId)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content[0].coverThumbnailUrl')
                .isEqualTo("/api/public/assets/${asset.assetPublicId}/thumbnail?size=THUMB_320".toString())
    }

    def "session list cover thumbnail is null when no asset has a thumbnail yet"() {
        given:
        SessionDocument session = givenSession(USER_A)
        givenAsset(USER_A, session.sessionPublicId)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content[0].coverThumbnailUrl').doesNotExist()
    }

    def "ownerId smuggled in the create session body is ignored"() {
        expect:
        asUser(USER_A).post().uri("/api/public/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([mode: "BULK", device: "phone", sessionName: "test", clientVersion: "1.0", ownerId: USER_B])
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.ownerId').isEqualTo(USER_A)
    }
}
