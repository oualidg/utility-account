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
import com.mycompany.api.account.dto.CreateUserResponse;
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

@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private CreateUserRequest adminRequest;
    private CreateUserRequest operatorRequest;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM users WHERE username != 'admin'");
        adminRequest = new CreateUserRequest("testadmin", "Test", "Admin", "testadmin@utility.local", Role.ROLE_ADMIN);
        operatorRequest = new CreateUserRequest("testoperator", "Test", "Operator", "testoperator@utility.local", Role.ROLE_OPERATOR);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users WHERE username != 'admin'");
    }

    @Test
    @DisplayName("Should create user and normalise username and email")
    void shouldCreateUserAndNormalise() {
        CreateUserRequest request = new CreateUserRequest("  TestAdmin  ", "Test", "Admin", "  TestAdmin@Utility.LOCAL  ", Role.ROLE_ADMIN);
        CreateUserResponse response = userService.createUser(request);
        assertThat(response.user().username()).isEqualTo("testadmin");
        assertThat(response.user().email()).isEqualTo("testadmin@utility.local");
        assertThat(response.user().role()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(response.user().enabled()).isTrue();
        assertThat(response.user().id()).isNotNull();
        assertThat(response.temporaryPassword()).isNotBlank();
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when username already exists")
    void shouldThrowOnDuplicateUsername() {
        userService.createUser(adminRequest);
        CreateUserRequest duplicate = new CreateUserRequest("testadmin", "Another", "Admin", "another@utility.local", Role.ROLE_ADMIN);
        assertThatThrownBy(() -> userService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("testadmin");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void shouldThrowOnDuplicateEmail() {
        userService.createUser(adminRequest);
        CreateUserRequest duplicate = new CreateUserRequest("anotheradmin", "Another", "Admin", "testadmin@utility.local", Role.ROLE_ADMIN);
        assertThatThrownBy(() -> userService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("testadmin@utility.local");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate username case insensitive")
    void shouldThrowOnDuplicateUsernameCaseInsensitive() {
        userService.createUser(adminRequest);
        CreateUserRequest duplicate = new CreateUserRequest("TESTADMIN", "Another", "Admin", "another@utility.local", Role.ROLE_ADMIN);
        assertThatThrownBy(() -> userService.createUser(duplicate)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateUserSuccessfully() {
        CreateUserResponse created = userService.createUser(operatorRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest("Updated", "Name", "updated@utility.local", Role.ROLE_OPERATOR, true);
        UserResponse updated = userService.updateUser(created.user().id(), updateRequest, "admin");
        assertThat(updated.firstName()).isEqualTo("Updated");
        assertThat(updated.lastName()).isEqualTo("Name");
        assertThat(updated.email()).isEqualTo("updated@utility.local");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when admin tries to change own role")
    void shouldThrowWhenAdminChangesOwnRole() {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        UpdateUserRequest request = new UpdateUserRequest("Admin", "User", "admin@utility.local", Role.ROLE_OPERATOR, true);
        assertThatThrownBy(() -> userService.updateUser(adminId, request, "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("own role");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating to existing email")
    void shouldThrowWhenUpdatingToExistingEmail() {
        userService.createUser(adminRequest);
        CreateUserResponse operator = userService.createUser(operatorRequest);
        UpdateUserRequest request = new UpdateUserRequest("Test", "Operator", "testadmin@utility.local", Role.ROLE_OPERATOR, true);
        assertThatThrownBy(() -> userService.updateUser(operator.user().id(), request, "admin"))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("testadmin@utility.local");
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        CreateUserResponse created = userService.createUser(operatorRequest);
        userService.deleteUser(created.user().id(), "admin");
        assertThatThrownBy(() -> userService.getUserById(created.user().id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when admin tries to delete own account")
    void shouldThrowWhenAdminDeletesOwnAccount() {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        assertThatThrownBy(() -> userService.deleteUser(adminId, "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("own account");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
    void shouldThrowWhenDeletingNonExistentUser() {
        assertThatThrownBy(() -> userService.deleteUser(999999L, "admin")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should allow user to change own password")
    void shouldAllowUserToChangeOwnPassword() {
        CreateUserResponse created = userService.createUser(operatorRequest);
        String tempPassword = created.temporaryPassword();
        UserDetails operatorDetails = User.builder().username("testoperator").password(tempPassword)
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_OPERATOR.name()))).build();
        userService.changePassword(created.user().id(), new ChangePasswordRequest(tempPassword, "NewPassword1@"), operatorDetails);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when admin tries to change another user's password")
    void shouldThrowWhenAdminChangesOtherUserPassword() {
        CreateUserResponse operator = userService.createUser(operatorRequest);
        UserDetails adminDetails = User.builder().username("admin").password("admin")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_ADMIN.name()))).build();
        assertThatThrownBy(() -> userService.changePassword(operator.user().id(),
                new ChangePasswordRequest(operator.temporaryPassword(), "NewPassword1@"), adminDetails))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("own password");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when operator tries to change another user's password")
    void shouldThrowWhenOperatorChangesOtherUserPassword() {
        userService.createUser(adminRequest);
        CreateUserResponse operator = userService.createUser(operatorRequest);
        UserDetails anotherOperator = User.builder().username("anotheroperator").password("password123")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_OPERATOR.name()))).build();
        assertThatThrownBy(() -> userService.changePassword(operator.user().id(),
                new ChangePasswordRequest(operator.temporaryPassword(), "NewPassword1@"), anotherOperator))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("own password");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when current password is incorrect")
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        CreateUserResponse created = userService.createUser(operatorRequest);
        UserDetails operatorDetails = User.builder().username("testoperator").password("wrongpassword")
                .authorities(List.of(new SimpleGrantedAuthority(Role.ROLE_OPERATOR.name()))).build();
        assertThatThrownBy(() -> userService.changePassword(created.user().id(),
                new ChangePasswordRequest("wrongpassword", "NewPassword1@"), operatorDetails))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("Should reset user password and return temporary password")
    void shouldResetPasswordAndReturnTempPassword() {
        CreateUserResponse operator = userService.createUser(operatorRequest);
        String tempPassword = userService.resetPassword(operator.user().id());
        assertThat(tempPassword).isNotBlank();
        assertThat(tempPassword).hasSize(12);
        String storedHash = userRepository.findById(operator.user().id()).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches(tempPassword, storedHash)).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when resetting non-existent user")
    void shouldThrowWhenResettingNonExistentUser() {
        assertThatThrownBy(() -> userService.resetPassword(999999L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() {
        userService.createUser(adminRequest);
        userService.createUser(operatorRequest);
        assertThat(userService.getAllUsers()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
    void shouldThrowWhenUserNotFound() {
        assertThatThrownBy(() -> userService.getUserById(999999L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("999999");
    }
}