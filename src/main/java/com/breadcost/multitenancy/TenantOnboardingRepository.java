package com.breadcost.multitenancy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantOnboardingRepository extends JpaRepository<TenantOnboardingEntity, String> {
    List<TenantOnboardingEntity> findByStatus(String status);
    Optional<TenantOnboardingEntity> findByTenantSlug(String tenantSlug);
    Optional<TenantOnboardingEntity> findByOwnerEmail(String email);
}
