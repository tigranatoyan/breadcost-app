package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Department domain object
 * Represents a production department (e.g., Bakery, Pizza, Pastry)
 * Each department has its own lead time and warehouse configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    private String departmentId;
    private String tenantId;
    private String name;
    private Integer leadTimeHours;       // Hours needed to produce from order to ready
    private WarehouseMode warehouseMode; // SHARED or ISOLATED inventory
    private DepartmentStatus status;

    public enum WarehouseMode {
        SHARED,    // Department shares inventory with others — FIFO allocation, management can override
        ISOLATED   // Department has its own inventory — no cross-department consumption
    }

    public enum DepartmentStatus {
        ACTIVE,
        INACTIVE
    }
}
