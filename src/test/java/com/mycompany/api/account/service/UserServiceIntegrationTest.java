/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/27/2026 at 8:55 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.dto.ChangePasswordRequest;
import com.mycompany.api.account.dto.CreateUserRequest;
import com.mycompany.api.account.dto.UpdateUserRequest;
import com.mycompany.api.account.dto.UserResponse;
import com.mycompany.api.account.enums.Role;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for UserService.
 * Tests actual database persistence with Testcontainers PostgreSQL.
 *
 * @author Oualid Gharach
 */
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder; // [ADDED] to verify resetPassword hashes correctly

    private CreateUserRequest adminRequest;
    private CreateUserRequest operatorRequest;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM users WHERE username != 'admin'");

        adminRequest = new CreateUserRequest(
                "testadmin",
                "Test",
                "Admin",
                "testadmin@utility.local",
                "password123",
                Role.ROLE_ADMIN
        );

        operatorRequest = new CreateUserRequest(
                "testoperator",
                "Test",
                "Operator",
                "testoperator@utility.local",
                "password123",
                Role.ROLE_OPERATOR
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users WHERE username != 'admin'");
    }

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create user and normalise username and email")
    void shouldCreateUserAndNormalise() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "  TestAdmin  ",
                "Test",
                "Admin",
                "  TestAdmin@Utility.LOCAL  ",
                "password123",
                Role.ROLE_ADMIN
        );

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertThat(response.username()).isEqualTo("testadmin");
        assertThat(response.email()).isEqualTo("testadmin@utility.local");
        assertThat(response.role()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(response.enabled()).isTrue();
        assertThat(response.id()).isNotNull();
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when username already exists")
    void shouldThrowOnDuplicateUsername() {
        // Given
        userService.createUser(adminRequest);

        CreateUserRequest duplicate = new CreateUserRequest(
                "testadmin",
                "Another",
                "Admin",
                "another@utility.local",
                "password123",
                Role.ROLE_ADMIN
        );

        // When & Then
        assertThatThrownBy(() -> userService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("testadmin");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void shouldThrowOnDuplicateEmail() {
        // Given
        userService.createUser(adminRequest);

        CreateUserRequest duplicate = new CreateUserRequest(
                "anotheradmin",
                "Another",
                "Admin",
                "testadmin@utility.local",
                "password123",
                Role.ROLE_ADMIN
        );

        // When & Then
        assertThatThrownBy(() -> userService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("testadmin@utility.local");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate username case insensitive")
    void shouldThrowOnDuplicateUsernameCaseInsensitive() {
        // Given
        userService.createUser(adminRequest);

        CreateUserRequest duplicate = new CreateUserRequest(
                "TESTADMIN",
                "Another",
                "Admin",
                "another@utility.local",
                "password123",
                Role.ROLE_ADMIN
        );

        // When & Then
        assertThatThrownBy(() -> userService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // -------------------------------------------------------------------------
    // updateUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateUserSuccessfully() {
        // Given
        UserResponse created = userService.createUser(operatorRequest);

        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "Updated",
                "Name",
                "updated@utility.local",
                Role.ROLE_OPERATOR,
                true
        );

        // When
        UserResponse updated = userService.updateUser(created.id(), updateRequest, "admin");

        // Then
        assertThat(updated.firstName()).isEqualTo("Updated");
        assertThat(updated.lastName()).isEqualTo("Name");
        assertThat(updated.email()).isEqualTo("updated@utility.local");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when admin tries to change own role")
    void shouldThrowWhenAdminChangesOwnRole() {
        // Given — get the seeded admin user ID
        Long adminId = userRepository.findByUsername("admin")
                .orElseThrow().getId();

        UpdateUserRequest request = new UpdateUserRequest(
                "Admin",
                "User",
                "admin@utility.local",
                Role.ROLE_OPERATOR, // Trying to downgrade own role
                true
        );

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(adminId, request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own role");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating to existing email")
    void shouldThrowWhenUpdatingToExistingEmail() {
        // Given
        userService.createUser(adminRequest);
        UserResponse operator = userService.createUser(operatorRequest);

        UpdateUserRequest request = new UpdateUserRequest(
                "Test",
                "Operator",
                "testadmin@utility.local", // Already taken by adminRequest
                Role.ROLE_OPERATOR,
                true
        );

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(operator.id(), request, "admin"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("testadmin@utility.local");
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        // Given
        UserResponse created = userService.createUser(operatorRequest);

        // When
        userService.deleteUser(created.id(), "admin");

        // Then
        assertThatThrownBy(() -> userService.getUserById(created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when admin tries to delete own account")
    void shouldThrowWhenAdminDeletesOwnAccount() {
        // Given
        Long adminId = userRepository.findByUsername("admin")
                .orElseThrow().getId();

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(adminId, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own account");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
    void shouldThrowWhenDeletingNonExistentUser() {
        assertThatThrownBy(() -> userService.deleteUser(999999L, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should allow user to change own password")
    void shouldAllowUserToChangeOwnPassword() {
        // Given
        UserResponse created = userService.createUser(operatorRequest);

        UserDetails operatorDetails = User.builder()
                .username("testoperator")
                .password("password123")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_OPERATOR.name())))
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newpassword123");

        // When & Then — should not throw
        userService.changePassword(created.id(), request, operatorDetails);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when admin tries to change another user's password")
        // [ADDED] changePassword is own-only — admin must use resetPassword instead
    void shouldThrowWhenAdminChangesOtherUserPassword() {
        // Given
        UserResponse operator = userService.createUser(operatorRequest);

        UserDetails adminDetails = User.builder()
                .username("admin")
                .password("admin")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_ADMIN.name())))
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newpassword123");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(operator.id(), request, adminDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own password");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when operator tries to change another user's password")
    void shouldThrowWhenOperatorChangesOtherUserPassword() {
        // Given
        userService.createUser(adminRequest);
        UserResponse operator = userService.createUser(operatorRequest);

        UserDetails anotherOperator = User.builder()
                .username("anotheroperator")
                .password("password123")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_OPERATOR.name())))
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newpassword123");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(operator.id(), request, anotherOperator))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own password");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when current password is incorrect")
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        // Given
        UserResponse created = userService.createUser(operatorRequest);

        UserDetails operatorDetails = User.builder()
                .username("testoperator")
                .password("password123")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_OPERATOR.name())))
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("wrongpassword", "newpassword123");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(created.id(), request, operatorDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    // -------------------------------------------------------------------------
    // resetPassword
    // [ADDED] Admin-only operation — generates and returns a temp password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should reset user password and return temporary password")
    void shouldResetPasswordAndReturnTempPassword() {
        // Given
        UserResponse operator = userService.createUser(operatorRequest);

        // When
        String tempPassword = userService.resetPassword(operator.id());

        // Then — temp password returned and non-blank
        assertThat(tempPassword).isNotBlank();
        assertThat(tempPassword).hasSize(12);

        // And — the stored hash matches the returned temp password
        String storedHash = userRepository.findById(operator.id())
                .orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches(tempPassword, storedHash)).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when resetting non-existent user")
    void shouldThrowWhenResettingNonExistentUser() {
        assertThatThrownBy(() -> userService.resetPassword(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    // -------------------------------------------------------------------------
    // getAllUsers / getUserById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() {
        // Given
        userService.createUser(adminRequest);
        userService.createUser(operatorRequest);

        // When
        List<UserResponse> users = userService.getAllUsers();

        // Then — at least the two we created plus the seeded admin
        assertThat(users).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
    void shouldThrowWhenUserNotFound() {
        assertThatThrownBy(() -> userService.getUserById(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }
}