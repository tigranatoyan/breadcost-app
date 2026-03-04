package com.breadcost.customers;

import jakarta.persistence.*;
import lombok.*;

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

    private String phone;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_addresses",
        joinColumns = @JoinColumn(name = "customerId"))
    private List<CustomerAddress> addresses = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

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
