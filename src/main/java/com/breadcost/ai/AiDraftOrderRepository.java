package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiDraftOrderRepository extends JpaRepository<AiDraftOrderEntity, String> {

    List<AiDraftOrderEntity> findByConversationId(String conversationId);

    Optional<AiDraftOrderEntity> findByConversationIdAndStatus(String conversationId, String status);

    List<AiDraftOrderEntity> findByTenantId(String tenantId);
}
