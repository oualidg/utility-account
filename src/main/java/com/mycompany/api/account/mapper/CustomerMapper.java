/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 9:49 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Customer entity and DTOs.
 * MapStruct automatically generates the implementation at compile time.
 * Includes data normalization (trim, lowercase email, clean mobile).
 *
 * @author Oualid Gharach
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerMapper {

    /**
     * Map CreateCustomerRequest to Customer entity.
     * customerId will be set separately in the service.
     *
     * @param request the create request
     * @return the customer entity
     */
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "firstName", source = "firstName", qualifiedByName = "normalizeString")
    @Mapping(target = "lastName", source = "lastName", qualifiedByName = "normalizeString")
    @Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
    @Mapping(target = "mobileNumber", source = "mobileNumber", qualifiedByName = "normalizeMobile")
    Customer toEntity(CreateCustomerRequest request);

    /**
     * Map Customer entity to CustomerResponse DTO.
     *
     * @param customer the customer entity
     * @return the customer response
     */
    CustomerResponse toResponse(Customer customer);

    /**
     * Update existing Customer entity with values from UpdateCustomerRequest.
     * Only non-null values from request are applied (partial update).
     *
     * @param request the update request
     * @param customer the existing customer to update
     */
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "firstName", source = "firstName", qualifiedByName = "normalizeString")
    @Mapping(target = "lastName", source = "lastName", qualifiedByName = "normalizeString")
    @Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
    @Mapping(target = "mobileNumber", source = "mobileNumber", qualifiedByName = "normalizeMobile")
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer customer);

    /**
     * Normalize string: trim whitespace.
     * Returns null if input is null.
     *
     * @param value the string to normalize
     * @return normalized string
     */
    @Named("normalizeString")
    default String normalizeString(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Normalize email: trim and convert to lowercase.
     * Returns null if input is null.
     *
     * @param email the email to normalize
     * @return normalized email
     */
    @Named("normalizeEmail")
    default String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /**
     * Normalize mobile number: remove spaces, dashes, parentheses, and trim.
     * Returns null if input is null.
     *
     * Example: "+27 (82) 123-4567" becomes "+27821234567"
     *
     * @param mobile the mobile number to normalize
     * @return normalized mobile number
     */
    @Named("normalizeMobile")
    default String normalizeMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        return mobile.trim()
                .replaceAll("[\\s\\-().]", "");  // Remove spaces, dashes, parentheses, dots
    }
}