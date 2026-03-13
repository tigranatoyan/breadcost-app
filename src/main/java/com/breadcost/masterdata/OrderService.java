package com.breadcost.masterdata;

import com.breadcost.domain.Order;
import com.breadcost.domain.OrderLine;
import com.breadcost.events.OrderCancelledEvent;
import com.breadcost.events.OrderConfirmedEvent;
import com.breadcost.events.OrderCreatedEvent;
import com.breadcost.domain.LedgerEntry;
import com.breadcost.eventstore.EventStore;
import com.breadcost.mobile.MobileAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.breadcost.domain.Recipe;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final EventStore eventStore;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final MobileAppService mobileAppService;

    /** Hour of day (0-23) after which new standard orders are blocked (default 22 = 10 PM) */
    @Value("${breadcost.order.cutoff-hour:22}")
    private int cutoffHour;

    /** Timezone for cutoff evaluation (default UTC) */
    @Value("${breadcost.order.timezone:Asia/Tashkent}")
    private String orderTimezone;

    /** Rush premium percentage added to all lines on a rush order (overridable per request) */
    @Value("${breadcost.order.rush-premium-pct:15}")
    private int defaultRushPremiumPct;

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    @Transactional
    public OrderEntity createOrder(String tenantId, String siteId, String customerId, String customerName,
                                   String createdByUserId, Instant requestedDeliveryTime,
                                   boolean forceRush, BigDecimal customRushPremiumPct,
                                   String notes, List<CreateOrderLineRequest> lineRequests,
                                   String idempotencyKey) {

        // Default siteId and customerId when not provided (frontend may omit them)
        if (siteId == null || siteId.isBlank()) siteId = "default";
        if (customerId == null || customerId.isBlank())
            customerId = customerName != null ? customerName.toLowerCase().replaceAll("[^a-z0-9]", "-") : "unknown";

        Instant now = Instant.now();
        boolean afterCutoff = isAfterCutoff(now);

        // Rush order logic
        boolean rushOrder = forceRush || afterCutoff;
        BigDecimal rushPremiumPct = BigDecimal.ZERO;
        if (rushOrder) {
            rushPremiumPct = customRushPremiumPct != null
                    ? customRushPremiumPct
                    : BigDecimal.valueOf(defaultRushPremiumPct);
        }

        // Build lines with lead time conflict detection
        List<OrderLineEntity> lineEntities = new ArrayList<>();
        List<OrderLine> domainLines = new ArrayList<>();
        String orderId = UUID.randomUUID().toString();

        for (CreateOrderLineRequest req : lineRequests) {
            String lineId = UUID.randomUUID().toString();

            // Determine lead time conflict — use recipe lead time (falls back to department)
            boolean leadTimeConflict = false;
            Instant earliestReadyAt = null;

            Optional<RecipeEntity> activeRecipe = (req.getProductId() != null)
                    ? recipeRepository.findByTenantIdAndProductIdAndStatus(tenantId, req.getProductId(), Recipe.RecipeStatus.ACTIVE).stream().findFirst()
                    : Optional.empty();
            Optional<DepartmentEntity> dept = (req.getDepartmentId() != null)
                    ? departmentRepository.findById(req.getDepartmentId())
                    : Optional.empty();

            if (requestedDeliveryTime != null) {
                int leadHours = activeRecipe
                        .map(RecipeEntity::getLeadTimeHours)
                        .filter(h -> h != null && h > 0)
                        .orElseGet(() -> dept.map(DepartmentEntity::getLeadTimeHours).orElse(0));
                if (leadHours > 0) {
                    earliestReadyAt = now.plusSeconds(leadHours * 3600L);
                    leadTimeConflict = earliestReadyAt.isAfter(requestedDeliveryTime);
                }
            }

            OrderLineEntity lineEntity = OrderLineEntity.builder()
                    .orderLineId(lineId)
                    .productId(req.getProductId())
                    .productName(req.getProductName())
                    .departmentId(req.getDepartmentId())
                    .departmentName(dept.map(DepartmentEntity::getName).orElse(req.getDepartmentName()))
                    .qty(req.getQty())
                    .uom(req.getUom())
                    .unitPrice(req.getUnitPrice())
                    .leadTimeConflict(leadTimeConflict)
                    .earliestReadyAt(earliestReadyAt)
                    .notes(req.getLineNotes())
                    .build();

            domainLines.add(OrderLine.builder()
                    .orderLineId(lineId)
                    .productId(req.getProductId())
                    .productName(req.getProductName())
                    .departmentId(req.getDepartmentId())
                    .qty(req.getQty())
                    .uom(req.getUom())
                    .unitPrice(req.getUnitPrice())
                    .leadTimeConflict(leadTimeConflict)
                    .earliestReadyAt(earliestReadyAt)
                    .build());

            lineEntities.add(lineEntity);
        }

        // Compute total
        BigDecimal finalMultiplier = BigDecimal.ONE.add(rushPremiumPct.divide(BigDecimal.valueOf(100)));
        BigDecimal total = domainLines.stream()
                .map(l -> (l.getUnitPrice() != null ? l.getUnitPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(l.getQty())).multiply(finalMultiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Persist
        OrderEntity entity = OrderEntity.builder()
                .orderId(orderId)
                .tenantId(tenantId)
                .siteId(siteId)
                .customerId(customerId)
                .customerName(customerName)
                .createdByUserId(createdByUserId)
                .status(Order.Status.DRAFT.name())
                .requestedDeliveryTime(requestedDeliveryTime)
                .orderPlacedAt(now)
                .rushOrder(rushOrder)
                .rushPremiumPct(rushPremiumPct)
                .notes(notes)
                .totalAmount(total)
                .lines(new ArrayList<>())
                .build();

        // Link lines back to parent
        lineEntities.forEach(l -> l.setOrder(entity));
        entity.getLines().addAll(lineEntities);

        OrderEntity saved = orderRepository.save(entity);

        recordHistory(tenantId, orderId, "DRAFT", "Order placed");

        // Emit event
        eventStore.appendEvent(OrderCreatedEvent.builder()
                .orderId(orderId)
                .tenantId(tenantId)
                .siteId(siteId != null ? siteId : "default")
                .customerId(customerId)
                .customerName(customerName)
                .createdByUserId(createdByUserId)
                .requestedDeliveryTime(requestedDeliveryTime)
                .rushOrder(rushOrder)
                .rushPremiumPct(rushPremiumPct)
                .notes(notes)
                .lines(domainLines)
                .occurredAtUtc(now)
                .idempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);

        return saved;
    }

    // ─── CONFIRM ─────────────────────────────────────────────────────────────────

    @Transactional
    public OrderEntity confirmOrder(String tenantId, String orderId, String confirmedByUserId) {
        OrderEntity entity = findByTenantAndId(tenantId, orderId);
        if (!Order.Status.DRAFT.name().equals(entity.getStatus())) {
            throw new IllegalStateException("Only DRAFT orders can be confirmed. Current status: " + entity.getStatus());
        }

        Instant now = Instant.now();
        entity.setStatus(Order.Status.CONFIRMED.name());
        entity.setConfirmedAt(now);
        OrderEntity saved = orderRepository.save(entity);

        recordHistory(tenantId, orderId, "CONFIRMED", "Order confirmed");

        eventStore.appendEvent(OrderConfirmedEvent.builder()
                .orderId(orderId)
                .tenantId(tenantId)
                .siteId(entity.getSiteId() != null ? entity.getSiteId() : "default")
                .confirmedByUserId(confirmedByUserId)
                .occurredAtUtc(now)
                .idempotencyKey(UUID.randomUUID().toString())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);

        return saved;
    }

    // ─── CANCEL ─────────────────────────────────────────────────────────────────

    @Transactional
    public OrderEntity cancelOrder(String tenantId, String orderId, String cancelledByUserId, String reason) {
        OrderEntity entity = findByTenantAndId(tenantId, orderId);

        String status = entity.getStatus();
        boolean cancellable = Order.Status.DRAFT.name().equals(status) || Order.Status.CONFIRMED.name().equals(status);
        if (!cancellable) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }

        Instant now = Instant.now();
        entity.setStatus(Order.Status.CANCELLED.name());
        OrderEntity saved = orderRepository.save(entity);

        recordHistory(tenantId, orderId, "CANCELLED", reason != null ? "Cancelled: " + reason : "Order cancelled");

        eventStore.appendEvent(OrderCancelledEvent.builder()
                .orderId(orderId)
                .tenantId(tenantId)
                .siteId(entity.getSiteId() != null ? entity.getSiteId() : "default")
                .cancelledByUserId(cancelledByUserId)
                .reason(reason)
                .occurredAtUtc(now)
                .idempotencyKey(UUID.randomUUID().toString())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);

        return saved;
    }

    // ─── STATUS TRANSITIONS ───────────────────────────────────────────────────

    @Transactional
    public OrderEntity advanceStatus(String tenantId, String orderId, Order.Status targetStatus, String byUserId) {
        OrderEntity entity = findByTenantAndId(tenantId, orderId);
        Order.Status current = Order.Status.valueOf(entity.getStatus());

        validateTransition(current, targetStatus);
        entity.setStatus(targetStatus.name());
        OrderEntity saved = orderRepository.save(entity);

        recordHistory(tenantId, orderId, targetStatus.name(), "Status changed to " + targetStatus.name());

        // G-3: Auto-notify customer on status change
        try {
            String customerId = saved.getCustomerId();
            if (customerId != null && !customerId.isBlank()) {
                mobileAppService.notifyOrderStatusChange(tenantId, customerId, orderId, targetStatus.name());
            }
        } catch (Exception e) {
            log.warn("Failed to send order status notification for {}: {}", orderId, e.getMessage());
        }

        return saved;
    }

    private void validateTransition(Order.Status from, Order.Status to) {
        Map<Order.Status, List<Order.Status>> allowed = Map.of(
                Order.Status.CONFIRMED, List.of(Order.Status.IN_PRODUCTION, Order.Status.CANCELLED),
                Order.Status.IN_PRODUCTION, List.of(Order.Status.READY),
                Order.Status.READY, List.of(Order.Status.OUT_FOR_DELIVERY, Order.Status.DELIVERED),
                Order.Status.OUT_FOR_DELIVERY, List.of(Order.Status.DELIVERED)
        );
        List<Order.Status> permitted = allowed.getOrDefault(from, List.of());
        if (!permitted.contains(to)) {
            throw new IllegalStateException("Invalid transition: " + from + " → " + to);
        }
    }

    // ─── QUERIES ─────────────────────────────────────────────────────────────────

    public List<OrderEntity> getOrdersByTenant(String tenantId) {
        return orderRepository.findByTenantId(tenantId);
    }

    public org.springframework.data.domain.Page<OrderEntity> getOrdersByTenantPaged(
            String tenantId, int page, int size) {
        return orderRepository.findByTenantId(tenantId,
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("orderPlacedAt").descending()));
    }

    public List<OrderEntity> getOrdersByStatus(String tenantId, String status) {
        return orderRepository.findByTenantIdAndStatus(tenantId, status);
    }

    public List<OrderEntity> getOrdersByCustomer(String tenantId, String customerId) {
        return orderRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    public Optional<OrderEntity> getOrder(String tenantId, String orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> tenantId.equals(o.getTenantId()));
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private OrderEntity findByTenantAndId(String tenantId, String orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> tenantId.equals(o.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    public List<OrderStatusHistoryEntity> getTimeline(String orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByTimestampEpochMsAsc(orderId);
    }

    private void recordHistory(String tenantId, String orderId, String status, String description) {
        orderStatusHistoryRepository.save(OrderStatusHistoryEntity.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .tenantId(tenantId)
                .status(status)
                .description(description)
                .timestampEpochMs(System.currentTimeMillis())
                .build());
    }

    private boolean isAfterCutoff(Instant now) {
        ZonedDateTime local = now.atZone(ZoneId.of(orderTimezone));
        return local.getHour() >= cutoffHour;
    }

    // ─── REQUEST DTOs ─────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateOrderLineRequest {
        private String productId;
        private String productName;
        private String departmentId;
        private String departmentName;
        private double qty;
        private String uom;
        private java.math.BigDecimal unitPrice;
        private String lineNotes;
    }
}
