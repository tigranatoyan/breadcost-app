package com.breadcost.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierApiConfigRepository extends JpaRepository<SupplierApiConfigEntity, String> {

    Optional<SupplierApiConfigEntity> findByTenantIdAndSupplierId(String tenantId, String supplierId);

    List<SupplierApiConfigEntity> findByTenantId(String tenantId);

    List<SupplierApiConfigEntity> findByTenantIdAndEnabled(String tenantId, boolean enabled);
}
