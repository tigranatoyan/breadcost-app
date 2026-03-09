package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiDraftOrderLineRepository extends JpaRepository<AiDraftOrderLineEntity, String> {

    List<AiDraftOrderLineEntity> findByDraftId(String draftId);

    List<AiDraftOrderLineEntity> findByDraftIdAndIsUpsell(String draftId, boolean isUpsell);
}
