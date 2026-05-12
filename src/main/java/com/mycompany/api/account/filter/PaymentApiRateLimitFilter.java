/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 5/11/2026 at 1:14 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Global rate-limiting filter for provider-facing payment endpoints.
 *
 * <p>Placed immediately after {@link CorrelationIdFilter} in the filter chain —
 * before API key authentication, Spring Security, deserialization, and controller
 * dispatch. This ensures that burst traffic is rejected at the earliest possible
 * point, protecting the UA Service and its PostgreSQL connection pool (HikariCP
 * max 4 connections) from saturation. Load testing established a UA Service
 * ceiling of ~196 req/s — early rejection is necessary to protect that headroom.</p>
 *
 * <p>The rate limiter is global and shared across all callers — this is intentional.
 * The goal is a protective ceiling for the UA Service, not per-provider fairness.
 * Note that invalid or anonymous traffic consumes the same global bucket as
 * authenticated provider traffic, which is acceptable given the protective intent.</p>
 *
 * <p>The {@code payment-api} {@link RateLimiter} instance is configured in
 * {@code application.properties} under
 * {@code resilience4j.ratelimiter.instances.payment-api} and injected via
 * {@link RateLimiterRegistry} which is autoconfigured by
 * {@code resilience4j-spring-boot3}.</p>
 *
 * <p>Extends {@link AbstractPaymentFilter} for shared endpoint pattern
 * matching and error response writing.</p>
 *
 * @author Oualid Gharach
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PaymentApiRateLimitFilter extends AbstractPaymentFilter {

    private static final String RATE_LIMITER_NAME = "payment-api";

    private final RateLimiter rateLimiter;

    /**
     * Manual constructor required — {@link RateLimiterRegistry#rateLimiter(String)}
     * resolves the named instance from application properties. Cannot use
     * {@code @RequiredArgsConstructor} here because the injected type is
     * {@link RateLimiterRegistry}, not {@link RateLimiter} directly.
     * See CONVENTIONS.md §4.
     *
     * @param rateLimiterRegistry Resilience4j registry autoconfigured from application.properties
     * @param objectMapper        Jackson mapper for writing error responses — passed to superclass
     */
    public PaymentApiRateLimitFilter(RateLimiterRegistry rateLimiterRegistry,
                                     ObjectMapper objectMapper) {
        super(objectMapper);
        this.rateLimiter = rateLimiterRegistry.rateLimiter(RATE_LIMITER_NAME);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimiter.acquirePermission()) {
            log.warn("Rate limit exceeded: {} {}", request.getMethod(), request.getRequestURI());
            response.setHeader("Retry-After", "1");
            sendError(response, request, HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Please slow down your request rate.");
            return;
        }

        filterChain.doFilter(request, response);
    }

}