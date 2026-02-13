package com.emat.vehicle_collector_service.api.admin

import com.emat.vehicle_collector_service.vehuser.domain.VehUser
import java.time.Instant

data class VehUserResponse(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val userRole: String,
    val email: String,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    companion object {
        fun fromDomain(vehUser: VehUser): VehUserResponse {
            return VehUserResponse(
                vehUser.id,
                vehUser.username,
                vehUser.firstName,
                vehUser.lastName,
                vehUser.userRole.name,
                vehUser.email,
                vehUser.enabled,
                vehUser.emailVerified,
                vehUser.createdAt,
                vehUser.updatedAt
            )
        }
    }
}
