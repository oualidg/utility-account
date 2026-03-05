/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/27/2026 at 9:33 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.BaseWebMvcTest;
import com.mycompany.api.account.dto.*;
import com.mycompany.api.account.enums.Role;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
 * Unit tests for UserController.
 * Tests HTTP layer — request/response, validation, and role-based access.
 *
 * @author Oualid Gharach
 */

@TestPropertySource(properties = "spring.main.banner-mode=off")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserResponse sampleUserResponse;
    private CreateUserRequest validCreateRequest;
    private UpdateUserRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        sampleUserResponse = new UserResponse(
                1L,
                "operator1",
                "John",
                "Doe",
                "john.doe@utility.local",
                Role.ROLE_OPERATOR,
                true,
                null,
                Instant.now()
        );

        validCreateRequest = new CreateUserRequest(
                "operator1",
                "John",
                "Doe",
                "john.doe@utility.local",
                Role.ROLE_OPERATOR
        );

        validUpdateRequest = new UpdateUserRequest(
                "Jane",
                "Smith",
                "jane.smith@utility.local",
                Role.ROLE_OPERATOR,
                true
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/users - Should return all users")
    void shouldReturnAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleUserResponse));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("operator1"))
                .andExpect(jsonPath("$[0].role").value("ROLE_OPERATOR"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("GET /api/v1/users - Should return empty list when no users")
    void shouldReturnEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return user by ID")
    void shouldReturnUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUserResponse);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("operator1"))
                .andExpect(jsonPath("$.email").value("john.doe@utility.local"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/users - Should create user and return 201")
    void shouldCreateUserSuccessfully() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(new CreateUserResponse(sampleUserResponse, "TempPass1@"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/users/1")))
                .andExpect(jsonPath("$.user.username").value("operator1"));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 409 when username already exists")
    void shouldReturn409OnDuplicateUsername() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: operator1"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 400 for missing required fields")
    void shouldReturn400ForMissingFields() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                null, // missing username
                "John",
                "Doe",
                "john@utility.local",
                Role.ROLE_OPERATOR
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 400 for invalid email")
    void shouldReturn400ForInvalidEmail() throws Exception {
        CreateUserRequest invalidEmail = new CreateUserRequest(
                "operator1",
                "John",
                "Doe",
                "not-an-email",
                Role.ROLE_OPERATOR
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        verify(userService, never()).createUser(any());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should update user successfully")
    void shouldUpdateUserSuccessfully() throws Exception {
        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class), any()))
                .thenReturn(sampleUserResponse);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("operator1"));

        verify(userService, times(1)).updateUser(eq(1L), any(UpdateUserRequest.class), any());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should return 404 when user not found")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        when(userService.updateUser(eq(999L), any(UpdateUserRequest.class), any()))
                .thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

        mockMvc.perform(put("/api/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should return 400 when admin tries to change own role")
    void shouldReturn400WhenAdminChangesOwnRole() throws Exception {
        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class), any()))
                .thenThrow(new IllegalArgumentException("You cannot change your own role"));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should delete user and return 204")
    void shouldDeleteUserSuccessfully() throws Exception {
        doNothing().when(userService).deleteUser(eq(1L), any());

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should return 404 when user not found")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with ID: 999"))
                .when(userService).deleteUser(eq(999L), any());

        mockMvc.perform(delete("/api/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should return 400 when admin tries to delete own account")
    void shouldReturn400WhenAdminDeletesOwnAccount() throws Exception {
        doThrow(new IllegalArgumentException("You cannot delete your own account"))
                .when(userService).deleteUser(eq(1L), any());

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/{id}/password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Should change password and return 204")
    void shouldChangePasswordSuccessfully() throws Exception {
        doNothing().when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class), any());

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1@", "NewPassword1@");

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).changePassword(eq(1L), any(ChangePasswordRequest.class), any());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Should return 400 for weak new password")
    void shouldReturn400ForWeakNewPassword() throws Exception {
        ChangePasswordRequest weakRequest = new ChangePasswordRequest(
                "OldPassword1@",
                "weakpassword" // no uppercase, no digit, no special char
        );

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.newPassword").exists());

        verify(userService, never()).changePassword(any(), any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Should return 400 when current password is incorrect")
    void shouldReturn400WhenCurrentPasswordIncorrect() throws Exception {
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class), any());

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword1@", "NewPassword1@");

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Should return 403 when admin tries to change another user's password")
    void shouldReturn403WhenAdminChangesOtherUserPassword() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("You can only change your own password"))
                .when(userService).changePassword(eq(2L), any(ChangePasswordRequest.class), any());

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1@", "NewPassword1@");

        mockMvc.perform(put("/api/v1/users/2/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Should return 403 when operator tries to change another user's password")
    @WithMockUser(roles = "OPERATOR")
    void shouldReturn403WhenOperatorChangesOtherUserPassword() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("You can only change your own password"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class), any());

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1@", "NewPassword1@");

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users/{id}/reset-password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/users/{id}/reset-password - Should reset password and return temp password")
    void shouldResetPasswordSuccessfully() throws Exception {
        when(userService.resetPassword(1L)).thenReturn("Temp@123456");

        mockMvc.perform(post("/api/v1/users/1/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("Temp@123456"));

        verify(userService, times(1)).resetPassword(1L);
    }

    @Test
    @DisplayName("POST /api/v1/users/{id}/reset-password - Should return 404 when user not found")
    void shouldReturn404WhenResettingNonExistentUser() throws Exception {
        when(userService.resetPassword(999L))
                .thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

        mockMvc.perform(post("/api/v1/users/999/reset-password"))
                .andExpect(status().isNotFound());
    }
}