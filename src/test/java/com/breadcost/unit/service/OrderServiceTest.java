package com.breadcost.unit.service;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.domain.Order;
import com.breadcost.eventstore.EventStore;
import com.breadcost.masterdata.*;
import com.breadcost.mobile.MobileAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private EventStore eventStore;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private MobileAppService mobileAppService;

    @InjectMocks private OrderService orderService;

    private OrderEntity draftOrder;
    private OrderEntity confirmedOrder;

    @BeforeEach
    void setUp() {
        draftOrder = OrderEntity.builder()
                .orderId("order-1").tenantId("t1").siteId("default")
                .customerId("cust-1").customerName("Test Customer")
                .status(Order.Status.DRAFT.name())
                .orderPlacedAt(Instant.now())
                .lines(new ArrayList<>())
                .build();

        confirmedOrder = OrderEntity.builder()
                .orderId("order-2").tenantId("t1").siteId("default")
                .customerId("cust-1").customerName("Test Customer")
                .status(Order.Status.CONFIRMED.name())
                .orderPlacedAt(Instant.now())
                .confirmedAt(Instant.now())
                .lines(new ArrayList<>())
                .build();
    }

    // ── confirmOrder tests ───────────────────────────────────────────────

    @Test
    void confirmOrder_draftOrder_setsConfirmedStatus() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(draftOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderEntity result = orderService.confirmOrder("t1", "order-1", "admin");

        assertEquals(Order.Status.CONFIRMED.name(), result.getStatus());
        assertNotNull(result.getConfirmedAt());
        verify(eventStore).appendEvent(any(), eq(LedgerEntry.EntryClass.OPERATIONAL));
    }

    @Test
    void confirmOrder_nonDraftOrder_throwsException() {
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(confirmedOrder));

        assertThrows(IllegalStateException.class,
                () -> orderService.confirmOrder("t1", "order-2", "admin"));
        verify(orderRepository, never()).save(any());
    }

    // ── cancelOrder tests ────────────────────────────────────────────────

    @Test
    void cancelOrder_draftOrder_succeeds() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(draftOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderEntity result = orderService.cancelOrder("t1", "order-1", "admin", "Changed mind");

        assertEquals(Order.Status.CANCELLED.name(), result.getStatus());
        verify(eventStore).appendEvent(any(), eq(LedgerEntry.EntryClass.OPERATIONAL));
    }

    @Test
    void cancelOrder_confirmedOrder_succeeds() {
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderEntity result = orderService.cancelOrder("t1", "order-2", "admin", "Out of stock");

        assertEquals(Order.Status.CANCELLED.name(), result.getStatus());
    }

    @Test
    void cancelOrder_inProductionOrder_throwsException() {
        OrderEntity inProdOrder = OrderEntity.builder()
                .orderId("order-3").tenantId("t1")
                .status(Order.Status.IN_PRODUCTION.name())
                .lines(new ArrayList<>()).build();
        when(orderRepository.findById("order-3")).thenReturn(Optional.of(inProdOrder));

        assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder("t1", "order-3", "admin", "Too late"));
    }

    // ── advanceStatus tests ──────────────────────────────────────────────

    @Test
    void advanceStatus_confirmedToInProduction_succeeds() {
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderEntity result = orderService.advanceStatus("t1", "order-2",
                Order.Status.IN_PRODUCTION, "floor-user");

        assertEquals(Order.Status.IN_PRODUCTION.name(), result.getStatus());
    }

    @Test
    void advanceStatus_confirmedToReady_throwsException() {
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(confirmedOrder));

        assertThrows(IllegalStateException.class,
                () -> orderService.advanceStatus("t1", "order-2",
                        Order.Status.READY, "floor-user"));
    }

    @Test
    void advanceStatus_readyToDelivered_succeeds() {
        OrderEntity readyOrder = OrderEntity.builder()
                .orderId("order-4").tenantId("t1").customerId("cust-1")
                .status(Order.Status.READY.name())
                .lines(new ArrayList<>()).build();
        when(orderRepository.findById("order-4")).thenReturn(Optional.of(readyOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderEntity result = orderService.advanceStatus("t1", "order-4",
                Order.Status.DELIVERED, "driver");

        assertEquals(Order.Status.DELIVERED.name(), result.getStatus());
    }

    @Test
    void advanceStatus_draftToInProduction_throwsException() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(draftOrder));

        assertThrows(IllegalStateException.class,
                () -> orderService.advanceStatus("t1", "order-1",
                        Order.Status.IN_PRODUCTION, "admin"));
    }

    @Test
    void advanceStatus_sendsNotificationOnSuccess() {
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.advanceStatus("t1", "order-2", Order.Status.IN_PRODUCTION, "user");

        verify(mobileAppService).notifyOrderStatusChange("t1", "cust-1", "order-2", "IN_PRODUCTION");
    }

    // ── findByTenantAndId tenant isolation ────────────────────────────────

    @Test
    void confirmOrder_wrongTenant_throwsNotFound() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(draftOrder));

        assertThrows(NoSuchElementException.class,
                () -> orderService.confirmOrder("other-tenant", "order-1", "admin"));
    }

    @Test
    void getOrder_wrongTenant_returnsEmpty() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(draftOrder));

        Optional<OrderEntity> result = orderService.getOrder("other-tenant", "order-1");

        assertTrue(result.isEmpty());
    }
}
