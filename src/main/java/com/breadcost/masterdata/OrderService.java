package com.breadcost.masterdata;

import com.breadcost.domain.Order;
import com.breadcost.domain.OrderLine;
import com.breadcost.events.OrderCancelledEvent;
import com.breadcost.events.OrderConfirmedEvent;
import com.breadcost.events.OrderCreatedEvent;
import com.breadcost.domain.LedgerEntry;
import com.breadcost.eventstore.EventStore;
import com.breadcost.mobile.MobileAppService;
import com.breadcost.notifications.EmailNotificationService;
import com.breadcost.delivery.DeliveryRunOrderRepository;
import com.breadcost.delivery.DeliveryRunRepository;
import com.breadcost.driver.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.breadcost.domain.Recipe;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
public class OrderService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String SITE_DEFAULT = "default";

    private final OrderRepository orderRepository;
    private final DepartmentRepository departmentRepository;
    private final RecipeRepository recipeRepository;
    private final EventStore eventStore;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final MobileAppService mobileAppService;
    private final EmailNotificationService emailNotificationService;
    private final DeliveryRunOrderRepository deliveryRunOrderRepository;
    private final DeliveryRunRepository deliveryRunRepository;
    private final DriverService driverService;

    private final StockAlertService stockAlertService;

    public OrderService(
            OrderRepository orderRepository,
            DepartmentRepository departmentRepository,
            RecipeRepository recipeRepository,
            EventStore eventStore,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            MobileAppService mobileAppService,
            EmailNotificationService emailNotificationService,
            DeliveryRunOrderRepository deliveryRunOrderRepository,
            DeliveryRunRepository deliveryRunRepository,
            DriverService driverService,
            @Lazy StockAlertService stockAlertService) {
        this.orderRepository = orderRepository;
        this.departmentRepository = departmentRepository;
        this.recipeRepository = recipeRepository;
        this.eventStore = eventStore;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.mobileAppService = mobileAppService;
        this.emailNotificationService = emailNotificationService;
        this.deliveryRunOrderRepository = deliveryRunOrderRepository;
        this.deliveryRunRepository = deliveryRunRepository;
        this.driverService = driverService;
        this.stockAlertService = stockAlertService;
    }

    /** Hour of day (0-23) after which new standard orders are blocked (default 22 = 10 PM) */
    @Value("${breadcost.order.cutoff-hour:22}")
    private int cutoffHour;

    /** Timezone for cutoff evaluation (default UTC) */
    @Value("${breadcost.order.timezone:Asia/Tashkent}")
    private String orderTimezone;

    /** Rush premium percentage added to all lines on a rush order (overridable per request) */
    @Value("${breadcost.order.rush-premium-pct:15}")
    private int defaultRushPremiumPct;

    // ─── REQUEST DTOs ─────────────────────────────────────────────────────────────

    public record CreateOrderRequest(
            String tenantId, String siteId, String customerId, String customerName,
            String createdByUserId, Instant requestedDeliveryTime,
            boolean forceRush, BigDecimal customRushPremiumPct,
            String notes, List<CreateOrderLineRequest> lineRequests,
            String idempotencyKey) {}

    public record UpdateDraftOrderRequest(
            String tenantId, String orderId,
            String customerName, String customerId,
            Instant requestedDeliveryTime,
            boolean forceRush, BigDecimal customRushPremiumPct,
            String notes, List<CreateOrderLineRequest> lineRequests) {}

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    @Transactional
    public OrderEntity createOrder(CreateOrderRequest req) {

        String siteId = req.siteId();
        if (siteId == null || siteId.isBlank()) siteId = SITE_DEFAULT;
        String customerId = resolveCustomerId(req.customerId(), req.customerName(), "unknown");

        Instant now = Instant.now();
        RushInfo rush = resolveRushInfo(req.forceRush(), now, req.customRushPremiumPct());

        String orderId = UUID.randomUUID().toString();
        List<OrderLineEntity> lineEntities = buildLineEntities(req.tenantId(), req.lineRequests(), req.requestedDeliveryTime(), now);
        BigDecimal total = computeTotal(lineEntities, rush.rushPremiumPct());

        int nextOrderNumber = orderRepository.findTopByTenantIdOrderByOrderNumberDesc(req.tenantId())
                .map(OrderEntity::getOrderNumber)
                .map(n -> n + 1)
                .orElse(1);

        OrderEntity entity = OrderEntity.builder()
                .orderId(orderId)
                .orderNumber(nextOrderNumber)
                .tenantId(req.tenantId())
                .siteId(siteId)
                .customerId(customerId)
                .customerName(req.customerName())
                .createdByUserId(req.createdByUserId())
                .status(Order.Status.DRAFT.name())
                .requestedDeliveryTime(req.requestedDeliveryTime())
                .orderPlacedAt(now)
                .rushOrder(rush.rushOrder())
                .rushPremiumPct(rush.rushPremiumPct())
                .notes(req.notes())
                .totalAmount(total)
                .lines(new ArrayList<>())
                .build();

        lineEntities.forEach(l -> l.setOrder(entity));
        entity.getLines().addAll(lineEntities);

        OrderEntity saved = orderRepository.save(entity);
        recordHistory(req.tenantId(), orderId, STATUS_DRAFT, "Order placed");

        eventStore.appendEvent(OrderCreatedEvent.builder()
                .orderId(orderId)
                .tenantId(req.tenantId())
                .siteId(siteId)
                .customerId(customerId)
                .customerName(req.customerName())
                .createdByUserId(req.createdByUserId())
                .requestedDeliveryTime(req.requestedDeliveryTime())
                .rushOrder(rush.rushOrder())
                .rushPremiumPct(rush.rushPremiumPct())
                .notes(req.notes())
                .lines(toDomainLines(lineEntities))
                .occurredAtUtc(now)
                .idempotencyKey(req.idempotencyKey() != null ? req.idempotencyKey() : UUID.randomUUID().toString())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);

        return saved;
    }

    // ─── UPDATE DRAFT ────────────────────────────────────────────────────────────

    @Transactional
    public OrderEntity updateDraftOrder(UpdateDraftOrderRequest req) {

        OrderEntity order = findByTenantAndId(req.tenantId(), req.orderId());
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalStateException("Only DRAFT orders can be edited");
        }

        String cid = resolveCustomerId(req.customerId(), req.customerName(), order.getCustomerId());

        Instant now = Instant.now();
        RushInfo rush = resolveRushInfo(req.forceRush(), now, req.customRushPremiumPct());

        order.setCustomerName(req.customerName());
        order.setCustomerId(cid);
        order.setRequestedDeliveryTime(req.requestedDeliveryTime());
        order.setRushOrder(rush.rushOrder());
        order.setRushPremiumPct(rush.rushPremiumPct());
        order.setNotes(req.notes());

        order.getLines().clear();

        List<OrderLineEntity> lineEntities = buildLineEntities(req.tenantId(), req.lineRequests(), req.requestedDeliveryTime(), now);
        lineEntities.forEach(l -> l.setOrder(order));
        order.getLines().addAll(lineEntities);

        order.setTotalAmount(computeTotal(lineEntities, rush.rushPremiumPct()));

        OrderEntity saved = orderRepository.save(order);
        recordHistory(req.tenantId(), req.orderId(), STATUS_DRAFT, "Order edited");
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
                .siteId(entity.getSiteId() != null ? entity.getSiteId() : SITE_DEFAULT)
                .confirmedByUserId(confirmedByUserId)
                .occurredAtUtc(now)
                .idempotencyKey(UUID.randomUUID().toString())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);

        // Auto-create production plan from confirmed orders
        try {
            stockAlertService.autoCreateProductionPlan(tenantId, entity.getSiteId(), confirmedByUserId);
        } catch (Exception e) {
            log.warn("Auto-plan creation failed for order {}: {}", orderId, e.getMessage());
        }

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
                .siteId(entity.getSiteId() != null ? entity.getSiteId() : SITE_DEFAULT)
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

        // BC-260: Auto-start driver session when order goes OUT_FOR_DELIVERY
        if (targetStatus == Order.Status.OUT_FOR_DELIVERY) {
            try {
                deliveryRunOrderRepository.findByTenantIdAndOrderId(tenantId, orderId).stream().findFirst()
                        .ifPresent(dro -> {
                            String runId = dro.getRunId();
                            deliveryRunRepository.findByTenantIdAndRunId(tenantId, runId).ifPresent(run -> {
                                if (run.getDriverId() != null && !run.getDriverId().isBlank()) {
                                    driverService.startSession(tenantId, run.getDriverId(), run.getDriverName(), runId);
                                    log.info("Auto-started driver session for run {} on order {}", runId, orderId);
                                }
                            });
                        });
            } catch (Exception e) {
                log.warn("Failed to auto-start driver session for order {}: {}", orderId, e.getMessage());
            }
        }

        // G-3: Auto-notify customer on status change (push + email)
        try {
            String customerId = saved.getCustomerId();
            if (customerId != null && !customerId.isBlank()) {
                mobileAppService.notifyOrderStatusChange(tenantId, customerId, orderId, targetStatus.name());
                emailNotificationService.sendOrderStatusEmail(
                        tenantId, customerId, orderId,
                        saved.getCustomerName(), targetStatus.name());
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

    // ─── ORDER LINE HELPERS ──────────────────────────────────────────────────────

    private record RushInfo(boolean rushOrder, BigDecimal rushPremiumPct) {}

    private String resolveCustomerId(String customerId, String customerName, String fallback) {
        if (customerId != null && !customerId.isBlank()) return customerId;
        if (customerName != null) return customerName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        return fallback;
    }

    private RushInfo resolveRushInfo(boolean forceRush, Instant now, BigDecimal customRushPremiumPct) {
        boolean rush = forceRush || isAfterCutoff(now);
        BigDecimal pct = BigDecimal.ZERO;
        if (rush) {
            pct = customRushPremiumPct != null ? customRushPremiumPct : BigDecimal.valueOf(defaultRushPremiumPct);
        }
        return new RushInfo(rush, pct);
    }

    private List<OrderLineEntity> buildLineEntities(String tenantId,
                                                     List<CreateOrderLineRequest> lineRequests,
                                                     Instant requestedDeliveryTime, Instant now) {
        return lineRequests.stream()
                .map(req -> buildSingleLineEntity(tenantId, req, requestedDeliveryTime, now))
                .toList();
    }

    private OrderLineEntity buildSingleLineEntity(String tenantId, CreateOrderLineRequest req,
                                                   Instant requestedDeliveryTime, Instant now) {
        String lineId = UUID.randomUUID().toString();

        Optional<RecipeEntity> activeRecipe = (req.getProductId() != null)
                ? recipeRepository.findByTenantIdAndProductIdAndStatus(
                        tenantId, req.getProductId(), Recipe.RecipeStatus.ACTIVE)
                        .stream().findFirst()
                : Optional.empty();
        Optional<DepartmentEntity> dept = (req.getDepartmentId() != null)
                ? departmentRepository.findById(req.getDepartmentId())
                : Optional.empty();

        boolean leadTimeConflict = false;
        Instant earliestReadyAt = null;

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

        return OrderLineEntity.builder()
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
    }

    private List<OrderLine> toDomainLines(List<OrderLineEntity> entities) {
        return entities.stream()
                .map(e -> OrderLine.builder()
                        .orderLineId(e.getOrderLineId())
                        .productId(e.getProductId())
                        .productName(e.getProductName())
                        .departmentId(e.getDepartmentId())
                        .qty(e.getQty())
                        .uom(e.getUom())
                        .unitPrice(e.getUnitPrice())
                        .leadTimeConflict(e.isLeadTimeConflict())
                        .earliestReadyAt(e.getEarliestReadyAt())
                        .build())
                .toList();
    }

    private BigDecimal computeTotal(List<OrderLineEntity> lines, BigDecimal rushPremiumPct) {
        BigDecimal multiplier = BigDecimal.ONE.add(rushPremiumPct.divide(BigDecimal.valueOf(100)));
        return lines.stream()
                .map(l -> (l.getUnitPrice() != null ? l.getUnitPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(l.getQty())).multiply(multiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
