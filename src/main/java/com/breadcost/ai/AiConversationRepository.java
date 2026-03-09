package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, String> {

    List<AiConversationEntity> findByTenantId(String tenantId);

    List<AiConversationEntity> findByTenantIdAndStatus(String tenantId, String status);

    Optional<AiConversationEntity> findByTenantIdAndCustomerPhoneAndStatus(
            String tenantId, String customerPhone, String status);

    List<AiConversationEntity> findByTenantIdAndCustomerPhone(String tenantId, String customerPhone);
}
