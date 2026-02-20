/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/18/2026 at 2:10 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.dto.ErrorResponse;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.service.ProviderService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that authenticates external provider requests using API keys.
 *
 * <p>Intercepts requests to payment endpoints, extracts the API key from
 * the {@code X-API-Key} header, validates it against the provider cache,
 * and stores the authenticated provider as a request attribute.</p>
 *
 * <p>The authenticated provider can be retrieved in controllers via:
 * {@code request.getAttribute("authenticatedProvider")}</p>
 *
 * <p>Runs after {@link com.mycompany.api.account.filter.CorrelationIdFilter}
 * so that authentication failures still have correlation IDs in logs.</p>
 *
 * @author Oualid Gharach
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String PROVIDER_ATTRIBUTE = "authenticatedProvider";

    private final ProviderService providerService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API key for {} {}", request.getMethod(), request.getRequestURI());
            sendError(response, request, HttpStatus.UNAUTHORIZED, "Missing API key");
            return;
        }

        Optional<PaymentProvider> provider = providerService.authenticate(apiKey);

        if (provider.isEmpty()) {
            log.warn("Invalid API key (prefix: {}) for {} {}",
                    apiKey.length() >= 8 ? apiKey.substring(0, 8) : apiKey,
                    request.getMethod(), request.getRequestURI());
            sendError(response, request, HttpStatus.UNAUTHORIZED, "Invalid API key");
            return;
        }

        log.debug("Authenticated provider: {} for {} {}",
                provider.get().getCode(), request.getMethod(), request.getRequestURI());

        request.setAttribute(PROVIDER_ATTRIBUTE, provider.get());
        filterChain.doFilter(request, response);
    }

    /**
     * Only apply this filter to provider-facing payment endpoints.
     *
     * TODO: Migrate to Spring Security in security phase.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // POST /api/v1/accounts/{accountNumber}/payments
        // POST /api/v1/customers/{customerId}/payments
        // GET  /api/v1/payments/confirmation/{reference}
        boolean isDepositToAccount = "POST".equals(method) && path.matches(".*/api/v1/accounts/\\d+/payments");
        boolean isDepositToCustomer = "POST".equals(method) && path.matches(".*/api/v1/customers/\\d+/payments");
        boolean isConfirmation = "GET".equals(method) && path.matches(".*/api/v1/payments/confirmation/.+");

        return !(isDepositToAccount || isDepositToCustomer || isConfirmation);
    }

    /**
     * Send a JSON error response consistent with GlobalExceptionHandler format.
     */
    private void sendError(HttpServletResponse response, HttpServletRequest request,
                           HttpStatus status, String message) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.of(status, message, request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}