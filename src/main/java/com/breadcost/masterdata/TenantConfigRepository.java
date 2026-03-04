package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantConfigRepository extends JpaRepository<TenantConfigEntity, String> {
}
