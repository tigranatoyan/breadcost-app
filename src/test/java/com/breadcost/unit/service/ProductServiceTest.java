package com.breadcost.unit.service;

import com.breadcost.domain.Product;
import com.breadcost.masterdata.*;
import com.breadcost.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepo;
    @Mock private DepartmentRepository deptRepo;
    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private ProductService svc;

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_success() {
        when(deptRepo.findById("d1")).thenReturn(Optional.of(
                DepartmentEntity.builder().departmentId("d1").tenantId("t1").build()));
        when(productRepo.existsByTenantIdAndNameAndDepartmentId("t1", "Bread", "d1")).thenReturn(false);
        when(subscriptionService.getMaxProducts("t1")).thenReturn(0); // unlimited
        when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ProductService.CreateProductRequest(
                "t1", "d1", "Bread", "desc", Product.SaleUnit.PIECE, "PCS",
                new BigDecimal("3.00"), 20.0, "admin");

        var product = svc.create(req);

        assertEquals("Bread", product.getName());
        assertEquals(Product.ProductStatus.ACTIVE, product.getStatus());
        assertEquals(new BigDecimal("3.00"), product.getPrice());
    }

    @Test
    void create_duplicateName_throws() {
        when(deptRepo.findById("d1")).thenReturn(Optional.of(
                DepartmentEntity.builder().departmentId("d1").tenantId("t1").build()));
        when(productRepo.existsByTenantIdAndNameAndDepartmentId("t1", "Bread", "d1")).thenReturn(true);

        var req = new ProductService.CreateProductRequest(
                "t1", "d1", "Bread", null, null, null, null, 0, null);

        assertThrows(IllegalArgumentException.class, () -> svc.create(req));
    }

    @Test
    void create_wrongTenantDepartment_throws() {
        when(deptRepo.findById("d1")).thenReturn(Optional.of(
                DepartmentEntity.builder().departmentId("d1").tenantId("t2").build()));

        var req = new ProductService.CreateProductRequest(
                "t1", "d1", "Bread", null, null, null, null, 0, null);

        assertThrows(IllegalArgumentException.class, () -> svc.create(req));
    }

    @Test
    void create_productLimitReached_throws() {
        when(deptRepo.findById("d1")).thenReturn(Optional.of(
                DepartmentEntity.builder().departmentId("d1").tenantId("t1").build()));
        when(productRepo.existsByTenantIdAndNameAndDepartmentId("t1", "New", "d1")).thenReturn(false);
        when(subscriptionService.getMaxProducts("t1")).thenReturn(1);
        when(productRepo.findByTenantId("t1")).thenReturn(List.of(
                ProductEntity.builder().productId("p1").build()));

        var req = new ProductService.CreateProductRequest(
                "t1", "d1", "New", null, null, null, null, 0, null);

        assertThrows(IllegalStateException.class, () -> svc.create(req));
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_setsFields() {
        var existing = ProductEntity.builder().productId("p1").tenantId("t1")
                .name("Old").price(BigDecimal.ONE).build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(existing));
        when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ProductService.UpdateProductRequest(
                "New", "desc", Product.SaleUnit.WEIGHT, "KG",
                new BigDecimal("5.00"), 10.0, Product.ProductStatus.ACTIVE);

        var result = svc.update("p1", req);

        assertEquals("New", result.getName());
        assertEquals(new BigDecimal("5.00"), result.getPrice());
    }

    @Test
    void update_notFound_throws() {
        when(productRepo.findById("bad")).thenReturn(Optional.empty());

        var req = new ProductService.UpdateProductRequest(null, null, null, null, null, 0, null);
        assertThrows(IllegalArgumentException.class, () -> svc.update("bad", req));
    }

    // ── setActiveRecipe ──────────────────────────────────────────────────────

    @Test
    void setActiveRecipe_setsRecipeId() {
        var product = ProductEntity.builder().productId("p1").build();
        when(productRepo.findById("p1")).thenReturn(Optional.of(product));
        when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.setActiveRecipe("p1", "r1");

        assertEquals("r1", result.getActiveRecipeId());
    }

    // ── getById ──────────────────────────────────────────────────────────────

    @Test
    void getById_notFound_throws() {
        when(productRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> svc.getById("bad"));
    }
}
