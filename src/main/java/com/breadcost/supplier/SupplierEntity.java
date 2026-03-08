package com.breadcost.supplier;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Supplier entity — BC-1301: Supplier catalog CRUD
 */
@Entity
@Table(
    name = "suppliers",
    uniqueConstraints = @UniqueConstraint(name = "uq_supplier_tenant_name",
        columnNames = {"tenantId", "name"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierEntity {

    @Id
    private String supplierId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String contactEmail;
    private String contactPhone;

    @Column(length = 2000)
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
