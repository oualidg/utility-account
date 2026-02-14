/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 7:07 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Summary view of an account.
 * Used when returning account lists.
 *
 * @author Oualid Gharach
 */
public record AccountSummaryResponse(
        Long accountNumber,
        BigDecimal balance,
        Boolean isMainAccount,
        Instant createdAt
) {}