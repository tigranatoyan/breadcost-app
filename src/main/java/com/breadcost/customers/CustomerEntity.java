package com.breadcost.customers;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity — a registered end-customer of the bakery's B2B/B2C portal.
 * BC-E11: Customer Portal & Registration
 */
@Entity
@Table(
    name = "customers",
    uniqueConstraints = @UniqueConstraint(name = "uq_customer_tenant_email",
        columnNames = {"tenantId", "email"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity {

    @Id
    private String customerId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    /** Unique per tenant — used as customer login identity in BC-1102. */
    @Column(nullable = false)
    private String email;

    /** BCrypt-hashed password — BC-1102. Nullable for legacy/invited customers. */
    private String passwordHash;

    private String phone;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_addresses",
        joinColumns = @JoinColumn(name = "customerId"))
    private List<CustomerAddress> addresses = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    /**
     * Payment terms in days — number of days after invoice issuance until due.
     * BC-1502: Payment terms per customer.
     */
    @Builder.Default
    private int paymentTermsDays = 30;

    /**
     * Maximum credit allowed (outstanding invoices + pending orders).
     * BC-1504: Credit limit enforcement and overdue order block.
     * Zero means unlimited.
     */
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;

    /**
     * Running total of unpaid invoice amounts for this customer.
     * BC-1504: Incremented on invoice generation, decremented on payment.
     */
    @Builder.Default
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
