package com.breadcost.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTierEntity, String> {
    Optional<SubscriptionTierEntity> findByLevel(SubscriptionTierEntity.TierLevel level);
}
