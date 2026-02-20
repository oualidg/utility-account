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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Reads info from application.properties to avoid duplication.
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 *
 * @author Oualid Gharach
 */
@Configuration
@SecurityScheme(
        name = "ApiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-KEY"
)
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

    @Bean
    public OpenApiCustomizer providerPaymentsSecurity() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {

            if (path.matches("/api/v1/accounts/\\{.*}/payments")
                    && item.getPost() != null) {

                item.getPost().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth")
                );
            }

            if (path.matches("/api/v1/customers/\\{.*}/payments")
                    && item.getPost() != null) {

                item.getPost().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth")
                );
            }

            if (path.matches("/api/v1/payments/confirmation/\\{.*}")
                    && item.getGet() != null) {

                item.getGet().addSecurityItem(
                        new SecurityRequirement().addList("ApiKeyAuth")
                );
            }
        });
    }

}