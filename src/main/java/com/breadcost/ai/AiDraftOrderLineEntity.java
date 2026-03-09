package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

/**
 * Line item within an AI draft order (FR-12.1).
 */
@Entity
@Table(name = "ai_draft_order_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiDraftOrderLineEntity {

    @Id
    private String lineId;

    @Column(nullable = false)
    private String draftId;

    @Column(nullable = false)
    private String tenantId;

    private String productId;

    @Column(length = 255)
    private String productName;

    @Column(nullable = false)
    @Builder.Default
    private double qty = 1;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private boolean isUpsell = false;
}
