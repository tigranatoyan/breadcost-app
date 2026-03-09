package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity, String> {

    List<AiMessageEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<AiMessageEntity> findByTenantIdAndConversationId(String tenantId, String conversationId);
}
