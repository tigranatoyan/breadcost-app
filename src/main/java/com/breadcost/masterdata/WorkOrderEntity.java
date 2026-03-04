package com.breadcost.masterdata;

import com.breadcost.domain.WorkOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderEntity {

    @Id
    private String workOrderId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlanEntity plan;

    @Column(nullable = false)
    private String tenantId;

    private String departmentId;
    private String departmentName;
    private String productId;
    private String productName;
    private String recipeId;

    @Column(nullable = false)
    private double targetQty;

    private String uom;
    private int batchCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrder.Status status;

    private String assignedToUserId;
    private Instant startedAt;
    private Instant completedAt;

    /** Comma-separated list of source order line IDs */
    @Column(columnDefinition = "TEXT")
    private String sourceOrderLineIds;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * How many hours after the plan's notional start time this work order begins.
     * 0 = starts immediately. Used for parallel and offset scheduling.
     */
    @Column(name = "start_offset_hours")
    @Builder.Default
    private Integer startOffsetHours = 0;

    /**
     * Expected duration in hours, typically sourced from the active recipe's leadTimeHours.
     * Null = unknown / not set.
     */
    @Column(name = "duration_hours")
    private Integer durationHours;

    /** Derived: startOffsetHours + durationHours. Not persisted — calculated on read. */
    @Transient
    public Integer getEndOffsetHours() {
        if (startOffsetHours == null || durationHours == null) return null;
        return startOffsetHours + durationHours;
    }
}
