package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.support.FakeKeycloak
import com.emat.vehicle_collector_service.support.PublicApiSpec
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder

import java.nio.charset.StandardCharsets
import java.util.function.Supplier

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin

@TestPropertySource(properties = ["app.security.web-panel-permit-all=false"])
class WebPanelAuthIT extends PublicApiSpec {

    static final String EXPECTED_ISSUER = "http://localhost:1/realms/integration"
    static final String PANEL_URL = "http://localhost:5173"
    static final FakeKeycloak FAKE_KEYCLOAK = new FakeKeycloak("integration", "vehicle-web-bff")

    @DynamicPropertySource
    static void fakeKeycloakBackChannel(DynamicPropertyRegistry registry) {
        registry.add("app.web.auth.keycloak-internal-url", { FAKE_KEYCLOAK.baseUrl } as Supplier)
    }

    def cleanupSpec() {
        FAKE_KEYCLOAK.stop()
    }

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
        String location = response.returnResult(Void).responseHeaders.getLocation().toString()
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

    def "a successful login callback redirects to the panel and the session cookie alone authenticates web API calls"() {
        given:
        givenAsset(USER_A, null)

        when:
        LoginResult login = loginThroughKeycloak(USER_A, EXPECTED_ISSUER)

        then:
        login.callback.status.value() == 302
        login.callback.responseHeaders.getLocation().toString() == PANEL_URL

        and:
        login.sessionId != login.preLoginSessionId

        and:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}")
                .cookie("SESSION", login.sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
    }

    def "a panel session does not authenticate PWA, admin or other API calls without a Bearer token: #path"() {
        given:
        LoginResult login = loginThroughKeycloak(USER_A, EXPECTED_ISSUER)

        expect:
        webTestClient.get().uri(path)
                .cookie("SESSION", login.sessionId)
                .exchange()
                .expectStatus().isUnauthorized()

        where:
        path << ["/api/app/assets", "/api/app/sessions", "/api/admin/users/regular"]
    }

    def "an ID token signed by the trusted key but issued by another issuer is rejected"() {
        when:
        LoginResult login = loginThroughKeycloak(USER_A, "https://wrong-issuer.example/realms/integration")

        then:
        login.callback.status.value() == 401
        login.callback.responseHeaders.getLocation() == null

        and:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}")
                .cookie("SESSION", login.sessionId)
                .exchange()
                .expectStatus().isUnauthorized()
    }

    private LoginResult loginThroughKeycloak(String subject, String issuer) {
        EntityExchangeResult<Void> authorization = webTestClient.get().uri("/oauth2/authorization/keycloak")
                .exchange()
                .expectBody(Void)
                .returnResult()
        String sessionId = authorization.responseCookies.getFirst("SESSION").value
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(authorization.responseHeaders.getLocation())
                .build()
                .queryParams

        FAKE_KEYCLOAK.willIssueIdToken(subject, decoded(params.getFirst("nonce")), issuer)

        EntityExchangeResult<Void> callback = webTestClient.get()
                .uri("/login/oauth2/code/keycloak?code=fake-code&state={state}", decoded(params.getFirst("state")))
                .cookie("SESSION", sessionId)
                .exchange()
                .expectBody(Void)
                .returnResult()
        ResponseCookie renewedSession = callback.responseCookies.getFirst("SESSION")
        new LoginResult(callback, sessionId, renewedSession?.value ?: sessionId)
    }

    private static String decoded(String queryValue) {
        URLDecoder.decode(queryValue, StandardCharsets.UTF_8)
    }

    private static class LoginResult {
        final EntityExchangeResult<Void> callback
        final String preLoginSessionId
        final String sessionId

        LoginResult(EntityExchangeResult<Void> callback, String preLoginSessionId, String sessionId) {
            this.callback = callback
            this.preLoginSessionId = preLoginSessionId
            this.sessionId = sessionId
        }
    }
}
