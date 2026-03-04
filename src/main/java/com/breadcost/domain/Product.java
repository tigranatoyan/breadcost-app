package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product domain object
 * Represents a producible/sellable product (e.g., Sourdough Loaf 400g)
 * Linked to a Department and has an active Recipe version
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productId;
    private String tenantId;
    private String departmentId;
    private String name;
    private String description;
    private SaleUnit saleUnit;           // How this product is sold
    private String baseUom;             // Base unit of measure (e.g., "PCS", "KG")
    private String activeRecipeId;      // ID of currently active recipe version
    private ProductStatus status;

    public enum SaleUnit {
        PIECE,   // Sold by piece (e.g., 1 loaf, 1 pizza)
        WEIGHT,  // Sold by weight (e.g., per kg)
        BOTH     // Can be sold either way
    }

    public enum ProductStatus {
        ACTIVE,
        DISCONTINUED
    }
}
