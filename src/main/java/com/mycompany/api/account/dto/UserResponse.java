/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/25/2026 at 9:37 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import com.mycompany.api.account.enums.Role;
import java.time.Instant;

/**
 * Response DTO for user data. Never exposes password_hash.
 */
public record UserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        Instant lastLoginAt,
        Instant createdAt
) {}