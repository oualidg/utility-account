/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/14/2026 at 9:45 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that generates or extracts a correlation ID for each HTTP request.
 *
 * <p>The correlation ID is used to trace requests across the entire system:
 * <ul>
 *   <li>Stored in MDC (Mapped Diagnostic Context) for inclusion in all log statements</li>
 *   <li>Added to response headers for client reference</li>
 *   <li>Can be provided by client or auto-generated if missing</li>
 * </ul>
 *
 * <p><b>Usage in logs:</b>
 * <pre>
 * 2026-02-14 20:30:45.123 [http-nio-8080-exec-1] [550e8400-e29b-41d4-a716-446655440000] INFO  c.m.a.a.c.CustomerController - Customer created
 *                                                  └─────────────────────────────────┘
 *                                                         Correlation ID
 * </pre>
 *
 * <p><b>Order:</b> This filter runs first (HIGHEST_PRECEDENCE) to ensure correlation ID
 * is available for all subsequent filters and request processing.
 *
 * @author Oualid Gharach
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * Header name for correlation ID.
     * Can be sent by client or will be added to response.
     */
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * MDC key for storing correlation ID.
     * This key is referenced in logback-spring.xml pattern: %X{correlationId}
     */
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Process each request to ensure it has a correlation ID.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Extract correlation ID from request header (if present)</li>
     *   <li>Generate new UUID if not provided by client</li>
     *   <li>Store in MDC for logging</li>
     *   <li>Add to response header</li>
     *   <li>Process request</li>
     *   <li>Clean up MDC (finally block)</li>
     * </ol>
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if request processing fails
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Record start time for duration calculation
        long startTime = System.currentTimeMillis();

        try {
            // Extract or generate correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);

            // If no correlation ID provided or blank, generate new one
            boolean wasGenerated = false;
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
                wasGenerated = true;
            }

            // Store in MDC for logging (will appear in all log statements)
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response headers for client reference
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Log request received (will include correlation ID from MDC)
            if (log.isInfoEnabled()) {
                String source = wasGenerated ? "generated" : "from client";
                log.info("Request received: {} {} (correlation ID {})",
                        request.getMethod(), request.getRequestURI(), source);
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Calculate request duration
            long duration = System.currentTimeMillis() - startTime;

            // Log request completion with duration and status
            if (log.isInfoEnabled()) {
                log.info("Request completed: {} {} - Status: {} - Duration: {}ms",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            }

            // CRITICAL: Always clear MDC to prevent memory leaks and cross-request contamination
            // Thread pools reuse threads, so MDC must be cleaned up after each request
            MDC.clear();
        }
    }

}