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
import com.mycompany.api.account.validation.ValidLuhn;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST Controller for Customer operations.
 * Provides endpoints for creating, reading, updating, and deleting customers.
 *
 * <p>Pagination is supported on list and search endpoints via Spring's {@link Pageable}
 * mechanism. Clients pass {@code ?page=0&size=10&sort=lastName,asc} as query parameters.
 * Default page size is 10, sorted by last name ascending.</p>
 *
 * @author Oualid Gharach
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
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

        return ResponseEntity.ok(customerService.getCustomer(customerId));
    }

    /**
     * Get a paginated list of all customers.
     *
     * <p>Supports standard Spring {@link Pageable} query parameters:
     * {@code ?page=0&size=10&sort=lastName,asc}</p>
     *
     * @param pageable pagination and sort parameters (default: page=0, size=10, sort=lastName asc)
     * @return page of customer summary responses
     */
    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieves a paginated list of all customers. " +
            "Supports ?page=0&size=10&sort=lastName,asc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    public ResponseEntity<Page<CustomerSummaryResponse>> getAllCustomers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("REST request to get all customers - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    /**
     * Search customers by mobile number or surname.
     *
     * <p>The {@code type} parameter controls the search mode:
     * <ul>
     *   <li>{@code mobile} — finds customers whose mobile number contains the given value</li>
     *   <li>{@code surname} — finds customers whose last name contains the given value (case-insensitive)</li>
     * </ul>
     * Supports standard Spring {@link Pageable} query parameters.</p>
     *
     * @param type     search type: {@code mobile} or {@code surname}
     * @param value    the search term
     * @param pageable pagination and sort parameters (default: page=0, size=10, sort=lastName asc)
     * @return page of matching customer summary responses
     */
    @GetMapping("/search")
    @Operation(summary = "Search customers",
            description = "Search customers by mobile number or surname. Use ?type=mobile&value=082 or ?type=surname&value=smith")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search type")
    })
    public ResponseEntity<Page<CustomerSummaryResponse>> search(
            @Parameter(description = "Search type: mobile or surname", example = "surname")
            @RequestParam String type,

            @Parameter(description = "Search value", example = "smith")
            @RequestParam String value,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("REST request to search customers - type: {}, value: {}", type, value);

        return switch (type.toLowerCase()) {
            case "mobile" -> ResponseEntity.ok(
                    customerService.searchByMobileNumber(value, pageable));
            case "surname" -> ResponseEntity.ok(
                    customerService.searchBySurname(value, pageable));
            default -> throw new IllegalArgumentException(
                    "Invalid search type '" + type + "'. Must be 'mobile' or 'surname'");
        };
    }

    /**
     * Update an existing customer.
     *
     * @param customerId the customer ID
     * @param request    the update request
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

        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    /**
     * Delete a customer.
     *
     * @param customerId the customer ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete customer", description = "Soft-deletes a customer")
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