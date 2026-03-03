/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/24/2026 at 9:22 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.filter;

import com.mycompany.api.account.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Filter that authenticates requests using JWT access tokens.
 *
 * <p>Supports two token delivery mechanisms:</p>
 * <ul>
 *   <li>HttpOnly cookie ({@code access_token}) — used by the Angular browser app.
 *       Cookie takes priority if present.</li>
 *   <li>{@code Authorization: Bearer} header — used by mobile apps, Swagger,
 *       and API tooling that use {@code X-Auth-Mode: bearer}.</li>
 * </ul>
 *
 * <p>If no token is present or the token is invalid, the filter continues
 * the chain without setting authentication — Spring Security's access rules
 * in {@link com.mycompany.api.account.config.SecurityConfig} determine
 * whether the request is ultimately permitted or rejected.</p>
 *
 * @author Oualid Gharach
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        extractToken(request).ifPresentOrElse(token -> {
            jwtService.validate(token).ifPresentOrElse(claims -> {

                if (!jwtService.getTokenType(claims).equals("access")) {
                    log.warn("Refresh token used as access token for {} {}",
                            request.getMethod(), request.getRequestURI());
                    return;
                }

                String username = jwtService.getUsername(claims);
                String role = jwtService.getRole(claims);

                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("") // not needed — already authenticated via JWT
                        .authorities(List.of(new SimpleGrantedAuthority(role)))
                        .build();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} with role: {} for {} {}",
                        username, role, request.getMethod(), request.getRequestURI());

            }, () -> log.debug("Invalid or expired JWT for {} {}",
                    request.getMethod(), request.getRequestURI()));

        }, () -> log.debug("No JWT token for {} {}",
                request.getMethod(), request.getRequestURI()));

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/api/auth/login")) return true;
        if (path.equals("/api/auth/refresh")) return true;
        if (path.equals("/api/auth/logout")) return true;

        // Payment endpoints are authenticated by ApiKeyAuthFilter, not JWT
        if ("POST".equals(method) && path.matches(".*/api/v1/accounts/\\d+/payments")) return true;
        if ("POST".equals(method) && path.matches(".*/api/v1/customers/\\d+/payments")) return true;
        if ("GET".equals(method) && path.matches(".*/api/v1/payments/confirmation/.+")) return true;

        return false;
    }

    /**
     * Extract the JWT string from the request.
     *
     * <p>Cookie takes priority — used by the Angular app in production.
     * Falls back to {@code Authorization: Bearer} header for mobile apps
     * and API tooling using bearer mode.</p>
     */
    private Optional<String> extractToken(HttpServletRequest request) {
        // 1. Cookie first — Angular browser app
        if (request.getCookies() != null) {
            Optional<String> cookieToken = Arrays.stream(request.getCookies())
                    .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
            if (cookieToken.isPresent()) {
                return cookieToken;
            }
        }

        // 2. Bearer header fallback — mobile apps, Swagger, API tooling
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }

        return Optional.empty();
    }
}