package com.breadcost.masterdata;

import com.breadcost.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // -------------------------------------------------------------------------
    // Request / Response DTOs
    // -------------------------------------------------------------------------

    public record CreateDepartmentRequest(
            String tenantId,
            String name,
            int leadTimeHours,
            Department.WarehouseMode warehouseMode,
            String createdBy
    ) {}

    public record UpdateDepartmentRequest(
            String name,
            int leadTimeHours,
            Department.WarehouseMode warehouseMode,
            Department.DepartmentStatus status
    ) {}

    // -------------------------------------------------------------------------
    // Operations
    // -------------------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"departments", "deptsActive", "department"}, allEntries = true)
    public DepartmentEntity create(CreateDepartmentRequest req) {
        if (departmentRepository.existsByTenantIdAndName(req.tenantId(), req.name())) {
            throw new IllegalArgumentException(
                    "Department with name '" + req.name() + "' already exists for this tenant");
        }

        DepartmentEntity entity = DepartmentEntity.builder()
                .departmentId(UUID.randomUUID().toString())
                .tenantId(req.tenantId())
                .name(req.name())
                .leadTimeHours(req.leadTimeHours())
                .warehouseMode(req.warehouseMode())
                .status(Department.DepartmentStatus.ACTIVE)
                .createdBy(req.createdBy())
                .build();

        DepartmentEntity saved = departmentRepository.save(entity);
        log.info("Department created: id={}, name={}, tenant={}", saved.getDepartmentId(), saved.getName(), saved.getTenantId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"departments", "deptsActive", "department"}, allEntries = true)
    public DepartmentEntity update(String departmentId, UpdateDepartmentRequest req) {
        DepartmentEntity entity = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        entity.setName(req.name());
        entity.setLeadTimeHours(req.leadTimeHours());
        entity.setWarehouseMode(req.warehouseMode());
        entity.setStatus(req.status());

        DepartmentEntity saved = departmentRepository.save(entity);
        log.info("Department updated: id={}", departmentId);
        return saved;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "#tenantId")
    public List<DepartmentEntity> listByTenant(String tenantId) {
        return departmentRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "deptsActive", key = "#tenantId")
    public List<DepartmentEntity> listActive(String tenantId) {
        return departmentRepository.findByTenantIdAndStatus(tenantId, Department.DepartmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "department", key = "#departmentId")
    public DepartmentEntity getById(String departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));
    }
}
