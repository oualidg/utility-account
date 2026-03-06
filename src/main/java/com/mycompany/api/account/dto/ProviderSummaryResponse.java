/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 3/6/2026 at 11:37 AM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import java.math.BigDecimal;

/**
 * Lightweight provider payment summary for a given period.
 * Contains totals only — no payment details.
 * Used by the provider detail page Load button to populate summary cards
 * without fetching the full payment list.
 *
 * @author Oualid Gharach
 */
public record ProviderSummaryResponse(
        String providerCode,
        String providerName,
        BigDecimal totalAmount,
        long totalCount
) {}