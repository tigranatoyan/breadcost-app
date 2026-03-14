package com.breadcost.api;

import com.breadcost.masterdata.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Reports + Dashboard data — FR-10.1, FR-10.4, FR-10.5
 * Covers: GET /v1/reports/revenue-summary, /top-products, /production-summary, /inventory-valuation
 * and GET /v1/inventory/alerts (min-stock threshold)
 */
@Tag(name = "Reports", description = "Legacy report views")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private final OrderRepository orderRepository;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductionPlanRepository planRepository;

    // ─── REVENUE SUMMARY ─────────────────────────────────────────────────────

    @GetMapping("/v1/reports/revenue-summary")
    @PreAuthorize("hasAnyRole('Admin','FinanceUser','Viewer')")
    public ResponseEntity<Map<String, Object>> revenueSummary(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "month") String period) {

        List<OrderEntity> orders = orderRepository.findByTenantId(tenantId).stream()
                .filter(o -> !o.getStatus().equals("CANCELLED") && !o.getStatus().equals("DRAFT"))
                .toList();

        Instant now = Instant.now();
        Instant startOfDay   = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfWeek  = now.minus(java.time.Duration.ofDays(7));
        Instant startOfMonth = now.minus(java.time.Duration.ofDays(30));

        BigDecimal today = sum(orders, startOfDay, now);
        BigDecimal week  = sum(orders, startOfWeek, now);
        BigDecimal month = sum(orders, startOfMonth, now);
        BigDecimal total = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "today", today,
                "week", week,
                "month", month,
                "allTime", total,
                "currency", "AMD"
        ));
    }

    // ─── TOP PRODUCTS ─────────────────────────────────────────────────────────

    @Data
    static class TopProduct {
        String productId;
        String productName;
        double totalQty;
        BigDecimal totalRevenue;
        long orderCount;
    }

    @GetMapping("/v1/reports/top-products")
    @PreAuthorize("hasAnyRole('Admin','FinanceUser','Viewer')")
    public ResponseEntity<List<TopProduct>> topProducts(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "5") int limit) {

        Instant since = Instant.now().minus(java.time.Duration.ofDays(7));
        List<OrderEntity> recentOrders = orderRepository.findByTenantId(tenantId).stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(since))
                .filter(o -> !o.getStatus().equals("CANCELLED"))
                .toList();

        Map<String, TopProduct> map = new LinkedHashMap<>();
        for (OrderEntity order : recentOrders) {
            for (OrderLineEntity line : order.getLines()) {
                TopProduct tp = map.computeIfAbsent(line.getProductId(), id -> {
                    TopProduct t = new TopProduct();
                    t.productId = id;
                    t.productName = line.getProductName();
                    t.totalQty = 0;
                    t.totalRevenue = BigDecimal.ZERO;
                    t.orderCount = 0;
                    return t;
                });
                tp.totalQty += line.getQty();
                if (line.getUnitPrice() != null) {
                    tp.totalRevenue = tp.totalRevenue.add(
                            line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty())));
                }
                tp.orderCount++;
            }
        }

        List<TopProduct> result = map.values().stream()
                .sorted((a, b) -> Double.compare(b.totalQty, a.totalQty))
                .limit(limit)
                .toList();

        return ResponseEntity.ok(result);
    }

    // ─── PRODUCTION SUMMARY ──────────────────────────────────────────────────

    @GetMapping("/v1/reports/production-summary")
    @PreAuthorize("hasAnyRole('Admin','FinanceUser','Viewer','ProductionUser')")
    public ResponseEntity<Map<String, Object>> productionSummary(
            @RequestParam String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        List<ProductionPlanEntity> plans = planRepository.findByTenantId(tenantId);

        if (dateFrom != null) {
            plans = plans.stream()
                    .filter(p -> !p.getPlanDate().isBefore(dateFrom))
                    .toList();
        }
        if (dateTo != null) {
            plans = plans.stream()
                    .filter(p -> !p.getPlanDate().isAfter(dateTo))
                    .toList();
        }

        long totalPlans  = plans.size();
        long completed   = plans.stream().filter(p -> p.getStatus().name().equals("COMPLETED")).count();
        long inProgress  = plans.stream().filter(p -> p.getStatus().name().equals("IN_PROGRESS")).count();
        long draft       = plans.stream().filter(p -> p.getStatus().name().equals("DRAFT")).count();
        long generated   = plans.stream().filter(p -> p.getStatus().name().equals("GENERATED")).count();
        long approved    = plans.stream().filter(p -> p.getStatus().name().equals("APPROVED")).count();

        long totalWOs   = plans.stream().mapToLong(p -> p.getWorkOrders().size()).sum();
        long completedWOs = plans.stream()
                .flatMap(p -> p.getWorkOrders().stream())
                .filter(wo -> wo.getStatus().name().equals("COMPLETED"))
                .count();

        return ResponseEntity.ok(Map.of(
                "totalPlans", totalPlans,
                "completed", completed,
                "inProgress", inProgress,
                "draft", draft,
                "generated", generated,
                "approved", approved,
                "totalWorkOrders", totalWOs,
                "completedWorkOrders", completedWOs,
                "completionRate", totalWOs > 0
                        ? BigDecimal.valueOf(completedWOs).divide(BigDecimal.valueOf(totalWOs), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        ));
    }

    // ─── ORDERS SUMMARY (dashboard widget) ───────────────────────────────────

    @GetMapping("/v1/reports/orders-summary")
    @PreAuthorize("hasAnyRole('Admin','FinanceUser','Viewer')")
    public ResponseEntity<Map<String, Object>> ordersSummary(@RequestParam String tenantId) {
        List<OrderEntity> all = orderRepository.findByTenantId(tenantId);
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);

        long todayCount = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfDay))
                .count();
        BigDecimal todayValue = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfDay))
                .filter(o -> !o.getStatus().equals("CANCELLED"))
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(OrderEntity::getStatus, Collectors.counting()));

        return ResponseEntity.ok(Map.of(
                "todayCount", todayCount,
                "todayValue", todayValue,
                "byStatus", byStatus,
                "total", all.size()
        ));
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private BigDecimal sum(List<OrderEntity> orders, Instant from, Instant to) {
        return orders.stream()
                .filter(o -> o.getCreatedAt() != null
                        && o.getCreatedAt().isAfter(from)
                        && o.getCreatedAt().isBefore(to))
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
