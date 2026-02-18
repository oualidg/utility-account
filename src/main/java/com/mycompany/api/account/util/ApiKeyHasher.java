/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/18/2026 at 2:06 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for hashing API keys using SHA-256.
 *
 * <p>This is a pure function — same input always produces the same output,
 * no state, no dependencies. Static by design.</p>
 *
 * @author Oualid Gharach
 */
public final class ApiKeyHasher {

    private ApiKeyHasher() {
        // Utility class
    }

    /**
     * Hash a raw API key using SHA-256.
     *
     * @param rawApiKey the raw API key
     * @return hex-encoded SHA-256 hash
     */
    public static String hash(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}