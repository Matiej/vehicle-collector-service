package com.emat.vehicle_collector_service.infrastructure.keycloak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class KeycloakClientImpl(
    private val keycloakAdminWebClient: WebClient,
    private val tokenProvider: KeycloakTokenProvider,
    private val properties: KeycloakAdminProperties
) : KeycloakClient {

    private val log = LoggerFactory.getLogger(KeycloakClient::class.java)

    override fun createUser(request: KeycloakUserRequest): Mono<String> {
        return tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.post()
                    .uri("/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        mapOf(
                            "username" to request.username,
                            "email" to request.email,
                            "firstName" to request.firstname,
                            "lastName" to request.lastname,
                            "enabled" to request.enabled,
                            "emailVerified" to request.emailVerified
                        )
                    )
                    .retrieve()
                    .onStatus({ it.value() == 409 }) { resp ->
                        resp.bodyToMono(String::class.java).defaultIfEmpty("")
                            .map { body ->
                                KeycloakException(
                                    "User creation failed. User with this email or username already exists.",
                                    KeycloakExceptionCode.USER_ALREADY_EXISTS,
                                    HttpStatus.CONFLICT
                                )
                            }
                    }
                    .onStatus({ it.isError }) { resp ->
                        resp.bodyToMono(String::class.java).defaultIfEmpty("")
                            .map { body ->
                                KeycloakException(
                                    "Keycloak error ${resp.statusCode().value()}: $body",
                                    KeycloakExceptionCode.GENERIC_ERROR,
                                    HttpStatus.resolve(resp.statusCode().value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
                                )
                            }
                    }.toBodilessEntity()
                    .flatMap { entity ->
                        val userId = entity.headers.location?.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        if (userId != null) Mono.just(userId)
                        else Mono.error(
                            KeycloakException(
                                "Keycloak returned success but Location header is missing.",
                                KeycloakExceptionCode.GENERIC_ERROR,
                                HttpStatus.CREATED
                            )
                        )
                    }
            }.flatMap { userId ->
                val emailActions = listOf("VERIFY_EMAIL", "UPDATE_PASSWORD")
                assignRole(userId, request.userRole.name)
                    .then(sendUserActionsEmail(userId, emailActions))
                    .thenReturn(userId)
            }
    }

    private fun assignRole(userId: String, role: String): Mono<Void> {
        val roles = mapOf("id" to properties.roles[role], "name" to role)
        return tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.post()
                    .uri("/users/{id}/role-mappings/realm", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(listOf(roles))
                    .retrieve()
                    .bodyToMono(Void::class.java)
            }
    }

    override fun listUsersByRole(role: String): Flux<KeycloakUserSummary> {
        return tokenProvider.getToken()
            .flatMapMany { token ->
                keycloakAdminWebClient.get()
                    .uri("/roles/{roleName}/users", role)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${token}")
                    .retrieve()
                    .onStatus({ it.isError }) { resp ->
                        resp.bodyToMono(String::class.java)
                            .defaultIfEmpty("")
                            .map { body ->
                                val status = HttpStatus.resolve(resp.statusCode().value())
                                log.error("Keycloak listRegularUsers error Status: $status, body: $body")
                                KeycloakException(
                                    "Keycloak error ${resp.statusCode().value()}: $body",
                                    KeycloakExceptionCode.GENERIC_ERROR,
                                    HttpStatus.resolve(resp.statusCode().value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
                                )

                            }
                    }
                    .bodyToFlux(JsonNode::class.java)
                    .map { node -> fromNode(node) }
            }
    }

    override fun getUserById(userId: String): Mono<KeycloakUserSummary> {
        return tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.get()
                    .uri("/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .onStatus({ it.isError }) { resp ->
                        resp.bodyToMono<String>()
                            .defaultIfEmpty("")
                            .map { body ->
                                val status = HttpStatus.resolve(resp.statusCode().value())
                                log.error("Keycloak getUserById error. Status: {}, body: {}", status?.value(), body)
                                KeycloakException(
                                    "Error fetching user from Keycloak. HTTP status: ${status?.value()}",
                                    KeycloakExceptionCode.GENERIC_ERROR,
                                    status ?: HttpStatus.INTERNAL_SERVER_ERROR
                                )
                            }
                    }
                    .bodyToMono<JsonNode>()
                    .map { node -> fromNode(node) }
            }
    }

    override fun updateUser(userId: String, request: KeycloakUserRequest): Mono<KeycloakUserSummary> {
        return tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.get()
                    .uri("/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono<ObjectNode>()
                    .flatMap { existing ->
                        // username pomijamy - nie edytujemy
                        existing.put("firstName", request.firstname)
                        existing.put("lastName", request.lastname)
                        existing.put("email", request.email)
                        existing.put("enabled", request.enabled)
                        existing.put("emailVerified", request.emailVerified)

                        val attributes = existing.putObject("attributes")
                        attributes.putArray("updatedAt").add(Instant.now().toEpochMilli().toString())

                        keycloakAdminWebClient.put()
                            .uri("/users/{id}", userId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(existing)
                            .retrieve()
                            .bodyToMono<Void>()
                    }
            }
            .then(getUserById(userId))
            .flatMap { user ->
                if (!request.emailVerified) {
                    sendUserActionsEmail(user.id, listOf("VERIFY_EMAIL"))
                        .thenReturn(user)
                } else {
                    Mono.just(user)
                }
            }
    }

    override fun changeUserStatus(userId: String, isEnabled: Boolean): Mono<Void> {
        return tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.get()
                    .uri("/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono<ObjectNode>()
                    .flatMap { existing ->
                        existing.put("enabled", isEnabled)

                        keycloakAdminWebClient.put()
                            .uri("/users/{id}", userId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(existing)
                            .retrieve()
                            .bodyToMono<Void>()
                    }
            }
    }

    override fun deleteUser(userId: String): Mono<Void> {
        return tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.delete()
                    .uri("/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono<Void>()
            }
    }

    override fun sendUserActionsEmail(userId: String, actions: List<String>): Mono<Void> =
        tokenProvider.getToken()
            .flatMap { token ->
                keycloakAdminWebClient.put()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/users/{id}/execute-actions-email")
                            .queryParam("client_id", properties.frontendClientId)
                            .build(userId)
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(actions)
                    .retrieve()
                    .bodyToMono<Void>()

            }

    private fun fromNode(node: JsonNode): KeycloakUserSummary =
        KeycloakUserSummary(
            node.get("id").asText(),
            node.get("username").asText(),
            node.get("firstName").asText(),
            node.get("lastName").asText(),
            node.get("email").asText(),
            node.get("enabled").asBoolean(),
            node.get("emailVerified").asBoolean(),
            node.get("createdTimestamp")?.asLong(),
            node.path("attributes").path("updatedAt").firstOrNull()?.asLong()
        )
}