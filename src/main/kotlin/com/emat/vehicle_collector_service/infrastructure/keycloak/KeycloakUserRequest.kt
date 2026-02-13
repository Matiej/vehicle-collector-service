package com.emat.vehicle_collector_service.infrastructure.keycloak

import com.emat.vehicle_collector_service.api.admin.CreateUserDto
import com.emat.vehicle_collector_service.api.admin.UpdateUserDto
import com.emat.vehicle_collector_service.vehuser.domain.VehUserRole

data class KeycloakUserRequest(
    val username: String?,
    val firstname: String,
    val lastname: String,
    val userRole: VehUserRole,
    val email: String,
    val enabled: Boolean,
    val emailVerified: Boolean
) {

    companion object {
        fun fromCreateDto(dto: CreateUserDto, type: VehUserRole): KeycloakUserRequest =
            KeycloakUserRequest(
                dto.username,
                dto.firstName,
                dto.lastName,
                type,
                dto.email,
                dto.enabled,
                dto.emailVerified
            )

        fun fromUpdateDto(dto: UpdateUserDto, type: VehUserRole): KeycloakUserRequest =
            KeycloakUserRequest(
                null,
                dto.firstName,
                dto.lastName,
                type,
                dto.email,
                dto.enabled,
                dto.emailVerified
            )
    }

}
