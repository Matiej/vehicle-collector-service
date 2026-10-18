package com.emat.vehicle_collector_service.support

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

import java.nio.charset.StandardCharsets

class FakeKeycloak {

    private final String realm
    private final String clientId
    private final RSAKey signingKey = new RSAKeyGenerator(2048).keyID("fake-keycloak").keyUse(KeyUse.SIGNATURE).generate()
    private final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
    private volatile JWTClaimsSet nextIdTokenClaims

    FakeKeycloak(String realm, String clientId) {
        this.realm = realm
        this.clientId = clientId
        server.createContext("/realms/$realm/protocol/openid-connect/certs") { exchange ->
            respondWithJson(exchange, new JWKSet(signingKey.toPublicJWK()).toString())
        }
        server.createContext("/realms/$realm/protocol/openid-connect/token") { exchange ->
            exchange.requestBody.readAllBytes()
            respondWithJson(exchange, tokenResponse())
        }
        server.start()
    }

    String getBaseUrl() {
        "http://localhost:${server.address.port}"
    }

    void stop() {
        server.stop(0)
    }

    void willIssueIdToken(String subject, String nonce, String issuer) {
        Date now = new Date()
        nextIdTokenClaims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(clientId)
                .claim("azp", clientId)
                .claim("nonce", nonce)
                .issueTime(now)
                .expirationTime(new Date(now.time + 300_000))
                .build()
    }

    private String tokenResponse() {
        """{"access_token":"fake-access-token","token_type":"Bearer","expires_in":300,""" +
                """"scope":"openid profile email","id_token":"${signedIdToken()}"}"""
    }

    private String signedIdToken() {
        SignedJWT idToken = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.keyID).build(),
                nextIdTokenClaims
        )
        idToken.sign(new RSASSASigner(signingKey))
        idToken.serialize()
    }

    private static void respondWithJson(HttpExchange exchange, String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, body.length)
        exchange.responseBody.withCloseable { it.write(body) }
    }
}
