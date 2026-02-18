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

import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.util.ApiKeyHasher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for PaymentProvider authentication with in-memory caching.
 *
 * <p>Active providers are loaded into a ConcurrentHashMap on startup, keyed
 * by their API key hash. The filter authenticates incoming requests by hashing
 * the raw API key and looking up the provider.</p>
 *
 * <p>Can be upgraded to Caffeine if more caching needs arise.</p>
 *
 * @author Oualid Gharach
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProviderService {

    private final PaymentProviderRepository providerRepository;

    /**
     * Cache of active providers keyed by API key hash (for authentication).
     */
    private final Map<String, PaymentProvider> apiKeyCache = new ConcurrentHashMap<>();

    /**
     * Load all active providers into the cache on startup.
     */
    @PostConstruct
    void loadProviders() {
        refreshCache();
    }

    /**
     * Authenticate a provider by raw API key.
     * Hashes the key with SHA-256 and looks up the provider.
     *
     * @param rawApiKey the raw API key from the request header
     * @return the authenticated provider, or empty if not found
     */
    public Optional<PaymentProvider> authenticate(String rawApiKey) {
        String hash = ApiKeyHasher.hash(rawApiKey);
        return Optional.ofNullable(apiKeyCache.get(hash));
    }

    /**
     * Refresh the cache from the database.
     * Call this after adding, updating, or deactivating a provider.
     */
    public void refreshCache() {
        List<PaymentProvider> activeProviders = providerRepository.findByActiveTrue();

        apiKeyCache.clear();
        activeProviders.forEach(provider ->
                apiKeyCache.put(provider.getApiKeyHash(), provider));

        log.info("Provider cache refreshed: {} active providers loaded", apiKeyCache.size());
    }
}