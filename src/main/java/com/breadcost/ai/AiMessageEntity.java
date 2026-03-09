package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AI message entity — BC-1801 (FR-12.2)
 * Individual message within an AI conversation, with parsed intent.
 */
@Entity
@Table(name = "ai_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiMessageEntity {

    @Id
    private String messageId;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String tenantId;

    /** INBOUND or OUTBOUND */
    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String messageType = "TEXT";

    @Column(length = 100)
    private String parsedIntent;

    private Double confidence;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
