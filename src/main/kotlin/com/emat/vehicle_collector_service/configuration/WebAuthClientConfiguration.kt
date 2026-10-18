package com.emat.vehicle_collector_service.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.core.oidc.OidcScopes

@Configuration
class WebAuthClientConfiguration(
    private val webAuthProperties: WebAuthProperties
) {

    @Bean
    fun webPanelClientRegistrations(): ReactiveClientRegistrationRepository =
        InMemoryReactiveClientRegistrationRepository(keycloakRegistration())

    private fun keycloakRegistration(): ClientRegistration =
        ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId(webAuthProperties.clientId)
            .clientSecret(webAuthProperties.clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
            .authorizationUri(webAuthProperties.publicRealmEndpoint("auth"))
            .tokenUri(webAuthProperties.internalRealmEndpoint("token"))
            .jwkSetUri(webAuthProperties.internalRealmEndpoint("certs"))
            .issuerUri(webAuthProperties.issuer)
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .build()

    companion object {
        const val REGISTRATION_ID = "keycloak"
    }
}
