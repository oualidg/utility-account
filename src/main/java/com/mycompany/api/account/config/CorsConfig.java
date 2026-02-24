/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/21/2026 at 12:48 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS configuration — controls which frontend origins can call this API.
 *
 * Values are fully externalized to application.properties so this class
 * never needs to change between environments. To allow a different origin,
 * just update app.cors.allowed-origins in the relevant profile.
 *
 * Dev:  app.cors.allowed-origins=http://localhost:4200
 * Prod: app.cors.allowed-origins=https://your-real-domain.com
 *
 * @author Oualid Gharach
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app.cors")
@Data
public class CorsConfig implements WebMvcConfigurer {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private boolean allowCredentials;
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods(allowedMethods.toArray(String[]::new))
                .allowedHeaders(allowedHeaders.toArray(String[]::new))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }
}