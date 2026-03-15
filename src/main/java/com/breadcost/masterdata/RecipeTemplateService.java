package com.breadcost.masterdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeTemplateService {

    private final RecipeTemplateRepository templateRepository;
    private final RecipeRepository recipeRepository;
    private final TechnologyStepRepository stepRepository;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record CreateTemplateRequest(
            String tenantId,
            String name,
            String description,
            String category,
            java.math.BigDecimal batchSize,
            String batchSizeUom,
            java.math.BigDecimal expectedYield,
            String yieldUom,
            String productionNotes,
            Integer leadTimeHours,
            String createdBy
    ) {}

    // ── List & Get ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecipeTemplateEntity> list(String tenantId) {
        return templateRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<RecipeTemplateEntity> listByCategory(String tenantId, String category) {
        return templateRepository.findByTenantIdAndCategory(tenantId, category);
    }

    @Transactional(readOnly = true)
    public RecipeTemplateEntity getById(String tenantId, String templateId) {
        return templateRepository.findById(templateId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }

    // ── Save recipe as template ───────────────────────────────────────────────

    @Transactional
    public RecipeTemplateEntity saveRecipeAsTemplate(
            String tenantId, String recipeId, String name, String description,
            String category, String createdBy) {

        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + recipeId));

        String templateId = UUID.randomUUID().toString();

        // Copy ingredients
        List<RecipeTemplateIngredientEntity> templateIngredients = recipe.getIngredients().stream()
                .map(i -> RecipeTemplateIngredientEntity.builder()
                        .ingredientLineId(UUID.randomUUID().toString())
                        .templateId(templateId)
                        .tenantId(tenantId)
                        .itemId(i.getItemId())
                        .itemName(i.getItemName())
                        .unitMode(i.getUnitMode().name())
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

        // Copy technology steps
        List<TechnologyStepEntity> recipeSteps =
                stepRepository.findByTenantIdAndRecipeIdOrderByStepNumberAsc(tenantId, recipeId);

        List<RecipeTemplateStepEntity> templateSteps = recipeSteps.stream()
                .map(s -> RecipeTemplateStepEntity.builder()
                        .stepId(UUID.randomUUID().toString())
                        .templateId(templateId)
                        .tenantId(tenantId)
                        .stepNumber(s.getStepNumber())
                        .name(s.getName())
                        .activities(s.getActivities())
                        .instruments(s.getInstruments())
                        .durationMinutes(s.getDurationMinutes())
                        .temperatureCelsius(s.getTemperatureCelsius())
                        .build())
                .toList();

        RecipeTemplateEntity template = RecipeTemplateEntity.builder()
                .templateId(templateId)
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .category(category != null ? category : "General")
                .batchSize(recipe.getBatchSize())
                .batchSizeUom(recipe.getBatchSizeUom())
                .expectedYield(recipe.getExpectedYield())
                .yieldUom(recipe.getYieldUom())
                .productionNotes(recipe.getProductionNotes())
                .leadTimeHours(recipe.getLeadTimeHours())
                .createdBy(createdBy)
                .ingredients(new java.util.ArrayList<>(templateIngredients))
                .steps(new java.util.ArrayList<>(templateSteps))
                .build();

        RecipeTemplateEntity saved = templateRepository.save(template);
        log.info("Recipe {} saved as template '{}': templateId={}", recipeId, name, templateId);
        return saved;
    }

    // ── Delete template ───────────────────────────────────────────────────────

    @Transactional
    public void delete(String tenantId, String templateId) {
        RecipeTemplateEntity tpl = getById(tenantId, templateId);
        templateRepository.delete(tpl);
        log.info("Template deleted: {}", templateId);
    }
}
