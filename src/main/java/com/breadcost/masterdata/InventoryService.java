package com.breadcost.masterdata;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.BackflushConsumptionEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.projections.InventoryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Centralized inventory operations — stock queries and consumption.
 * Bridges POS/Production services with EventStore + InventoryProjection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final EventStore eventStore;
    private final InventoryProjection inventoryProjection;
    private final RecipeService recipeService;

    /**
     * Get total on-hand quantity for an item across all lots/locations.
     */
    public BigDecimal getTotalOnHand(String tenantId, String itemId) {
        return inventoryProjection.getTotalOnHand(tenantId, itemId);
    }

    /**
     * Check whether sufficient stock exists for all ingredients of a recipe × batchCount.
     * Returns list of shortages (empty if all materials available).
     */
    public List<MaterialShortage> checkMaterialAvailability(
            String tenantId, String recipeId, int batchCount) {
        var requirements = recipeService.calculateMaterialRequirements(
                tenantId, recipeId, BigDecimal.valueOf(batchCount));

        return requirements.stream()
                .map(req -> {
                    BigDecimal onHand = getTotalOnHand(tenantId, req.itemId());
                    BigDecimal shortage = req.purchasingUnitsNeeded().subtract(onHand);
                    if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                        return new MaterialShortage(
                                req.itemId(), req.itemName(),
                                req.purchasingUnitsNeeded(), onHand, shortage,
                                req.purchasingUom());
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Consume ingredients for a production batch or POS sale.
     * Emits BackflushConsumptionEvent per ingredient line.
     */
    public void consumeIngredients(String tenantId, String siteId,
                                    String recipeId, int batchCount,
                                    String source, String referenceId) {
        var requirements = recipeService.calculateMaterialRequirements(
                tenantId, recipeId, BigDecimal.valueOf(batchCount));

        for (var req : requirements) {
            BackflushConsumptionEvent event = BackflushConsumptionEvent.builder()
                    .tenantId(tenantId)
                    .siteId(siteId != null ? siteId : "DEFAULT")
                    .itemId(req.itemId())
                    .qty(req.purchasingUnitsNeeded())
                    .uom(req.purchasingUom())
                    .source(source)
                    .referenceId(referenceId)
                    .occurredAtUtc(Instant.now())
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();

            eventStore.appendEvent(event, LedgerEntry.EntryClass.FINANCIAL);
            log.info("Consumed {} {} of {} for {} {}",
                    req.purchasingUnitsNeeded(), req.purchasingUom(),
                    req.itemName(), source, referenceId);
        }
    }

    public record MaterialShortage(
            String itemId, String itemName,
            BigDecimal required, BigDecimal onHand, BigDecimal shortage,
            String uom
    ) {}
}
