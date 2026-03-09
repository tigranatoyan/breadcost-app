package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AI draft order — order parsed from an AI conversation (FR-12.1, FR-12.3).
 */
@Entity
@Table(name = "ai_draft_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiDraftOrderEntity {

    @Id
    private String draftId;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String tenantId;

    private String customerId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_CONFIRMATION";

    private String confirmedOrderId;

    @Column(nullable = false)
    @Builder.Default
    private boolean upsellOffered = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean upsellAccepted = false;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
