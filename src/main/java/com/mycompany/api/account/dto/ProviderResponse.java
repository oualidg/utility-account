/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 8:20 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import java.time.Instant;

/**
 * Provider details response.
 * Used for all provider read operations — never includes the raw API key.
 *
 * @author Oualid Gharach
 */
public record ProviderResponse(
        Long id,
        String code,
        String name,
        String apiKeyPrefix,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}