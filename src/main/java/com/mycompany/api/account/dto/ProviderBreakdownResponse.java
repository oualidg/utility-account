/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/20/2026 at 6:10 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import java.math.BigDecimal;

/**
 * Per-provider payment totals.
 * Nested inside PaymentSummaryResponse.
 *
 * @author Oualid Gharach
 */
public record ProviderBreakdownResponse(
        String providerCode,
        String providerName,
        BigDecimal totalAmount,
        long count
) {}