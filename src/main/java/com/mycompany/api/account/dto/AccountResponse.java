/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 8:43 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Full account details response.
 * Used when retrieving a specific account.
 *
 * @author Oualid Gharach
 */
public record AccountResponse(
        Long accountNumber,
        Long customerId,
        BigDecimal balance,
        Boolean isMainAccount,
        Instant createdAt,
        Instant updatedAt
) {}