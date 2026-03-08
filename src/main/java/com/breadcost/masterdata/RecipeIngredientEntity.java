package com.breadcost.masterdata;

import com.breadcost.domain.RecipeIngredient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * JPA entity for a single ingredient line in a Recipe
 */
@Entity
@Table(name = "recipe_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientEntity {

    @Id
    @Column(name = "ingredient_line_id")
    private String ingredientLineId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "recipe_id", nullable = false)
    private String recipeId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "item_name")
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_mode", nullable = false)
    private RecipeIngredient.UnitMode unitMode;

    // --- WEIGHT mode ---
    @Column(name = "recipe_qty", precision = 19, scale = 4)
    private BigDecimal recipeQty;

    @Column(name = "recipe_uom")
    private String recipeUom;

    // --- PIECE / COMBO mode ---
    @Column(name = "piece_qty")
    private Integer pieceQty;

    @Column(name = "weight_per_piece", precision = 19, scale = 4)
    private BigDecimal weightPerPiece;

    @Column(name = "piece_weight_uom")
    private String pieceWeightUom;

    // --- Purchasing unit ---
    @Column(name = "purchasing_unit_size", precision = 19, scale = 4)
    private BigDecimal purchasingUnitSize;

    @Column(name = "purchasing_uom")
    private String purchasingUom;

    // --- Waste ---
    // e.g., 0.05 = 5% waste on top of recipe quantity
    @Column(name = "waste_factor", precision = 7, scale = 4)
    private BigDecimal wasteFactor;

    /**
     * Calculate total recipe weight for this line (before waste).
     */
    public BigDecimal totalRecipeWeight() {
        return switch (unitMode) {
            case WEIGHT -> recipeQty != null ? recipeQty : BigDecimal.ZERO;
            case PIECE, COMBO -> (weightPerPiece != null && pieceQty != null)
                    ? weightPerPiece.multiply(BigDecimal.valueOf(pieceQty))
                    : BigDecimal.ZERO;
        };
    }

    /**
     * Calculate purchasing units needed per batch (after waste).
     * Formula: totalRecipeWeight * (1 + wasteFactor) / purchasingUnitSize
     */
    public BigDecimal purchasingUnitsNeeded() {
        if (purchasingUnitSize == null || purchasingUnitSize.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal waste = wasteFactor != null ? wasteFactor : BigDecimal.ZERO;
        BigDecimal withWaste = totalRecipeWeight().multiply(BigDecimal.ONE.add(waste));
        return withWaste.divide(purchasingUnitSize, 4, java.math.RoundingMode.HALF_UP);
    }
}
