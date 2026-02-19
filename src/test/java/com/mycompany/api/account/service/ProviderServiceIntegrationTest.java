/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 10:03 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.dto.CreateProviderRequest;
import com.mycompany.api.account.dto.ProviderCreatedResponse;
import com.mycompany.api.account.dto.ProviderResponse;
import com.mycompany.api.account.dto.UpdateProviderRequest;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.util.ApiKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for ProviderService.
 * Tests the full stack: service → repository → real PostgreSQL via Testcontainers.
 *
 * @author Oualid Gharach
 */
@DisplayName("ProviderService Integration Tests")
class ProviderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private PaymentProviderRepository providerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM payment_providers");
    }

    // =========================================================================
    // Onboard
    // =========================================================================

    @Test
    @DisplayName("Should onboard provider and return raw API key once")
    void shouldOnboardProviderAndReturnRawApiKey() {
        // Given
        CreateProviderRequest request = new CreateProviderRequest("MPESA", "M-Pesa");

        // When
        ProviderCreatedResponse response = providerService.onboardProvider(request);

        // Then
        assertThat(response.id()).isNotNull();
        assertThat(response.code()).isEqualTo("MPESA");
        assertThat(response.name()).isEqualTo("M-Pesa");
        assertThat(response.active()).isTrue();
        assertThat(response.apiKey()).isNotBlank();
        assertThat(response.apiKeyPrefix()).isEqualTo(response.apiKey().substring(0, 8));

        // Verify raw key is not stored — only hash is in DB
        PaymentProvider saved = providerRepository.findByCode("MPESA").orElseThrow();
        assertThat(saved.getApiKeyHash()).isNotEqualTo(response.apiKey());
        assertThat(saved.getApiKeyHash()).isEqualTo(ApiKeyHasher.hash(response.apiKey()));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when provider code already exists")
    void shouldThrowWhenProviderCodeAlreadyExists() {
        // Given
        providerService.onboardProvider(new CreateProviderRequest("MPESA", "M-Pesa"));

        // When & Then
        assertThatThrownBy(() ->
                providerService.onboardProvider(new CreateProviderRequest("MPESA", "M-Pesa Duplicate")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("MPESA");
    }

    // =========================================================================
    // Get
    // =========================================================================

    @Test
    @DisplayName("Should return all providers including inactive")
    void shouldReturnAllProvidersIncludingInactive() {
        // Given
        ProviderCreatedResponse mpesa = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));
        providerService.onboardProvider(new CreateProviderRequest("MTN", "MTN Mobile Money"));
        providerService.deactivateProvider(mpesa.id());

        // When
        List<ProviderResponse> providers = providerService.getAllProviders();

        // Then
        assertThat(providers).hasSize(2);
        assertThat(providers).anyMatch(p -> p.code().equals("MPESA") && !p.active());
        assertThat(providers).anyMatch(p -> p.code().equals("MTN") && p.active());
    }

    @Test
    @DisplayName("Should return provider by ID")
    void shouldReturnProviderById() {
        // Given
        ProviderCreatedResponse created = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));

        // When
        ProviderResponse response = providerService.getProvider(created.id());

        // Then
        assertThat(response.id()).isEqualTo(created.id());
        assertThat(response.code()).isEqualTo("MPESA");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when provider not found")
    void shouldThrowWhenProviderNotFound() {
        assertThatThrownBy(() -> providerService.getProvider(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // =========================================================================
    // Update
    // =========================================================================

    @Test
    @DisplayName("Should update provider name")
    void shouldUpdateProviderName() {
        // Given
        ProviderCreatedResponse created = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));

        // When
        ProviderResponse updated = providerService.updateProvider(
                created.id(), new UpdateProviderRequest("M-Pesa Kenya"));

        // Then
        assertThat(updated.name()).isEqualTo("M-Pesa Kenya");
        assertThat(updated.code()).isEqualTo("MPESA"); // code unchanged
    }

    // =========================================================================
    // Deactivate / Reactivate
    // =========================================================================

    @Test
    @DisplayName("Should deactivate provider and invalidate cache")
    void shouldDeactivateProviderAndInvalidateCache() {
        // Given
        ProviderCreatedResponse created = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));
        String rawKey = created.apiKey();

        // Warm up cache
        providerService.authenticate(rawKey);

        // When
        providerService.deactivateProvider(created.id());

        // Then — provider inactive in DB
        PaymentProvider saved = providerRepository.findById(created.id()).orElseThrow();
        assertThat(saved.isActive()).isFalse();

        // Then — cache invalidated, auth rejected
        Optional<PaymentProvider> auth = providerService.authenticate(rawKey);
        assertThat(auth).isEmpty();
    }

    @Test
    @DisplayName("Should reactivate provider and allow authentication again")
    void shouldReactivateProviderAndAllowAuthentication() {
        // Given
        ProviderCreatedResponse created = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));
        providerService.deactivateProvider(created.id());

        // When
        providerService.reactivateProvider(created.id());

        // Then — auth succeeds again via DB fallback (cache was invalidated on deactivate)
        Optional<PaymentProvider> auth = providerService.authenticate(created.apiKey());
        assertThat(auth).isPresent();
        assertThat(auth.get().getCode()).isEqualTo("MPESA");
    }

    // =========================================================================
    // Regenerate key
    // =========================================================================

    @Test
    @DisplayName("Should regenerate API key and invalidate old key")
    void shouldRegenerateApiKeyAndInvalidateOldKey() {
        // Given
        ProviderCreatedResponse created = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));
        String oldKey = created.apiKey();

        // Warm up cache with old key
        providerService.authenticate(oldKey);

        // When
        ProviderCreatedResponse regenerated = providerService.regenerateApiKey(created.id());
        String newKey = regenerated.apiKey();

        // Then — old key no longer works
        assertThat(providerService.authenticate(oldKey)).isEmpty();

        // Then — new key works
        assertThat(providerService.authenticate(newKey)).isPresent();

        // Then — new hash stored in DB
        PaymentProvider saved = providerRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getApiKeyHash()).isEqualTo(ApiKeyHasher.hash(newKey));
        assertThat(saved.getApiKeyHash()).isNotEqualTo(ApiKeyHasher.hash(oldKey));
    }

    // =========================================================================
    // Authentication cache-aside
    // =========================================================================

    @Test
    @DisplayName("Should authenticate provider via DB on cache miss")
    void shouldAuthenticateProviderViaDatabaseOnCacheMiss() {
        // Given — onboard without warming cache
        ProviderCreatedResponse created = providerService.onboardProvider(
                new CreateProviderRequest("MPESA", "M-Pesa"));

        // When — first auth hits DB (cache miss)
        Optional<PaymentProvider> first = providerService.authenticate(created.apiKey());

        // Then
        assertThat(first).isPresent();
        assertThat(first.get().getCode()).isEqualTo("MPESA");

        // When — second auth hits cache
        Optional<PaymentProvider> second = providerService.authenticate(created.apiKey());

        // Then
        assertThat(second).isPresent();
        assertThat(second.get().getCode()).isEqualTo("MPESA");
    }

    @Test
    @DisplayName("Should reject authentication for invalid API key")
    void shouldRejectAuthenticationForInvalidApiKey() {
        Optional<PaymentProvider> result = providerService.authenticate("invalid-key");
        assertThat(result).isEmpty();
    }
}