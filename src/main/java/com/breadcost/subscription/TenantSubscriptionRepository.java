package com.breadcost.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscriptionEntity, String> {
    Optional<TenantSubscriptionEntity> findByTenantIdAndActive(String tenantId, boolean active);
    Optional<TenantSubscriptionEntity> findByTenantId(String tenantId);
}
