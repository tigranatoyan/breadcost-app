package com.breadcost.supplier;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Supplier catalog item — BC-1301
 * Represents an ingredient/item that a supplier provides, with pricing info.
 */
@Entity
@Table(name = "supplier_catalog_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierCatalogItemEntity {

    @Id
    private String itemId;

    @Column(nullable = false)
    private String supplierId;

    @Column(nullable = false)
    private String tenantId;

    /** Reference to the ingredient/raw material */
    @Column(nullable = false)
    private String ingredientId;

    private String ingredientName;

    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "USD";

    /** Lead time in business days */
    @Builder.Default
    private int leadTimeDays = 1;

    /** Minimum order quantity */
    @Builder.Default
    private double moq = 1.0;

    private String unit; // kg, litre, piece

    /** G-10: Whether this supplier is the preferred source for this ingredient. */
    @Builder.Default
    private boolean preferred = false;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }
}
