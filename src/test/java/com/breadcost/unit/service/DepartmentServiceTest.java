package com.breadcost.unit.service;

import com.breadcost.domain.Department;
import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository deptRepo;
    @InjectMocks private DepartmentService svc;

    @Test
    void create_success() {
        when(deptRepo.existsByTenantIdAndName("t1", "Bakery")).thenReturn(false);
        when(deptRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new DepartmentService.CreateDepartmentRequest(
                "t1", "Bakery", 8, Department.WarehouseMode.SHARED, "admin");

        var dept = svc.create(req);

        assertEquals("Bakery", dept.getName());
        assertEquals(8, dept.getLeadTimeHours());
        assertEquals(Department.DepartmentStatus.ACTIVE, dept.getStatus());
    }

    @Test
    void create_duplicateName_throws() {
        when(deptRepo.existsByTenantIdAndName("t1", "Bakery")).thenReturn(true);

        var req = new DepartmentService.CreateDepartmentRequest(
                "t1", "Bakery", 8, Department.WarehouseMode.SHARED, "admin");

        assertThrows(IllegalArgumentException.class, () -> svc.create(req));
    }

    @Test
    void update_setsFields() {
        var dept = DepartmentEntity.builder().departmentId("d1").tenantId("t1")
                .name("Old").leadTimeHours(4).build();
        when(deptRepo.findById("d1")).thenReturn(Optional.of(dept));
        when(deptRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new DepartmentService.UpdateDepartmentRequest(
                "New", 12, Department.WarehouseMode.ISOLATED, Department.DepartmentStatus.ACTIVE);

        var result = svc.update("d1", req);

        assertEquals("New", result.getName());
        assertEquals(12, result.getLeadTimeHours());
    }

    @Test
    void update_notFound_throws() {
        when(deptRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.update("bad", new DepartmentService.UpdateDepartmentRequest(
                        "X", 1, null, null)));
    }

    @Test
    void getById_notFound_throws() {
        when(deptRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> svc.getById("bad"));
    }
}
