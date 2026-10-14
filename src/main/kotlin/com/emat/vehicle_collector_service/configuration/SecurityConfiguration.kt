package com.emat.vehicle_collector_service.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration(
    private val appData: AppData,
    private val webAuthProperties: WebAuthProperties
) {

    @Value("\${app.security.swagger-public:false}")
    private val swaggerPublic = false

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun webPanelSecurityWebFilterChain(
        http: ServerHttpSecurity,
        clientRegistrations: ReactiveClientRegistrationRepository
    ): SecurityWebFilterChain {
        val sessionContextRepository = WebSessionServerSecurityContextRepository()
        val unauthorized = HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)

        http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(*WEB_PANEL_PATHS))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(sessionContextRepository)
            .exceptionHandling { exceptions -> exceptions.authenticationEntryPoint(unauthorized) }
            .authorizeExchange { exchanges ->
                if (appData.isWebPanelPermitAll()) {
                    exchanges.anyExchange().permitAll()
                } else {
                    exchanges.anyExchange().authenticated()
                }
            }
            .oauth2Login { login ->
                login.securityContextRepository(sessionContextRepository)
                    .authorizationRequestResolver(pkceAuthorizationRequestResolver(clientRegistrations))
                    .authenticationSuccessHandler(RedirectServerAuthenticationSuccessHandler(webAuthProperties.panelUrl))
                    .authenticationFailureHandler(ServerAuthenticationEntryPointFailureHandler(unauthorized))
            }
        return http.build()
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange { exchanges ->
                exchanges  // public client test (link with token)
                    .pathMatchers("/actuator/health/").permitAll()
                    .pathMatchers("/actuator/health/**").permitAll()
                    .apply { configureSwagger(this) }
                    .pathMatchers("/api/admin/**").hasAnyRole("TECH_ADMIN", "ADMIN")
                    .pathMatchers("/api/app/**").hasAnyRole("ADMIN", "TECH_ADMIN", "REGULAR_USER")
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

    private fun pkceAuthorizationRequestResolver(
        clientRegistrations: ReactiveClientRegistrationRepository
    ): ServerOAuth2AuthorizationRequestResolver {
        val resolver = DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations)
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce())
        return resolver
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

    companion object {
        private val WEB_PANEL_PATHS = arrayOf("/api/web/**", "/oauth2/**", "/login/oauth2/**")
    }
}
