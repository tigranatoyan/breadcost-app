package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, String> {
    List<WorkOrderEntity> findByTenantId(String tenantId);
    List<WorkOrderEntity> findByTenantIdAndDepartmentId(String tenantId, String departmentId);
    List<WorkOrderEntity> findByTenantIdAndStatus(String tenantId, String status);
}
