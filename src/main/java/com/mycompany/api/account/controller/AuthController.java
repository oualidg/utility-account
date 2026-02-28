/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/24/2026 at 10:08 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.ErrorResponse;
import com.mycompany.api.account.dto.LoginRequest;
import com.mycompany.api.account.dto.LoginResponse;
import com.mycompany.api.account.dto.RefreshResponse;
import com.mycompany.api.account.filter.JwtAuthFilter;
import com.mycompany.api.account.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

/**
 * Handles authentication for the admin UI and API clients.
 *
 * <p>Supports two authentication modes via the {@code X-Auth-Mode} header:</p>
 * <ul>
 *   <li>{@code cookie} (default) — issues JWT tokens as HttpOnly cookies.
 *       Intended for Angular browser app. CSRF protection applies.</li>
 *   <li>{@code bearer} — returns JWT tokens in the response body.
 *       Intended for mobile apps, Swagger, and API tooling. No cookies set.</li>
 * </ul>
 *
 * <p>The controller is a thin HTTP layer — all business logic lives in
 * {@link AuthService}. The controller only decides how to deliver tokens
 * based on the auth mode header.</p>
 *
 * @author Oualid Gharach
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_MODE_HEADER = "X-Auth-Mode";
    private static final String AUTH_MODE_BEARER = "bearer";

    private final AuthService authService;

    @Value("${app.jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${app.jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    /**
     * Authenticate a user and issue JWT tokens.
     *
     * <p>If {@code X-Auth-Mode: bearer}, tokens are returned in the response body.
     * Otherwise tokens are issued as HttpOnly cookies (default).</p>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse response) {
        LoginResponse loginResponse;
        try {
            loginResponse = authService.login(request.username(), request.password());
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username: {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isBearerMode = isBearerMode(httpRequest);
        log.info("Login successful for user: {} mode: {}", loginResponse.username(),
                isBearerMode ? "bearer" : "cookie");

        if (isBearerMode) {
            return ResponseEntity.ok(loginResponse);
        }

        addCookie(response, JwtAuthFilter.ACCESS_TOKEN_COOKIE,
                loginResponse.accessToken(), (int) accessTokenExpiry);
        addCookie(response, "refresh_token",
                loginResponse.refreshToken(), (int) refreshTokenExpiry);

        return ResponseEntity.ok(loginResponse.toCookieResponse());
    }

    /**
     * Issue a new access token using a valid refresh token.
     *
     * <p>Cookie mode — refresh token read from cookie, new access token issued as cookie.
     * Bearer mode — refresh token read from {@code Authorization} header,
     * new access token returned in response body.</p>
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        boolean isBearerMode = isBearerMode(request);

        Optional<String> refreshToken = isBearerMode
                ? extractBearerToken(request)
                : extractCookie(request, "refresh_token");

        if (refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                            "No refresh token provided", request.getRequestURI()));
        }

        Optional<String> newAccessToken = authService.refresh(refreshToken.get());

        if (newAccessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                            "Refresh token invalid or user disabled", request.getRequestURI()));
        }

        if (isBearerMode) {
            return ResponseEntity.ok(new RefreshResponse(newAccessToken.get()));
        }

        addCookie(response, JwtAuthFilter.ACCESS_TOKEN_COOKIE,
                newAccessToken.get(), (int) accessTokenExpiry);
        return ResponseEntity.ok().build();
    }

    /**
     * Clear JWT cookies to log the user out.
     * In bearer mode the client discards the token — no server action needed.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        if (!isBearerMode(request)) {
            clearCookie(response, JwtAuthFilter.ACCESS_TOKEN_COOKIE);
            clearCookie(response, "refresh_token");
        }
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isBearerMode(HttpServletRequest request) {
        return AUTH_MODE_BEARER.equalsIgnoreCase(request.getHeader(AUTH_MODE_HEADER));
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(JwtAuthFilter.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(JwtAuthFilter.BEARER_PREFIX)) {
            return Optional.of(header.substring(JwtAuthFilter.BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}