package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, String> {
    List<ItemEntity> findByTenantId(String tenantId);
    List<ItemEntity> findByTenantIdAndType(String tenantId, String type);
    List<ItemEntity> findByTenantIdAndActiveTrue(String tenantId);
    boolean existsByTenantIdAndNameIgnoreCase(String tenantId, String name);
}
