package com.breadcost.masterdata;

import com.breadcost.domain.Department;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity for Department master data
 */
@Entity
@Table(name = "departments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEntity {

    @Id
    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "lead_time_hours")
    private Integer leadTimeHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_mode", nullable = false)
    private Department.WarehouseMode warehouseMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department.DepartmentStatus status;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    public void prePersist() {
        createdAtUtc = Instant.now();
        updatedAtUtc = createdAtUtc;
        if (status == null) status = Department.DepartmentStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAtUtc = Instant.now();
    }
}
