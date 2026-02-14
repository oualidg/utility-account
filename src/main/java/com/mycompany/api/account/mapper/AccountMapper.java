/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 11:33 AM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.AccountResponse;
import com.mycompany.api.account.dto.AccountSummaryResponse;
import com.mycompany.api.account.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Account entity.
 * Minimal implementation - only maps to summary for CustomerResponse.
 *
 * @author Oualid Gharach
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    /**
     * Map Account entity to AccountSummaryResponse.
     * Used in CustomerDetailedResponse to show account list.
     *
     * @param account account entity
     * @return account summary
     */
    @Mapping(source = "mainAccount", target = "isMainAccount")
    AccountSummaryResponse toSummaryResponse(Account account);


    /**
     * Map Account entity to AccountResponse (full details).
     * Used when retrieving a specific account.
     *
     * @param account account entity
     * @return full account response
     */
    @Mapping(source = "customer.customerId", target = "customerId")
    @Mapping(source = "mainAccount", target = "isMainAccount")
    AccountResponse toResponse(Account account);


}