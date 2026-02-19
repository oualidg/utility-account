/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 2:01 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import java.util.UUID;

/**
 * Utility class for generating cryptographically secure API keys.
 *
 * <p>Uses UUID v4 (cryptographically random, 122 bits of entropy).
 * Not UUID v7 — v7 embeds a timestamp making part of the key predictable,
 * which is unacceptable for security tokens.</p>
 *
 * @author Oualid Gharach
 */
public final class ApiKeyGenerator {

    private ApiKeyGenerator() {}

    /**
     * Generate a cryptographically secure API key.
     *
     * @return UUID v4 string (36 characters, e.g. "e7e75fe1-4192-4e34-af5e-6010d787c029")
     */
    public static String generate() {
        return UUID.randomUUID().toString(); // v4 — pure random, not time-ordered
    }
}