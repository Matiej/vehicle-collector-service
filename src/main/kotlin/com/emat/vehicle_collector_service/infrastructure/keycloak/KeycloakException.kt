package com.emat.vehicle_collector_service.infrastructure.keycloak

import org.springframework.http.HttpStatus

class KeycloakException(
    message: String,
    val code: KeycloakExceptionCode,
    val status: HttpStatus,
): RuntimeException(message)

enum class KeycloakExceptionCode {
    LIST_CALCULATOR_USER_ERROR,
    CREATE_CALCULATOR_USER_ERROR,
    USER_ALREADY_EXISTS,
    GENERIC_ERROR
}