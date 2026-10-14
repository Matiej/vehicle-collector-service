package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.support.PublicApiSpec
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin

@TestPropertySource(properties = ["app.security.web-panel-permit-all=false"])
class WebPanelAuthIT extends PublicApiSpec {

    def "web endpoint without a session answers 401 instead of redirecting to Keycloak: #path"() {
        expect:
        webTestClient.get().uri(path)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(HttpHeaders.LOCATION)

        where:
        path << [
                "/api/web/photos",
                "/api/web/photos/asset_unknown",
                "/api/web/assets/asset_unknown/thumb/320",
                "/api/web/assets/asset_unknown/preview"
        ]
    }

    def "web endpoint with an OIDC session is let through"() {
        given:
        givenAsset(USER_A, null)

        expect:
        webTestClient.mutateWith(mockOidcLogin().idToken { token -> token.subject(USER_A) })
                .get().uri("/api/web/photos?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
    }

    def "login starts with a PKCE authorization request to the public Keycloak address"() {
        when:
        WebTestClient.ResponseSpec response = webTestClient.get().uri("/oauth2/authorization/keycloak").exchange()

        then:
        response.expectStatus().isFound()

        and:
        String location = response.returnResult(Void).responseHeaders.location.toString()
        location.startsWith("http://localhost:1/realms/integration/protocol/openid-connect/auth?")
        location.contains("response_type=code")
        location.contains("client_id=vehicle-web-bff")
        location.contains("code_challenge=")
        location.contains("code_challenge_method=S256")
        location.contains("login/oauth2/code/keycloak")
    }

    def "login keeps its state in an HttpOnly SameSite=Lax session cookie"() {
        when:
        WebTestClient.ResponseSpec response = webTestClient.get().uri("/oauth2/authorization/keycloak").exchange()

        then:
        response.expectCookie().exists("SESSION")
        response.expectCookie().httpOnly("SESSION", true)
        response.expectCookie().secure("SESSION", true)
        response.expectCookie().sameSite("SESSION", "Lax")
    }

    def "a failed login callback ends with 401, not with a redirect to a login page"() {
        expect:
        webTestClient.get().uri("/login/oauth2/code/keycloak?code=bogus&state=bogus")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(HttpHeaders.LOCATION)
    }
}
