/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/27/2026 at 10:50 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.LoginResponse;
import com.mycompany.api.account.entity.User;
import com.mycompany.api.account.enums.Role;
import com.mycompany.api.account.mapper.UserMapper;
import com.mycompany.api.account.repository.UserRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Mocks all dependencies — no DB or Spring context required.
 *
 * @author Oualid Gharach
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEmail("admin@utility.local");
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setEnabled(true);
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should login successfully and return LoginResponse with tokens")
    void shouldLoginSuccessfully() {
        // Given
        when(userMapper.normalizeUsername("  ADMIN  ")).thenReturn("admin");
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(jwtService.issueAccessToken(adminUser)).thenReturn("access-token");
        when(jwtService.issueRefreshToken(adminUser)).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.login("  ADMIN  ", "password");

        // Then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo("ROLE_ADMIN");
        assertThat(response.firstName()).isEqualTo("Admin");
        assertThat(response.lastName()).isEqualTo("User");
    }

    @Test
    @DisplayName("Should normalise username before authentication")
    void shouldNormaliseUsername() {
        // Given
        when(userMapper.normalizeUsername("  ADMIN  ")).thenReturn("admin");
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(jwtService.issueAccessToken(any())).thenReturn("access-token");
        when(jwtService.issueRefreshToken(any())).thenReturn("refresh-token");

        // When
        authService.login("  ADMIN  ", "password");

        // Then — verify normalised username was passed to authenticationManager
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("admin", "password")
        );
    }

    @Test
    @DisplayName("Should update lastLoginAt on successful login")
    void shouldUpdateLastLoginAt() {
        // Given
        when(userMapper.normalizeUsername("admin")).thenReturn("admin");
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(jwtService.issueAccessToken(any())).thenReturn("access-token");
        when(jwtService.issueRefreshToken(any())).thenReturn("refresh-token");

        // When
        authService.login("admin", "password");

        // Then
        assertThat(adminUser.getLastLoginAt()).isNotNull();
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when credentials are invalid")
    void shouldThrowOnBadCredentials() {
        // Given
        when(userMapper.normalizeUsername("admin")).thenReturn("admin");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login("admin", "wrongpassword"))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsername(any());
        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user not found after authentication")
    void shouldThrowWhenUserNotFoundAfterAuth() {
        // Given
        when(userMapper.normalizeUsername("admin")).thenReturn("admin");
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login("admin", "password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found after successful authentication");
    }

    // -------------------------------------------------------------------------
    // refresh
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return new access token for valid refresh token")
    void shouldReturnNewAccessTokenForValidRefreshToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin")
                .claim("type", "refresh")
                .expirationTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        when(jwtService.validate("valid-refresh-token")).thenReturn(Optional.of(claims));
        when(jwtService.getTokenType(claims)).thenReturn("refresh");
        when(jwtService.getUsername(claims)).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(jwtService.issueAccessToken(adminUser)).thenReturn("new-access-token");

        Optional<String> result = authService.refresh("valid-refresh-token");

        assertThat(result).isPresent().contains("new-access-token");
    }

    @Test
    @DisplayName("Should return empty for invalid refresh token")
    void shouldReturnEmptyForInvalidRefreshToken() {
        // Given
        when(jwtService.validate("invalid-token")).thenReturn(Optional.empty());

        // When
        Optional<String> result = authService.refresh("invalid-token");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token type is not refresh")
    void shouldReturnEmptyWhenTokenTypeIsNotRefresh() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin")
                .claim("type", "access")
                .expirationTime(new Date(System.currentTimeMillis() + 900000))
                .build();

        when(jwtService.validate("access-token")).thenReturn(Optional.of(claims));
        when(jwtService.getTokenType(claims)).thenReturn("access");

        Optional<String> result = authService.refresh("access-token");

        assertThat(result).isEmpty();
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should return empty when user is disabled")
    void shouldReturnEmptyWhenUserIsDisabled() throws Exception {
        adminUser.setEnabled(false);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin")
                .claim("type", "refresh")
                .expirationTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        when(jwtService.validate("valid-refresh-token")).thenReturn(Optional.of(claims));
        when(jwtService.getTokenType(claims)).thenReturn("refresh");
        when(jwtService.getUsername(claims)).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Optional<String> result = authService.refresh("valid-refresh-token");

        assertThat(result).isEmpty();
        verify(jwtService, never()).issueAccessToken(any());
    }
}