    /*
     * Copyright (c) 2026 Oualid Gharach. All rights reserved.
     *
     * Aiming for production-grade standards through clean code and best practices
     * for educational and informational purposes.
     *
     * Created on: 2/24/2026 at 9:36 PM
     *
     * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
     */
    package com.mycompany.api.account.config;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.mycompany.api.account.filter.ApiKeyAuthFilter;
    import com.mycompany.api.account.filter.JwtAuthFilter;
    import com.mycompany.api.account.security.RestAccessDeniedHandler;
    import com.mycompany.api.account.security.RestAuthenticationEntryPoint;
    import com.mycompany.api.account.service.ProviderService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.config.Customizer;
    import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
    import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
    import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.security.web.csrf.CsrfToken;
    import org.springframework.web.filter.OncePerRequestFilter;
    import java.io.IOException;

    /**
     * Spring Security configuration.
     *
     * <p>All security rules are defined here — this is the single source of truth
     * for what is protected and how:</p>
     * <ul>
     *   <li>Provider-facing payment endpoints — authenticated via {@link ApiKeyAuthFilter}
     *       which validates the {@code X-Api-Key} header.</li>
     *   <li>Admin UI endpoints — authenticated via {@link JwtAuthFilter} which populates
     *       SecurityContext from the {@code access_token} HttpOnly cookie or
     *       {@code Authorization: Bearer} header.</li>
     *   <li>Auth and actuator endpoints — public.</li>
     * </ul>
     *
     * <p>Session management is stateless — no HTTP session is created.
     * All state is carried in the JWT token.</p>
     *
     * <p>CSRF is selectively applied:</p>
     * <ul>
     *   <li>Cookie-based requests (Angular) — CSRF enabled via {@code XSRF-TOKEN} cookie</li>
     *   <li>Bearer token requests (mobile, Swagger, tooling) — CSRF disabled, stateless auth
     *       via {@code Authorization: Bearer} header is not subject to CSRF attacks</li>
     *   <li>Payment endpoints — CSRF disabled, authenticated via API key</li>
     * </ul>
     *
     * <p>Fine-grained role-based access control is handled via {@code @PreAuthorize}
     * annotations on controllers — this config only distinguishes public from
     * authenticated endpoints.</p>
     *
     * <p>{@code UserDetailsServiceImpl} is registered automatically via {@code @Service}
     * component scan — no explicit bean method needed here.</p>
     *
     * @author Oualid Gharach
     */
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    @RequiredArgsConstructor
    public class SecurityConfig {

        private final JwtAuthFilter jwtAuthFilter;
        private final ProviderService providerService;
        private final ObjectMapper objectMapper;
        private final RestAuthenticationEntryPoint authenticationEntryPoint;
        private final RestAccessDeniedHandler accessDeniedHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .cors(Customizer.withDefaults())
                    .csrf(csrf -> csrf
                            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                            .requireCsrfProtectionMatcher(request -> {
                                // Bearer token requests — stateless, not subject to CSRF
                                String authHeader = request.getHeader(JwtAuthFilter.AUTHORIZATION_HEADER);
                                if (authHeader != null && authHeader.startsWith(JwtAuthFilter.BEARER_PREFIX)) {
                                    return false;
                                }
                                // Payment endpoints — authenticated via API key, not cookies
                                String path = request.getRequestURI();
                                if (path.contains("/payments") || path.contains("/confirmation")) {
                                    return false;
                                }
                                // Auth endpoints — establishing session, not acting within one
                                if (path.startsWith("/api/auth/")) {
                                    return false;
                                }
                                // Cookie-based requests — CSRF required for state-changing methods
                                return !request.getMethod().equals("GET")
                                        && !request.getMethod().equals("HEAD")
                                        && !request.getMethod().equals("OPTIONS")
                                        && !request.getMethod().equals("TRACE");
                            }))
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(auth -> auth
                            // Public — auth endpoints (login, refresh, logout only)
                            .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                            // /api/auth/me requires authentication
                            // Public — actuator health
                            .requestMatchers("/api/health/**").permitAll()
                            // Public — actuator info
                            .requestMatchers("/api/info").permitAll()
                            // Public — OpenAPI/Swagger
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            // Provider payment endpoints — API key auth, not Spring Security
                            .requestMatchers("/api/v1/accounts/*/payments").permitAll()
                            .requestMatchers("/api/v1/customers/*/payments").permitAll()
                            .requestMatchers("/api/v1/payments/confirmation/**").permitAll()
                            // Everything else requires authentication
                            // Fine-grained role rules handled via @PreAuthorize on controllers
                            .anyRequest().authenticated())
                    .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);
            ;

            return http.build();
        }

        /**
         * ApiKeyAuthFilter is instantiated manually here rather than as a Spring
         * component — this keeps all security wiring in one place and avoids
         * double-registration in the servlet filter chain.
         */
        @Bean
        public ApiKeyAuthFilter apiKeyAuthFilter() {
            return new ApiKeyAuthFilter(providerService, objectMapper);
        }

        /**
         * Exposes the AuthenticationManager bean for use in AuthService.
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();
        }

        /**
         * BCrypt password encoder at cost factor 12.
         * Used by AuthService to verify passwords at login.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(12);
        }

        static final class CsrfCookieFilter extends OncePerRequestFilter {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        }

    }