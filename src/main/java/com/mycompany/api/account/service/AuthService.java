/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/27/2026 at 3:53 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.LoginResponse;
import com.mycompany.api.account.entity.User;
import com.mycompany.api.account.mapper.UserMapper;
import com.mycompany.api.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service responsible for authentication business logic.
 *
 * <p>Handles username normalisation, credential verification, last-login
 * timestamp updates, and JWT token issuance. The controller layer decides
 * how to deliver the tokens — as HttpOnly cookies or in the response body.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Authenticate a user and issue JWT tokens.
     *
     * <p>Always returns a {@link LoginResponse} with tokens populated —
     * the controller decides whether to deliver them as cookies or in the body.</p>
     *
     * @param username raw username from request
     * @param password raw password from request
     * @return LoginResponse with user info and tokens
     * @throws BadCredentialsException  if credentials are invalid
     * @throws IllegalStateException    if user not found after successful authentication
     */
    @Transactional
    public LoginResponse login(String username, String password) {
        String normalizedUsername = userMapper.normalizeUsername(username);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, password)
        );

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = jwtService.issueRefreshToken(user);

        log.info("Successful login for user: {} with role: {}", user.getUsername(), user.getRole());

        return new LoginResponse(username, user.getFirstName(), user.getLastName(), user.getRole().name(), accessToken, refreshToken);

    }

    /**
     * Issue a new access token using a valid refresh token.
     *
     * @param refreshToken the raw refresh token string
     * @return new access token string, or empty if token is invalid or user is disabled
     */
    public Optional<String> refresh(String refreshToken) {
        return jwtService.validate(refreshToken)
                .filter(claims -> "refresh".equals(jwtService.getTokenType(claims)))
                .flatMap(claims -> userRepository.findByUsername(jwtService.getUsername(claims)))
                .filter(User::isEnabled)
                .map(user -> {
                    log.debug("Access token refreshed for user: {}", user.getUsername());
                    return jwtService.issueAccessToken(user);
                });
    }
}