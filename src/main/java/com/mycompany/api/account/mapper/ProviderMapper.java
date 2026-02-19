/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 8:33 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.ProviderCreatedResponse;
import com.mycompany.api.account.dto.ProviderResponse;
import com.mycompany.api.account.entity.PaymentProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for PaymentProvider entity.
 *
 * @author Oualid Gharach
 */
@Mapper(componentModel = "spring")
public interface ProviderMapper {

    /**
     * Map PaymentProvider to ProviderResponse.
     * Used for all read operations — never includes raw API key.
     *
     * @param provider the provider entity
     * @return provider response
     */
    ProviderResponse toResponse(PaymentProvider provider);

    /**
     * Map PaymentProvider to ProviderCreatedResponse.
     * Used only on creation and key regeneration — includes raw API key.
     * The raw key is not on the entity — it must be passed explicitly.
     *
     * @param provider the provider entity
     * @param apiKey the raw API key (one-time display)
     * @return provider created response
     */
    @Mapping(target = "apiKey", source = "apiKey")
    ProviderCreatedResponse toCreatedResponse(PaymentProvider provider, String apiKey);
}