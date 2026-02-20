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
import java.util.List;

/**
 * Dashboard summary response.
 * Aggregated payment totals across all providers for a given date range.
 *
 * @author Oualid Gharach
 */
public record PaymentSummaryResponse(
        BigDecimal totalAmount,
        long totalCount,
        List<ProviderBreakdownResponse> byProvider
) {}