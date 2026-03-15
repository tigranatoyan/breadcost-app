package com.breadcost.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A production plan groups all WorkOrders for a given site/date/shift.
 * Lifecycle: DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED
 */
@Getter
@Builder
@With
public class ProductionPlan {

    public enum Status {
        DRAFT,
        GENERATED,
        APPROVED,
        PUBLISHED,   // legacy alias for APPROVED
        IN_PROGRESS,
        COMPLETED,
        REJECTED
    }

    public enum Shift {
        MORNING,
        AFTERNOON,
        NIGHT
    }

    private final String planId;
    private final String tenantId;
    private final String siteId;

    private final LocalDate planDate;
    private final Shift shift;

    private final Status status;

    private final String createdByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;

    /** Notes/instructions for the production team */
    private final String notes;

    private final List<WorkOrder> workOrders;
}
