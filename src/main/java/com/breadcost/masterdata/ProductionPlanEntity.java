package com.breadcost.masterdata;

import com.breadcost.domain.ProductionPlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionPlanEntity {

    @Id
    private String planId;

    @Column(nullable = false)
    private String tenantId;

    private String siteId;

    @Column(nullable = false)
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    private ProductionPlan.Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionPlan.Status status;

    private String createdByUserId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<WorkOrderEntity> workOrders = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = ProductionPlan.Status.DRAFT;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
