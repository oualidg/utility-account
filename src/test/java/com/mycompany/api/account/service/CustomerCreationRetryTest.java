/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/14/2026 at 1:13 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/14/2026 at 12:00 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.util.LuhnGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for @Retryable collision handling during customer creation.
 * Uses a mocked LuhnGenerator to force ID collisions and verify that the
 * retry mechanism generates a fresh ID and succeeds on subsequent attempts.
 *
 * <p>Separate from CustomerServiceIntegrationTest because @MockitoBean replaces
 * the LuhnGenerator for the entire test class context.</p>
 *
 * @author Oualid Gharach
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Customer Creation Retry Tests")
class CustomerCreationRetryTest {

    @Autowired
    private CustomerService customerService;

    @MockitoBean
    private LuhnGenerator luhnGenerator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
    }

    @Test
    @DisplayName("Should retry and succeed when customer ID collides")
    void shouldRetryWhenCustomerIdCollides() {
        // Given - A customer already exists in the DB with a known ID
        Long collidingCustomerId = 12345674L;  // Valid 8-digit Luhn
        Long freshCustomerId = 98765439L;      // Different valid 8-digit Luhn
        Long accountNumber1 = 1234567897L;     // Valid 10-digit Luhn
        Long accountNumber2 = 9876543209L;     // Valid 10-digit Luhn

        // Pre-insert a customer directly via JDBC so Hibernate can't merge over it
        jdbcTemplate.update(
                "INSERT INTO customers (customer_id, first_name, last_name, email, mobile_number, active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                collidingCustomerId, "Existing", "Customer", "existing@example.com", "+27820000000", true
        );
        // Also give them a main account so the DB state is consistent
        jdbcTemplate.update(
                "INSERT INTO accounts (account_number, customer_id, balance, is_main_account, created_at, updated_at) " +
                        "VALUES (?, ?, 0.00, true, NOW(), NOW())",
                accountNumber1, collidingCustomerId
        );

        // Mock: first call returns colliding ID, second call returns fresh ID
        when(luhnGenerator.generateCustomerId())
                .thenReturn(collidingCustomerId)   // 1st attempt → PK collision
                .thenReturn(freshCustomerId);      // 2nd attempt → success
        when(luhnGenerator.generateAccountNumber()).thenReturn(accountNumber2);

        // When - createCustomer should hit the collision, retry, and succeed
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829876543"
        );
        CustomerDetailedResponse response = customerService.createCustomer(request);

        // Then - Created with the fresh (non-colliding) ID
        assertThat(response.customerId()).isEqualTo(freshCustomerId);
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.accounts()).hasSize(1);
    }
}