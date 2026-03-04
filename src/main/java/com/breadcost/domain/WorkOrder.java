package com.breadcost.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * A work order is the instruction to produce a specific quantity of one product
 * in one department on one production plan.
 * Lifecycle: PENDING → STARTED → COMPLETED | CANCELLED
 */
@Getter
@Builder
@With
public class WorkOrder {

    public enum Status {
        PENDING,
        STARTED,
        COMPLETED,
        CANCELLED
    }

    private final String workOrderId;
    private final String planId;
    private final String tenantId;
    private final String departmentId;
    private final String departmentName;
    private final String productId;
    private final String productName;

    /** Active recipe used for this work order */
    private final String recipeId;

    /** Total quantity to produce (sum of all sourcing order lines) */
    private final double targetQty;
    private final String uom;

    /**
     * Number of recipe batches needed: ceil(targetQty / recipe.batchSize)
     * Stored for quick display — used by material requirements calculator.
     */
    private final int batchCount;

    private final Status status;

    /** Operator this work order is assigned to (nullable) */
    private final String assignedToUserId;

    private final Instant startedAt;
    private final Instant completedAt;

    private final String notes;

    /** IDs of order lines that were consolidated into this work order */
    private final List<String> sourceOrderLineIds;
}
