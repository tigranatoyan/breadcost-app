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

        // ── G-1: Deduct inventory per recipe for each sale line ──────────────
        for (SaleLineEntity line : saved.getLines()) {
            try {
                RecipeEntity recipe = recipeRepository
                        .findByTenantIdAndProductIdAndStatus(tenantId, line.getProductId(),
                                Recipe.RecipeStatus.ACTIVE)
                        .stream().findFirst().orElse(null);

                if (recipe != null) {
                    // Each sale line qty represents finished product units;
                    // batchCount = ceil(qty / batchSize)
                    int batchCount = line.getQuantity()
                            .divide(recipe.getBatchSize(), 0, java.math.RoundingMode.CEILING)
                            .intValue();
                    if (batchCount < 1) batchCount = 1;

                    inventoryService.consumeIngredients(
                            tenantId, siteId,
                            recipe.getRecipeId(), batchCount,
                            "POS_SALE", saved.getSaleId());
                } else {
                    log.warn("No active recipe for product {} — skipping inventory deduction",
                            line.getProductId());
                }
            } catch (Exception e) {
                // Log but don't fail the sale — inventory deduction is best-effort
                log.error("Failed to deduct inventory for sale line {}: {}",
                        line.getLineId(), e.getMessage());
            }
        }

        log.info("POS sale completed: {} - {} - total={}", saved.getSaleId(),
                saved.getPaymentMethod(), saved.getTotalAmount());
        return saved;
    }

    public record SaleLineInput(
            String productId, String productName,
            BigDecimal quantity, String unit, BigDecimal unitPrice
    ) {}
}
