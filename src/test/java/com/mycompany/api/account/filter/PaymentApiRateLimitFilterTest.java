/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 5/11/2026 at 8:57 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentApiRateLimitFilter.
 *
 * <p>Uses a real {@link RateLimiter} configured to a low limit to verify
 * filter behaviour without requiring a Spring context.</p>
 *
 * @author Oualid Gharach
 */
@DisplayName("PaymentApiRateLimitFilter Tests")
class PaymentApiRateLimitFilterTest {

    private PaymentApiRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Real RateLimiter configured to 2 req/s for deterministic testing
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        filter = new PaymentApiRateLimitFilter(registry, objectMapper);
    }

    // =========================================================================
    // shouldNotFilter
    // =========================================================================

    @Test
    @DisplayName("Should not filter non-payment paths")
    void shouldNotFilterNonPaymentPaths() {
        assertThat(filter.shouldNotFilter(request("GET", "/api/v1/accounts"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/auth/login"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/health"))).isTrue();
    }

    @Test
    @DisplayName("Should filter deposit to account endpoint")
    void shouldFilterDepositToAccount() {
        assertThat(filter.shouldNotFilter(request("POST", "/api/v1/accounts/1234567897/payments"))).isFalse();
    }

    @Test
    @DisplayName("Should filter deposit to customer endpoint")
    void shouldFilterDepositToCustomer() {
        assertThat(filter.shouldNotFilter(request("POST", "/api/v1/customers/12345674/payments"))).isFalse();
    }

    @Test
    @DisplayName("Should filter payment confirmation endpoint")
    void shouldFilterConfirmation() {
        assertThat(filter.shouldNotFilter(request("GET", "/api/v1/payments/confirmation/REF-001"))).isFalse();
    }

    @Test
    @DisplayName("Should filter account validate endpoint")
    void shouldFilterValidateAccount() {
        assertThat(filter.shouldNotFilter(request("GET", "/api/v1/accounts/1234567897/validate"))).isFalse();
    }

    @Test
    @DisplayName("Should filter customer validate endpoint")
    void shouldFilterValidateCustomer() {
        assertThat(filter.shouldNotFilter(request("GET", "/api/v1/customers/12345674/validate"))).isFalse();
    }

    // =========================================================================
    // Rate limiting behaviour
    // =========================================================================

    @Test
    @DisplayName("Should pass request through when within rate limit")
    void shouldPassRequestThroughWhenWithinRateLimit() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = request("POST", "/api/v1/accounts/1234567897/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Should return 429 when rate limit is exceeded")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = request("POST", "/api/v1/accounts/1234567897/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Exhaust the limit (2 req/s)
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        // Third request should be rejected
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Should not call filter chain when rate limit is exceeded")
    void shouldNotCallFilterChainWhenRateLimitExceeded() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = request("GET", "/api/v1/accounts/1234567897/validate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Exhaust the limit
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        // Third request
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(path);
        return request;
    }
}