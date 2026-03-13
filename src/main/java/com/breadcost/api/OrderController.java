package com.breadcost.api;

import com.breadcost.domain.Order;
import com.breadcost.masterdata.OrderEntity;
import com.breadcost.masterdata.OrderService;
import com.breadcost.masterdata.OrderService.CreateOrderLineRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ─── GET ALL ─────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','Manager','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<List<OrderEntity>> getOrders(
            @RequestParam String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId) {

        if (status != null) {
            return ResponseEntity.ok(orderService.getOrdersByStatus(tenantId, status.toUpperCase()));
        }
        if (customerId != null) {
            return ResponseEntity.ok(orderService.getOrdersByCustomer(tenantId, customerId));
        }
        return ResponseEntity.ok(orderService.getOrdersByTenant(tenantId));
    }

    // ─── GET ALL (PAGINATED) ─────────────────────────────────────────────────────

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('Admin','Manager','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<org.springframework.data.domain.Page<OrderEntity>> getOrdersPaged(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getOrdersByTenantPaged(tenantId, page, Math.min(size, 100)));
    }

    // ─── GET ONE ─────────────────────────────────────────────────────────────────

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('Admin','Manager','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<OrderEntity> getOrder(
            @PathVariable String orderId,
            @RequestParam String tenantId) {

        Optional<OrderEntity> order = orderService.getOrder(tenantId, orderId);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<OrderEntity> createOrder(@RequestBody CreateOrderRequest req) {
        OrderEntity created = orderService.createOrder(
                req.getTenantId(),
                req.getSiteId(),
                req.getCustomerId(),
                req.getCustomerName(),
                getPrincipalName(),
                req.getRequestedDeliveryTime(),
                req.isForceRush(),
                req.getCustomRushPremiumPct(),
                req.getNotes(),
                req.getLines(),
                req.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── CONFIRM ─────────────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<OrderEntity> confirmOrder(
            @PathVariable String orderId,
            @RequestParam String tenantId) {

        return ResponseEntity.ok(orderService.confirmOrder(tenantId, orderId, getPrincipalName()));
    }

    // ─── CANCEL ─────────────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<OrderEntity> cancelOrder(
            @PathVariable String orderId,
            @RequestParam String tenantId,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(orderService.cancelOrder(tenantId, orderId, getPrincipalName(), reason));
    }

    // ─── STATUS ADVANCE ───────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<OrderEntity> advanceStatus(
            @PathVariable String orderId,
            @RequestParam String tenantId,
            @RequestParam String targetStatus) {

        Order.Status target;
        try {
            target = Order.Status.valueOf(targetStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderService.advanceStatus(tenantId, orderId, target, getPrincipalName()));
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private String getPrincipalName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ─── REQUEST DTO ─────────────────────────────────────────────────────────────

    @Data
    public static class CreateOrderRequest {
        private String tenantId;
        private String siteId;
        private String customerId;
        private String customerName;
        private Instant requestedDeliveryTime;
        private boolean forceRush;
        private BigDecimal customRushPremiumPct;
        private String notes;
        private String idempotencyKey;
        private List<CreateOrderLineRequest> lines;
    }
}
