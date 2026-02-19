/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 8:21 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to onboard a new payment provider.
 * Provider code must be uppercase — normalized in the service if not.
 *
 * @author Oualid Gharach
 */
public record CreateProviderRequest(

        @NotBlank(message = "Provider code is required")
        @Size(max = 50, message = "Provider code must not exceed 50 characters")
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Provider code must be uppercase letters, digits, or underscores")
        String code,

        @NotBlank(message = "Provider name is required")
        @Size(max = 100, message = "Provider name must not exceed 100 characters")
        String name
) {}