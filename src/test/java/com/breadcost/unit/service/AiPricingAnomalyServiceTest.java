package com.breadcost.unit.service;

import com.breadcost.ai.*;
import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiPricingAnomalyServiceTest {

    @Mock private AiPricingSuggestionRepository pricingRepo;
    @Mock private AiAnomalyAlertRepository anomalyRepo;
    @Mock private OrderRepository orderRepo;
    @Mock private ProductRepository productRepo;
    @InjectMocks private AiPricingAnomalyService svc;

    // ── generatePricingSuggestions ────────────────────────────────────────────

    @Test
    void generatePricingSuggestions_highDemand_suggestsDiscount() {
        var product = ProductEntity.builder().productId("p1").name("Bread")
                .price(new BigDecimal("10.00")).tenantId("t1").build();
        when(productRepo.findByTenantId("t1")).thenReturn(List.of(product));

        // Create lots of orders within 90 days
        var lines = List.of(OrderLineEntity.builder()
                .productId("p1").productName("Bread").qty(600).build());
        var orders = new ArrayList<OrderEntity>();
        for (int i = 0; i < 10; i++) {
            orders.add(OrderEntity.builder().orderId("o" + i).tenantId("t1")
                    .customerId("c" + i).status("CONFIRMED")
                    .orderPlacedAt(Instant.now().minus(i, ChronoUnit.DAYS))
                    .lines(lines).build());
        }
        when(orderRepo.findByTenantId("t1")).thenReturn(orders);
        when(pricingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var suggestions = svc.generatePricingSuggestions("t1");

        assertFalse(suggestions.isEmpty());
        // High demand → discount
        assertTrue(suggestions.get(0).getSuggestedPrice().compareTo(new BigDecimal("10.00")) < 0);
    }

    @Test
    void generatePricingSuggestions_lowDemand_suggestsMarkup() {
        var product = ProductEntity.builder().productId("p1").name("Special")
                .price(new BigDecimal("10.00")).tenantId("t1").build();
        when(productRepo.findByTenantId("t1")).thenReturn(List.of(product));

        var lines = List.of(OrderLineEntity.builder()
                .productId("p1").productName("Special").qty(20).build());
        var order = OrderEntity.builder().orderId("o1").tenantId("t1")
                .customerId("c1").status("CONFIRMED")
                .orderPlacedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .lines(lines).build();
        when(orderRepo.findByTenantId("t1")).thenReturn(List.of(order));
        when(pricingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var suggestions = svc.generatePricingSuggestions("t1");

        assertFalse(suggestions.isEmpty());
        // Low demand → markup
        assertTrue(suggestions.get(0).getSuggestedPrice().compareTo(new BigDecimal("10.00")) > 0);
    }

    @Test
    void generatePricingSuggestions_noProducts_returnsEmpty() {
        when(productRepo.findByTenantId("t1")).thenReturn(List.of());
        when(orderRepo.findByTenantId("t1")).thenReturn(List.of());

        var suggestions = svc.generatePricingSuggestions("t1");

        assertTrue(suggestions.isEmpty());
    }

    // ── dismiss/accept suggestion ────────────────────────────────────────────

    @Test
    void dismissPricingSuggestion_setsStatus() {
        var sug = AiPricingSuggestionEntity.builder()
                .suggestionId("s1").status("PENDING").build();
        when(pricingRepo.findById("s1")).thenReturn(Optional.of(sug));
        when(pricingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.dismissPricingSuggestion("s1");

        assertEquals("DISMISSED", result.getStatus());
    }

    @Test
    void acceptPricingSuggestion_setsStatus() {
        var sug = AiPricingSuggestionEntity.builder()
                .suggestionId("s1").status("PENDING").build();
        when(pricingRepo.findById("s1")).thenReturn(Optional.of(sug));
        when(pricingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.acceptPricingSuggestion("s1");

        assertEquals("ACCEPTED", result.getStatus());
    }

    @Test
    void dismissPricingSuggestion_notFound_throws() {
        when(pricingRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> svc.dismissPricingSuggestion("bad"));
    }

    // ── acknowledgeAlert / dismissAlert ──────────────────────────────────────

    @Test
    void acknowledgeAlert_setsStatus() {
        var alert = AiAnomalyAlertEntity.builder()
                .alertId("a1").status("ACTIVE").build();
        when(anomalyRepo.findById("a1")).thenReturn(Optional.of(alert));
        when(anomalyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.acknowledgeAlert("a1");

        assertEquals("ACKNOWLEDGED", result.getStatus());
    }

    @Test
    void dismissAlert_setsStatus() {
        var alert = AiAnomalyAlertEntity.builder()
                .alertId("a1").status("ACTIVE").build();
        when(anomalyRepo.findById("a1")).thenReturn(Optional.of(alert));
        when(anomalyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.dismissAlert("a1");

        assertEquals("DISMISSED", result.getStatus());
    }

    @Test
    void acknowledgeAlert_notFound_throws() {
        when(anomalyRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> svc.acknowledgeAlert("bad"));
    }
}
