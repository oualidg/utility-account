/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 9:23 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.CreateProviderRequest;
import com.mycompany.api.account.dto.ProviderCreatedResponse;
import com.mycompany.api.account.dto.ProviderResponse;
import com.mycompany.api.account.dto.UpdateProviderRequest;
import com.mycompany.api.account.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * REST controller for payment provider management.
 * Handles onboarding, updates, deactivation, reactivation, and API key regeneration.
 *
 * TODO Phase 6D: Secure with JWT — ROLE_ADMIN for onboard/deactivate/reactivate/regenerate,
 *                ROLE_STAFF for read operations.
 *
 * @author Oualid Gharach
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Payment provider management")
@Slf4j
public class ProviderController {

    private final ProviderService providerService;

    /**
     * Onboard a new payment provider.
     * Generates an API key shown once — the caller must store it securely.
     *
     * @param request provider details (code, name)
     * @return created provider with raw API key and 201 status with Location header
     */
    @PostMapping
    @Operation(summary = "Onboard provider", description = "Register a new payment provider. API key is shown once — store it securely.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Provider onboarded successfully",
                    content = @Content(schema = @Schema(implementation = ProviderCreatedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Provider with code already exists")
    })
    public ResponseEntity<ProviderCreatedResponse> onboardProvider(
            @Valid @RequestBody CreateProviderRequest request) {

        log.info("REST request to onboard provider: code={}", request.code());

        ProviderCreatedResponse response = providerService.onboardProvider(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get all providers, active and inactive.
     *
     * @return list of all providers
     */
    @GetMapping
    @Operation(summary = "List all providers", description = "Returns all providers, active and inactive.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Providers retrieved successfully")
    })
    public ResponseEntity<List<ProviderResponse>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    /**
     * Get provider by ID.
     *
     * @param id provider ID
     * @return provider details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get provider by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider found",
                    content = @Content(schema = @Schema(implementation = ProviderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<ProviderResponse> getProvider(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProvider(id));
    }

    /**
     * Update provider display name.
     *
     * @param id provider ID
     * @param request update request containing new name
     * @return updated provider details
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Update provider name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider updated successfully",
                    content = @Content(schema = @Schema(implementation = ProviderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<ProviderResponse> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderRequest request) {

        log.info("REST request to update provider: id={}", id);
        return ResponseEntity.ok(providerService.updateProvider(id, request));
    }

    /**
     * Deactivate a provider (soft delete).
     * Cache entry is invalidated immediately.
     *
     * @param id provider ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate provider", description = "Soft deletes the provider. Cache invalidated immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Provider deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<Void> deactivateProvider(@PathVariable Long id) {

        log.info("REST request to deactivate provider: id={}", id);
        providerService.deactivateProvider(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivate a previously deactivated provider.
     * Cache will self-populate on their next authentication attempt.
     *
     * @param id provider ID
     * @return reactivated provider details
     */
    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate provider", description = "Reactivates a previously deactivated provider.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider reactivated successfully",
                    content = @Content(schema = @Schema(implementation = ProviderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<ProviderResponse> reactivateProvider(@PathVariable Long id) {

        log.info("REST request to reactivate provider: id={}", id);
        return ResponseEntity.ok(providerService.reactivateProvider(id));
    }

    /**
     * Regenerate API key for a provider.
     * New key is shown once — old key is invalidated immediately.
     *
     * @param id provider ID
     * @return provider with new raw API key (one-time display)
     */
    @PostMapping("/{id}/regenerate-key")
    @Operation(summary = "Regenerate API key", description = "Issues a new API key. Shown once — old key invalidated immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API key regenerated successfully",
                    content = @Content(schema = @Schema(implementation = ProviderCreatedResponse.class))),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<ProviderCreatedResponse> regenerateApiKey(@PathVariable Long id) {

        log.info("REST request to regenerate API key: id={}", id);
        return ResponseEntity.ok(providerService.regenerateApiKey(id));
    }
}