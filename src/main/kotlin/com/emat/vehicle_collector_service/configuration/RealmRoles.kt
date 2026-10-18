package com.emat.vehicle_collector_service.configuration

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

object RealmRoles {

    private const val AUTHORITY_PREFIX = "ROLE_"
    private val APPLICATION_ROLES = setOf("ADMIN", "TECH_ADMIN", "REGULAR_USER")

    fun authoritiesFromClaims(claims: Map<String, Any?>): List<GrantedAuthority> {
        val realmAccess = claims["realm_access"] as? Map<*, *> ?: return emptyList()
        val roles = realmAccess["roles"] as? List<*> ?: return emptyList()

        return roles
            .filterIsInstance<String>()
            .map { roleName -> SimpleGrantedAuthority("$AUTHORITY_PREFIX$roleName") }
    }

    fun applicationRoles(authorities: Collection<GrantedAuthority>): List<String> =
        authorities
            .mapNotNull { authority -> authority.authority?.removePrefix(AUTHORITY_PREFIX) }
            .filter { role -> role in APPLICATION_ROLES }
            .distinct()
}
