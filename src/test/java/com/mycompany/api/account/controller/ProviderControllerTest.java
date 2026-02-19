/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/19/2026 at 9:55 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.dto.CreateProviderRequest;
import com.mycompany.api.account.dto.ProviderCreatedResponse;
import com.mycompany.api.account.dto.ProviderResponse;
import com.mycompany.api.account.dto.UpdateProviderRequest;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProviderController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 *
 * @author Oualid Gharach
 */
@WebMvcTest(ProviderController.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
@DisplayName("ProviderController Unit Tests")
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProviderService providerService;

    private ProviderResponse mpesaResponse;
    private ProviderCreatedResponse mpesaCreatedResponse;

    @BeforeEach
    void setUp() {
        mpesaResponse = new ProviderResponse(
                1L,
                "MPESA",
                "M-Pesa",
                "e7e75fe1",
                true,
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T10:00:00Z")
        );

        mpesaCreatedResponse = new ProviderCreatedResponse(
                1L,
                "MPESA",
                "M-Pesa",
                "e7e75fe1",
                true,
                "e7e75fe1-4192-4e34-af5e-6010d787c029",
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T10:00:00Z")
        );
    }

    // =========================================================================
    // POST /api/v1/providers - Onboard provider
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/providers - Should onboard provider successfully")
    void shouldOnboardProviderSuccessfully() throws Exception {
        // Given
        CreateProviderRequest request = new CreateProviderRequest("MPESA", "M-Pesa");
        when(providerService.onboardProvider(any(CreateProviderRequest.class)))
                .thenReturn(mpesaCreatedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.code", is("MPESA")))
                .andExpect(jsonPath("$.name", is("M-Pesa")))
                .andExpect(jsonPath("$.apiKeyPrefix", is("e7e75fe1")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.apiKey", is("e7e75fe1-4192-4e34-af5e-6010d787c029")));

        verify(providerService, times(1)).onboardProvider(any(CreateProviderRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/providers - Should return 400 when code is blank")
    void shouldReturn400WhenCodeIsBlank() throws Exception {
        // Given
        CreateProviderRequest request = new CreateProviderRequest("", "M-Pesa");

        // When & Then
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(providerService, never()).onboardProvider(any());
    }

    @Test
    @DisplayName("POST /api/v1/providers - Should return 400 when code contains lowercase")
    void shouldReturn400WhenCodeContainsLowercase() throws Exception {
        // Given
        CreateProviderRequest request = new CreateProviderRequest("mpesa", "M-Pesa");

        // When & Then
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.code",
                        is("Provider code must be uppercase letters, digits, or underscores")));

        verify(providerService, never()).onboardProvider(any());
    }

    @Test
    @DisplayName("POST /api/v1/providers - Should return 400 when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        // Given
        CreateProviderRequest request = new CreateProviderRequest("MPESA", "");

        // When & Then
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(providerService, never()).onboardProvider(any());
    }

    @Test
    @DisplayName("POST /api/v1/providers - Should return 409 when provider code already exists")
    void shouldReturn409WhenProviderCodeAlreadyExists() throws Exception {
        // Given
        CreateProviderRequest request = new CreateProviderRequest("MPESA", "M-Pesa");
        when(providerService.onboardProvider(any(CreateProviderRequest.class)))
                .thenThrow(new DuplicateResourceException("Provider with code MPESA already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Provider with code MPESA already exists")));
    }

    // =========================================================================
    // GET /api/v1/providers - List all providers
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/providers - Should return all providers")
    void shouldReturnAllProviders() throws Exception {
        // Given
        ProviderResponse mtnResponse = new ProviderResponse(
                2L, "MTN", "MTN Mobile Money", "0df87c1d", true,
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T10:00:00Z")
        );
        when(providerService.getAllProviders()).thenReturn(List.of(mpesaResponse, mtnResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("MPESA")))
                .andExpect(jsonPath("$[1].code", is("MTN")));
    }

    @Test
    @DisplayName("GET /api/v1/providers - Should return empty list when no providers")
    void shouldReturnEmptyListWhenNoProviders() throws Exception {
        // Given
        when(providerService.getAllProviders()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================================
    // GET /api/v1/providers/{id} - Get provider by ID
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/providers/{id} - Should return provider by ID")
    void shouldReturnProviderById() throws Exception {
        // Given
        when(providerService.getProvider(1L)).thenReturn(mpesaResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/providers/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.code", is("MPESA")))
                .andExpect(jsonPath("$.apiKey").doesNotExist()); // raw key never returned
    }

    @Test
    @DisplayName("GET /api/v1/providers/{id} - Should return 404 when provider not found")
    void shouldReturn404WhenProviderNotFound() throws Exception {
        // Given
        when(providerService.getProvider(99L))
                .thenThrow(new ResourceNotFoundException("Provider not found with ID: 99"));

        // When & Then
        mockMvc.perform(get("/api/v1/providers/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Provider not found with ID: 99")));
    }

    // =========================================================================
    // PATCH /api/v1/providers/{id} - Update provider name
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/providers/{id} - Should update provider name successfully")
    void shouldUpdateProviderNameSuccessfully() throws Exception {
        // Given
        UpdateProviderRequest request = new UpdateProviderRequest("M-Pesa Kenya");
        ProviderResponse updated = new ProviderResponse(
                1L, "MPESA", "M-Pesa Kenya", "e7e75fe1", true,
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T11:00:00Z")
        );
        when(providerService.updateProvider(eq(1L), any(UpdateProviderRequest.class)))
                .thenReturn(updated);

        // When & Then
        mockMvc.perform(patch("/api/v1/providers/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("M-Pesa Kenya")));
    }

    @Test
    @DisplayName("PATCH /api/v1/providers/{id} - Should return 400 when name is blank")
    void shouldReturn400WhenUpdateNameIsBlank() throws Exception {
        // Given
        UpdateProviderRequest request = new UpdateProviderRequest("");

        // When & Then
        mockMvc.perform(patch("/api/v1/providers/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(providerService, never()).updateProvider(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/providers/{id} - Should return 404 when provider not found")
    void shouldReturn404WhenUpdatingNonExistentProvider() throws Exception {
        // Given
        UpdateProviderRequest request = new UpdateProviderRequest("New Name");
        when(providerService.updateProvider(eq(99L), any(UpdateProviderRequest.class)))
                .thenThrow(new ResourceNotFoundException("Provider not found with ID: 99"));

        // When & Then
        mockMvc.perform(patch("/api/v1/providers/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // DELETE /api/v1/providers/{id} - Deactivate provider
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/providers/{id} - Should deactivate provider successfully")
    void shouldDeactivateProviderSuccessfully() throws Exception {
        // Given
        doNothing().when(providerService).deactivateProvider(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/providers/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(providerService, times(1)).deactivateProvider(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/providers/{id} - Should return 404 when provider not found")
    void shouldReturn404WhenDeactivatingNonExistentProvider() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Provider not found with ID: 99"))
                .when(providerService).deactivateProvider(99L);

        // When & Then
        mockMvc.perform(delete("/api/v1/providers/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /api/v1/providers/{id}/reactivate - Reactivate provider
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/providers/{id}/reactivate - Should reactivate provider successfully")
    void shouldReactivateProviderSuccessfully() throws Exception {
        // Given
        ProviderResponse reactivated = new ProviderResponse(
                1L, "MPESA", "M-Pesa", "e7e75fe1", true,
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T12:00:00Z")
        );
        when(providerService.reactivateProvider(1L)).thenReturn(reactivated);

        // When & Then
        mockMvc.perform(post("/api/v1/providers/{id}/reactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/providers/{id}/reactivate - Should return 404 when provider not found")
    void shouldReturn404WhenReactivatingNonExistentProvider() throws Exception {
        // Given
        when(providerService.reactivateProvider(99L))
                .thenThrow(new ResourceNotFoundException("Provider not found with ID: 99"));

        // When & Then
        mockMvc.perform(post("/api/v1/providers/{id}/reactivate", 99L))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /api/v1/providers/{id}/regenerate-key - Regenerate API key
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/providers/{id}/regenerate-key - Should regenerate key successfully")
    void shouldRegenerateApiKeySuccessfully() throws Exception {
        // Given
        ProviderCreatedResponse regenerated = new ProviderCreatedResponse(
                1L, "MPESA", "M-Pesa", "c9ca2daa", true,
                "c9ca2daa-1234-4e34-af5e-6010d787c029",
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T12:00:00Z")
        );
        when(providerService.regenerateApiKey(1L)).thenReturn(regenerated);

        // When & Then
        mockMvc.perform(post("/api/v1/providers/{id}/regenerate-key", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKeyPrefix", is("c9ca2daa")))
                .andExpect(jsonPath("$.apiKey", is("c9ca2daa-1234-4e34-af5e-6010d787c029")));
    }

    @Test
    @DisplayName("POST /api/v1/providers/{id}/regenerate-key - Should return 404 when provider not found")
    void shouldReturn404WhenRegeneratingKeyForNonExistentProvider() throws Exception {
        // Given
        when(providerService.regenerateApiKey(99L))
                .thenThrow(new ResourceNotFoundException("Provider not found with ID: 99"));

        // When & Then
        mockMvc.perform(post("/api/v1/providers/{id}/regenerate-key", 99L))
                .andExpect(status().isNotFound());
    }
}