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
 * Settlement report for a single provider.
 * Contains totals and the full payment list for the requested date range.
 *
 * @author Oualid Gharach
 */
public record ProviderReconciliationResponse(
        String providerCode,
        String providerName,
        BigDecimal totalAmount,
        long totalCount,
        List<PaymentResponse> payments
) {}