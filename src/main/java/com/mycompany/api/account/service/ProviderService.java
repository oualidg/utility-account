/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/17/2026 at 9:28 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.CreateProviderRequest;
import com.mycompany.api.account.dto.ProviderCreatedResponse;
import com.mycompany.api.account.dto.ProviderResponse;
import com.mycompany.api.account.dto.UpdateProviderRequest;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.ProviderMapper;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.util.ApiKeyGenerator;
import com.mycompany.api.account.util.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for payment provider management and authentication.
 *
 * <p>Handles two concerns:
 * <ul>
 *   <li>Authentication — cache-aside pattern for validating incoming API keys</li>
 *   <li>Management — onboarding, updates, deactivation, reactivation, key regeneration</li>
 * </ul>
 *
 * <p>API key lifecycle:
 * - Raw key is generated, hashed, and only the hash is persisted
 * - Raw key is returned once in {@link ProviderCreatedResponse} and discarded
 * - On regeneration: new key saved first (inside transaction), old cache entry invalidated after
 *
 * TODO Phase 6D: Split authorization — ROLE_ADMIN for onboard/deactivate/reactivate/regenerate,
 *                ROLE_STAFF for read operations.
 *
 * @author Oualid Gharach
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderService {

    private final PaymentProviderRepository providerRepository;
    private final ProviderMapper providerMapper;

    private final Map<String, PaymentProvider> apiKeyCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    /**
     * Authenticate a provider by raw API key.
     *
     * <p>Cache-aside: check cache first, on miss query DB.
     * Inactive providers are never cached and always rejected.</p>
     *
     * @param rawApiKey raw API key from request header
     * @return authenticated provider, or empty if invalid or inactive
     */
    public Optional<PaymentProvider> authenticate(String rawApiKey) {
        String hash = ApiKeyHasher.hash(rawApiKey);

        // 1. Cache hit — fast path
        PaymentProvider cached = apiKeyCache.get(hash);
        if (cached != null) {
            log.debug("Provider cache hit: {}", cached.getCode());
            return Optional.of(cached);
        }

        // 2. Cache miss — fall back to DB
        log.debug("Provider cache miss, querying DB for prefix: {}",
                rawApiKey.length() >= 8 ? rawApiKey.substring(0, 8) : rawApiKey);

        Optional<PaymentProvider> provider =
                providerRepository.findByApiKeyHashAndActiveTrue(hash);

        // 3. Populate cache on hit
        provider.ifPresent(p -> {
            apiKeyCache.put(hash, p);
            log.debug("Provider cached after DB hit: {}", p.getCode());
        });

        return provider;
    }

    /**
     * Invalidate a provider's cache entry by their API key hash.
     * Called after regenerating a key or deactivating a provider.
     *
     * @param apiKeyHash the hash to remove from cache
     */
    private void invalidate(String apiKeyHash) {
        PaymentProvider removed = apiKeyCache.remove(apiKeyHash);
        if (removed != null) {
            log.info("Provider cache entry invalidated: {}", removed.getCode());
        }
    }

    // -------------------------------------------------------------------------
    // Management
    // -------------------------------------------------------------------------

    /**
     * Onboard a new payment provider and generate their initial API key.
     * The raw key is returned once — it cannot be retrieved again.
     *
     * @param request provider details
     * @return provider response with raw API key (one-time display)
     * @throws DuplicateResourceException if provider code already exists
     */
    @Transactional
    public ProviderCreatedResponse onboardProvider(CreateProviderRequest request) {
        log.info("Onboarding new provider: code={}", request.code());

        if (providerRepository.findByCode(request.code()).isPresent()) {
            throw new DuplicateResourceException(
                    "Provider with code " + request.code() + " already exists");
        }

        String rawKey = ApiKeyGenerator.generate();

        PaymentProvider provider = new PaymentProvider();
        provider.setCode(request.code());
        provider.setName(request.name());
        provider.setApiKeyHash(ApiKeyHasher.hash(rawKey));
        provider.setApiKeyPrefix(rawKey.substring(0, 8));
        provider.setActive(true);

        PaymentProvider saved = providerRepository.saveAndFlush(provider);

        log.info("Provider onboarded successfully: code={}, id={}, prefix={}",
                saved.getCode(), saved.getId(), saved.getApiKeyPrefix());

        return providerMapper.toCreatedResponse(saved, rawKey);
    }

    /**
     * Get all providers (active and inactive).
     *
     * @return list of all providers
     */
    @Transactional(readOnly = true)
    public List<ProviderResponse> getAllProviders() {
        return providerRepository.findAll().stream()
                .map(providerMapper::toResponse)
                .toList();
    }

    /**
     * Get provider by ID.
     *
     * @param id provider ID
     * @return provider response
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional(readOnly = true)
    public ProviderResponse getProvider(Long id) {
        return providerRepository.findById(id)
                .map(providerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found with ID: " + id));
    }

    /**
     * Update provider display name.
     *
     * @param id provider ID
     * @param request update request
     * @return updated provider response
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional
    public ProviderResponse updateProvider(Long id, UpdateProviderRequest request) {
        log.info("Updating provider name: id={}", id);

        PaymentProvider provider = findByIdOrThrow(id);
        provider.setName(request.name());

        PaymentProvider updated = providerRepository.saveAndFlush(provider);
        log.info("Provider updated: code={}", updated.getCode());

        return providerMapper.toResponse(updated);
    }

    /**
     * Deactivate a provider (soft delete).
     * Cache entry is invalidated immediately — provider cannot authenticate after this call.
     *
     * @param id provider ID
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional
    public void deactivateProvider(Long id) {
        log.info("Deactivating provider: id={}", id);

        PaymentProvider provider = findByIdOrThrow(id);
        String oldHash = provider.getApiKeyHash();

        provider.setActive(false);
        providerRepository.saveAndFlush(provider);

        invalidate(oldHash);

        log.info("Provider deactivated: code={}", provider.getCode());
    }

    /**
     * Reactivate a previously deactivated provider.
     * Cache will self-populate on their next authentication attempt.
     *
     * @param id provider ID
     * @return updated provider response
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional
    public ProviderResponse reactivateProvider(Long id) {
        log.info("Reactivating provider: id={}", id);

        PaymentProvider provider = findByIdOrThrow(id);
        provider.setActive(true);

        PaymentProvider updated = providerRepository.saveAndFlush(provider);
        log.info("Provider reactivated: code={}", updated.getCode());

        return providerMapper.toResponse(updated);
    }

    /**
     * Regenerate API key for a provider.
     *
     * <p>Order of operations:
     * 1. Generate new key and save to DB (inside transaction)
     * 2. Invalidate old cache entry
     * New key auto-populates cache on first authentication via cache-aside.</p>
     *
     * @param id provider ID
     * @return provider response with new raw API key (one-time display)
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional
    public ProviderCreatedResponse regenerateApiKey(Long id) {
        log.info("Regenerating API key for provider: id={}", id);

        PaymentProvider provider = findByIdOrThrow(id);
        String oldHash = provider.getApiKeyHash();

        String rawKey = ApiKeyGenerator.generate();
        provider.setApiKeyHash(ApiKeyHasher.hash(rawKey));
        provider.setApiKeyPrefix(rawKey.substring(0, 8));

        PaymentProvider saved = providerRepository.saveAndFlush(provider);

        invalidate(oldHash);

        log.info("API key regenerated: code={}, newPrefix={}", saved.getCode(), saved.getApiKeyPrefix());

        return providerMapper.toCreatedResponse(saved, rawKey);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private PaymentProvider findByIdOrThrow(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found with ID: " + id));
    }
}