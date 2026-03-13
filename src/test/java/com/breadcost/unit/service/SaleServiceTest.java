package com.breadcost.unit.service;

import com.breadcost.domain.Recipe;
import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock private SaleRepository saleRepo;
    @Mock private RecipeRepository recipeRepo;
    @Mock private InventoryService inventoryService;
    @InjectMocks private SaleService svc;

    @Test
    void createSale_calculatesSubtotalAndChange() {
        when(saleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipeRepo.findByTenantIdAndProductIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(List.of());

        var lines = List.of(
                new SaleService.SaleLineInput("p1", "Bread", new BigDecimal("2"), "PCS", new BigDecimal("3.00")),
                new SaleService.SaleLineInput("p2", "Cake", new BigDecimal("1"), "PCS", new BigDecimal("10.00")));

        var sale = svc.createSale("t1", "site1", lines,
                SaleEntity.PaymentMethod.CASH, new BigDecimal("20.00"), null, "cashier1");

        // 2*3 + 1*10 = 16
        assertEquals(0, new BigDecimal("16.00").compareTo(sale.getSubtotal()));
        assertEquals(0, new BigDecimal("16.00").compareTo(sale.getTotalAmount()));
        // 20 - 16 = 4
        assertEquals(0, new BigDecimal("4.00").compareTo(sale.getChangeGiven()));
        assertEquals(SaleEntity.Status.COMPLETED, sale.getStatus());
        assertEquals("cashier1", sale.getCashierId());
    }

    @Test
    void createSale_cardPayment_noChange() {
        when(saleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipeRepo.findByTenantIdAndProductIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(List.of());

        var lines = List.of(
                new SaleService.SaleLineInput("p1", "Bread", new BigDecimal("1"), "PCS", new BigDecimal("5.00")));

        var sale = svc.createSale("t1", "site1", lines,
                SaleEntity.PaymentMethod.CARD, null, "ref123", "cashier1");

        assertEquals(BigDecimal.ZERO, sale.getChangeGiven());
        assertEquals("ref123", sale.getCardReference());
    }

    @Test
    void createSale_withActiveRecipe_deductsInventory() {
        when(saleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var recipe = RecipeEntity.builder().recipeId("r1").tenantId("t1").productId("p1")
                .batchSize(new BigDecimal("10")).status(Recipe.RecipeStatus.ACTIVE).build();
        when(recipeRepo.findByTenantIdAndProductIdAndStatus("t1", "p1", Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of(recipe));

        var lines = List.of(
                new SaleService.SaleLineInput("p1", "Bread", new BigDecimal("25"), "PCS", new BigDecimal("3.00")));

        svc.createSale("t1", "site1", lines, SaleEntity.PaymentMethod.CASH,
                new BigDecimal("100"), null, "cashier1");

        // 25 / 10 = 2.5 → ceiling = 3 batches
        verify(inventoryService).consumeIngredients(eq("t1"), eq("site1"), eq("r1"), eq(3), eq("POS_SALE"), any());
    }

    @Test
    void createSale_noRecipe_skipsInventoryGracefully() {
        when(saleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipeRepo.findByTenantIdAndProductIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(List.of());

        var lines = List.of(
                new SaleService.SaleLineInput("p1", "Bread", new BigDecimal("1"), "PCS", new BigDecimal("5.00")));

        // Should not throw even without recipe
        var sale = svc.createSale("t1", "site1", lines,
                SaleEntity.PaymentMethod.CASH, new BigDecimal("5"), null, "c1");

        assertNotNull(sale.getSaleId());
        verify(inventoryService, never()).consumeIngredients(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void createSale_inventoryFailure_doesNotAbort() {
        when(saleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var recipe = RecipeEntity.builder().recipeId("r1").tenantId("t1").productId("p1")
                .batchSize(new BigDecimal("10")).status(Recipe.RecipeStatus.ACTIVE).build();
        when(recipeRepo.findByTenantIdAndProductIdAndStatus("t1", "p1", Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of(recipe));
        doThrow(new RuntimeException("stock error"))
                .when(inventoryService).consumeIngredients(any(), any(), any(), anyInt(), any(), any());

        var lines = List.of(
                new SaleService.SaleLineInput("p1", "Bread", new BigDecimal("5"), "PCS", new BigDecimal("3.00")));

        // Sale should still complete even if inventory deduction fails
        var sale = svc.createSale("t1", "site1", lines,
                SaleEntity.PaymentMethod.CASH, new BigDecimal("20"), null, "c1");

        assertNotNull(sale.getSaleId());
    }
}
