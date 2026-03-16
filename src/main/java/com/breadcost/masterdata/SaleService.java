package com.breadcost.masterdata;

import com.breadcost.domain.Recipe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * POS sale service — creates sales and triggers inventory deduction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final RecipeRepository recipeRepository;
    private final InventoryService inventoryService;

    @Transactional
    public SaleEntity createSale(String tenantId, String siteId,
                                  List<SaleLineInput> lineInputs,
                                  SaleEntity.PaymentMethod paymentMethod,
                                  BigDecimal cashReceived, String cardReference,
                                  String cashierId) {

        preCheckIngredientAvailability(tenantId, lineInputs);

        // Build lines
        List<SaleLineEntity> lines = lineInputs.stream().map(l -> {
            BigDecimal total = l.unitPrice().multiply(l.quantity());
            return SaleLineEntity.builder()
                    .lineId(UUID.randomUUID().toString())
                    .productId(l.productId())
                    .productName(l.productName())
                    .quantity(l.quantity())
                    .unit(l.unit())
                    .unitPrice(l.unitPrice())
                    .lineTotal(total)
                    .build();
        }).toList();

        BigDecimal subtotal = lines.stream()
                .map(SaleLineEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal changeGiven = BigDecimal.ZERO;
        if (paymentMethod == SaleEntity.PaymentMethod.CASH && cashReceived != null) {
            changeGiven = cashReceived.subtract(subtotal).max(BigDecimal.ZERO);
        }

        SaleEntity sale = SaleEntity.builder()
                .saleId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .siteId(siteId)
                .cashierId(cashierId)
                .cashierName(cashierId)
                .status(SaleEntity.Status.COMPLETED)
                .paymentMethod(paymentMethod)
                .subtotal(subtotal)
                .totalAmount(subtotal)
                .cashReceived(cashReceived)
                .changeGiven(changeGiven)
                .cardReference(cardReference)
                .completedAt(Instant.now())
                .build();

        lines.forEach(l -> l.setSale(sale));
        sale.setLines(new ArrayList<>(lines));

        SaleEntity saved = saleRepository.save(sale);

        deductSaleLineInventory(tenantId, siteId, saved);

        log.info("POS sale completed: {} - {} - total={}", saved.getSaleId(),
                saved.getPaymentMethod(), saved.getTotalAmount());
        return saved;
    }

    /** R5: Pre-check ingredient availability before completing sale. */
    private void preCheckIngredientAvailability(String tenantId, List<SaleLineInput> lineInputs) {
        for (SaleLineInput input : lineInputs) {
            var recipes = recipeRepository.findByTenantIdAndProductIdAndStatus(
                    tenantId, input.productId(), Recipe.RecipeStatus.ACTIVE);
            if (recipes.isEmpty()) {
                continue;
            }
            RecipeEntity recipe = recipes.get(0);
            var shortages = inventoryService.checkMaterialAvailability(
                    tenantId, recipe.getRecipeId(),
                    input.quantity().divide(recipe.getBatchSize(), 0,
                            java.math.RoundingMode.CEILING).intValue());
            if (!shortages.isEmpty()) {
                log.warn("POS stock warning for {}: {}", input.productName(), shortages);
            }
        }
    }

    /** G-1: Deduct inventory per recipe for each sale line (best-effort). */
    private void deductSaleLineInventory(String tenantId, String siteId, SaleEntity saved) {
        for (SaleLineEntity line : saved.getLines()) {
            try {
                deductSingleLineInventory(tenantId, siteId, saved.getSaleId(), line);
            } catch (Exception e) {
                log.error("Failed to deduct inventory for sale line {}: {}",
                        line.getLineId(), e.getMessage());
            }
        }
    }

    private void deductSingleLineInventory(String tenantId, String siteId,
                                            String saleId, SaleLineEntity line) {
        RecipeEntity recipe = recipeRepository
                .findByTenantIdAndProductIdAndStatus(tenantId, line.getProductId(),
                        Recipe.RecipeStatus.ACTIVE)
                .stream().findFirst().orElse(null);

        if (recipe == null) {
            log.warn("No active recipe for product {} — skipping inventory deduction",
                    line.getProductId());
            return;
        }

        int batchCount = line.getQuantity()
                .divide(recipe.getBatchSize(), 0, java.math.RoundingMode.CEILING)
                .intValue();
        if (batchCount < 1) batchCount = 1;

        inventoryService.consumeIngredients(
                tenantId, siteId,
                recipe.getRecipeId(), batchCount,
                "POS_SALE", saleId);
    }

    public record SaleLineInput(
            String productId, String productName,
            BigDecimal quantity, String unit, BigDecimal unitPrice
    ) {}
}
