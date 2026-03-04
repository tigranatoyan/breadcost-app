package com.breadcost.api;

import com.breadcost.masterdata.OrderEntity;
import com.breadcost.masterdata.OrderService;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Customer Portal — Order Placement API
 *
 * BC-1104: Place order via customer portal
 *   POST /v2/orders → 201 { orderId, status, totalAmount }
 *
 * Public (customer JWT optional in future; currently tenant-scoped by tenantId).
 */
@RestController
@RequestMapping("/v2/orders")
@RequiredArgsConstructor
@Slf4j
public class CustomerOrderController {

    private final OrderService orderService;
    private final ProductRepository productRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class OrderItemRequest {
        @NotBlank private String productId;
        @Min(1) private double qty;
        private String notes;
    }

    @Data
    public static class PlaceOrderRequest {
        @NotBlank private String tenantId;
        @NotBlank private String customerId;
        private String customerName;
        @NotEmpty private List<OrderItemRequest> items;
        private Instant requestedDeliveryTime;
        private String notes;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * BC-1104: Place an order from the customer portal.
     * POST /v2/orders → 201 { orderId, status, totalAmount }
     *
     * Resolves product details (name, price, departmentId) from the catalog.
     * Falls back to rush order pricing when after cutoff.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest req) {

        List<OrderService.CreateOrderLineRequest> lines = req.getItems().stream()
                .map(item -> {
                    ProductEntity product = productRepository.findById(item.getProductId())
                            .filter(p -> p.getTenantId().equals(req.getTenantId()))
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Product not found: " + item.getProductId()));

                    return new OrderService.CreateOrderLineRequest(
                            product.getProductId(),
                            product.getName(),
                            product.getDepartmentId(),
                            product.getDepartmentId(), // departmentName fallback to id
                            item.getQty(),
                            product.getBaseUom(),
                            product.getPrice(),
                            item.getNotes()
                    );
                })
                .toList();

        OrderEntity order = orderService.createOrder(
                req.getTenantId(),
                null,                      // siteId — default
                req.getCustomerId(),
                req.getCustomerName(),
                req.getCustomerId(),       // createdByUserId = customer
                req.getRequestedDeliveryTime(),
                false,                     // forceRush = false
                null,                      // customRushPremiumPct
                req.getNotes(),
                lines,
                null                       // idempotencyKey
        );

        log.info("Customer order placed: tenantId={} customerId={} orderId={}",
                req.getTenantId(), req.getCustomerId(), order.getOrderId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId",     order.getOrderId(),
                "status",      order.getStatus(),
                "totalAmount", order.getTotalAmount() != null ? order.getTotalAmount() : 0
        ));
    }
}
