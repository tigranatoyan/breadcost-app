package com.breadcost.multitenancy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantBrandingRepository extends JpaRepository<TenantBrandingEntity, String> {
}
