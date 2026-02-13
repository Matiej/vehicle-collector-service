package com.emat.vehicle_collector_service.vehuser.domain

import java.time.Instant

data class VehUser(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val userRole: VehUserRole,
    val email: String,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)