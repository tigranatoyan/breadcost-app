package com.breadcost.unit.service;

import com.breadcost.masterdata.*;
import com.breadcost.projections.InventoryProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private InventoryProjection inventoryProjection;
    @Mock private RecipeRepository recipeRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductionPlanRepository planRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private InventoryService inventoryService;
    @InjectMocks private StockAlertService svc;

    // ── detectLowStock ───────────────────────────────────────────────────────

    @Test
    void detectLowStock_noActiveItems_returnsEmpty() {
        when(itemRepository.findByTenantId("t1")).thenReturn(List.of());

        var alerts = svc.detectLowStock("t1");

        assertTrue(alerts.isEmpty());
    }

    @Test
    void detectLowStock_criticalLevel_zeroOnHand() {
        var item = ItemEntity.builder().itemId("i1").tenantId("t1").name("Flour")
                .baseUom("kg").active(true).minStockThreshold(50).build();
        when(itemRepository.findByTenantId("t1")).thenReturn(List.of(item));
        when(inventoryService.getTotalOnHand("t1", "i1")).thenReturn(BigDecimal.ZERO);
        when(recipeRepository.findAll()).thenReturn(List.of());

        var alerts = svc.detectLowStock("t1");

        assertEquals(1, alerts.size());
        assertEquals("CRITICAL", alerts.get(0).severity());
        assertEquals("i1", alerts.get(0).itemId());
    }

    @Test
    void detectLowStock_lowLevel_belowThreshold() {
        var item = ItemEntity.builder().itemId("i1").tenantId("t1").name("Sugar")
                .baseUom("kg").active(true).minStockThreshold(100).build();
        when(itemRepository.findByTenantId("t1")).thenReturn(List.of(item));
        when(inventoryService.getTotalOnHand("t1", "i1")).thenReturn(new BigDecimal("30"));
        when(recipeRepository.findAll()).thenReturn(List.of());

        var alerts = svc.detectLowStock("t1");

        assertEquals(1, alerts.size());
        assertEquals("LOW", alerts.get(0).severity());
    }

    @Test
    void detectLowStock_aboveThreshold_noAlert() {
        var item = ItemEntity.builder().itemId("i1").tenantId("t1").name("Flour")
                .baseUom("kg").active(true).minStockThreshold(50).build();
        when(itemRepository.findByTenantId("t1")).thenReturn(List.of(item));
        when(inventoryService.getTotalOnHand("t1", "i1")).thenReturn(new BigDecimal("100"));

        var alerts = svc.detectLowStock("t1");

        assertTrue(alerts.isEmpty());
    }

    // ── autoCreateProductionPlan ─────────────────────────────────────────────

    @Test
    void autoCreateProductionPlan_noConfirmedOrders_returnsMessage() {
        when(orderRepository.findByTenantIdAndStatus("t1", "CONFIRMED"))
                .thenReturn(List.of());

        var result = svc.autoCreateProductionPlan("t1", "MAIN", "user1");

        assertNull(result.plan());
        assertEquals("No confirmed orders to plan", result.message());
    }

    @Test
    void autoCreateProductionPlan_noProducts_returnsNoWorkOrders() {
        var line = OrderLineEntity.builder().productId("p1").productName("Bread").qty(10).build();
        var order = OrderEntity.builder().orderId("o1").tenantId("t1")
                .customerId("c1").status("CONFIRMED").lines(List.of(line)).build();
        when(orderRepository.findByTenantIdAndStatus("t1", "CONFIRMED"))
                .thenReturn(List.of(order));
        when(productRepository.findById("p1")).thenReturn(Optional.empty());

        var result = svc.autoCreateProductionPlan("t1", "MAIN", "user1");

        assertNull(result.plan());
        assertTrue(result.message().contains("No work orders"));
    }

    @Test
    void autoCreateProductionPlan_success_createsPlanWithWorkOrders() {
        var line = OrderLineEntity.builder().productId("p1").productName("Bread").qty(50).build();
        var order = OrderEntity.builder().orderId("o1").tenantId("t1")
                .customerId("c1").status("CONFIRMED").lines(List.of(line)).build();
        when(orderRepository.findByTenantIdAndStatus("t1", "CONFIRMED"))
                .thenReturn(List.of(order));

        var product = ProductEntity.builder().productId("p1").tenantId("t1")
                .name("Bread").departmentId("dept1").build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        var recipe = RecipeEntity.builder().recipeId("rec1").tenantId("t1")
                .productId("p1").status(com.breadcost.domain.Recipe.RecipeStatus.ACTIVE)
                .batchSize(new BigDecimal("10")).batchSizeUom("pcs")
                .leadTimeHours(2).build();
        when(recipeRepository.findByTenantIdAndProductIdAndStatus(
                "t1", "p1", com.breadcost.domain.Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of(recipe));
        when(inventoryService.checkMaterialAvailability("t1", "rec1", 5))
                .thenReturn(List.of());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.autoCreateProductionPlan("t1", "MAIN", "user1");

        assertNotNull(result.plan());
        assertEquals(1, result.plan().getWorkOrders().size());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void autoCreateProductionPlan_withMaterialShortage_addsWarning() {
        var line = OrderLineEntity.builder().productId("p1").productName("Bread").qty(50).build();
        var order = OrderEntity.builder().orderId("o1").tenantId("t1")
                .customerId("c1").status("CONFIRMED").lines(List.of(line)).build();
        when(orderRepository.findByTenantIdAndStatus("t1", "CONFIRMED"))
                .thenReturn(List.of(order));

        var product = ProductEntity.builder().productId("p1").tenantId("t1")
                .name("Bread").departmentId("dept1").build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        var recipe = RecipeEntity.builder().recipeId("rec1").tenantId("t1")
                .productId("p1").status(com.breadcost.domain.Recipe.RecipeStatus.ACTIVE)
                .batchSize(new BigDecimal("10")).batchSizeUom("pcs")
                .leadTimeHours(2).build();
        when(recipeRepository.findByTenantIdAndProductIdAndStatus(
                "t1", "p1", com.breadcost.domain.Recipe.RecipeStatus.ACTIVE))
                .thenReturn(List.of(recipe));

        var shortage = new InventoryService.MaterialShortage("i1", "Flour",
                new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("40"), "kg");
        when(inventoryService.checkMaterialAvailability("t1", "rec1", 5))
                .thenReturn(List.of(shortage));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.autoCreateProductionPlan("t1", "MAIN", "user1");

        assertNotNull(result.plan());
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().get(0).contains("Flour"));
    }
}
