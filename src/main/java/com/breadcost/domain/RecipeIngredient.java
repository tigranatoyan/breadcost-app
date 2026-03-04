package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * RecipeIngredient domain object
 * One ingredient line within a Recipe
 *
 * Supports three unit modes:
 *   WEIGHT  — pure weight (e.g., 500g of flour per batch)
 *   PIECE   — pieces with a weight per piece (e.g., 5 eggs, each ~60g)
 *   COMBO   — explicit piece count AND total weight (e.g., 5 sausages × 4g = 20g)
 *
 * Purchasing unit: the unit in which this ingredient is bought from suppliers
 *   e.g., flour comes in 25kg sacks → purchasingUnitSize=25, purchasingUom="KG"
 *   e.g., sausage comes in a 400g pack → purchasingUnitSize=400, purchasingUom="G"
 *
 * Cost calculation per batch:
 *   1. totalRecipeWeight = pieceQty * weightPerPiece   (for PIECE / COMBO)
 *      OR = recipeQty                                  (for WEIGHT)
 *   2. totalWithWaste    = totalRecipeWeight * (1 + wasteFactor)
 *   3. purchasingUnits   = totalWithWaste / purchasingUnitSize
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredient {
    private String ingredientLineId;
    private String recipeId;
    private String itemId;          // References Item (type INGREDIENT or PACKAGING)
    private String itemName;        // Denormalized for display
    private UnitMode unitMode;

    // --- WEIGHT mode fields ---
    private BigDecimal recipeQty;   // e.g., 500 (grams of flour)
    private String recipeUom;       // e.g., "G"

    // --- PIECE / COMBO mode fields ---
    private Integer pieceQty;           // e.g., 5 (sausages)
    private BigDecimal weightPerPiece;  // e.g., 4 (grams per sausage)
    private String pieceWeightUom;      // e.g., "G"

    // --- Purchasing unit ---
    private BigDecimal purchasingUnitSize;  // e.g., 400 (grams per pack)
    private String purchasingUom;           // e.g., "G" or "KG"

    // --- Waste ---
    private BigDecimal wasteFactor;  // e.g., 0.05 for 5% waste on top of recipe qty

    public enum UnitMode {
        WEIGHT,  // Ingredient measured purely by weight
        PIECE,   // Ingredient measured by piece count (weight per piece for cost calc)
        COMBO    // Explicit piece count + weight per piece (e.g., 5 pieces × 4g = 20g)
    }

    /**
     * Calculate the total recipe weight for this ingredient (before waste).
     * Returns in the same unit as recipeUom / pieceWeightUom.
     */
    public BigDecimal totalRecipeWeight() {
        return switch (unitMode) {
            case WEIGHT -> recipeQty;
            case PIECE, COMBO -> weightPerPiece.multiply(BigDecimal.valueOf(pieceQty));
        };
    }

    /**
     * Calculate purchasing units needed per batch (after waste).
     * purchasingUnits = totalRecipeWeight * (1 + wasteFactor) / purchasingUnitSize
     */
    public BigDecimal purchasingUnitsNeeded() {
        BigDecimal withWaste = totalRecipeWeight()
                .multiply(BigDecimal.ONE.add(wasteFactor != null ? wasteFactor : BigDecimal.ZERO));
        return withWaste.divide(purchasingUnitSize, 4, java.math.RoundingMode.HALF_UP);
    }
}
