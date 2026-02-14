/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 9:57 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Created on: 2/7/2026 at 9:45 AM
 */
package com.mycompany.api.account.dto;

import com.mycompany.api.account.model.PaymentProvider;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment transaction response.
 * Returned after successful payment processing.
 *
 * @author Oualid Gharach
 */
public record PaymentResponse(
        String receiptNumber,
        Long accountNumber,
        BigDecimal amount,
        PaymentProvider paymentProvider,
        String paymentReference,
        Instant paymentDate
) {}