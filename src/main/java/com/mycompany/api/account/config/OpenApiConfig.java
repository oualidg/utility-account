/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/4/2026 at 3:16 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * <p>Two security schemes are registered:</p>
 * <ul>
 *   <li>{@code ApiKeyAuth} — for provider-facing payment endpoints via {@code X-Api-Key} header</li>
 *   <li>{@code BearerAuth} — for admin UI endpoints via {@code Authorization: Bearer} header.
 *       Login with {@code X-Auth-Mode: bearer} to receive a token, then click Authorize
 *       and paste the token.</li>
 * </ul>
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 *
 * @author Oualid Gharach
 */
@Configuration
@SecuritySchemes({
        @SecurityScheme(
                name = "ApiKeyAuth",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-Api-Key"
        ),
        @SecurityScheme(
                name = "BearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT"
        )
})
public class OpenApiConfig {

    @Value("${info.app.name}")
    private String appName;

    @Value("${info.app.description}")
    private String appDescription;

    @Value("${info.app.version}")
    private String appVersion;

    @Value("${info.app.author}")
    private String author;

    @Value("${info.app.contact.email}")
    private String email;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .version(appVersion)
                        .description(appDescription)
                        .contact(new Contact()
                                .name(author)
                                .email(email)));
    }

    /**
     * Applies security requirements to all endpoints:
     * - Payment endpoints → ApiKeyAuth
     * - Login endpoint → X-Auth-Mode header pre-filled with "bearer"
     * - All other admin endpoints → BearerAuth
     */
    @Bean
    public OpenApiCustomizer securityCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {

            // Payment endpoints — API key auth
            if (path.matches("/api/v1/accounts/\\{.*}/payments")
                    && item.getPost() != null) {
                item.getPost().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth"));
                return;
            }

            if (path.matches("/api/v1/customers/\\{.*}/payments")
                    && item.getPost() != null) {
                item.getPost().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth"));
                return;
            }

            if (path.matches("/api/v1/payments/confirmation/\\{.*}")
                    && item.getGet() != null) {
                item.getGet().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth"));
                return;
            }

            // Login endpoint — add X-Auth-Mode header pre-filled with "bearer"
            if (path.equals("/api/auth/login") && item.getPost() != null) {
                item.getPost().addParametersItem(
                        new HeaderParameter()
                                .name("X-Auth-Mode")
                                .description("Authentication mode — use 'bearer' to receive tokens in response body for Swagger/Postman/mobile. Omit for cookie mode (Angular).")
                                .required(false)
                                .schema(new StringSchema()._default("bearer")));
                return;
            }

            // Auth endpoints — public, no security requirement (except /me which requires auth)
            if (path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) {
                return;
            }

            // All other endpoints — Bearer auth
            if (item.getGet() != null) {
                item.getGet().addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getPost() != null) {
                item.getPost().addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getPut() != null) {
                item.getPut().addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getPatch() != null) {
                item.getPatch().addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getDelete() != null) {
                item.getDelete().addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            }
        });
    }
}