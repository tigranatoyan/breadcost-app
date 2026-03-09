package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiPricingSuggestionRepository extends JpaRepository<AiPricingSuggestionEntity, String> {
    List<AiPricingSuggestionEntity> findByTenantId(String tenantId);
    List<AiPricingSuggestionEntity> findByTenantIdAndStatus(String tenantId, String status);
    List<AiPricingSuggestionEntity> findByTenantIdAndProductId(String tenantId, String productId);
}
