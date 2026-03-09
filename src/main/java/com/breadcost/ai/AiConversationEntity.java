package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AI conversation entity — BC-1802 (FR-12.1)
 * Represents a WhatsApp AI ordering session.
 */
@Entity
@Table(name = "ai_conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiConversationEntity {

    @Id
    private String conversationId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 100)
    private String customerPhone;

    private String customerId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String channel = "WHATSAPP";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Builder.Default
    private boolean escalated = false;

    @Column(length = 500)
    private String escalationReason;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
