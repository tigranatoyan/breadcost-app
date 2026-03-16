package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Recipe domain object
 * Defines how to produce a product: ingredients, yield, waste
 * Versioned — editing creates a new version; previous versions are retained
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
    private String recipeId;
    private String tenantId;
    private String productId;
    private Integer versionNumber;
    private RecipeStatus status;

    /** Batch size this recipe is defined for, e.g. produces 50 loaves per batch. */
    private BigDecimal batchSize;
    private String batchSizeUom;    // e.g., "PCS" or "KG"

    // Expected yield from one batch
    private BigDecimal expectedYield;
    private String yieldUom;        // e.g., "PCS" or "KG"

    // Notes / instructions for production team
    private String productionNotes;

    private List<RecipeIngredient> ingredients;

    public enum RecipeStatus {
        DRAFT,    // Being built by technologist
        ACTIVE,   // Currently used for production planning and cost calculations
        ARCHIVED  // Superseded by newer version — retained for history
    }
}
