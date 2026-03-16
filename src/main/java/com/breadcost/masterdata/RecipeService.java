package com.breadcost.masterdata;

import com.breadcost.domain.Recipe;
import com.breadcost.domain.RecipeIngredient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeService {

    private static final String RECIPE_NOT_FOUND = "Recipe not found: ";
    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final RecipeTemplateRepository templateRepository;
    private final TechnologyStepRepository stepRepository;

    // -------------------------------------------------------------------------
    // Request DTOs
    // -------------------------------------------------------------------------

    public record IngredientRequest(
            String itemId,
            String itemName,
            RecipeIngredient.UnitMode unitMode,
            // WEIGHT mode
            BigDecimal recipeQty,
            String recipeUom,
            // PIECE / COMBO mode
            Integer pieceQty,
            BigDecimal weightPerPiece,
            String pieceWeightUom,
            // Purchasing unit
            BigDecimal purchasingUnitSize,
            String purchasingUom,
            // Waste
            BigDecimal wasteFactor
    ) {}

    public record CreateRecipeRequest(
            String tenantId,
            String productId,
            BigDecimal batchSize,
            String batchSizeUom,
            BigDecimal expectedYield,
            String yieldUom,
            String productionNotes,
            Integer leadTimeHours,
            List<IngredientRequest> ingredients,
            String createdBy
    ) {}

    // -------------------------------------------------------------------------
    // Operations
    // -------------------------------------------------------------------------

    /**
     * Create a new recipe version for a product.
     * Version number is auto-incremented. New recipe starts in DRAFT status.
     */
    @CacheEvict(value = {"recipes", "activeRecipe", "recipe", "recipeMaterials"}, allEntries = true)
    @Transactional
    public RecipeEntity createVersion(CreateRecipeRequest req) {
        productRepository.findById(req.productId())
                .filter(p -> p.getTenantId().equals(req.tenantId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found or does not belong to tenant: " + req.productId()));

        int nextVersion = recipeRepository.findMaxVersionNumber(req.tenantId(), req.productId()) + 1;

        List<RecipeIngredientEntity> ingredientEntities = req.ingredients().stream()
                .map(i -> buildIngredientEntity(i, null)) // recipeId set after save not needed — we set it below
                .toList();

        String recipeId = UUID.randomUUID().toString();

        // Set recipeId on each ingredient
        ingredientEntities.forEach(i -> i.setRecipeId(recipeId));

        RecipeEntity entity = RecipeEntity.builder()
                .recipeId(recipeId)
                .tenantId(req.tenantId())
                .productId(req.productId())
                .versionNumber(nextVersion)
                .status(Recipe.RecipeStatus.DRAFT)
                .batchSize(req.batchSize())
                .batchSizeUom(req.batchSizeUom())
                .expectedYield(req.expectedYield())
                .yieldUom(req.yieldUom())
                .productionNotes(req.productionNotes())
                .leadTimeHours(req.leadTimeHours())
                .ingredients(ingredientEntities)
                .createdBy(req.createdBy())
                .build();

        RecipeEntity saved = recipeRepository.save(entity);
        log.info("Recipe version {} created for product {}: recipeId={}", nextVersion, req.productId(), recipeId);
        return saved;
    }

    /**
     * Activate a recipe version for a product.
     * - Sets target recipe to ACTIVE
     * - Archives any previously ACTIVE recipe for the same product
     * - Updates product's activeRecipeId
     */
    @CacheEvict(value = {"recipes", "activeRecipe", "recipe", "recipeMaterials"}, allEntries = true)
    @Transactional
    public RecipeEntity activate(String tenantId, String recipeId) {
        RecipeEntity target = recipeRepository.findById(recipeId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException(RECIPE_NOT_FOUND + recipeId));

        if (target.getStatus() == Recipe.RecipeStatus.ACTIVE) {
            return target; // Already active — idempotent
        }

        // Archive current active version if any
        recipeRepository
                .findByTenantIdAndProductIdAndStatus(tenantId, target.getProductId(), Recipe.RecipeStatus.ACTIVE)
                .forEach(existing -> {
                    existing.setStatus(Recipe.RecipeStatus.ARCHIVED);
                    recipeRepository.save(existing);
                    log.info("Recipe version {} archived for product {}", existing.getVersionNumber(), existing.getProductId());
                });

        // Activate target
        target.setStatus(Recipe.RecipeStatus.ACTIVE);
        RecipeEntity saved = recipeRepository.save(target);

        // Update product's active recipe pointer
        productRepository.findById(target.getProductId()).ifPresent(product -> {
            product.setActiveRecipeId(recipeId);
            productRepository.save(product);
        });

        log.info("Recipe version {} activated for product {}: recipeId={}",
                target.getVersionNumber(), target.getProductId(), recipeId);
        return saved;
    }

    /**
     * Get all recipe versions for a product (full history).
     */
    @Cacheable(value = "recipes", key = "#tenantId + ':' + #productId")
    @Transactional(readOnly = true)
    public List<RecipeEntity> getVersionHistory(String tenantId, String productId) {
        return recipeRepository.findByTenantIdAndProductId(tenantId, productId);
    }

    /**
     * Get the active recipe for a product.
     */
    @Cacheable(value = "activeRecipe", key = "#tenantId + ':' + #productId")
    @Transactional(readOnly = true)
    public RecipeEntity getActiveRecipe(String tenantId, String productId) {
        return recipeRepository
                .findByTenantIdAndProductIdAndStatus(tenantId, productId, Recipe.RecipeStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active recipe found for product: " + productId));
    }

    /**
     * Get a single recipe by ID.
     */
    @Cacheable(value = "recipe", key = "#tenantId + ':' + #recipeId")
    @Transactional(readOnly = true)
    public RecipeEntity getById(String tenantId, String recipeId) {
        return recipeRepository.findById(recipeId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException(RECIPE_NOT_FOUND + recipeId));
    }

    /**
     * Calculate total purchasing units needed per ingredient for a given batch multiplier.
     * e.g., batchMultiplier=3 means "produce 3 batches"
     */
    @Cacheable(value = "recipeMaterials", key = "#tenantId + ':' + #recipeId + ':' + #batchMultiplier")
    @Transactional(readOnly = true)
    public List<MaterialRequirement> calculateMaterialRequirements(
            String tenantId, String recipeId, BigDecimal batchMultiplier) {
        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException(RECIPE_NOT_FOUND + recipeId));
        return recipe.getIngredients().stream()
                .map(ing -> new MaterialRequirement(
                        ing.getItemId(),
                        ing.getItemName(),
                        ing.purchasingUnitsNeeded().multiply(batchMultiplier),
                        ing.getPurchasingUom(),
                        ing.totalRecipeWeight().multiply(batchMultiplier),
                        ing.getRecipeUom() != null ? ing.getRecipeUom() : ing.getPieceWeightUom()
                ))
                .toList();
    }

    public record MaterialRequirement(
            String itemId,
            String itemName,
            BigDecimal purchasingUnitsNeeded,
            String purchasingUom,
            BigDecimal totalRecipeWeight,
            String weightUom
    ) {}

    // -------------------------------------------------------------------------
    // Create from template
    // -------------------------------------------------------------------------

    @CacheEvict(value = {"recipes", "activeRecipe", "recipe", "recipeMaterials"}, allEntries = true)
    @Transactional
    public RecipeEntity createFromTemplate(String tenantId, String templateId,
                                            String productId, String createdBy) {
        RecipeTemplateEntity tpl = templateRepository.findById(templateId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        productRepository.findById(productId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found or does not belong to tenant: " + productId));

        int nextVersion = recipeRepository.findMaxVersionNumber(tenantId, productId) + 1;
        String recipeId = UUID.randomUUID().toString();

        // Copy ingredients from template
        List<RecipeIngredientEntity> ingredientEntities = tpl.getIngredients().stream()
                .map(i -> RecipeIngredientEntity.builder()
                        .ingredientLineId(UUID.randomUUID().toString())
                        .recipeId(recipeId)
                        .tenantId(tenantId)
                        .itemId(i.getItemId())
                        .itemName(i.getItemName())
                        .unitMode(RecipeIngredient.UnitMode.valueOf(i.getUnitMode()))
                        .recipeQty(i.getRecipeQty())
                        .recipeUom(i.getRecipeUom())
                        .pieceQty(i.getPieceQty())
                        .weightPerPiece(i.getWeightPerPiece())
                        .pieceWeightUom(i.getPieceWeightUom())
                        .purchasingUnitSize(i.getPurchasingUnitSize())
                        .purchasingUom(i.getPurchasingUom())
                        .wasteFactor(i.getWasteFactor())
                        .build())
                .toList();

        RecipeEntity entity = RecipeEntity.builder()
                .recipeId(recipeId)
                .tenantId(tenantId)
                .productId(productId)
                .versionNumber(nextVersion)
                .status(Recipe.RecipeStatus.DRAFT)
                .batchSize(tpl.getBatchSize())
                .batchSizeUom(tpl.getBatchSizeUom())
                .expectedYield(tpl.getExpectedYield())
                .yieldUom(tpl.getYieldUom())
                .productionNotes(tpl.getProductionNotes())
                .leadTimeHours(tpl.getLeadTimeHours())
                .ingredients(ingredientEntities)
                .createdBy(createdBy)
                .build();

        RecipeEntity saved = recipeRepository.save(entity);

        // Copy technology steps from template
        for (RecipeTemplateStepEntity ts : tpl.getSteps()) {
            TechnologyStepEntity step = TechnologyStepEntity.builder()
                    .stepId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .recipeId(recipeId)
                    .stepNumber(ts.getStepNumber())
                    .name(ts.getName())
                    .activities(ts.getActivities())
                    .instruments(ts.getInstruments())
                    .durationMinutes(ts.getDurationMinutes())
                    .temperatureCelsius(ts.getTemperatureCelsius())
                    .build();
            stepRepository.save(step);
        }

        log.info("Recipe created from template '{}' for product {}: recipeId={}, version={}",
                tpl.getName(), productId, recipeId, nextVersion);
        return saved;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RecipeIngredientEntity buildIngredientEntity(IngredientRequest req, String recipeId) {
        validateIngredientRequest(req);
        return RecipeIngredientEntity.builder()
                .ingredientLineId(UUID.randomUUID().toString())
                .recipeId(recipeId)
                .itemId(req.itemId())
                .itemName(req.itemName())
                .unitMode(req.unitMode())
                .recipeQty(req.recipeQty())
                .recipeUom(req.recipeUom())
                .pieceQty(req.pieceQty())
                .weightPerPiece(req.weightPerPiece())
                .pieceWeightUom(req.pieceWeightUom())
                .purchasingUnitSize(req.purchasingUnitSize())
                .purchasingUom(req.purchasingUom())
                .wasteFactor(req.wasteFactor())
                .build();
    }

    /**
     * Replace all ingredients on a DRAFT recipe with the provided list.
     * Not allowed on ACTIVE or ARCHIVED recipes.
     */
    @CacheEvict(value = {"recipes", "activeRecipe", "recipe", "recipeMaterials"}, allEntries = true)
    @Transactional
    public RecipeEntity updateIngredients(String tenantId, String recipeId, List<IngredientRequest> ingredientRequests) {
        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException(RECIPE_NOT_FOUND + recipeId));
        if (recipe.getStatus() != com.breadcost.domain.Recipe.RecipeStatus.DRAFT) {
            throw new IllegalStateException("Ingredients can only be updated on DRAFT recipes");
        }
        recipe.getIngredients().clear();
        List<RecipeIngredientEntity> newIngredients = ingredientRequests.stream()
                .map(i -> buildIngredientEntity(i, recipeId))
                .toList();
        recipe.getIngredients().addAll(newIngredients);
        recipe.setUpdatedAtUtc(java.time.Instant.now());
        return recipeRepository.save(recipe);
    }

    private void validateIngredientRequest(IngredientRequest req) {
        if (req.itemId() == null || req.itemId().isBlank()) {
            throw new IllegalArgumentException("Ingredient itemId is required");
        }
        if (req.purchasingUnitSize() == null || req.purchasingUnitSize().signum() <= 0) {
            throw new IllegalArgumentException("purchasingUnitSize must be positive for item: " + req.itemId());
        }
        switch (req.unitMode()) {
            case RecipeIngredient.UnitMode u when u == RecipeIngredient.UnitMode.WEIGHT
                    && (req.recipeQty() == null || req.recipeQty().signum() <= 0) ->
                throw new IllegalArgumentException("recipeQty required for WEIGHT mode, item: " + req.itemId());
            case RecipeIngredient.UnitMode u when (u == RecipeIngredient.UnitMode.PIECE || u == RecipeIngredient.UnitMode.COMBO)
                    && (req.pieceQty() == null || req.pieceQty() <= 0) ->
                throw new IllegalArgumentException("pieceQty required for PIECE/COMBO mode, item: " + req.itemId());
            case RecipeIngredient.UnitMode u when (u == RecipeIngredient.UnitMode.PIECE || u == RecipeIngredient.UnitMode.COMBO)
                    && (req.weightPerPiece() == null || req.weightPerPiece().signum() <= 0) ->
                throw new IllegalArgumentException("weightPerPiece required for PIECE/COMBO mode, item: " + req.itemId());
            default -> { /* valid – no further checks */ }
        }
    }
}
