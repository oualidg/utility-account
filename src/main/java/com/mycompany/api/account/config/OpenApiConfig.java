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
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the Utility Account Service.
 *
 * <p>Two API groups are defined:</p>
 * <ul>
 *   <li><b>Payments</b> — always visible, in all profiles. Exposes payment deposit,
 *       confirmation, and validation endpoints. Secured via {@code ApiKeyAuth}.</li>
 *   <li><b>Admin</b> — visible in all profiles. Exposes customer, account,
 *       provider, reporting, user management, and authentication endpoints.
 *       Secured via {@code BearerAuth}.</li>
 * </ul>
 *
 * <p>Two security schemes are registered:</p>
 * <ul>
 *   <li>{@code ApiKeyAuth} — provider-facing payment endpoints via {@code X-Api-Key} header</li>
 *   <li>{@code BearerAuth} — admin UI endpoints via {@code Authorization: Bearer} header.
 *       Login with {@code X-Auth-Mode: bearer} to receive a token in the response body,
 *       then click Authorize and paste the token.</li>
 * </ul>
 *
 * Swagger UI is publicly accessible at: https://utility.oualidg.dev/swagger-ui/index.html
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

    /**
     * Global OpenAPI metadata — title, version, description, contact.
     * Applies to all groups.
     */
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

    // =========================================================================
    // API Groups
    // =========================================================================

    /**
     * Payments API group — always visible in all profiles.
     *
     * <p>Exposes the provider-facing payment endpoints only:</p>
     * <ul>
     *   <li>POST /api/v1/accounts/{accountNumber}/payments</li>
     *   <li>POST /api/v1/customers/{customerId}/payments</li>
     *   <li>GET  /api/v1/payments/confirmation/{reference}</li>
     *   <li>GET  /api/v1/accounts/{accountNumber}/validate</li>
     *   <li>GET  /api/v1/customers/{customerId}/validate</li>
     * </ul>
     *
     * <p>All endpoints are secured via {@code ApiKeyAuth} (X-Api-Key header).
     * This group is safe to expose in prod — it contains no admin or customer data.</p>
     */
    @Bean
    public GroupedOpenApi paymentsGroup() {
        return GroupedOpenApi.builder()
                .group("payments")
                .displayName("Payments API")
                .pathsToMatch(
                        "/api/v1/accounts/*/payments",
                        "/api/v1/customers/*/payments",
                        "/api/v1/payments/confirmation/*",
                        "/api/v1/accounts/*/validate",
                        "/api/v1/customers/*/validate"
                )
                .addOpenApiCustomizer(paymentsSecurityCustomizer())
                .build();
    }

    /**
     * Admin API group — visible in dev profile only.
     *
     * <p>Exposes all non-payment endpoints:</p>
     * <ul>
     *   <li>/api/auth/**     — authentication (login, refresh, logout, me)</li>
     *   <li>/api/v1/customers/** — customer management</li>
     *   <li>/api/v1/accounts/**  — account management</li>
     *   <li>/api/v1/providers/** — provider management</li>
     *   <li>/api/v1/reports/**   — payment reporting</li>
     *   <li>/api/v1/users/**     — user management</li>
     *   <li>/api/health          — actuator health</li>
     *   <li>/api/info            — actuator info</li>
     * </ul>
     *
     * <p>Uses {@code BearerAuth}: login via POST /api/auth/login with
     * {@code X-Auth-Mode: bearer}, then paste the returned token into Authorize.</p>
     */
    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin API")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/v1/customers/**",
                        "/api/v1/accounts/**",
                        "/api/v1/providers/**",
                        "/api/v1/reports/**",
                        "/api/v1/users/**",
                        "/api/health",
                        "/api/info"
                )
                .addOpenApiCustomizer(adminSecurityCustomizer())
                .build();
    }

    // =========================================================================
    // Security Customizers
    // =========================================================================

    /**
     * Applies ApiKeyAuth security requirement to all payment and validation endpoints.
     * Called only for the payments group.
     */
    private OpenApiCustomizer paymentsSecurityCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (item.getPost() != null) {
                item.getPost().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth"));
            }
            if (item.getGet() != null) {
                item.getGet().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth"));
            }
        });
    }

    /**
     * Applies security requirements to all admin endpoints:
     * <ul>
     *   <li>Login endpoint — adds X-Auth-Mode header pre-filled with "bearer"
     *       so Swagger users receive tokens in the response body for easy copy-paste</li>
     *   <li>Public auth endpoints (/refresh, /logout) — no security requirement</li>
     *   <li>/api/auth/me — BearerAuth required</li>
     *   <li>All other endpoints — BearerAuth required</li>
     * </ul>
     * Called only for the admin group.
     */
    private OpenApiCustomizer adminSecurityCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {

            // Login — add X-Auth-Mode: bearer header pre-filled for Swagger convenience.
            // No security requirement — login is public.
            if (path.equals("/api/auth/login") && item.getPost() != null) {
                item.getPost().addParametersItem(
                        new HeaderParameter()
                                .name("X-Auth-Mode")
                                .description("Use 'bearer' to receive tokens in the response body for Swagger. Omit for cookie mode (Angular).")
                                .required(false)
                                .schema(new StringSchema()._default("bearer")));
                return;
            }

            // Public auth endpoints — no security requirement.
            if (path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) {
                return;
            }

            // Actuator endpoints — public, no security requirement.
            if (path.equals("/api/health") || path.equals("/api/info")) {
                return;
            }

            // All other admin endpoints — BearerAuth required.
            if (item.getGet() != null) {
                item.getGet().addSecurityItem(
                        new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getPost() != null) {
                item.getPost().addSecurityItem(
                        new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getPut() != null) {
                item.getPut().addSecurityItem(
                        new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getPatch() != null) {
                item.getPatch().addSecurityItem(
                        new SecurityRequirement().addList("BearerAuth"));
            }
            if (item.getDelete() != null) {
                item.getDelete().addSecurityItem(
                        new SecurityRequirement().addList("BearerAuth"));
            }
        });
    }
}