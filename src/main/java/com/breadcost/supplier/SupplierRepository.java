package com.breadcost.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<SupplierEntity, String> {

    List<SupplierEntity> findByTenantId(String tenantId);

    Optional<SupplierEntity> findByTenantIdAndSupplierId(String tenantId, String supplierId);

    Optional<SupplierEntity> findByTenantIdAndName(String tenantId, String name);
}
