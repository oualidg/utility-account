/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/4/2026 at 5:08 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CustomerMapper.
 * Tests MapStruct mapping logic and data normalization.
 *
 * @author Oualid Gharach
 */
@DisplayName("CustomerMapper Unit Tests")
class CustomerMapperTest {

    private CustomerMapper customerMapper;

    @BeforeEach
    void setUp() {
        // Get the MapStruct-generated implementation
        customerMapper = Mappers.getMapper(CustomerMapper.class);
    }

    @Test
    @DisplayName("Should map CreateCustomerRequest to Customer entity")
    void shouldMapCreateRequestToEntity() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John",
                "Doe",
                "john@example.com",
                "+27821234567"
        );

        // When
        Customer customer = customerMapper.toEntity(request);

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getCustomerId()).isNull(); // Not set by mapper
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo("john@example.com");
        assertThat(customer.getMobileNumber()).isEqualTo("+27821234567");
    }

    @Test
    @DisplayName("Should normalize CreateCustomerRequest")
    void shouldNormalizeRequest() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
                "  John  ",  // Extra spaces
                "  Doe  ",  // Extra spaces
                "  JoHn@EXAMPLE.COM  ",  // Mixed case with spaces
                "+27821234567"
        );

        // When
        Customer customer = customerMapper.toEntity(request);

        // Then
        assertThat(customer.getFirstName()).isEqualTo("John");  // Trimmed
        assertThat(customer.getLastName()).isEqualTo("Doe");  // Trimmed
        assertThat(customer.getEmail()).isEqualTo("john@example.com");  // Trimmed and lowercase
        assertThat(customer.getMobileNumber()).isEqualTo("+27821234567");  // Cleaned
    }

    @Test
    @DisplayName("Should map Customer entity to CustomerResponse")
    void shouldMapEntityToResponse() {
        // Given
        Customer customer = new Customer();
        customer.setCustomerId(12345670L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setMobileNumber("+27821234567");
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());

        // When
        CustomerDetailedResponse response = customerMapper.toDetailedResponse(customer);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(12345670L);
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.mobileNumber()).isEqualTo("+27821234567");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update entity with non-null fields only")
    void shouldUpdateEntityWithNonNullFieldsOnly() {
        // Given - Existing customer
        Customer customer = new Customer();
        customer.setCustomerId(12345670L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setMobileNumber("+27821234567");

        // Update request with partial data (only firstName and email)
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Jane",  // Change firstName
                null,    // Keep lastName
                "jane@example.com",  // Change email
                null     // Keep mobileNumber
        );

        // When
        customerMapper.updateEntity(request, customer);

        // Then
        assertThat(customer.getFirstName()).isEqualTo("Jane");  // Updated
        assertThat(customer.getLastName()).isEqualTo("Doe");    // Not changed
        assertThat(customer.getEmail()).isEqualTo("jane@example.com");  // Updated
        assertThat(customer.getMobileNumber()).isEqualTo("+27821234567");  // Not changed
    }

    @Test
    @DisplayName("Should normalize data during partial update")
    void shouldNormalizeDataDuringPartialUpdate() {
        // Given
        Customer customer = new Customer();
        customer.setCustomerId(12345670L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setMobileNumber("+27821234567");

        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "  Jane  ",  // With spaces
                null,
                "  JANE@EXAMPLE.COM  ",  // Mixed case with spaces
                "+27 (83) 999-8888"  // With formatting
        );

        // When
        customerMapper.updateEntity(request, customer);

        // Then
        assertThat(customer.getFirstName()).isEqualTo("Jane");  // Trimmed
        assertThat(customer.getEmail()).isEqualTo("jane@example.com");  // Trimmed and lowercase
        assertThat(customer.getMobileNumber()).isEqualTo("+27839998888");  // Cleaned
    }

    @Test
    @DisplayName("Should handle null values in normalization methods")
    void shouldHandleNullValuesInNormalization() {
        // Test normalizeString
        String normalizedString = customerMapper.normalizeString(null);
        assertThat(normalizedString).isNull();

        // Test normalizeEmail
        String normalizedEmail = customerMapper.normalizeEmail(null);
        assertThat(normalizedEmail).isNull();

        // Test normalizeMobile
        String normalizedMobile = customerMapper.normalizeMobile(null);
        assertThat(normalizedMobile).isNull();
    }

    @Test
    @DisplayName("Should normalize complex mobile number formats")
    void shouldNormalizeComplexMobileFormats() {
        // Test various formats
        assertThat(customerMapper.normalizeMobile("+27 82 123 4567"))
                .isEqualTo("+27821234567");

        assertThat(customerMapper.normalizeMobile("+27-82-123-4567"))
                .isEqualTo("+27821234567");

        assertThat(customerMapper.normalizeMobile("+27 (82) 123-4567"))
                .isEqualTo("+27821234567");

        assertThat(customerMapper.normalizeMobile("+27.82.123.4567"))
                .isEqualTo("+27821234567");

        assertThat(customerMapper.normalizeMobile("082 123 4567"))
                .isEqualTo("0821234567");
    }

    @Test
    @DisplayName("Should normalize email independently")
    void shouldNormalizeEmailIndependently() {
        // Test normalizeEmail method directly
        String email = customerMapper.normalizeEmail("  JoHn@EXAMPLE.COM  ");

        assertThat(email).isEqualTo("john@example.com");
    }
}