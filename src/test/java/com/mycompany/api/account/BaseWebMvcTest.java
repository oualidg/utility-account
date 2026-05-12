/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/24/2026 at 10:29 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account;

import com.mycompany.api.account.service.JwtService;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for {@code @WebMvcTest} controller tests.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>{@code @MockitoBean JwtService} — required by {@code JwtAuthFilter}</li>
 *   <li>{@code @MockitoBean RateLimiterRegistry} — required by {@code PaymentApiRateLimitFilter}
 *       which is picked up as a {@code @Component} by the web layer context</li>
 *   <li>{@code @ActiveProfiles("test")} — activates test properties</li>
 * </ul>
 *
 * <p>Subclasses must declare:</p>
 * <ul>
 *   <li>{@code @AutoConfigureMockMvc(addFilters = false)} — disables all servlet filters including security</li>
 *   <li>{@code @WithMockUser(roles = "ADMIN")} or appropriate role</li>
 *   <li>{@code @TestPropertySource(properties = "spring.main.banner-mode=off")}</li>
 * </ul>
 */
@ActiveProfiles("test")
public abstract class BaseWebMvcTest {

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected RateLimiterRegistry rateLimiterRegistry;
}