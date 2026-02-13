package com.emat.vehicle_collector_service.infrastructure.keycloak

import com.emat.vehicle_collector_service.vehuser.domain.VehUser
import com.emat.vehicle_collector_service.vehuser.domain.VehUserRole
import java.time.Instant

data class KeycloakUserSummary(
    val id: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val createdAt: Long?,
    val updatedAt: Long?
) {
    fun toDomain(role: VehUserRole): VehUser = VehUser(
        id,
        username,
        firstname,
        lastname,
        role,
        email,
        enabled,
        emailVerified,
        createdAt?.let { Instant.ofEpochMilli(it) },
        updatedAt?.let { Instant.ofEpochMilli(it) }
    )
}
