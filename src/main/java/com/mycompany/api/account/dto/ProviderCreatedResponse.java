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
 * Response returned only on provider creation and API key regeneration.
 * Includes the raw API key which is shown once and never stored — the caller
 * must store it securely. All subsequent reads use {@link ProviderResponse}.
 *
 * @author Oualid Gharach
 */
public record ProviderCreatedResponse(
        Long id,
        String code,
        String name,
        String apiKeyPrefix,
        boolean active,
        String apiKey,
        Instant createdAt,
        Instant updatedAt
) {}