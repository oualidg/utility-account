/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 11:34 AM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Payment entity.
 *
 * @author Oualid Gharach
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    /**
     * Map Payment entity to PaymentResponse.
     * Used for both deposit responses and confirmation queries.
     *
     * @param payment payment entity
     * @return payment response
     */
    @Mapping(source = "account.accountNumber", target = "accountNumber")
    @Mapping(source = "paymentProvider.code", target = "providerCode")
    @Mapping(source = "paymentProvider.name", target = "providerName")
    PaymentResponse toResponse(Payment payment);
}