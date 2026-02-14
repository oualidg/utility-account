/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/4/2026 at 3:10 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.dto.CustomerSummaryResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import com.mycompany.api.account.validation.ValidLuhn;

/**
 * REST Controller for Customer operations.
 * Provides endpoints for creating, reading, updating, and deleting customers.
 *
 * @author Oualid Gharach
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Create a new customer.
     *
     * @param request the customer creation request
     * @return the created customer with 201 status and Location header
     */
    @PostMapping
    @Operation(summary = "Create a new customer", description = "Creates a new customer with auto-generated customer ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerDetailedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Customer with email already exists")
    })
    public ResponseEntity<CustomerDetailedResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        log.info("REST request to create customer with email: {}", request.email());

        CustomerDetailedResponse response = customerService.createCustomer(request);

        // Build Location header: /api/v1/customers/{customerId}
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{customerId}")
                .buildAndExpand(response.customerId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get customer by ID.
     *
     * @param customerId the customer ID
     * @return the customer details
     */
    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer details by customer ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found",
                    content = @Content(schema = @Schema(implementation = CustomerDetailedResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerDetailedResponse> getCustomer(
            @Parameter(description = "Customer ID", example = "12345670")
            @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number")
            @PathVariable Long customerId) {

        log.info("REST request to get customer: {}", customerId);

        CustomerDetailedResponse response = customerService.getCustomer(customerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all customers.
     *
     * @return list of all customers
     */
    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieves all customers in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    public ResponseEntity<List<CustomerSummaryResponse>> getAllCustomers() {

        log.info("REST request to get all customers");

        List<CustomerSummaryResponse> customers = customerService.getAllCustomers();

        return ResponseEntity.ok(customers);
    }

    /**
     * Search customers by mobile number.
     *
     * @param mobile the mobile number to search for
     * @return list of matching customers
     */
    @GetMapping("/search")
    @Operation(summary = "Search customers by mobile number",
            description = "Searches for customers whose mobile number contains the given string")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<List<CustomerSummaryResponse>> searchByMobile(
            @Parameter(description = "Mobile number to search for", example = "082")
            @RequestParam String mobile) {

        log.info("REST request to search customers by mobile: {}", mobile);

        List<CustomerSummaryResponse> customers = customerService.searchByMobileNumber(mobile);

        return ResponseEntity.ok(customers);
    }

    /**
     * Update an existing customer.
     *
     * @param customerId the customer ID
     * @param request the update request
     * @return the updated customer
     */
    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer", description = "Updates an existing customer (partial update supported)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerDetailedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<CustomerDetailedResponse> updateCustomer(
            @Parameter(description = "Customer ID", example = "12345670")
            @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number")
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {

        log.info("REST request to update customer: {}", customerId);

        CustomerDetailedResponse response = customerService.updateCustomer(customerId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a customer.
     *
     * @param customerId the customer ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete customer", description = "Permanently deletes a customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Customer ID", example = "12345670")
            @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number")
            @PathVariable Long customerId) {

        log.info("REST request to delete customer: {}", customerId);

        customerService.deleteCustomer(customerId);

        return ResponseEntity.noContent().build();
    }
}