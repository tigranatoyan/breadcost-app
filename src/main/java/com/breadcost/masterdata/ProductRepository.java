package com.breadcost.masterdata;

import com.breadcost.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    List<ProductEntity> findByTenantId(String tenantId);
    List<ProductEntity> findByTenantIdAndDepartmentId(String tenantId, String departmentId);
    List<ProductEntity> findByTenantIdAndStatus(String tenantId, Product.ProductStatus status);
    boolean existsByTenantIdAndNameAndDepartmentId(String tenantId, String name, String departmentId);
}
