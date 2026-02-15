package com.emat.vehicle_collector_service.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.*
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfiguration(
    private val appData: AppData
) {
    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .servers(listOf(Server().url(appData.getSwaggerUrl())))
        .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securitySchema()))
        .info(apiInfo())

    private fun apiInfo() = Info()
        .title("VEHICLE COLLECTOR")
        .description("API and documentation for vehicle collector service")
        .version(appData.getApplicationVersion())
        .contact(Contact().name("Maciej Wójcik").email("myEmail@gmail.com"))
        .license(License().name("Apache 2.0").url("https://springdoc.org"))

    private fun securitySchema() = SecurityScheme()
        .type(SecurityScheme.Type.OAUTH2)
        .description(
            """
                        Keycloak logging (password flow).
                        Enter:
                        • username / password (eg tech.admin)
                        • swagger uses client_id/client_secret from configuration
                        and retrieves access_token, then will use as Bearer JWT.
                        """
        )
        .flows(
            OAuthFlows()
                .password(
                    OAuthFlow().tokenUrl(appData.getSwaggerTokenUrl()).scopes(
                        Scopes()
                            .addString("openid", "OpenID scope")
                            .addString("profile", "Profil użytkownika")
                    )
                )
        )

    companion object {
        const val SECURITY_SCHEME_NAME = "bearerAuth"
    }
}