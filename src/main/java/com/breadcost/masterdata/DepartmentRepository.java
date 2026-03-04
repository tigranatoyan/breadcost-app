package com.breadcost.masterdata;

import com.breadcost.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, String> {
    List<DepartmentEntity> findByTenantId(String tenantId);
    List<DepartmentEntity> findByTenantIdAndStatus(String tenantId, Department.DepartmentStatus status);
    boolean existsByTenantIdAndName(String tenantId, String name);
}
