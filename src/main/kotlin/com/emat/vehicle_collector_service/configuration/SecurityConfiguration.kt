package com.emat.vehicle_collector_service.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration(
    private val appData: AppData
) {

    @Value("\${app.security.swagger-public:false}")
    private val swaggerPublic = false

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange { exchanges ->
                exchanges  // public client test (link with token)
                    .pathMatchers("/actuator/health/").permitAll()
                    .pathMatchers("/actuator/health/**").permitAll()
                    .apply { configureSwagger(this) }
                    .pathMatchers("/api/admin/**").hasAnyRole("TECH_ADMIN", "ADMIN")
                    .pathMatchers("/api/public/**").hasAnyRole("ADMIN", "TECH_ADMIN", "REGULAR_USER")
                    .pathMatchers("/api/**").hasAnyRole("ADMIN", "TECH_ADMIN")
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthConverter())
                }
            }
        return http.build()
    }

    @Bean
    fun jwtAuthConverter(): ReactiveJwtAuthenticationConverterAdapter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
            extractAuthoritiesFromRealmRoles(jwt)
        }
        return ReactiveJwtAuthenticationConverterAdapter(converter)
    }

    private fun configureSwagger(spec: ServerHttpSecurity.AuthorizeExchangeSpec): ServerHttpSecurity.AuthorizeExchangeSpec {
        val swaggerPaths = arrayOf("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs/**")
        return if (appData.isSwaggerPublic()) {
            spec.pathMatchers(*swaggerPaths).permitAll()
        } else {
            spec.pathMatchers(*swaggerPaths).hasRole("TECH_ADMIN")
        }
    }

    private fun extractAuthoritiesFromRealmRoles(jwt: Jwt): Collection<GrantedAuthority?> {
        val realmAccessClaim = jwt.claims["realm_access"] as? Map<*, *> ?: return emptyList()
        val rolesObj: List<*> = realmAccessClaim["roles"] as? List<*> ?: return emptyList()

        return rolesObj
            .filterIsInstance<String>()
            .map { roleName -> SimpleGrantedAuthority("ROLE_$roleName") } // TECH_ADMIN -> ROLE_TECH_ADMIN
    }
}