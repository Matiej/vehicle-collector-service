package com.emat.vehicle_collector_service.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority

class RealmRolesTest {

    @Test
    fun `maps every realm role to a prefixed authority`() {
        val claims = mapOf("realm_access" to mapOf("roles" to listOf("REGULAR_USER", "offline_access")))

        val authorities = RealmRoles.authoritiesFromClaims(claims)

        assertThat(authorities.map { it.authority }).containsExactly("ROLE_REGULAR_USER", "ROLE_offline_access")
    }

    @Test
    fun `returns no authorities when realm access is missing or malformed`() {
        assertThat(RealmRoles.authoritiesFromClaims(emptyMap())).isEmpty()
        assertThat(RealmRoles.authoritiesFromClaims(mapOf("realm_access" to "ADMIN"))).isEmpty()
        assertThat(RealmRoles.authoritiesFromClaims(mapOf("realm_access" to mapOf("roles" to "ADMIN")))).isEmpty()
    }

    @Test
    fun `application roles skip Keycloak defaults and non-role authorities`() {
        val authorities = listOf(
            SimpleGrantedAuthority("ROLE_default-roles-vehicle"),
            SimpleGrantedAuthority("ROLE_ADMIN"),
            SimpleGrantedAuthority("SCOPE_openid"),
            SimpleGrantedAuthority("ROLE_TECH_ADMIN"),
            SimpleGrantedAuthority("ROLE_ADMIN")
        )

        assertThat(RealmRoles.applicationRoles(authorities)).containsExactly("ADMIN", "TECH_ADMIN")
    }
}
