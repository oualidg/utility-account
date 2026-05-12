/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 5/12/2026 at 6:41 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.security;

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.filter.ApiKeyAuthFilter;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for payment endpoint security and filter chain wiring.
 *
 * <p>Verifies that {@link ApiKeyAuthFilter} and
 * {@link com.mycompany.api.account.filter.PaymentApiRateLimitFilter}
 * are correctly wired into the filter chain with filters enabled. Controller
 * unit tests use {@code addFilters = false} and cannot prove this.</p>
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Missing API key returns 401</li>
 *   <li>Invalid API key returns 401</li>
 *   <li>Valid API key reaches the controller on deposit and validation endpoints</li>
 *   <li>Confirmation endpoint is API-key protected</li>
 *   <li>Inactive provider returns 401</li>
 * </ul>
 *
 * <p>Rate limiting behaviour is tested separately in
 * {@code PaymentApiRateLimitFilterTest}
 * using a local limiter — this avoids shared state issues with the
 * application-scoped {@code RateLimiterRegistry}.</p>
 *
 * @author Oualid Gharach
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Payment Security Integration Tests")
class PaymentSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentProviderRepository providerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String VALID_API_KEY   = "e7e75fe1-4192-4e34-af5e-6010d787c029";
    private static final String INVALID_API_KEY = "invalid-key-000000000000000000000000";

    private Account testAccount;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM payment_providers");

        // Seed provider with known SHA-256 hash matching VALID_API_KEY
        PaymentProvider mpesaProvider = new PaymentProvider();
        mpesaProvider.setCode("MPESA");
        mpesaProvider.setName("Lipa na M-PESA");
        mpesaProvider.setApiKeyHash("59e043f0424f97e15a3ca18a71329dd48beed0085fcc137e25fd74f8da6a8738");
        mpesaProvider.setApiKeyPrefix("e7e75fe1");
        mpesaProvider.setActive(true);
        providerRepository.save(mpesaProvider);

        testCustomer = new Customer();
        testCustomer.setCustomerId(12345674L);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setEmail("john.doe@example.com");
        testCustomer.setMobileNumber("0821234567");
        testCustomer.setActive(true);
        testCustomer = customerRepository.save(testCustomer);

        testAccount = new Account();
        testAccount.setAccountNumber(1234567897L);
        testAccount.setCustomer(testCustomer);
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount.setMainAccount(true);
        testAccount = accountRepository.save(testAccount);
    }

    // =========================================================================
    // API Key Authentication — validate endpoints
    // =========================================================================

    @Test
    @DisplayName("Should return 401 when X-Api-Key header is missing")
    void shouldReturn401WhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}/validate",
                        testAccount.getAccountNumber()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing API key"));
    }

    @Test
    @DisplayName("Should return 401 when X-Api-Key is invalid")
    void shouldReturn401WhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}/validate",
                        testAccount.getAccountNumber())
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, INVALID_API_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }

    @Test
    @DisplayName("Should reach controller on account validate endpoint when API key is valid")
    void shouldReachControllerWhenApiKeyIsValid() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}/validate",
                        testAccount.getAccountNumber())
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_API_KEY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 401 on customer validate endpoint when API key is missing")
    void shouldReturn401OnCustomerValidateWhenApiKeyMissing() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{customerId}/validate",
                        testCustomer.getCustomerId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing API key"));
    }

    @Test
    @DisplayName("Should return 200 on customer validate endpoint when API key is valid")
    void shouldReturn200OnCustomerValidateWhenApiKeyValid() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{customerId}/validate",
                        testCustomer.getCustomerId())
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_API_KEY))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // API Key Authentication — deposit endpoint
    // =========================================================================

    @Test
    @DisplayName("Should reach controller on deposit endpoint when API key is valid")
    void shouldReachControllerOnDepositEndpointWhenApiKeyIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments",
                        testAccount.getAccountNumber())
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 50.00,
                                    "paymentReference": "MPESA-REF-SEC-001"
                                }
                                """))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // API Key Authentication — confirmation endpoint
    // =========================================================================

    @Test
    @DisplayName("Should return 401 on confirmation endpoint when API key is missing")
    void shouldReturn401OnConfirmationEndpointWhenApiKeyMissing() throws Exception {
        mockMvc.perform(get("/api/v1/payments/confirmation/{reference}", "MPESA-REF-SEC-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing API key"));
    }

    @Test
    @DisplayName("Should return 401 on confirmation endpoint when API key is invalid")
    void shouldReturn401OnConfirmationEndpointWhenApiKeyInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/payments/confirmation/{reference}", "MPESA-REF-SEC-001")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, INVALID_API_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }

    // =========================================================================
    // Inactive provider
    // =========================================================================

    @Test
    @DisplayName("Should return 401 when provider is inactive")
    void shouldReturn401WhenProviderIsInactive() throws Exception {
        // Seed MTN as inactive — hash matches the raw key we send,
        // proving the provider exists but is rejected due to active=false
        PaymentProvider inactiveProvider = new PaymentProvider();
        inactiveProvider.setCode("MOMO");
        inactiveProvider.setName("MoMo from MTN");
        inactiveProvider.setApiKeyHash("dff3275e5484cf1dcd101e5a2e226081d7383af96cebfbec01e187c265872c77");
        inactiveProvider.setApiKeyPrefix("0df87c1d");
        inactiveProvider.setActive(false);
        providerRepository.save(inactiveProvider);

        mockMvc.perform(get("/api/v1/accounts/{accountNumber}/validate",
                        testAccount.getAccountNumber())
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "0df87c1d-2dde-4a08-8f9f-7739b471073a"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }
}