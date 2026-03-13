package com.breadcost.api;

import com.breadcost.masterdata.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * POS Controller — FR-8.1 through FR-8.6
 */
@Tag(name = "POS", description = "Point-of-sale transactions and reconciliation")
@RestController
@RequestMapping("/v1/pos")
@RequiredArgsConstructor
@Slf4j
public class PosController {

    private final SaleRepository saleRepository;
    private final SaleService saleService;
    private final InventoryService inventoryService;
    private final RecipeRepository recipeRepository;

    // ─── DTOs ────────────────────────────────────────────────────────────────

    @Data
    public static class SaleLineRequest {
        @NotBlank private String productId;
        @NotBlank private String productName;
        @NotNull private BigDecimal quantity;
        private String unit;
        @NotNull private BigDecimal unitPrice;
    }

    @Data
    public static class CreateSaleRequest {
        @NotBlank private String tenantId;
        private String siteId;
        @NotEmpty private List<SaleLineRequest> lines;
        @NotNull private SaleEntity.PaymentMethod paymentMethod;
        /** Cash received — required when paymentMethod == CASH */
        private BigDecimal cashReceived;
        /** Card terminal reference — required when paymentMethod == CARD */
        private String cardReference;
    }

    @Data
    public static class ReconcileRequest {
        @NotBlank private String tenantId;
        private String siteId;
        /** The date to reconcile (default: today) */
        private LocalDate date;
    }

    // ─── ENDPOINTS ───────────────────────────────────────────────────────────

    @Operation(summary = "List sales", description = "List POS sales, optionally filtered by date")
    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('Admin','Cashier','FinanceUser')")
    public ResponseEntity<List<SaleEntity>> getSales(
            @RequestParam String tenantId,
            @RequestParam(required = false) String date) {
        if (date != null) {
            LocalDate d = LocalDate.parse(date);
            Instant from = d.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to   = d.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            return ResponseEntity.ok(saleRepository.findByTenantIdAndDateRange(tenantId, from, to));
        }
        return ResponseEntity.ok(saleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
    }

    @Operation(summary = "Create POS sale", description = "Process a point-of-sale transaction with automatic inventory deduction")
    @ApiResponse(responseCode = "201", description = "Sale recorded and inventory consumed")
    @PostMapping("/sales")
    @PreAuthorize("hasAnyRole('Admin','Cashier')")
    public ResponseEntity<SaleEntity> createSale(
            @Valid @RequestBody CreateSaleRequest req,
            Authentication auth) {

        String cashierId = auth != null ? auth.getName() : "system";

        List<SaleService.SaleLineInput> lineInputs = req.getLines().stream()
                .map(l -> new SaleService.SaleLineInput(
                        l.getProductId(), l.getProductName(),
                        l.getQuantity(), l.getUnit(), l.getUnitPrice()))
                .toList();

        SaleEntity saved = saleService.createSale(
                req.getTenantId(), req.getSiteId(),
                lineInputs, req.getPaymentMethod(),
                req.getCashReceived(), req.getCardReference(),
                cashierId);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * BC-5002: Check stock availability for products before POS sale.
     * Returns warnings (not blocking) for products with low or zero stock.
     */
    @PostMapping("/stock-check")
    @PreAuthorize("hasAnyRole('Admin','Cashier')")
    public ResponseEntity<List<Map<String, Object>>> checkStock(
            @RequestParam String tenantId,
            @RequestBody List<SaleLineRequest> lines) {

        List<Map<String, Object>> warnings = new java.util.ArrayList<>();
        for (SaleLineRequest line : lines) {
            var recipes = recipeRepository.findByTenantIdAndProductIdAndStatus(
                    tenantId, line.getProductId(),
                    com.breadcost.domain.Recipe.RecipeStatus.ACTIVE);
            if (recipes.isEmpty()) continue;

            RecipeEntity recipe = recipes.get(0);
            int batchCount = line.getQuantity()
                    .divide(recipe.getBatchSize(), 0, java.math.RoundingMode.CEILING)
                    .intValue();
            if (batchCount < 1) batchCount = 1;

            var shortages = inventoryService.checkMaterialAvailability(
                    tenantId, recipe.getRecipeId(), batchCount);

            if (!shortages.isEmpty()) {
                warnings.add(Map.of(
                        "productId", line.getProductId(),
                        "productName", line.getProductName(),
                        "shortages", shortages.stream().map(s -> Map.of(
                                "itemId", s.itemId(),
                                "itemName", s.itemName(),
                                "required", s.required(),
                                "onHand", s.onHand(),
                                "shortage", s.shortage(),
                                "uom", s.uom()
                        )).toList()
                ));
            }
        }
        return ResponseEntity.ok(warnings);
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasAnyRole('Admin','Cashier','FinanceUser')")
    public ResponseEntity<Map<String, Object>> reconcile(@Valid @RequestBody ReconcileRequest req) {
        LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now(ZoneOffset.UTC);
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<SaleEntity> sales = saleRepository.findByTenantIdAndDateRange(req.getTenantId(), from, to);

        BigDecimal cashTotal = sales.stream()
                .filter(s -> s.getPaymentMethod() == SaleEntity.PaymentMethod.CASH && s.getStatus() == SaleEntity.Status.COMPLETED)
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cardTotal = sales.stream()
                .filter(s -> s.getPaymentMethod() == SaleEntity.PaymentMethod.CARD && s.getStatus() == SaleEntity.Status.COMPLETED)
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refunds = sales.stream()
                .filter(s -> s.getStatus() == SaleEntity.Status.REFUNDED)
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = cashTotal.add(cardTotal).subtract(refunds);

        return ResponseEntity.ok(Map.of(
                "date", date.toString(),
                "totalTransactions", sales.size(),
                "cashTotal", cashTotal,
                "cardTotal", cardTotal,
                "refunds", refunds,
                "netSales", net,
                "expectedCashInDrawer", cashTotal
        ));
    }
}
