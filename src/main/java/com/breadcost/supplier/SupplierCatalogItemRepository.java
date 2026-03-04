package com.breadcost.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierCatalogItemRepository extends JpaRepository<SupplierCatalogItemEntity, String> {

    List<SupplierCatalogItemEntity> findByTenantIdAndSupplierId(String tenantId, String supplierId);

    List<SupplierCatalogItemEntity> findByTenantIdAndIngredientId(String tenantId, String ingredientId);
}
