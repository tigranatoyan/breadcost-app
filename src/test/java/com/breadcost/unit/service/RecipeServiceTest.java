package com.breadcost.unit.service;

import com.breadcost.domain.Recipe;
import com.breadcost.domain.RecipeIngredient;
import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock private RecipeRepository recipeRepo;
    @Mock private ProductRepository productRepo;
    @InjectMocks private RecipeService svc;

    // ── createVersion ────────────────────────────────────────────────────────

    @Test
    void createVersion_autoIncrementsVersionNumber() {
        var product = ProductEntity.builder().productId("p1").tenantId("t1").build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));
        when(recipeRepo.findMaxVersionNumber("t1", "p1")).thenReturn(2);
        when(recipeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new RecipeService.CreateRecipeRequest(
                "t1", "p1", bd("10"), "KG", bd("9"), "KG", "notes", 4,
                List.of(weightIngredient("flour", bd("5"), bd("25"))), "user1");

        var result = svc.createVersion(req);

        assertEquals(3, result.getVersionNumber());
        assertEquals(Recipe.RecipeStatus.DRAFT, result.getStatus());
    }

    @Test
    void createVersion_wrongTenantProduct_throws() {
        var product = ProductEntity.builder().productId("p1").tenantId("t2").build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));

        var req = new RecipeService.CreateRecipeRequest(
                "t1", "p1", bd("10"), "KG", bd("9"), "KG", null, null, List.of(), "user1");

        assertThrows(IllegalArgumentException.class, () -> svc.createVersion(req));
    }

    @Test
    void createVersion_unknownProduct_throws() {
        when(productRepo.findById("p99")).thenReturn(Optional.empty());

        var req = new RecipeService.CreateRecipeRequest(
                "t1", "p99", bd("10"), "KG", bd("9"), "KG", null, null, List.of(), "user1");

        assertThrows(IllegalArgumentException.class, () -> svc.createVersion(req));
    }

    // ── activate ─────────────────────────────────────────────────────────────

    @Test
    void activate_archivesPreviousActive() {
        var existing = RecipeEntity.builder()
                .recipeId("r1").tenantId("t1").productId("p1")
                .status(Recipe.RecipeStatus.ACTIVE).versionNumber(1).build();
        var target = RecipeEntity.builder()
                .recipeId("r2").tenantId("t1").productId("p1")
                .status(Recipe.RecipeStatus.DRAFT).versionNumber(2).build();
        var product = ProductEntity.builder().productId("p1").tenantId("t1").build();

        when(recipeRepo.findById("r2")).thenReturn(Optional.of(target));
        when(recipeRepo.findByTenantIdAndProductIdAndStatus("t1", "p1", Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of(existing));
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));
        when(recipeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.activate("t1", "r2");

        assertEquals(Recipe.RecipeStatus.ACTIVE, result.getStatus());
        assertEquals(Recipe.RecipeStatus.ARCHIVED, existing.getStatus());
        assertEquals("r2", product.getActiveRecipeId());
    }

    @Test
    void activate_alreadyActive_idempotent() {
        var recipe = RecipeEntity.builder()
                .recipeId("r1").tenantId("t1").status(Recipe.RecipeStatus.ACTIVE).build();
        when(recipeRepo.findById("r1")).thenReturn(Optional.of(recipe));

        var result = svc.activate("t1", "r1");

        assertEquals(Recipe.RecipeStatus.ACTIVE, result.getStatus());
        verify(recipeRepo, never()).save(any());
    }

    @Test
    void activate_wrongTenant_throws() {
        var recipe = RecipeEntity.builder().recipeId("r1").tenantId("t2").build();
        when(recipeRepo.findById("r1")).thenReturn(Optional.of(recipe));

        assertThrows(IllegalArgumentException.class, () -> svc.activate("t1", "r1"));
    }

    // ── getActiveRecipe ──────────────────────────────────────────────────────

    @Test
    void getActiveRecipe_found() {
        var recipe = RecipeEntity.builder().recipeId("r1").tenantId("t1").productId("p1")
                .status(Recipe.RecipeStatus.ACTIVE).build();
        when(recipeRepo.findByTenantIdAndProductIdAndStatus("t1", "p1", Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of(recipe));

        assertEquals("r1", svc.getActiveRecipe("t1", "p1").getRecipeId());
    }

    @Test
    void getActiveRecipe_notFound_throws() {
        when(recipeRepo.findByTenantIdAndProductIdAndStatus("t1", "p1", Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> svc.getActiveRecipe("t1", "p1"));
    }

    // ── updateIngredients ────────────────────────────────────────────────────

    @Test
    void updateIngredients_draftRecipe_replaces() {
        var recipe = RecipeEntity.builder()
                .recipeId("r1").tenantId("t1")
                .status(Recipe.RecipeStatus.DRAFT)
                .ingredients(new ArrayList<>())
                .build();
        when(recipeRepo.findById("r1")).thenReturn(Optional.of(recipe));
        when(recipeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.updateIngredients("t1", "r1",
                List.of(weightIngredient("sugar", bd("3"), bd("10"))));

        assertEquals(1, result.getIngredients().size());
    }

    @Test
    void updateIngredients_activeRecipe_throws() {
        var recipe = RecipeEntity.builder()
                .recipeId("r1").tenantId("t1").status(Recipe.RecipeStatus.ACTIVE).build();
        when(recipeRepo.findById("r1")).thenReturn(Optional.of(recipe));

        List<RecipeService.IngredientRequest> emptyList = List.of();
        assertThrows(IllegalStateException.class,
                () -> svc.updateIngredients("t1", "r1", emptyList));
    }

    // ── ingredient validation ────────────────────────────────────────────────

    @Test
    void createVersion_weightMode_missingQty_throws() {
        var product = ProductEntity.builder().productId("p1").tenantId("t1").build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));
        when(recipeRepo.findMaxVersionNumber("t1", "p1")).thenReturn(0);

        var badIng = new RecipeService.IngredientRequest(
                "flour", "Flour", RecipeIngredient.UnitMode.WEIGHT,
                null, "KG", null, null, null, bd("25"), "KG", null);

        var req = new RecipeService.CreateRecipeRequest(
                "t1", "p1", bd("10"), "KG", bd("9"), "KG", null, null, List.of(badIng), "user1");

        assertThrows(IllegalArgumentException.class, () -> svc.createVersion(req));
    }

    @Test
    void createVersion_pieceMode_missingPieceQty_throws() {
        var product = ProductEntity.builder().productId("p1").tenantId("t1").build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));
        when(recipeRepo.findMaxVersionNumber("t1", "p1")).thenReturn(0);

        var badIng = new RecipeService.IngredientRequest(
                "eggs", "Eggs", RecipeIngredient.UnitMode.PIECE,
                null, null, null, null, null, bd("1"), "DOZEN", null);

        var req = new RecipeService.CreateRecipeRequest(
                "t1", "p1", bd("10"), "KG", bd("9"), "KG", null, null, List.of(badIng), "user1");

        assertThrows(IllegalArgumentException.class, () -> svc.createVersion(req));
    }

    @Test
    void createVersion_missingPurchasingUnitSize_throws() {
        var product = ProductEntity.builder().productId("p1").tenantId("t1").build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));
        when(recipeRepo.findMaxVersionNumber("t1", "p1")).thenReturn(0);

        var badIng = new RecipeService.IngredientRequest(
                "flour", "Flour", RecipeIngredient.UnitMode.WEIGHT,
                bd("5"), "KG", null, null, null, null, "KG", null);

        var req = new RecipeService.CreateRecipeRequest(
                "t1", "p1", bd("10"), "KG", bd("9"), "KG", null, null, List.of(badIng), "user1");

        assertThrows(IllegalArgumentException.class, () -> svc.createVersion(req));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }

    private RecipeService.IngredientRequest weightIngredient(String itemId, BigDecimal qty, BigDecimal purchasingUnitSize) {
        return new RecipeService.IngredientRequest(
                itemId, itemId, RecipeIngredient.UnitMode.WEIGHT,
                qty, "KG", null, null, null, purchasingUnitSize, "KG", null);
    }
}
