package com.emat.vehicle_collector_service.infrastructure.keycloak

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface KeycloakClient {
    fun createUser(request: KeycloakUserRequest): Mono<String>
    fun listUsersByRole(role: String): Flux<KeycloakUserSummary>
    fun getUserById(userId: String): Mono<KeycloakUserSummary>
    fun updateUser(userId: String, request: KeycloakUserRequest): Mono<KeycloakUserSummary>
    fun changeUserStatus(userId: String, isEnabled: Boolean): Mono<Void>
    fun deleteUser(userId: String): Mono<Void>
    fun sendUserActionsEmail(userId: String, actions: List<String>): Mono<Void>
}