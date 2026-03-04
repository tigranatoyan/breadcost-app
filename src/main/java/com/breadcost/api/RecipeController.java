package com.breadcost.api;

import com.breadcost.domain.RecipeIngredient;
import com.breadcost.masterdata.RecipeEntity;
import com.breadcost.masterdata.RecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST API for Recipe management (FR-4.1 → FR-4.9)
 * Only Technologist (or Admin) can create/activate recipes
 */
@RestController
@RequestMapping("/v1/recipes")
@Slf4j
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    // -------------------------------------------------------------------------
    // Request DTOs
    // -------------------------------------------------------------------------

    public record IngredientRequest(
            @NotBlank String itemId,
            String itemName,
            @NotNull RecipeIngredient.UnitMode unitMode,
            // WEIGHT
            BigDecimal recipeQty,
            String recipeUom,
            // PIECE / COMBO
            Integer pieceQty,
            BigDecimal weightPerPiece,
            String pieceWeightUom,
            // Purchasing
            @NotNull @Positive BigDecimal purchasingUnitSize,
            @NotBlank String purchasingUom,
            // Waste
            BigDecimal wasteFactor
    ) {}

    public record CreateRecipeRequest(
            @NotBlank String tenantId,
            @NotBlank String productId,
            @NotNull @Positive BigDecimal batchSize,
            @NotBlank String batchSizeUom,
            @NotNull @Positive BigDecimal expectedYield,
            @NotBlank String yieldUom,
            String productionNotes,
            Integer leadTimeHours,
            @NotNull List<IngredientRequest> ingredients
    ) {}

    public record MaterialRequirementsRequest(
            @NotBlank String tenantId,
            @NotNull @Positive BigDecimal batchMultiplier
    ) {}

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    /**
     * GET /v1/recipes?tenantId=xxx&productId=yyy
     * Get full version history for a product's recipes
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor')")
    public ResponseEntity<List<RecipeEntity>> list(
            @RequestParam String tenantId,
            @RequestParam String productId) {
        return ResponseEntity.ok(recipeService.getVersionHistory(tenantId, productId));
    }

    /**
     * GET /v1/recipes/{recipeId}?tenantId=xxx
     * Get a specific recipe version
     */
    @GetMapping("/{recipeId}")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor')")
    public ResponseEntity<RecipeEntity> get(
            @PathVariable String recipeId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(recipeService.getById(tenantId, recipeId));
    }

    /**
     * GET /v1/recipes/active?tenantId=xxx&productId=yyy
     * Get the currently active recipe for a product
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor', 'ProductionUser')")
    public ResponseEntity<RecipeEntity> getActive(
            @RequestParam String tenantId,
            @RequestParam String productId) {
        return ResponseEntity.ok(recipeService.getActiveRecipe(tenantId, productId));
    }

    /**
     * POST /v1/recipes
     * Create a new recipe version (always starts as DRAFT) (FR-4.1, FR-4.2)
     * Only Technologist or Admin
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<RecipeEntity> create(@Valid @RequestBody CreateRecipeRequest req) {
        List<RecipeService.IngredientRequest> ingredients = req.ingredients().stream()
                .map(i -> new RecipeService.IngredientRequest(
                        i.itemId(), i.itemName(), i.unitMode(),
                        i.recipeQty(), i.recipeUom(),
                        i.pieceQty(), i.weightPerPiece(), i.pieceWeightUom(),
                        i.purchasingUnitSize(), i.purchasingUom(),
                        i.wasteFactor()
                )).toList();

        RecipeEntity created = recipeService.createVersion(
                new RecipeService.CreateRecipeRequest(
                        req.tenantId(), req.productId(),
                        req.batchSize(), req.batchSizeUom(),
                        req.expectedYield(), req.yieldUom(),
                        req.productionNotes(), req.leadTimeHours(), ingredients,
                        getPrincipalName()
                ));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /v1/recipes/{recipeId}/activate?tenantId=xxx
     * Activate this recipe version — archives the previous active version (FR-4.2, FR-4.7)
     * Only Technologist or Admin
     */
    @PostMapping("/{recipeId}/activate")
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<RecipeEntity> activate(
            @PathVariable String recipeId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(recipeService.activate(tenantId, recipeId));
    }

    /**
     * PUT /v1/recipes/{recipeId}/ingredients?tenantId=xxx
     * Replace the full ingredient list on a DRAFT recipe
     */
    @PutMapping("/{recipeId}/ingredients")
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<RecipeEntity> updateIngredients(
            @PathVariable String recipeId,
            @RequestParam String tenantId,
            @Valid @RequestBody List<IngredientRequest> body) {
        List<RecipeService.IngredientRequest> ingredients = body.stream()
                .map(i -> new RecipeService.IngredientRequest(
                        i.itemId(), i.itemName(), i.unitMode(),
                        i.recipeQty(), i.recipeUom(),
                        i.pieceQty(), i.weightPerPiece(), i.pieceWeightUom(),
                        i.purchasingUnitSize(), i.purchasingUom(),
                        i.wasteFactor()
                )).toList();
        return ResponseEntity.ok(recipeService.updateIngredients(tenantId, recipeId, ingredients));
    }

    /**
     * GET /v1/recipes/{recipeId}/material-requirements?tenantId=xxx&batchMultiplier=3
     * Calculate purchasing units needed per ingredient for N batches (FR-4.4, FR-4.5)
     */
    @GetMapping("/{recipeId}/material-requirements")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor', 'WarehouseKeeper')")
    public ResponseEntity<List<RecipeService.MaterialRequirement>> materialRequirements(
            @PathVariable String recipeId,
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "1") BigDecimal batchMultiplier) {
        return ResponseEntity.ok(
                recipeService.calculateMaterialRequirements(tenantId, recipeId, batchMultiplier));
    }

    private String getPrincipalName() {
        return "system";
    }
}
