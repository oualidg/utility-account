/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/25/2026 at 10:24 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/25/2026 at 10:24 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.ChangePasswordRequest;
import com.mycompany.api.account.dto.CreateUserRequest;
import com.mycompany.api.account.dto.ResetPasswordResponse;
import com.mycompany.api.account.dto.UpdateUserRequest;
import com.mycompany.api.account.dto.UserResponse;
import com.mycompany.api.account.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for user management.
 *
 * <p>All endpoints require {@code ROLE_ADMIN} except:</p>
 * <ul>
 *   <li>{@code PUT /{id}/password} — also allows {@code ROLE_OPERATOR}
 *       to change their own password (enforced in service layer)</li>
 * </ul>
 *
 * <p>Password operations are split into two distinct endpoints:</p>
 * <ul>
 *   <li>{@code PUT /{id}/password} — user changes own password, must provide current</li>
 *   <li>{@code POST /{id}/reset-password} — admin resets any user's password,
 *       system generates a temporary password returned once</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users
     * Returns all users.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("REST request to get all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/v1/users/{id}
     * Returns a single user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("REST request to get user by ID: {}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * POST /api/v1/users
     * Creates a new user. Returns 201 with Location header.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("REST request to create user: {}", request.username());
        UserResponse created = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * PUT /api/v1/users/{id}
     * Updates a user's profile, role, and enabled status.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        log.info("REST request to update user ID: {}", id);
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser.getUsername()));
    }

    /**
     * DELETE /api/v1/users/{id}
     * Permanently deletes a user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        log.info("REST request to delete user ID: {}", id);
        userService.deleteUser(id, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/users/{id}/password
     * Changes a user's own password. Must provide current password for verification.
     * Both admins and operators can only change their own password.
     * Admins resetting another user's password should use POST /{id}/reset-password.
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        log.info("REST request to change password for user ID: {}", id);
        userService.changePassword(id, request, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/users/{id}/reset-password
     * Admin resets a user's password to a system-generated temporary password.
     * The temporary password is returned once — admin must communicate it securely.
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        log.info("REST request to reset password for user ID: {}", id);
        String temporaryPassword = userService.resetPassword(id);
        return ResponseEntity.ok(new ResetPasswordResponse(temporaryPassword));
    }
}