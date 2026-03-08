package com.breadcost.customers;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Customer-specific pricing rule: discount percentage or fixed price override.
 * BC-1505: Customer-specific pricing and discount rules.
 */
@Entity
@Table(name = "customer_discount_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDiscountRuleEntity {

    @Id
    private String ruleId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    /** Type of item this rule applies to: PRODUCT or INGREDIENT. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ItemType itemType = ItemType.PRODUCT;

    /** The product/ingredient ID this rule applies to. Null = applies to all items. */
    private String itemId;

    /** Percentage discount (0-100). e.g. 10.0 = 10% off. */
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    /** Fixed price override per unit. If set, overrides catalogue price. */
    private BigDecimal fixedPrice;

    /** Minimum quantity that must be ordered for rule to apply. */
    @Builder.Default
    private BigDecimal minQty = BigDecimal.ONE;

    @Builder.Default
    private boolean active = true;

    private String notes;
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public enum ItemType {
        PRODUCT, INGREDIENT
    }
}
