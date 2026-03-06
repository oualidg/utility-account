/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 9:38 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.repository;

import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Provides database access methods for payment transaction management.
 * <p>
 * Null-safe date range handling: PostgreSQL cannot infer the type of null Instant
 * parameters in JPQL queries. Public-facing default methods normalise null dates
 * to sentinel values (Instant.EPOCH / FAR_FUTURE) before delegating to the
 * underlying @Query methods, keeping the service layer clean.
 *
 * @author Oualid Gharach
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Instant FAR_FUTURE = Instant.parse("2100-01-01T00:00:00Z");

    /**
     * Find payment by provider and reference with eager fetch of provider and account.
     * Used for confirmation queries where full payment details are needed outside
     * a transaction boundary.
     *
     * @param provider  payment provider entity
     * @param reference payment reference from provider
     * @return optional containing fully loaded payment
     */
    @Query("SELECT p FROM Payment p " +
            "JOIN FETCH p.paymentProvider " +
            "JOIN FETCH p.account " +
            "WHERE p.paymentProvider = :provider AND p.paymentReference = :reference")
    Optional<Payment> findByProviderAndReferenceWithDetails(
            PaymentProvider provider,
            String reference
    );

    /**
     * Search payments with all-optional filters.
     * Null parameters are ignored — only provided filters are applied.
     * Results ordered by paymentDate descending (most recent first).
     *
     * <p><strong>Receipt number search — prefix match only:</strong>
     * The {@code receiptNumber} parameter uses a prefix LIKE pattern ({@code fragment%})
     * rather than a contains pattern ({@code %fragment%}). This is intentional.
     * A leading wildcard would prevent PostgreSQL from using the primary key B-tree
     * index on {@code receipt_number}, forcing a full table scan at high volumes.
     * Prefix match preserves index usage and keeps receipt searches sub-millisecond.
     * Do NOT add a leading {@code %}. Operators naturally search from the start of a receipt number.</p>
     *
     * @param accountNumber  optional account number filter (exact match)
     * @param customerId     optional customer ID filter (exact match)
     * @param providerCode   optional provider code filter (exact match)
     * @param receiptNumber  optional receipt number prefix filter (case-insensitive, prefix match)
     * @param from           optional start of date range (inclusive)
     * @param to             optional end of date range (inclusive)
     * @return list of matching payments
     */
    default List<Payment> searchPayments(Long accountNumber, Long customerId,
                                         String providerCode, String receiptNumber,
                                         Instant from, Instant to) {
        return searchPaymentsQuery(
                accountNumber, customerId, providerCode,
                receiptNumber != null ? receiptNumber.toLowerCase() + "%" : null,
                from != null ? from : Instant.EPOCH,
                to   != null ? to   : FAR_FUTURE
        );
    }

    @Query("SELECT p FROM Payment p " +
            "JOIN FETCH p.paymentProvider pp " +
            "JOIN FETCH p.account a " +
            "JOIN FETCH a.customer c " +
            "WHERE (:accountNumber IS NULL OR a.accountNumber = :accountNumber) " +
            "AND (:customerId   IS NULL OR c.customerId   = :customerId) " +
            "AND (:providerCode IS NULL OR pp.code        = :providerCode) " +
            "AND (:receiptNumber IS NULL OR LOWER(p.receiptNumber) LIKE :receiptNumber) " +
            "AND p.paymentDate >= :from " +
            "AND p.paymentDate <= :to " +
            "ORDER BY p.paymentDate DESC")
    List<Payment> searchPaymentsQuery(
            @Param("accountNumber") Long accountNumber,
            @Param("customerId")    Long customerId,
            @Param("providerCode")  String providerCode,
            @Param("receiptNumber") String receiptNumber,
            @Param("from")          Instant from,
            @Param("to")            Instant to
    );

    /**
     * Total payment amount and count across all providers for a date range.
     * Null dates default to all-time range.
     *
     * @param from optional start of date range (inclusive)
     * @param to   optional end of date range (inclusive)
     * @return Object[]{BigDecimal totalAmount, Long count}
     */
    default Object[] getGlobalTotals(Instant from, Instant to) {
        return getGlobalTotalsQuery(
                from != null ? from : Instant.EPOCH,
                to   != null ? to   : FAR_FUTURE
        );
    }

    @Query("SELECT SUM(p.amount), COUNT(p) FROM Payment p " +
            "WHERE p.paymentDate >= :from " +
            "AND   p.paymentDate <= :to")
    Object[] getGlobalTotalsQuery(
            @Param("from") Instant from,
            @Param("to")   Instant to
    );

    /**
     * Payment totals grouped by provider for a date range.
     * Null dates default to all-time range.
     * Results ordered by total amount descending.
     *
     * @param from optional start of date range (inclusive)
     * @param to   optional end of date range (inclusive)
     * @return list of Object[]{String providerCode, String providerName, BigDecimal total, Long count}
     */
    default List<Object[]> getTotalsByProvider(Instant from, Instant to) {
        return getTotalsByProviderQuery(
                from != null ? from : Instant.EPOCH,
                to   != null ? to   : FAR_FUTURE
        );
    }

    @Query("SELECT pp.code, pp.name, SUM(p.amount), COUNT(p) " +
            "FROM Payment p " +
            "JOIN p.paymentProvider pp " +
            "WHERE p.paymentDate >= :from " +
            "AND   p.paymentDate <= :to " +
            "GROUP BY pp.code, pp.name " +
            "ORDER BY SUM(p.amount) DESC")
    List<Object[]> getTotalsByProviderQuery(
            @Param("from") Instant from,
            @Param("to")   Instant to
    );

    /**
     * Total payment amount and count for a single account across all time.
     *
     * @param accountNumber account number to aggregate
     * @return Object[]{BigDecimal totalAmount, Long count}
     */
    @Query("SELECT SUM(p.amount), COUNT(p) FROM Payment p " +
            "JOIN p.account a " +
            "WHERE a.accountNumber = :accountNumber")
    Object[] getAccountTotals(@Param("accountNumber") Long accountNumber);

    /**
     * Payment totals grouped by provider for a single account.
     * Results ordered by total amount descending.
     *
     * @param accountNumber account number to aggregate
     * @return list of Object[]{String providerCode, String providerName, BigDecimal total, Long count}
     */
    default List<Object[]> getAccountTotalsByProvider(Long accountNumber) {
        return getAccountTotalsByProviderQuery(accountNumber);
    }

    @Query("SELECT pp.code, pp.name, SUM(p.amount), COUNT(p) " +
            "FROM Payment p " +
            "JOIN p.paymentProvider pp " +
            "JOIN p.account a " +
            "WHERE a.accountNumber = :accountNumber " +
            "GROUP BY pp.code, pp.name " +
            "ORDER BY SUM(p.amount) DESC")
    List<Object[]> getAccountTotalsByProviderQuery(@Param("accountNumber") Long accountNumber);

    /**
     * Total payment amount and count for a single provider within a date range.
     * Used by the provider detail page Load button via {@code getProviderSummary}.
     * Null dates default to all-time range.
     *
     * @param providerCode provider code to filter by
     * @param from         optional start of date range (inclusive)
     * @param to           optional end of date range (inclusive)
     * @return Object[]{BigDecimal totalAmount, Long count}
     */
    default Object[] getProviderTotals(String providerCode, Instant from, Instant to) {
        return getProviderTotalsQuery(
                providerCode,
                from != null ? from : Instant.EPOCH,
                to   != null ? to   : FAR_FUTURE
        );
    }

    @Query("SELECT SUM(p.amount), COUNT(p) FROM Payment p " +
            "JOIN p.paymentProvider pp " +
            "WHERE pp.code = :providerCode " +
            "AND p.paymentDate >= :from " +
            "AND p.paymentDate <= :to")
    Object[] getProviderTotalsQuery(
            @Param("providerCode") String providerCode,
            @Param("from")         Instant from,
            @Param("to")           Instant to
    );

    /**
     * All payments for a provider within a date range, with account details.
     * Used to build the settlement report for CSV export.
     * Null dates default to all-time range.
     *
     * @param providerCode provider code to filter by
     * @param from         optional start of date range (inclusive)
     * @param to           optional end of date range (inclusive)
     * @return list of payments ordered by paymentDate ascending
     */
    default List<Payment> findByProviderCodeAndDateRange(String providerCode,
                                                         Instant from, Instant to) {
        return findByProviderCodeAndDateRangeQuery(
                providerCode,
                from != null ? from : Instant.EPOCH,
                to   != null ? to   : FAR_FUTURE
        );
    }

    @Query("SELECT p FROM Payment p " +
            "JOIN FETCH p.paymentProvider pp " +
            "JOIN FETCH p.account a " +
            "WHERE pp.code = :providerCode " +
            "AND p.paymentDate >= :from " +
            "AND p.paymentDate <= :to " +
            "ORDER BY p.paymentDate ASC")
    List<Payment> findByProviderCodeAndDateRangeQuery(
            @Param("providerCode") String providerCode,
            @Param("from")         Instant from,
            @Param("to")           Instant to
    );
}