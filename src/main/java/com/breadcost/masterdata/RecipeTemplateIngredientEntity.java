package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_template_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeTemplateIngredientEntity {

    @Id
    @Column(name = "ingredient_line_id")
    private String ingredientLineId;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "unit_mode", nullable = false)
    private String unitMode;

    @Column(name = "recipe_qty", precision = 19, scale = 4)
    private BigDecimal recipeQty;

    @Column(name = "recipe_uom")
    private String recipeUom;

    @Column(name = "piece_qty")
    private Integer pieceQty;

    @Column(name = "weight_per_piece", precision = 19, scale = 4)
    private BigDecimal weightPerPiece;

    @Column(name = "piece_weight_uom")
    private String pieceWeightUom;

    @Column(name = "purchasing_unit_size", precision = 19, scale = 4)
    private BigDecimal purchasingUnitSize;

    @Column(name = "purchasing_uom")
    private String purchasingUom;

    @Column(name = "waste_factor", precision = 7, scale = 4)
    private BigDecimal wasteFactor;
}
