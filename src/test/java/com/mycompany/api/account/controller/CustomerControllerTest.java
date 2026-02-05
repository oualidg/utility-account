/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/5/2026 at 12:40 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/5/2026 at 12:40 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CustomerController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 *
 * @author Oualid Gharach
 */
@WebMvcTest(CustomerController.class)
@DisplayName("CustomerController Unit Tests")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private CustomerResponse sampleCustomerResponse;
    private CreateCustomerRequest sampleCreateRequest;
    private UpdateCustomerRequest sampleUpdateRequest;

    @BeforeEach
    void setUp() {
        // Sample data for tests - using VALID 8-digit Luhn customer ID: 12345674
        sampleCustomerResponse = new CustomerResponse(
                12345674L,  // Valid Luhn checksum
                "John",
                "Doe",
                "john@example.com",
                "+27821234567",
                Instant.now(),
                Instant.now()
        );

        sampleCreateRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john@example.com",
                "+27821234567"
        );

        sampleUpdateRequest = new UpdateCustomerRequest(
                "Jane",
                "Smith",
                "jane@example.com",
                "+27829999999"
        );
    }

    @Test
    @DisplayName("POST /api/v1/customers - Should create customer successfully")
    void shouldCreateCustomerSuccessfully() throws Exception {
        // Given
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenReturn(sampleCustomerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/customers/" + sampleCustomerResponse.customerId())))
                .andExpect(jsonPath("$.customerId").value(sampleCustomerResponse.customerId()))
                .andExpect(jsonPath("$.firstName").value(sampleCustomerResponse.firstName()))
                .andExpect(jsonPath("$.lastName").value(sampleCustomerResponse.lastName()))
                .andExpect(jsonPath("$.email").value(sampleCustomerResponse.email()))
                .andExpect(jsonPath("$.mobileNumber").value(sampleCustomerResponse.mobileNumber()));

        verify(customerService, times(1)).createCustomer(any(CreateCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers - Should return 400 for invalid request")
    void shouldReturnBadRequestForInvalidCreateRequest() throws Exception {
        // Given - Invalid request with missing firstName
        CreateCustomerRequest invalidRequest = new CreateCustomerRequest(
                null, // Invalid: firstName is required
                "Doe",
                "john@example.com",
                "+27821234567"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).createCustomer(any(CreateCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers - Should return 409 for duplicate email")
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        // Given
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("Customer with email already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer with email already exists"));

        verify(customerService, times(1)).createCustomer(any(CreateCustomerRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Should return customer by ID")
    void shouldReturnCustomerById() throws Exception {
        // Given - Valid 8-digit Luhn customer ID
        Long customerId = 12345674L;
        when(customerService.getCustomer(customerId))
                .thenReturn(sampleCustomerResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.firstName").value(sampleCustomerResponse.firstName()))
                .andExpect(jsonPath("$.lastName").value(sampleCustomerResponse.lastName()))
                .andExpect(jsonPath("$.email").value(sampleCustomerResponse.email()));

        verify(customerService, times(1)).getCustomer(customerId);
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Should return 400 for invalid Luhn customer ID")
    void shouldReturnBadRequestForInvalidLuhnCustomerId() throws Exception {
        // Given - Invalid Luhn customer ID (wrong checksum)
        Long invalidCustomerId = 12345678L;  // Invalid Luhn checksum

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{id}", invalidCustomerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Customer ID must be a valid 8-digit number")));

        // Service should NOT be called due to validation failure
        verify(customerService, never()).getCustomer(any());
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Should return 404 for non-existent customer")
    void shouldReturnNotFoundForNonExistentCustomer() throws Exception {
        // Given - Valid Luhn but non-existent customer
        Long customerId = 99999997L;  // Valid Luhn checksum
        when(customerService.getCustomer(customerId))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")));

        verify(customerService, times(1)).getCustomer(customerId);
    }

    @Test
    @DisplayName("GET /api/v1/customers - Should return all customers")
    void shouldReturnAllCustomers() throws Exception {
        // Given
        CustomerResponse customer2 = new CustomerResponse(
                87654323L,  // Valid Luhn checksum
                "Jane",
                "Smith",
                "jane@example.com",
                "+27829999999",
                Instant.now(),
                Instant.now()
        );
        List<CustomerResponse> customers = List.of(sampleCustomerResponse, customer2);
        when(customerService.getAllCustomers()).thenReturn(customers);

        // When & Then
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerId").value(sampleCustomerResponse.customerId()))
                .andExpect(jsonPath("$[1].customerId").value(customer2.customerId()));

        verify(customerService, times(1)).getAllCustomers();
    }

    @Test
    @DisplayName("GET /api/v1/customers - Should return empty list when no customers")
    void shouldReturnEmptyListWhenNoCustomers() throws Exception {
        // Given
        when(customerService.getAllCustomers()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(customerService, times(1)).getAllCustomers();
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - Should update customer successfully")
    void shouldUpdateCustomerSuccessfully() throws Exception {
        // Given - Valid Luhn customer ID
        Long customerId = 12345674L;
        CustomerResponse updatedResponse = new CustomerResponse(
                customerId,
                sampleUpdateRequest.firstName(),
                sampleUpdateRequest.lastName(),
                sampleUpdateRequest.email(),
                sampleUpdateRequest.mobileNumber(),
                Instant.now(),
                Instant.now()
        );
        when(customerService.updateCustomer(eq(customerId), any(UpdateCustomerRequest.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.firstName").value(sampleUpdateRequest.firstName()))
                .andExpect(jsonPath("$.lastName").value(sampleUpdateRequest.lastName()))
                .andExpect(jsonPath("$.email").value(sampleUpdateRequest.email()));

        verify(customerService, times(1)).updateCustomer(eq(customerId), any(UpdateCustomerRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - Should return 400 for invalid Luhn customer ID")
    void shouldReturnBadRequestWhenUpdatingWithInvalidLuhnId() throws Exception {
        // Given - Invalid Luhn customer ID
        Long invalidCustomerId = 99999999L;  // Invalid Luhn checksum

        // When & Then
        mockMvc.perform(put("/api/v1/customers/{id}", invalidCustomerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Customer ID must be a valid 8-digit number")));

        // Service should NOT be called due to validation failure
        verify(customerService, never()).updateCustomer(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - Should return 404 when updating non-existent customer")
    void shouldReturnNotFoundWhenUpdatingNonExistentCustomer() throws Exception {
        // Given - Valid Luhn but non-existent customer
        Long customerId = 99999997L;  // Valid Luhn checksum
        when(customerService.updateCustomer(eq(customerId), any(UpdateCustomerRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // When & Then
        mockMvc.perform(put("/api/v1/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")));

        verify(customerService, times(1)).updateCustomer(eq(customerId), any(UpdateCustomerRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Should delete customer successfully")
    void shouldDeleteCustomerSuccessfully() throws Exception {
        // Given - Valid Luhn customer ID
        Long customerId = 12345674L;
        doNothing().when(customerService).deleteCustomer(customerId);

        // When & Then
        mockMvc.perform(delete("/api/v1/customers/{id}", customerId))
                .andExpect(status().isNoContent());

        verify(customerService, times(1)).deleteCustomer(customerId);
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Should return 400 for invalid Luhn customer ID")
    void shouldReturnBadRequestWhenDeletingWithInvalidLuhnId() throws Exception {
        // Given - Invalid Luhn customer ID
        Long invalidCustomerId = 11111111L;  // Invalid Luhn checksum

        // When & Then
        mockMvc.perform(delete("/api/v1/customers/{id}", invalidCustomerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Customer ID must be a valid 8-digit number")));

        // Service should NOT be called due to validation failure
        verify(customerService, never()).deleteCustomer(any());
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Should return 404 when deleting non-existent customer")
    void shouldReturnNotFoundWhenDeletingNonExistentCustomer() throws Exception {
        // Given - Valid Luhn but non-existent customer
        Long customerId = 99999997L;  // Valid Luhn checksum
        doThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId))
                .when(customerService).deleteCustomer(customerId);

        // When & Then
        mockMvc.perform(delete("/api/v1/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")));

        verify(customerService, times(1)).deleteCustomer(customerId);
    }
}