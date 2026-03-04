package com.breadcost.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTierEntity, String> {
    List<LoyaltyTierEntity> findByTenantIdOrderByMinPointsAsc(String tenantId);
    Optional<LoyaltyTierEntity> findByTenantIdAndName(String tenantId, String name);
}
