/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 8:30 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a payment provider's display name.
 * For activation state changes use the dedicated deactivate/reactivate endpoints.
 *
 * @author Oualid Gharach
 */
public record UpdateProviderRequest(

        @NotBlank(message = "Provider name is required")
        @Size(max = 100, message = "Provider name must not exceed 100 characters")
        String name
) {}