package com.breadcost.api;

import com.breadcost.masterdata.DepartmentEntity;
import com.breadcost.masterdata.DepartmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.breadcost.domain.Department;
import java.util.List;

/**
 * REST API for Department management
 * Departments are configurable from the UI — up to 10 per tenant (FR-11.1, FR-11.2)
 */
@RestController
@RequestMapping("/v1/departments")
@Slf4j
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    public record CreateDepartmentRequest(
            @NotBlank String tenantId,
            @NotBlank String name,
            int leadTimeHours,
            @NotNull Department.WarehouseMode warehouseMode
    ) {}

    public record UpdateDepartmentRequest(
            @NotBlank String name,
            int leadTimeHours,
            @NotNull Department.WarehouseMode warehouseMode,
            @NotNull Department.DepartmentStatus status
    ) {}

    /**
     * GET /v1/departments?tenantId=xxx
     * List all departments for a tenant
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'ProductionSupervisor')")
    public ResponseEntity<List<DepartmentEntity>> list(@RequestParam String tenantId) {
        return ResponseEntity.ok(departmentService.listByTenant(tenantId));
    }

    /**
     * GET /v1/departments/{departmentId}
     */
    @GetMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'ProductionSupervisor')")
    public ResponseEntity<DepartmentEntity> get(@PathVariable String departmentId) {
        return ResponseEntity.ok(departmentService.getById(departmentId));
    }

    /**
     * POST /v1/departments
     * Create a new department
     */
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<DepartmentEntity> create(
            @Valid @RequestBody CreateDepartmentRequest req) {
        DepartmentEntity created = departmentService.create(
                new DepartmentService.CreateDepartmentRequest(
                        req.tenantId(), req.name(), req.leadTimeHours(),
                        req.warehouseMode(), getPrincipalName()
                ));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /v1/departments/{departmentId}
     * Update department configuration (lead time, warehouse mode, etc.)
     */
    @PutMapping("/{departmentId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<DepartmentEntity> update(
            @PathVariable String departmentId,
            @Valid @RequestBody UpdateDepartmentRequest req) {
        DepartmentEntity updated = departmentService.update(departmentId,
                new DepartmentService.UpdateDepartmentRequest(
                        req.name(), req.leadTimeHours(), req.warehouseMode(), req.status()
                ));
        return ResponseEntity.ok(updated);
    }

    private String getPrincipalName() {
        // TODO: replace with actual Spring Security principal extraction
        return "system";
    }
}
