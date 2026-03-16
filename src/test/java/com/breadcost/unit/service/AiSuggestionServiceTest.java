package com.breadcost.unit.service;

import com.breadcost.ai.*;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.StoredEvent;
import com.breadcost.events.IssueToBatchEvent;
import com.breadcost.masterdata.*;
import com.breadcost.projections.InventoryProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSuggestionServiceTest {

    @Mock private AiReplenishmentHintRepository hintRepo;
    @Mock private AiDemandForecastRepository forecastRepo;
    @Mock private AiProductionSuggestionRepository prodSugRepo;
    @Mock private InventoryProjection inventoryProjection;
    @Mock private EventStore eventStore;
    @Mock private OrderRepository orderRepo;
    @Mock private ProductRepository productRepo;
    @Mock private RecipeRepository recipeRepo;
    @InjectMocks private AiSuggestionService svc;

    // ── generateReplenishmentHints ───────────────────────────────────────────

    @Test
    void generateReplenishmentHints_noPositions_returnsEmpty() {
        when(inventoryProjection.getAllPositions()).thenReturn(List.of());
        when(eventStore.getAllEvents()).thenReturn(List.of());

        var hints = svc.generateReplenishmentHints("t1", "WEEKLY");

        assertTrue(hints.isEmpty());
    }

    @Test
    void generateReplenishmentHints_sufficientStock_returnsEmpty() {
        var pos = InventoryProjection.InventoryPosition.builder()
                .tenantId("t1").itemId("i1").siteId("MAIN").lotId("L1")
                .onHandQty(new BigDecimal("9999")).uom("kg").build();
        when(inventoryProjection.getAllPositions()).thenReturn(List.of(pos));
        when(eventStore.getAllEvents()).thenReturn(List.of());

        var hints = svc.generateReplenishmentHints("t1", "WEEKLY");

        assertTrue(hints.isEmpty());
    }

    @Test
    void generateReplenishmentHints_lowStock_createsHint() {
        var pos = InventoryProjection.InventoryPosition.builder()
                .tenantId("t1").itemId("i1").siteId("MAIN").lotId("L1")
                .onHandQty(new BigDecimal("5")).uom("kg").build();
        when(inventoryProjection.getAllPositions()).thenReturn(List.of(pos));

        // Simulate consumption events
        var ibe = IssueToBatchEvent.builder()
                .tenantId("t1").itemId("i1").qty(new BigDecimal("300"))
                .occurredAtUtc(Instant.now().minus(5, ChronoUnit.DAYS)).build();
        var se = StoredEvent.builder()
                .eventType("IssueToBatch").event(ibe)
                .recordedAtUtc(Instant.now().minus(5, ChronoUnit.DAYS)).build();
        when(eventStore.getAllEvents()).thenReturn(List.of(se));
        when(hintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var hints = svc.generateReplenishmentHints("t1", "WEEKLY");

        assertFalse(hints.isEmpty());
        assertEquals("i1", hints.get(0).getItemId());
        assertEquals("WEEKLY", hints.get(0).getPeriod());
        assertTrue(hints.get(0).getSuggestedQty() > 0);
    }

    // ── dismissHint ──────────────────────────────────────────────────────────

    @Test
    void dismissHint_setsStatusDismissed() {
        var hint = AiReplenishmentHintEntity.builder()
                .hintId("h1").status("PENDING").build();
        when(hintRepo.findById("h1")).thenReturn(Optional.of(hint));
        when(hintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.dismissHint("h1");

        assertEquals("DISMISSED", result.getStatus());
    }

    @Test
    void dismissHint_notFound_throws() {
        when(hintRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> svc.dismissHint("bad"));
    }

    // ── generateDemandForecast ───────────────────────────────────────────────

    @Test
    void generateDemandForecast_noOrders_returnsEmpty() {
        when(orderRepo.findByTenantId("t1")).thenReturn(List.of());

        var forecasts = svc.generateDemandForecast("t1", 7);

        assertTrue(forecasts.isEmpty());
    }

    @Test
    void generateDemandForecast_withRecentOrders_createsForecast() {
        var line = OrderLineEntity.builder()
                .productId("p1").productName("Bread").qty(100).build();
        var order = OrderEntity.builder().orderId("o1").tenantId("t1")
                .customerId("c1").status("CONFIRMED")
                .orderPlacedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .lines(List.of(line)).build();
        when(orderRepo.findByTenantId("t1")).thenReturn(List.of(order));
        when(forecastRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var forecasts = svc.generateDemandForecast("t1", 7);

        assertEquals(1, forecasts.size());
        assertEquals("p1", forecasts.get(0).getProductId());
        assertTrue(forecasts.get(0).getForecastQty() > 0);
        assertEquals(30, forecasts.get(0).getBasedOnDays());
    }

    @Test
    void generateDemandForecast_oldOrders_ignored() {
        var line = OrderLineEntity.builder()
                .productId("p1").productName("Bread").qty(100).build();
        var order = OrderEntity.builder().orderId("o1").tenantId("t1")
                .customerId("c1").status("CONFIRMED")
                .orderPlacedAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .lines(List.of(line)).build();
        when(orderRepo.findByTenantId("t1")).thenReturn(List.of(order));

        var forecasts = svc.generateDemandForecast("t1", 7);

        assertTrue(forecasts.isEmpty());
    }

    // ── generateProductionSuggestions ─────────────────────────────────────────

    @Test
    void generateProductionSuggestions_noForecasts_generatesFirst() {
        when(forecastRepo.findByTenantId("t1")).thenReturn(List.of());
        // When no forecasts, it calls generateDemandForecast internally
        when(orderRepo.findByTenantId("t1")).thenReturn(List.of());

        var suggestions = svc.generateProductionSuggestions("t1", LocalDate.now());

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void generateProductionSuggestions_withForecasts_createsSuggestions() {
        var forecast = AiDemandForecastEntity.builder()
                .forecastId("f1").tenantId("t1").productId("p1").productName("Bread")
                .periodStart(LocalDate.now()).periodEnd(LocalDate.now().plusDays(7))
                .forecastQty(70.0).confidence(0.8).basedOnDays(30).build();
        when(forecastRepo.findByTenantId("t1")).thenReturn(List.of(forecast));
        var recipe = RecipeEntity.builder().recipeId("rec1").tenantId("t1")
                .productId("p1").batchSize(new BigDecimal("10")).build();
        when(recipeRepo.findByTenantIdAndProductId("t1", "p1")).thenReturn(List.of(recipe));
        when(prodSugRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var suggestions = svc.generateProductionSuggestions("t1", LocalDate.now());

        assertEquals(1, suggestions.size());
        assertEquals("p1", suggestions.get(0).getProductId());
        assertTrue(suggestions.get(0).getSuggestedBatches() >= 1);
        assertNotNull(suggestions.get(0).getReason());
    }

    @Test
    void generateProductionSuggestions_noRecipe_usesBatchSizeOne() {
        var forecast = AiDemandForecastEntity.builder()
                .forecastId("f1").tenantId("t1").productId("p1").productName("Bread")
                .periodStart(LocalDate.now()).periodEnd(LocalDate.now().plusDays(7))
                .forecastQty(35.0).confidence(0.7).basedOnDays(30).build();
        when(forecastRepo.findByTenantId("t1")).thenReturn(List.of(forecast));
        when(recipeRepo.findByTenantIdAndProductId("t1", "p1")).thenReturn(List.of());
        when(prodSugRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var suggestions = svc.generateProductionSuggestions("t1", LocalDate.now());

        assertEquals(1, suggestions.size());
        assertTrue(suggestions.get(0).getSuggestedBatches() >= 1);
    }
}
