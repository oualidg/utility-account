/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/25/2026 at 9:39 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/25/2026 at 9:39 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.ChangePasswordRequest;
import com.mycompany.api.account.dto.CreateUserRequest;
import com.mycompany.api.account.dto.UpdateUserRequest;
import com.mycompany.api.account.dto.UserResponse;
import com.mycompany.api.account.entity.User;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.UserMapper;
import com.mycompany.api.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Service for user management operations.
 *
 * <p>Enforces business rules around user lifecycle — creation, updates,
 * password management, and deletion. Guards against self-role modification
 * to prevent privilege escalation accidents.</p>
 *
 * <p>Password operations are split into two distinct flows:</p>
 * <ul>
 *   <li>{@link #changePassword} — user changes their own password, must verify current</li>
 *   <li>{@link #resetPassword} — admin resets another user's password, system generates temp</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    /**
     * Creates a new user with a BCrypt-encoded password.
     *
     * @throws DuplicateResourceException if username or email already exists
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = userMapper.toEntity(request);

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + user.getEmail());
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        try {
            User saved = userRepository.save(user);
            log.info("Created user: {} with role: {}", saved.getUsername(), saved.getRole());
            return userMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Username or email already exists");
        }
    }

    /**
     * Updates a user's profile and status.
     *
     * @throws DuplicateResourceException if new email already exists
     * @throws IllegalArgumentException   if attempting self-role modification
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUsername) {
        User user = findById(id);

        if (user.getUsername().equals(currentUsername) && !user.getRole().equals(request.role())) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        String normalizedEmail = userMapper.normalizeEmail(request.email());
        if (!user.getEmail().equals(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists: " + normalizedEmail);
        }

        userMapper.updateEntity(request, user);

        try {
            log.info("Updated user: {}", user.getUsername());
            return userMapper.toResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    /**
     * Permanently deletes a user.
     *
     * @throws IllegalArgumentException if attempting self-deletion
     */
    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User user = findById(id);

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    /**
     * Changes a user's own password after verifying the current password.
     *
     * <p>Users can only change their own password. Admins resetting another
     * user's password should use {@link #resetPassword(Long)}.</p>
     *
     * @throws AccessDeniedException    if attempting to change another user's password
     * @throws IllegalArgumentException if the current password is incorrect
     */
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request, UserDetails currentUser) {
        User user = findById(id);

        if (!user.getUsername().equals(currentUser.getUsername())) {
            throw new AccessDeniedException("You can only change your own password");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Resets a user's password to a system-generated temporary password.
     *
     * <p>Admin-only operation. The temporary password is returned once and
     * must be communicated to the user securely. The user should change it
     * on next login.</p>
     *
     * @return the generated temporary password (plaintext, shown once)
     */
    @Transactional
    public String resetPassword(Long id) {
        User user = findById(id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getUsername());
        return temporaryPassword;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}