/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/28/2026 at 12:27 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.dto.CreateUserRequest;
import com.mycompany.api.account.enums.Role;
import com.mycompany.api.account.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security slice tests verifying {@code @PreAuthorize} role enforcement
 * using real JWT tokens acquired via {@code POST /api/auth/login}.
 *
 * <p>Unlike controller unit tests which disable filters, these tests run the
 * full security stack — login → token → protected endpoint — to prove that
 * role rules are actually enforced end-to-end.</p>
 *
 * <p><strong>Note on unauthenticated POST/PUT/DELETE requests:</strong>
 * These return 403 (not 401) because Spring Security's CSRF filter fires before
 * the authentication check. An unauthenticated request has no CSRF token, so it
 * is rejected with 403 Forbidden. The request is still correctly blocked —
 * the distinction is which filter rejects it first.</p>
 *
 * @author Oualid Gharach
 */
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Security Controller Tests")
class SecurityControllerTest extends BaseIntegrationTest  {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String operatorToken;
    private Long adminId;
    private Long operatorId;

    @BeforeAll
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM users WHERE username != 'admin'");

        adminId = userService.getAllUsers().stream()
                .filter(u -> u.username().equals("admin"))
                .findFirst().orElseThrow().id();

        operatorId = userService.createUser(new CreateUserRequest(
                "operator1", "Test", "Operator", "operator1@utility.local", "Password1@", Role.ROLE_OPERATOR
        )).id();

        adminToken = "Bearer " + login("admin", "admin");
        operatorToken = "Bearer " + login("operator1", "Password1@");
    }

    private String login(String username, String password) throws Exception {
        String body = """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Auth-Mode", "bearer")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    // -------------------------------------------------------------------------
    // UserController — ADMIN only
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/users - Admin should get 200")
    void adminCanGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users - Operator should get 403")
    void operatorCannotGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users - Unauthenticated should get 401")
    void unauthenticatedCannotGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // ProviderController — ADMIN only
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/providers - Admin should get 200")
    void adminCanGetProviders() throws Exception {
        mockMvc.perform(get("/api/v1/providers").header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/providers - Operator should get 403")
    void operatorCannotGetProviders() throws Exception {
        mockMvc.perform(get("/api/v1/providers").header("Authorization", operatorToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // ReportingController — ADMIN only
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/reports/summary - Admin should get 200")
    void adminCanGetReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary").header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/reports/summary - Operator should get 403")
    void operatorCannotGetReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary").header("Authorization", operatorToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // CustomerController — ADMIN and OPERATOR
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/customers - Admin should get 200")
    void adminCanGetCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers").header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/customers - Operator should get 200")
    void operatorCanGetCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers").header("Authorization", operatorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/customers - Unauthenticated should get 401")
    void unauthenticatedCannotGetCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // AccountController — ADMIN and OPERATOR
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/accounts/1234567897 - Admin should get 404 (auth passed)")
    void adminCanAccessAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1234567897").header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/1234567897 - Operator should get 404 (auth passed)")
    void operatorCanAccessAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1234567897").header("Authorization", operatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/1234567897 - Unauthenticated should get 401")
    void unauthenticatedCannotAccessAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1234567897"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // UserController — changePassword (own password only, ADMIN and OPERATOR)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Admin can change own password")
    void adminCanChangeOwnPassword() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + adminId + "/password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"admin\",\"newPassword\":\"NewAdmin1@\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Operator can change own password")
    void operatorCanChangeOwnPassword() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + operatorId + "/password")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password1@\",\"newPassword\":\"NewPassword1@\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Operator cannot change another user's password")
    void operatorCannotChangeOtherUserPassword() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + adminId + "/password")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password1@\",\"newPassword\":\"NewPassword1@\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Admin cannot change another user's password")
    void adminCannotChangeOtherUserPassword() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + operatorId + "/password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"admin\",\"newPassword\":\"NewAdmin1@\"}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // UserController — resetPassword (ADMIN only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/users/{id}/reset-password - Admin can reset any user's password")
    void adminCanResetAnyUserPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users/" + operatorId + "/reset-password")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/users/{id}/reset-password - Operator should get 403")
    void operatorCannotResetPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users/" + adminId + "/reset-password")
                        .header("Authorization", operatorToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // UserController — unauthenticated blocked on all endpoints
    // GET endpoints return 401. POST/PUT/DELETE return 403 because
    // CSRF filter fires before auth check for requests without a Bearer token.
    // The request is still correctly blocked in both cases.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/users/{id} - Unauthenticated should get 401")
    void unauthenticatedCannotGetUserById() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/users - Unauthenticated should be blocked (403 - CSRF fires before auth)")
    void unauthenticatedCannotCreateUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"firstName\":\"X\",\"lastName\":\"X\",\"email\":\"x@x.com\",\"password\":\"Password1@\",\"role\":\"ROLE_OPERATOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Unauthenticated should be blocked (403 - CSRF fires before auth)")
    void unauthenticatedCannotUpdateUser() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\",\"lastName\":\"X\",\"email\":\"x@x.com\",\"role\":\"ROLE_OPERATOR\",\"enabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Unauthenticated should be blocked (403 - CSRF fires before auth)")
    void unauthenticatedCannotDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password - Unauthenticated should be blocked (403 - CSRF fires before auth)")
    void unauthenticatedCannotChangePassword() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password1@\",\"newPassword\":\"NewPassword1@\"}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // ProviderController — unauthenticated blocked
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/providers/{id} - Unauthenticated should get 401")
    void unauthenticatedCannotGetProvider() throws Exception {
        mockMvc.perform(get("/api/v1/providers/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/providers - Unauthenticated should be blocked (403 - CSRF fires before auth)")
    void unauthenticatedCannotCreateProvider() throws Exception {
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST\",\"name\":\"Test Provider\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/providers - Unauthenticated should get 401")
    void unauthenticatedCannotGetProviders() throws Exception {
        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // ReportingController — unauthenticated blocked
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/reports/payments - Unauthenticated should get 401")
    void unauthenticatedCannotSearchPayments() throws Exception {
        mockMvc.perform(get("/api/v1/reports/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/reports/summary - Unauthenticated should get 401")
    void unauthenticatedCannotGetReportSummary() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // CustomerController — unauthenticated blocked
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Unauthenticated should get 401")
    void unauthenticatedCannotGetCustomerById() throws Exception {
        mockMvc.perform(get("/api/v1/customers/12345670"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/customers - Unauthenticated should be blocked (403 - CSRF fires before auth)")
    void unauthenticatedCannotCreateCustomer() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\",\"lastName\":\"X\",\"email\":\"x@x.com\",\"mobile\":\"0821234567\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/customers/search - Unauthenticated should get 401")
    void unauthenticatedCannotSearchCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers/search").param("mobile", "082"))
                .andExpect(status().isUnauthorized());
    }
}