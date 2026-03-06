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
import com.mycompany.api.account.BaseWebMvcTest;
import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.dto.CustomerSummaryResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.service.CustomerService;
import com.mycompany.api.account.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CustomerController.
 * Tests HTTP layer — request/response, validation, and service exception mapping.
 *
 * @author Oualid Gharach
 */
@WebMvcTest(CustomerController.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
@DisplayName("CustomerController Unit Tests")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class CustomerControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private ProviderService providerService;

    private CustomerDetailedResponse sampleCustomerResponse;
    private CustomerSummaryResponse sampleSummary;
    private CreateCustomerRequest sampleCreateRequest;
    private UpdateCustomerRequest sampleUpdateRequest;

    @BeforeEach
    void setUp() {
        sampleCustomerResponse = new CustomerDetailedResponse(
                12345674L,
                "John",
                "Doe",
                "john@email.com",
                "0123456789",
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        sampleSummary = new CustomerSummaryResponse(
                12345674L,
                "John",
                "Doe",
                "john@email.com",
                "0123456789"
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

    // -------------------------------------------------------------------------
    // POST /api/v1/customers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create customer and return 201 with Location header")
    void shouldCreateCustomerAndReturn201() throws Exception {
        when(customerService.createCustomer(any())).thenReturn(sampleCustomerResponse);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.customerId", is(12345674)))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(customerService.createCustomer(any()))
                .thenThrow(new DuplicateResourceException("Customer with email already exists"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Should return 400 when create request is missing required fields")
    void shouldReturn400WhenCreateRequestInvalid() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/customers/{customerId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return customer when found by ID")
    void shouldReturnCustomerWhenFoundById() throws Exception {
        when(customerService.getCustomer(12345674L)).thenReturn(sampleCustomerResponse);

        mockMvc.perform(get("/api/v1/customers/12345674"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(12345674)))
                .andExpect(jsonPath("$.firstName", is("John")));
    }

    @Test
    @DisplayName("Should return 404 when customer not found by ID")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.getCustomer(12345674L))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: 12345674"));

        mockMvc.perform(get("/api/v1/customers/12345674"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @DisplayName("Should return 400 when customer ID fails Luhn validation")
    void shouldReturn400WhenCustomerIdFailsLuhn() throws Exception {
        mockMvc.perform(get("/api/v1/customers/12345671"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Customer ID must be a valid 8-digit number")));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/customers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return paginated customer list")
    void shouldReturnPaginatedCustomerList() throws Exception {
        Page<CustomerSummaryResponse> page = new PageImpl<>(
                List.of(sampleSummary),
                PageRequest.of(0, 10),
                1
        );
        when(customerService.getAllCustomers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/customers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].customerId", is(12345674)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("Should return empty page when no customers exist")
    void shouldReturnEmptyPageWhenNoCustomers() throws Exception {
        Page<CustomerSummaryResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        );
        when(customerService.getAllCustomers(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/customers/search
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return paginated results when searching by mobile")
    void shouldReturnPaginatedResultsWhenSearchingByMobile() throws Exception {
        Page<CustomerSummaryResponse> page = new PageImpl<>(
                List.of(sampleSummary),
                PageRequest.of(0, 10),
                1
        );
        when(customerService.searchByMobileNumber(eq("082"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/customers/search")
                        .param("type", "mobile")
                        .param("value", "082"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].mobileNumber", is("0123456789")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("Should return paginated results when searching by surname")
    void shouldReturnPaginatedResultsWhenSearchingBySurname() throws Exception {
        Page<CustomerSummaryResponse> page = new PageImpl<>(
                List.of(sampleSummary),
                PageRequest.of(0, 10),
                1
        );
        when(customerService.searchBySurname(eq("doe"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/customers/search")
                        .param("type", "surname")
                        .param("value", "doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].lastName", is("Doe")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("Should return empty page when no customers match search")
    void shouldReturnEmptyPageWhenNoSearchMatches() throws Exception {
        Page<CustomerSummaryResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        );
        when(customerService.searchByMobileNumber(any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/customers/search")
                        .param("type", "mobile")
                        .param("value", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("Should return 400 when search type is invalid")
    void shouldReturn400WhenSearchTypeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/customers/search")
                        .param("type", "email")
                        .param("value", "john@example.com"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/customers/{customerId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should update customer successfully")
    void shouldUpdateCustomerSuccessfully() throws Exception {
        when(customerService.updateCustomer(eq(12345674L), any()))
                .thenReturn(sampleCustomerResponse);

        mockMvc.perform(put("/api/v1/customers/12345674")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(12345674)));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent customer")
    void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
        when(customerService.updateCustomer(eq(12345674L), any()))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: 12345674"));

        mockMvc.perform(put("/api/v1/customers/12345674")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when updating customer with duplicate email")
    void shouldReturn409WhenUpdatingWithDuplicateEmail() throws Exception {
        when(customerService.updateCustomer(eq(12345674L), any()))
                .thenThrow(new DuplicateResourceException("Customer with email already exists"));

        mockMvc.perform(put("/api/v1/customers/12345674")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/customers/{customerId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should delete customer and return 204")
    void shouldDeleteCustomerAndReturn204() throws Exception {
        doNothing().when(customerService).deleteCustomer(12345674L);

        mockMvc.perform(delete("/api/v1/customers/12345674"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent customer")
    void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
        doThrow(new ResourceNotFoundException("Customer not found: 12345674"))
                .when(customerService).deleteCustomer(12345674L);

        mockMvc.perform(delete("/api/v1/customers/12345674"))
                .andExpect(status().isNotFound());
    }
}