/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 5/12/2026 at 6:25 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Abstract base filter for provider-facing payment endpoint filters.
 *
 * <p>Centralises two concerns shared by {@link ApiKeyAuthFilter} and
 * {@link PaymentApiRateLimitFilter}:</p>
 * <ul>
 *   <li>Endpoint pattern matching — single source of truth for which paths
 *       are considered payment endpoints. All subclasses use
 *       {@link #isPaymentEndpoint(HttpServletRequest)} in their
 *       {@code shouldNotFilter} implementations, eliminating the duplication
 *       that existed across {@code SecurityConfig}, {@code ApiKeyAuthFilter},
 *       and {@code PaymentApiRateLimitFilter}.</li>
 *   <li>Error response writing — {@link #sendError} produces a JSON error
 *       response consistent with {@link com.mycompany.api.account.exception.GlobalExceptionHandler}
 *       format without duplicating the serialisation logic in each filter.</li>
 * </ul>
 *
 * <p>{@link ApiKeyAuthFilter} is instantiated manually in
 * {@link com.mycompany.api.account.config.SecurityConfig} rather than as a
 * Spring {@code @Component}. This base class extends {@link OncePerRequestFilter}
 * which does not require Spring management, so both manually instantiated and
 * component-managed subclasses work correctly.</p>
 *
 * @author Oualid Gharach
 */
public abstract class AbstractPaymentFilter extends OncePerRequestFilter {

    // =========================================================================
    // Payment endpoint patterns — single source of truth
    // =========================================================================

    protected static final String DEPOSIT_TO_ACCOUNT_PATTERN   = ".*/api/v1/accounts/\\d+/payments";
    protected static final String DEPOSIT_TO_CUSTOMER_PATTERN  = ".*/api/v1/customers/\\d+/payments";
    protected static final String CONFIRMATION_PATTERN         = ".*/api/v1/payments/confirmation/.+";
    protected static final String VALIDATE_ACCOUNT_PATTERN     = ".*/api/v1/accounts/\\d+/validate";
    protected static final String VALIDATE_CUSTOMER_PATTERN    = ".*/api/v1/customers/\\d+/validate";

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper Jackson mapper for writing JSON error responses
     */
    protected AbstractPaymentFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Returns {@code true} if the request targets a provider-facing payment
     * or validation endpoint that subclass filters should intercept.
     *
     * <p>This is the single source of truth for endpoint matching.
     * {@code shouldNotFilter} implementations in subclasses delegate here
     * rather than repeating the pattern list.</p>
     *
     * @param request the incoming HTTP request
     * @return {@code true} if the request matches a payment endpoint
     */
    protected boolean isPaymentEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return ("POST".equals(method) && path.matches(DEPOSIT_TO_ACCOUNT_PATTERN))
                || ("POST".equals(method) && path.matches(DEPOSIT_TO_CUSTOMER_PATTERN))
                || ("GET".equals(method)  && path.matches(CONFIRMATION_PATTERN))
                || ("GET".equals(method)  && path.matches(VALIDATE_ACCOUNT_PATTERN))
                || ("GET".equals(method)  && path.matches(VALIDATE_CUSTOMER_PATTERN));
    }

    /**
     * Write a JSON error response consistent with
     * {@link com.mycompany.api.account.exception.GlobalExceptionHandler} format.
     *
     * @param response HTTP response to write to
     * @param request  HTTP request (used for the URI in the error body)
     * @param status   HTTP status to return
     * @param message  human-readable error message
     * @throws IOException if writing to the response fails
     */
    protected void sendError(HttpServletResponse response, HttpServletRequest request,
                             HttpStatus status, String message) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.of(status, message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isPaymentEndpoint(request);
    }
}