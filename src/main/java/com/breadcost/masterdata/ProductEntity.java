package com.breadcost.masterdata;

import com.breadcost.domain.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for Product master data
 */
@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name", "department_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "department_id", nullable = false)
    private String departmentId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_unit", nullable = false)
    private Product.SaleUnit saleUnit;

    @Column(name = "base_uom", nullable = false)
    private String baseUom;

    /** Base selling price per unit */
    @Column(name = "price", precision = 18, scale = 4)
    private BigDecimal price;

    /** VAT rate percent (e.g. 12.0 for 12%) */
    @Column(name = "vat_rate_pct")
    @Builder.Default
    private double vatRatePct = 0.0;

    // ID of the currently active Recipe for this product
    @Column(name = "active_recipe_id")
    private String activeRecipeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Product.ProductStatus status;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    public void prePersist() {
        createdAtUtc = Instant.now();
        updatedAtUtc = createdAtUtc;
        if (status == null) status = Product.ProductStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAtUtc = Instant.now();
    }
}
