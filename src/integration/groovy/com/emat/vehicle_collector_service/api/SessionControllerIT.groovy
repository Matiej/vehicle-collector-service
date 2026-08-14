package com.emat.vehicle_collector_service.api

import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import org.springframework.http.MediaType
import com.emat.vehicle_collector_service.support.PublicApiSpec

class SessionControllerIT extends PublicApiSpec {

    def "sessions list returns only sessions of the token owner"() {
        given:
        SessionDocument ownSession = givenSession(USER_A)
        givenSession(USER_B)

        expect:
        asUser(USER_A).get().uri("/api/public/sessions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(1)
                .jsonPath('$[0].sessionPublicId').isEqualTo(ownSession.sessionPublicId)
                .jsonPath('$[0].ownerId').isEqualTo(USER_A)
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
